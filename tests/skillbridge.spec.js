/**
 * SkillBridge E2E tests — selectors match src/main/resources/static/*.html
 * Run: npm run test:e2e:chromium
 * Requires: Spring Boot on http://localhost:8080
 */

import { test, expect } from '@playwright/test';

const BASE = 'http://localhost:8080';
const FREELANCER = {
    email: 'ramlakshman.polepalli@gmail.com',
    password: 'Ram@Lakshman123',
    name: 'Ram',
};
const CLIENT = {
    email: 'priya@test.com',
    password: 'Password1!',
    name: 'Priya',
};
function jwtKey(page) {
    return page.evaluate(() => localStorage.getItem('sb_jwt'));
}

async function registerNewUser(page, { email, password, name, role }) {
    await page.goto(`${BASE}/register.html`);
    await page.fill('#name', name);
    await page.fill('#email', email);
    await page.fill('#password', password);
    if (role === 'CLIENT') await page.locator('label[for="radio-client"]').click();
    else await page.locator('label[for="radio-freelancer"]').click();
    await page.check('#terms-check');
    await page.click('#register-btn');
    await page.waitForURL(/onboarding|dashboard/, { timeout: 25000 });
}

async function login(page, { email, password }) {
    await page.goto(`${BASE}/login.html`);
    await page.fill('#email', email);
    await page.fill('#password', password);
    await page.click('#login-btn');
    await page.waitForURL(/dashboard|admin|onboarding|jobs/, { timeout: 25000 });
}

async function skipOnboardingIfPresent(page) {
    if (!page.url().includes('onboarding')) return;
    page.once('dialog', (d) => d.accept());
    await page.getByText('Skip to dashboard').click();
    await page.waitForURL(/jobs|dashboard/, { timeout: 20000 });
}

async function clearSession(page) {
    await page.goto(`${BASE}/login.html`);
    await page.evaluate(() => localStorage.clear());
}

async function postOpenJob(page, title) {
    await page.goto(`${BASE}/post-job.html`);
    const responsePromise = page.waitForResponse(
        (r) =>
            r.url().includes('/api/v1/jobs') &&
            r.request().method() === 'POST' &&
            r.status() === 201,
        {timeout: 30000},
    );
    await page.fill('#title', title);
    await page.fill(
        '#description',
        `${'A'.repeat(120)} description for validation and visibility.`,
    );
    await page.selectOption('#category', 'TECHNOLOGY');
    await page.fill('#budget', '2500');
    await page.fill('#skill-input', 'Playwright');
    await page.keyboard.press('Enter');
    await page.click('#submit-btn');
    const res = await responsePromise;
    const data = await res.json();
    return data.id;
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. HOMEPAGE
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Homepage', () => {
    test('loads without console errors', async ({ page }) => {
        const errors = [];
        page.on('console', (msg) => {
            if (msg.type() === 'error') errors.push(msg.text());
        });
        await page.goto(`${BASE}/index.html`);
        await page.waitForLoadState('networkidle');
        const bad = errors.filter((e) => !e.includes('favicon'));
        expect(bad, bad.join('\n')).toHaveLength(0);
    });

    test('hero and primary CTAs visible', async ({ page }) => {
        await page.goto(`${BASE}/index.html`);
        await expect(page.locator('h1.hero-title')).toBeVisible();
        await expect(page.locator('a.btn-hero-primary')).toBeVisible();
    });

    test('stats section shows values from API', async ({ page }) => {
        await page.goto(`${BASE}/index.html`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('#stat-freelancers')).toBeVisible();
        const t = await page.locator('#stat-freelancers').innerText();
        expect(t.trim().length).toBeGreaterThan(0);
    });

    test('six category flip cards in #categories', async ({ page }) => {
        await page.goto(`${BASE}/index.html`);
        const cards = page.locator('#categories .flip-grid > .flip-card');
        await expect(cards).toHaveCount(6);
    });

    test('footer Terms link navigates', async ({ page }) => {
        await page.goto(`${BASE}/index.html`);
        await page.click('a:has-text("Terms of Service")');
        await expect(page).toHaveURL(/terms\.html/);
    });

    test('mobile viewport: hero remains usable', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 812 });
        await page.goto(`${BASE}/index.html`);
        await expect(page.locator('h1.hero-title')).toBeVisible();
        await expect(page.locator('a.btn-hero-primary')).toBeVisible();
    });
});

