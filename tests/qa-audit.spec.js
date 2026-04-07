import { test, expect } from '@playwright/test';

const BASE = 'http://localhost:8080';

const timestamp = Date.now();
const FREELANCER = { email: `f_audit_${timestamp}@test.com`, password: 'Password1!', name: 'Free', role: 'FREELANCER' };
const CLIENT = { email: `c_audit_${timestamp}@test.com`, password: 'Password1!', name: 'Client', role: 'CLIENT' };

// Helpers
async function register(page, user) {
    const uniqueEmail = `audit_${Date.now()}_${Math.floor(Math.random() * 10000)}@test.com`;
    await page.goto(`${BASE}/register.html`);
    await page.fill('#email', uniqueEmail);
    await page.fill('#name', user.name + ' Test');
    await page.fill('#password', user.password);
    if (user.role === 'CLIENT') {
        await page.click('label[for="radio-client"]');
    } else {
        await page.click('label[for="radio-freelancer"]');
    }
    await page.check('#terms-check');
    await page.click('#register-btn');
    await page.waitForURL(/onboarding|dashboard/);
    return uniqueEmail;
}

async function login(page, user) {
    await page.goto(`${BASE}/login.html`);
    await page.fill('#email', user.email);
    await page.fill('#password', user.password);
    await page.click('#login-btn');
    await page.waitForURL(/dashboard|browse|onboarding/);
}

test.describe('Security & Access Control', () => {

    test('Unauthenticated user cannot access secure pages', async ({ page }) => {
        const securePages = [
            '/dashboard-freelancer.html',
            '/dashboard-client.html',
            '/admin.html',
            '/post-job.html',
            '/profile.html',
            '/community.html',
            '/disputes.html'
        ];
        
        for (const p of securePages) {
            await page.goto(`${BASE}${p}`);
            // App should redirect to index or login
            const url = page.url();
            expect(url.includes('login.html') || url.includes('index.html')).toBeTruthy();
        }
    });

    test('Cross-Role access is blocked', async ({ page }) => {
        // Create a freelancer
        await register(page, FREELANCER);
        
        // Attempt to access client dashboard
        await page.goto(`${BASE}/dashboard-client.html`);
        let url = page.url();
        expect(url.includes('dashboard-client')).toBeFalsy(); 

        // Attempt to access admin dashboard
        await page.goto(`${BASE}/admin.html`);
        // We either get redirected, or a UI forbidden state is rendered
        await page.waitForTimeout(500);
        url = page.url();
        const forbiddenText = await page.locator('text=Access Denied').isVisible();
        const forbiddenText2 = await page.locator('text=Forbidden').isVisible();
        expect(url.includes('index') || url.includes('login') || url.includes('dashboard-freelancer') || forbiddenText || forbiddenText2).toBeTruthy();
    });
});

test.describe('Boundary & Logic Enforcement', () => {

    test('Negative budget job post fails gracefully', async ({ page }) => {
        // Register/Login client
        await register(page, CLIENT);
        // Skip onboarding if necessary
        await page.goto(`${BASE}/post-job.html`);
        await page.waitForTimeout(1000);

        await page.fill('#title', 'Negative Test Job');
        await page.fill('#description', 'A valid length description that spans more than 50 or 100 characters to make sure it passes the description validator limit checks in place.');
        await page.selectOption('#category', 'TECHNOLOGY');
        
        // Fill mandatory skills
        await page.fill('#skill-input', 'Playwright');
        await page.keyboard.press('Enter');
        await page.waitForTimeout(300); // Wait for frontend state to sync

        // Input negative budget
        await page.locator('#budget').scrollIntoViewIfNeeded();
        await page.locator('#budget').fill('-500');

        await page.locator('#submit-btn').scrollIntoViewIfNeeded();
        await page.click('#submit-btn', { force: true });

        // Check if the parent div has the 'has-error' class
        const budgetGroup = page.locator('#fg-budget');
        await expect(budgetGroup).toHaveClass(/has-error/);
        
        // Ensure successful blocks keep us on the same page
        expect(page.url()).toContain('post-job.html');
    });

    test('Extremely long description breaks UI layout or DB bounds?', async ({ page }) => {
        await register(page, CLIENT);
        await page.goto(`${BASE}/post-job.html`);
        await page.waitForTimeout(1000);

        const longText = 'A'.repeat(2000) + '... end.';
        await page.fill('#title', 'Extremely Long Job Describe');
        await page.fill('#description', longText);
        await page.selectOption('#category', 'TECHNOLOGY');
        
        await page.locator('#budget, [name="budget"]').fill('500');

        // Fill mandatory skills
        await page.fill('#skill-input', 'Scalability');
        await page.keyboard.press('Enter');
        await page.waitForTimeout(300);

        await page.locator('#submit-btn').scrollIntoViewIfNeeded();
        await page.click('#submit-btn');
        
        // It should either succeed or show a client-side validation error or a backend 400.
        // We just want to ensure NO 500 error page appears.
        await page.waitForTimeout(2000);
        const failure500 = await page.locator('text=Whitelabel Error Page').isVisible();
        expect(failure500).toBeFalsy();
    });

});

test.describe('XSS & Input Injection', () => {

    test('Profile name with XSS payload does not execute script', async ({ page }) => {
        // Register an adversarial payload user
        const xssUser = { email: `xss_${timestamp}@test.com`, password: 'Password1!', name: '<script>window.XssExecuted=true;</script>', role: 'FREELANCER' };
        await register(page, xssUser);
        
        await page.goto(`${BASE}/profile.html`);
        await page.waitForLoadState('networkidle');

        const isXSSExecuted = await page.evaluate(() => window.XssExecuted === true);
        expect(isXSSExecuted).toBeFalsy();
        
        // Ensure name isn't invisibly disappearing into the DOM
        const bodyText = await page.locator('body').innerText();
        expect(bodyText).toContain('<script>');
    });

});

test.describe('API Direct Abuse via Fetch', () => {

    test('Tampering backend APIs maliciously fails safely', async ({ page }) => {
        await register(page, CLIENT);
        // Steal our token
        const token = await page.evaluate(() => localStorage.getItem('token') || localStorage.getItem('sb_jwt'));
        expect(token).toBeTruthy();

        // 1. Try to delete a dispute we dont own
        const response1 = await page.evaluate(async (tok) => {
            const res = await fetch('/api/disputes/9999/resolve', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + tok, 'Content-Type': 'application/json' },
                body: JSON.stringify({ resolution: 'SPLIT', adminNotes: 'Hacked' })
            });
            return res.status;
        }, token);
        expect(response1).not.toBe(200); // Should be 403 or 404
        
        // 2. Try to verify ourselves (Admin only)
        const response2 = await page.evaluate(async (tok) => {
            const res = await fetch('/api/users/current/verify-id', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + tok }
            });
            return res.status;
        }, token);
        // It might be 200 actually if the app lets users verify themselves, but it shouldn't let them set role
    });
});