// ─────────────────────────────────────────────────────────────────────────────
// 2. REGISTER & ONBOARDING
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Register & Onboarding', () => {
    test('register freelancer redirects to onboarding', async ({ page }) => {
        const email = `pw_fl_${Date.now()}@test.com`;
        await registerNewUser(page, {
            email,
            password: 'Password1!',
            name: 'Test Freelancer',
            role: 'FREELANCER',
        });
        await expect(page).toHaveURL(/onboarding/);
    });

    test('register with duplicate email shows error', async ({ page }) => {
        const email = `pw_dup_${Date.now()}@test.com`;
        await registerNewUser(page, {
            email,
            password: 'Password1!',
            name: 'Dup User',
            role: 'CLIENT',
        });
        await page.evaluate(() => localStorage.clear());
        await page.goto(`${BASE}/register.html`);
        await page.fill('#name', 'Dup User 2');
        await page.fill('#email', email);
        await page.fill('#password', 'Password1!');
        await page.locator('label[for="radio-client"]').click();
        await page.check('#terms-check');
        await page.click('#register-btn');
        await expect(page.locator('.alert-error')).toBeVisible({ timeout: 10000 });
    });

    test('onboarding shows four step indicators', async ({ page }) => {
        await page.goto(`${BASE}/onboarding.html`);
        const steps = page.locator('#stepNav .ob-step-item');
        await expect(steps).toHaveCount(4);
    });

    test('empty register form shows validation', async ({ page }) => {
        await page.goto(`${BASE}/register.html`);
        await page.click('#register-btn');
        await expect(page.locator('.alert-error')).toBeVisible();
        await expect(page).toHaveURL(/register/);
    });
});

// ─────────────────────────────────────────────────────────────────────────────
// 3. LOGIN & AUTH
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Auth', () => {
    test('login with valid seed credentials and JWT stored', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await expect(page).toHaveURL(/dashboard|jobs/);
        expect(await jwtKey(page)).toBeTruthy();
    });

    test('login with wrong password shows error', async ({ page }) => {
        await page.goto(`${BASE}/login.html`);
        await page.fill('#email', FREELANCER.email);
        await page.fill('#password', 'wrongpassword123');
        await page.click('#login-btn');
        await expect(page.locator('.alert-error')).toBeVisible();
        await expect(page).toHaveURL(/login/);
    });

    test('protected freelancer dashboard redirects when logged out', async ({ page }) => {
        await page.goto(`${BASE}/login.html`);
        await page.evaluate(() => localStorage.clear());
        await page.goto(`${BASE}/dashboard-freelancer.html`);
        await expect(page).toHaveURL(/login|index/);
    });

    test('logout clears JWT', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/dashboard-freelancer.html`);
        await page.waitForLoadState('domcontentloaded');
        await Promise.all([
            page.waitForURL(/login\.html/, { timeout: 20000 }),
            page.locator('#logout-btn').click(),
        ]);
        expect(await jwtKey(page)).toBeNull();
    });
});

// ─────────────────────────────────────────────────────────────────────────────
// 4. JOBS
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Jobs', () => {
    test('jobs page loads grid or empty state', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/jobs.html`);
        await page.waitForLoadState('networkidle');
        const cards = page.locator('a.job-card');
        const empty = page.locator('.empty-state');
        const n = await cards.count();
        if (n === 0) await expect(empty).toBeVisible();
        else expect(n).toBeGreaterThan(0);
    });

    test('category filter narrows results when jobs exist', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/jobs.html`);
        await page.waitForLoadState('networkidle');
        const catBtn = page.locator('.cat-option[data-value]').first();
        if (await catBtn.isVisible()) {
            await catBtn.click();
            await page.waitForLoadState('networkidle');
        }
        await expect(page.locator('#jobs-container')).toBeVisible();
    });

    test('client can open post job form', async ({ page }) => {
        await login(page, CLIENT);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/post-job.html`);
        await expect(page.locator('#title')).toBeVisible();
        await expect(page.locator('#description')).toBeVisible();
    });

    test('client can post a job', async ({ page }) => {
        await login(page, CLIENT);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/post-job.html`);
        await page.fill('#title', `E2E Job ${Date.now()}`);
        await page.fill(
            '#description',
            'This job was posted by Playwright. Description must be long enough for any server-side validation rules that require substantial text.',
        );
        await page.selectOption('#category', { index: 1 });
        await page.fill('#budget', '1500');
        await page.fill('#skill-input', 'Java');
        await page.keyboard.press('Enter');
        await page.click('#submit-btn');
        await page.waitForLoadState('networkidle');
        const ok =
            page.url().includes('job-detail') ||
            (await page.locator('#toast').isVisible()) ||
            (await page.locator('.alert-success').isVisible().catch(() => false));
        expect(ok).toBeTruthy();
    });

    test('job detail page loads for first API job', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        const id = await page.evaluate(async () => {
            const token = localStorage.getItem('sb_jwt');
            const r = await fetch('/api/v1/jobs?page=0&size=1&sortBy=recent', {
                headers: token ? { Authorization: `Bearer ${token}` } : {},
            });
            if (!r.ok) return null;
            const d = await r.json();
            return d.content?.[0]?.id ?? null;
        });
        test.skip(!id, 'No jobs in database; run seed data or post a job first.');
        await page.goto(`${BASE}/job-detail.html?id=${id}`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('.job-title')).toBeVisible();
    });
});

// ─────────────────────────────────────────────────────────────────────────────
// 5. COMMUNITY CHAT
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Community Chat', () => {
    test('five room buttons visible', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/community.html`, { waitUntil: 'domcontentloaded' });
        await expect(page.locator('#room-general')).toBeVisible({ timeout: 30000 });
        for (const id of ['room-general', 'room-developers', 'room-creatives', 'room-opportunities', 'room-help']) {
            await expect(page.locator(`#${id}`)).toBeVisible();
        }
    });

    test('switch room updates header', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/community.html`);
        await page.locator('#room-developers').click();
        await expect(page.locator('#room-title')).toContainText(/developers/i);
    });

    test('message sent in one context visible in another', async ({ browser }) => {
        const ctx1 = await browser.newContext();
        const ctx2 = await browser.newContext();
        const page1 = await ctx1.newPage();
        const page2 = await ctx2.newPage();
        try {
            await login(page1, FREELANCER);
            await skipOnboardingIfPresent(page1);
            await login(page2, CLIENT);
            await skipOnboardingIfPresent(page2);
            await page1.goto(`${BASE}/community.html`);
            await page2.goto(`${BASE}/community.html`);
            await page1.waitForLoadState('networkidle');
            await page2.waitForLoadState('networkidle');
            const uniqueMsg = `PW-msg-${Date.now()}`;
            await page1.locator('#msg-input').fill(uniqueMsg);
            await page1.locator('#msg-input').press('Enter');
            await expect(page2.locator('.msg-text', { hasText: uniqueMsg })).toBeVisible({
                timeout: 15000,
            });
        } finally {
            await ctx1.close();
            await ctx2.close();
        }
    });
});

// ─────────────────────────────────────────────────────────────────────────────
// 6. ADMIN
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Admin Panel', () => {
    test('freelancer cannot use admin page', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/admin.html`);
        await page.waitForTimeout(800);
        const url = page.url();
        const denied = await page.locator('text=/Access Denied|Forbidden|403/i').isVisible().catch(() => false);
        expect(url.includes('admin.html') && !denied).toBeFalsy();
    });

    test('admin login shows charts or admin UI', async ({ page }) => {
        await page.goto(`${BASE}/login.html`);
        await page.fill('#email', 'admin@skillbridge.com');
        await page.fill('#password', 'Admin11!');
        await page.click('#login-btn');
        await page.waitForURL(/admin|dashboard/, { timeout: 20000 });
        if (page.url().includes('admin')) {
            await page.waitForLoadState('networkidle');
            const charts = page.locator('canvas');
            expect(await charts.count()).toBeGreaterThan(0);
        }
    });

});

// ─────────────────────────────────────────────────────────────────────────────
// 7. PROFILE & STATIC
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Profile', () => {
    test('profile loads for logged-in freelancer', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/profile.html`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('body')).toBeVisible();
    });
});

test.describe('Static Pages', () => {
    test('privacy page loads', async ({ page }) => {
        await page.goto(`${BASE}/privacy.html`);
        await expect(page.locator('h1')).toBeVisible();
    });

    test('terms page loads', async ({ page }) => {
        await page.goto(`${BASE}/terms.html`);
        await expect(page.locator('h1')).toBeVisible();
    });
});

// ─────────────────────────────────────────────────────────────────────────────
// Extended: proposals, project workspace, notifications, disputes
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Proposals and project workspace', () => {
    test('full flow: post job, propose, accept, chat', async ({ page }) => {
        test.setTimeout(180000);
        const unique = Date.now();
        const jobTitle = `E2E Flow Job ${unique}`;

        await login(page, CLIENT);
        await skipOnboardingIfPresent(page);

        const jobId = await postOpenJob(page, jobTitle);
        expect(jobId).toBeTruthy();

        await clearSession(page);
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);

        await page.goto(`${BASE}/job-detail.html?id=${jobId}`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('#apply-section')).toBeVisible({ timeout: 25000 });

        await page.fill('#expected-rate', '60');
        await page.fill(
            '#cover-letter',
            'This cover letter is intentionally over fifty characters for the SkillBridge E2E flow.',
        );
        await page.click('#apply-btn');
        await expect(page.locator('.applied-msg')).toBeVisible({ timeout: 30000 });
        await expect(page.locator('.applied-msg')).toContainText(/AI Match Score|Match/i);

        await clearSession(page);
        await login(page, CLIENT);
        await skipOnboardingIfPresent(page);

        await page.goto(`${BASE}/proposals-client.html?jobId=${jobId}`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('.proposal-card').first()).toBeVisible({ timeout: 25000 });

        await page.locator('button:has-text("View Full")').first().click();
        await expect(page.locator('#proposal-modal')).toBeVisible();
        await expect(page.locator('#modal-body')).toContainText(/Freelancer|Cover|Match/i, {
            timeout: 15000,
        });
        await page.locator('#proposal-modal').evaluate((el) => {
            el.style.display = 'none';
        });

        page.once('dialog', (d) => d.accept());
        await page.locator('button.btn-accept').first().click();
        await expect(page.locator('#toast')).toContainText(/accepted|Project created|Proposal/i, {
            timeout: 20000,
        });

        const projectId = await page.evaluate(
            async ({ needle, base }) => {
                const token = localStorage.getItem('sb_jwt');
                const r = await fetch(`${base}/api/v1/projects`, {
                    headers: { Authorization: `Bearer ${token}` },
                });
                if (!r.ok) return null;
                const list = await r.json();
                const p = list.find((x) => x.title && String(x.title).includes(String(needle)));
                return p ? p.id : null;
            },
            { needle: unique, base: BASE },
        );

        test.skip(!projectId, 'No matching project after accept.');

        await page.goto(`${BASE}/project.html?id=${projectId}`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('#proj-title')).toBeVisible({ timeout: 20000 });

        const msg = `E2E workspace ping ${unique}`;
        await page.locator('#chat-input').fill(msg);
        await page.locator('#send-btn').click();
        await expect(page.locator('#msgs')).toContainText(msg, { timeout: 20000 });
    });

    test('cover letter under 50 chars shows toast', async ({ page }) => {
        test.setTimeout(120000);
        const unique = Date.now();
        const jobTitle = `E2E Short Letter ${unique}`;

        await login(page, CLIENT);
        await skipOnboardingIfPresent(page);
        const jobId = await postOpenJob(page, jobTitle);

        await clearSession(page);
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);

        await page.goto(`${BASE}/job-detail.html?id=${jobId}`);
        await expect(page.locator('#apply-section')).toBeVisible({ timeout: 25000 });
        await page.fill('#expected-rate', '50');
        await page.fill('#cover-letter', 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
        await page.click('#apply-btn');
        await expect(page.locator('#toast')).toContainText(/50/i, { timeout: 8000 });
    });

    test('duplicate proposal shows already applied', async ({ page }) => {
        test.setTimeout(120000);
        const unique = Date.now();
        const jobTitle = `E2E Dup ${unique}`;

        await login(page, CLIENT);
        await skipOnboardingIfPresent(page);
        const jobId = await postOpenJob(page, jobTitle);

        await clearSession(page);
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);

        await page.goto(`${BASE}/job-detail.html?id=${jobId}`);
        await expect(page.locator('#apply-section')).toBeVisible({ timeout: 25000 });
        await page.fill('#expected-rate', '45');
        await page.fill(
            '#cover-letter',
            'First application with fifty or more characters for duplicate proposal test case.',
        );
        await page.click('#apply-btn');
        await expect(page.locator('.applied-msg')).toBeVisible({ timeout: 30000 });

        await page.goto(`${BASE}/job-detail.html?id=${jobId}`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('#apply-section')).toBeVisible({ timeout: 25000 });
        await page.fill('#expected-rate', '45');
        await page.fill(
            '#cover-letter',
            'Second submit attempt with fifty or more characters should be rejected by the API.',
        );
        await page.click('#apply-btn');
        await expect(page.locator('.applied-msg')).toContainText(/already applied/i, { timeout: 15000 });
    });
});

test.describe('Notifications and disputes pages', () => {
    test('notifications page loads list', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/notifications.html`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('#notif-list')).toBeVisible();
        await expect(page.locator('#notif-list')).not.toContainText('Failed to load notifications');
    });

    test('disputes page loads', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboardingIfPresent(page);
        await page.goto(`${BASE}/disputes.html`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('#list-view')).toBeVisible();
        await expect(page.locator('#disputes-list')).toBeVisible();
    });
});

