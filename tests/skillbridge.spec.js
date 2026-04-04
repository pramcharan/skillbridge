// SkillBridge — Playwright E2E Test Suite
// File: tests/skillbridge.spec.js
//
// Setup:
//   npm init -y
//   npm install --save-dev @playwright/test
//   npx playwright install chromium
//
// Run:
//   npx playwright test                  (headless)
//   npx playwright test --headed         (watch it run)
//   npx playwright test --ui             (visual debugger)
//   npx playwright show-report           (HTML report)

import { test, expect, chromium } from '@playwright/test';

const BASE = 'http://localhost:8080';
const FREELANCER = { email: 'ram@playwright.com', password: 'Password1!', name: 'Ram' };
const CLIENT     = { email: 'priya@playwright.com', password: 'Password1!', name: 'Priya' };

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function register(page, user, role) {
    await page.goto(`${BASE}/register.html`);
    await page.fill('#email', user.email);
    await page.fill('#firstName', user.name);
    await page.fill('#lastName', 'Test');
    await page.fill('#password', user.password);
    await page.selectOption('#role', role);
    await page.click('#registerBtn');
    await page.waitForURL(/onboarding|dashboard/);
}

async function login(page, user) {
    await page.goto(`${BASE}/login.html`);
    await page.fill('#email', user.email);
    await page.fill('#password', user.password);
    await page.click('#loginBtn');
    await page.waitForURL(/dashboard|browse|onboarding/);
}

async function skipOnboarding(page) {
    if (page.url().includes('onboarding')) {
        const skip = page.locator('a:has-text("Skip"), button:has-text("Skip")');
        if (await skip.isVisible()) await skip.click();
        await page.waitForURL(/dashboard|browse/);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. HOMEPAGE
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Homepage', () => {

    test('loads without console errors', async ({ page }) => {
        const errors = [];
        page.on('console', msg => { if (msg.type() === 'error') errors.push(msg.text()); });

        await page.goto(`${BASE}/index.html`);
        await page.waitForLoadState('networkidle');

        expect(errors.filter(e => !e.includes('favicon'))).toHaveLength(0);
    });

    test('hero section and CTA buttons visible', async ({ page }) => {
        await page.goto(`${BASE}/index.html`);
        await expect(page.locator('h1, .hero-title, [class*="hero"] h1')).toBeVisible();
        await expect(page.locator('a:has-text("Browse Jobs"), button:has-text("Browse Jobs")')).toBeVisible();
    });

    test('stats bar loads real numbers', async ({ page }) => {
        await page.goto(`${BASE}/index.html`);
        await page.waitForLoadState('networkidle');
        // Stats should not be 0 or empty after API call
        const stat = page.locator('.stat-number, [class*="stat"] span').first();
        await expect(stat).not.toHaveText('0');
    });

    test('flip cards render all 6 categories', async ({ page }) => {
        await page.goto(`${BASE}/index.html`);
        const cards = page.locator('.flip-card, [class*="flip-card"]');
        await expect(cards).toHaveCount(6);
    });

    test('footer links work', async ({ page }) => {
        await page.goto(`${BASE}/index.html`);
        await page.click('a:has-text("Terms")');
        await expect(page).toHaveURL(/terms/);
    });

    test('mobile hamburger menu works', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 812 });
        await page.goto(`${BASE}/index.html`);

        const hamburger = page.locator('.hamburger, #hamburgerBtn, [class*="hamburger"]');
        await expect(hamburger).toBeVisible();
        await hamburger.click();

        const drawer = page.locator('.nav-drawer, .mobile-nav, [class*="nav-drawer"]');
        await expect(drawer).toBeVisible();
    });

});

// ─────────────────────────────────────────────────────────────────────────────
// 2. REGISTER & ONBOARDING
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Register & Onboarding', () => {

    test('register freelancer redirects to onboarding', async ({ page }) => {
        await page.goto(`${BASE}/register.html`);
        await page.fill('#email', `ram_${Date.now()}@test.com`);
        await page.fill('#firstName', 'Ram');
        await page.fill('#lastName', 'Kumar');
        await page.fill('#password', 'Password1!');
        await page.selectOption('#role', 'FREELANCER');
        await page.click('#registerBtn');
        await expect(page).toHaveURL(/onboarding/);
    });

    test('register with duplicate email shows error', async ({ page }) => {
        // First registration
        const email = `dup_${Date.now()}@test.com`;
        await page.goto(`${BASE}/register.html`);
        await page.fill('#email', email);
        await page.fill('#firstName', 'Test');
        await page.fill('#lastName', 'User');
        await page.fill('#password', 'Password1!');
        await page.selectOption('#role', 'CLIENT');
        await page.click('#registerBtn');
        await page.waitForURL(/onboarding|dashboard/);

        // Second registration same email
        await page.goto(`${BASE}/register.html`);
        await page.fill('#email', email);
        await page.fill('#firstName', 'Test');
        await page.fill('#lastName', 'User');
        await page.fill('#password', 'Password1!');
        await page.selectOption('#role', 'CLIENT');
        await page.click('#registerBtn');

        await expect(page.locator('.error-message, .alert-error, [class*="error"]')).toBeVisible();
    });

    test('onboarding wizard has 4 steps', async ({ page }) => {
        await page.goto(`${BASE}/onboarding.html`);
        // Step indicators
        const steps = page.locator('.step, [class*="step-indicator"], .wizard-step');
        await expect(steps).toHaveCount(4);
    });

    test('empty form validation prevents submission', async ({ page }) => {
        await page.goto(`${BASE}/register.html`);
        await page.click('#registerBtn');
        // Should not navigate away
        await expect(page).toHaveURL(/register/);
    });

});

// ─────────────────────────────────────────────────────────────────────────────
// 3. LOGIN & AUTH
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Auth', () => {

    test('login with valid credentials succeeds', async ({ page }) => {
        // Register first to ensure user exists
        await register(page, FREELANCER, 'FREELANCER');
        await skipOnboarding(page);
        // Check we're on dashboard
        await expect(page).toHaveURL(/dashboard/);
        // Check token in localStorage
        const token = await page.evaluate(() => localStorage.getItem('token'));
        expect(token).toBeTruthy();
    });

    test('login with wrong password shows error', async ({ page }) => {
        await page.goto(`${BASE}/login.html`);
        await page.fill('#email', FREELANCER.email);
        await page.fill('#password', 'wrongpassword123');
        await page.click('#loginBtn');
        await expect(page.locator('.error-message, .alert-error, [class*="error"]')).toBeVisible();
        await expect(page).toHaveURL(/login/);
    });

    test('accessing protected page without token redirects to login', async ({ page }) => {
        // Clear any stored token
        await page.goto(`${BASE}/login.html`);
        await page.evaluate(() => localStorage.clear());

        await page.goto(`${BASE}/dashboard-freelancer.html`);
        await expect(page).toHaveURL(/login/);
    });

    test('logout clears token and redirects', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);

        // Click logout
        const logout = page.locator('a:has-text("Logout"), button:has-text("Logout"), #logoutBtn');
        await logout.click();

        // Token should be cleared
        const token = await page.evaluate(() => localStorage.getItem('token'));
        expect(token).toBeNull();
        await expect(page).toHaveURL(/login|index/);
    });

});

// ─────────────────────────────────────────────────────────────────────────────
// 4. JOBS
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Jobs', () => {

    test('browse jobs page loads with job cards', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);
        await page.goto(`${BASE}/browse-jobs.html`);
        await page.waitForLoadState('networkidle');

        const cards = page.locator('.job-card, [class*="job-card"]');
        // Should have at least some results or an empty state
        const count = await cards.count();
        const empty = page.locator('.empty-state, [class*="empty"], :has-text("No jobs found")');
        if (count === 0) {
            await expect(empty).toBeVisible();
        } else {
            expect(count).toBeGreaterThan(0);
        }
    });

    test('category filter updates results', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);
        await page.goto(`${BASE}/browse-jobs.html`);
        await page.waitForLoadState('networkidle');

        await page.selectOption('#categoryFilter, [name="category"]', 'TECHNOLOGY');
        await page.waitForLoadState('networkidle');
        await page.waitForTimeout(500);

        // All visible cards should be TECHNOLOGY (or empty state)
        const techBadges = page.locator('.job-card [class*="category"]:has-text("Technology"), .job-card [class*="badge"]:has-text("Technology")');
        const cards = page.locator('.job-card');
        const cardCount = await cards.count();

        if (cardCount > 0) {
            expect(await techBadges.count()).toEqual(cardCount);
        }
    });

    test('post job form visible for client', async ({ page }) => {
        await register(page, CLIENT, 'CLIENT');
        await skipOnboarding(page);
        await page.goto(`${BASE}/post-job.html`);
        await expect(page.locator('#jobTitle, [name="title"]')).toBeVisible();
        await expect(page.locator('#jobDescription, [name="description"]')).toBeVisible();
    });

    test('post job successfully', async ({ page }) => {
        await login(page, CLIENT);
        await skipOnboarding(page);
        await page.goto(`${BASE}/post-job.html`);

        await page.fill('#jobTitle, [name="title"]', 'Automated Test Job ' + Date.now());
        await page.fill('#jobDescription, [name="description"]', 'This job was posted by Playwright automated testing. Minimum 100 characters needed for description validation.');
        await page.selectOption('#jobCategory, [name="category"]', 'TECHNOLOGY');
        await page.fill('#budgetMin, [name="budgetMin"]', '500');
        await page.fill('#budgetMax, [name="budgetMax"]', '2000');

        // Add a skill tag
        const skillInput = page.locator('#skillInput, [placeholder*="skill"], [placeholder*="Skill"]');
        if (await skillInput.isVisible()) {
            await skillInput.fill('Java');
            await skillInput.press('Enter');
        }

        await page.click('#postJobBtn, button[type="submit"]');
        await page.waitForLoadState('networkidle');

        // Should either redirect to job detail or show success
        const success = page.locator('.success-message, [class*="success"], .alert-success');
        const isRedirected = page.url().includes('job-detail');
        expect(await success.isVisible() || isRedirected).toBeTruthy();
    });

    test('job detail page shows full info', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);
        await page.goto(`${BASE}/browse-jobs.html`);
        await page.waitForLoadState('networkidle');

        const firstJob = page.locator('.job-card a, .job-card').first();
        if (await firstJob.isVisible()) {
            await firstJob.click();
            await expect(page).toHaveURL(/job-detail/);
            await expect(page.locator('h1, .job-title')).toBeVisible();
        }
    });

});

// ─────────────────────────────────────────────────────────────────────────────
// 5. COMMUNITY CHAT
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Community Chat', () => {

    test('community page loads with 5 rooms', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);
        await page.goto(`${BASE}/community.html`);
        await page.waitForLoadState('networkidle');

        // Check rooms exist
        for (const room of ['general', 'developers', 'creatives', 'opportunities', 'help']) {
            const roomLink = page.locator(`[data-room="${room}"], a:has-text("${room}"), .room-item:has-text("${room}")`);
            await expect(roomLink.first()).toBeVisible();
        }
    });

    test('can switch between rooms', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);
        await page.goto(`${BASE}/community.html`);
        await page.waitForLoadState('networkidle');

        const devRoom = page.locator('[data-room="developers"], a:has-text("Developers"), .room-item:has-text("developers")').first();
        await devRoom.click();
        await page.waitForTimeout(500);

        // Chat area should be visible
        await expect(page.locator('#chatMessages, .chat-messages, [class*="chat-message"]')).toBeVisible();
    });

    test('real-time chat - message appears in second browser', async ({ browser }) => {
        // Two separate browser contexts = two different users
        const ctx1 = await browser.newContext();
        const ctx2 = await browser.newContext();
        const page1 = await ctx1.newPage();
        const page2 = await ctx2.newPage();

        try {
            // Login both users
            await login(page1, FREELANCER);
            await skipOnboarding(page1);
            await login(page2, CLIENT);
            await skipOnboarding(page2);

            // Both go to community general room
            await page1.goto(`${BASE}/community.html`);
            await page2.goto(`${BASE}/community.html`);
            await page1.waitForLoadState('networkidle');
            await page2.waitForLoadState('networkidle');

            // Page1 sends a unique message
            const uniqueMsg = `Playwright test ${Date.now()}`;
            const input = page1.locator('#messageInput, [placeholder*="message"], .message-input').first();
            await input.fill(uniqueMsg);
            await input.press('Enter');

            // Page2 should see it within 3 seconds (WebSocket)
            await expect(
                page2.locator(`.chat-message:has-text("${uniqueMsg}"), .message:has-text("${uniqueMsg}")`)
            ).toBeVisible({ timeout: 5000 });

        } finally {
            await ctx1.close();
            await ctx2.close();
        }
    });

    test('emoji reaction updates in real time', async ({ browser }) => {
        const ctx1 = await browser.newContext();
        const ctx2 = await browser.newContext();
        const page1 = await ctx1.newPage();
        const page2 = await ctx2.newPage();

        try {
            await login(page1, FREELANCER);
            await skipOnboarding(page1);
            await login(page2, CLIENT);
            await skipOnboarding(page2);

            await page1.goto(`${BASE}/community.html`);
            await page2.goto(`${BASE}/community.html`);
            await page1.waitForLoadState('networkidle');
            await page2.waitForLoadState('networkidle');

            // Send a message first
            const uniqueMsg = `React to this ${Date.now()}`;
            await page1.locator('#messageInput, .message-input').first().fill(uniqueMsg);
            await page1.locator('#messageInput, .message-input').first().press('Enter');
            await page2.waitForTimeout(1000);

            // Hover to get reaction picker
            const msg = page2.locator(`.chat-message:has-text("${uniqueMsg}")`).first();
            if (await msg.isVisible()) {
                await msg.hover();
                const reactionBtn = page2.locator('.reaction-btn, [class*="emoji"], .emoji-picker-trigger').first();
                if (await reactionBtn.isVisible()) {
                    await reactionBtn.click();
                    const thumbsUp = page2.locator('[data-emoji="👍"], button:has-text("👍")').first();
                    if (await thumbsUp.isVisible()) {
                        await thumbsUp.click();
                        // Check reaction count appears
                        await expect(
                            page1.locator('.reaction-count, [class*="reaction"]:has-text("1")')
                        ).toBeVisible({ timeout: 3000 });
                    }
                }
            }
        } finally {
            await ctx1.close();
            await ctx2.close();
        }
    });

});

// ─────────────────────────────────────────────────────────────────────────────
// 6. ADMIN PANEL
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Admin Panel', () => {

    test('non-admin cannot access admin page', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);
        await page.goto(`${BASE}/admin.html`);
        // Should redirect away or show forbidden
        await page.waitForTimeout(1000);
        const isForbidden = page.url().includes('login') ||
                            page.url().includes('index') ||
                            await page.locator(':has-text("Forbidden"), :has-text("Access Denied")').isVisible();
        expect(isForbidden).toBeTruthy();
    });

    test('admin panel charts render', async ({ page }) => {
        // Login as admin (must exist in DB)
        await page.goto(`${BASE}/login.html`);
        await page.fill('#email', 'admin@skillbridge.com');
        await page.fill('#password', 'Admin1!');
        await page.click('#loginBtn');
        await page.waitForURL(/admin|dashboard/);

        if (page.url().includes('admin')) {
            await page.waitForLoadState('networkidle');
            // Chart canvases should exist
            const charts = page.locator('canvas');
            expect(await charts.count()).toBeGreaterThan(0);
        }
    });

    test('admin tabs are all visible', async ({ page }) => {
        await page.goto(`${BASE}/login.html`);
        await page.fill('#email', 'admin@skillbridge.com');
        await page.fill('#password', 'Admin1!');
        await page.click('#loginBtn');
        await page.waitForURL(/admin|dashboard/);

        if (page.url().includes('admin')) {
            for (const tab of ['Users', 'Jobs', 'Disputes']) {
                await expect(
                    page.locator(`button:has-text("${tab}"), a:has-text("${tab}"), [data-tab]:has-text("${tab}")`)
                ).toBeVisible();
            }
        }
    });

});

// ─────────────────────────────────────────────────────────────────────────────
// 7. PROFILE
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Profile', () => {

    test('profile page loads for logged in user', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);
        await page.goto(`${BASE}/profile.html`);
        await page.waitForLoadState('networkidle');

        await expect(page.locator('.profile-name, h1, [class*="profile"]')).toBeVisible();
    });

    test('portfolio tab shows add button for freelancer', async ({ page }) => {
        await login(page, FREELANCER);
        await skipOnboarding(page);
        await page.goto(`${BASE}/profile.html`);

        const portfolioTab = page.locator('button:has-text("Portfolio"), a:has-text("Portfolio")');
        if (await portfolioTab.isVisible()) {
            await portfolioTab.click();
            await expect(
                page.locator('button:has-text("Add"), button:has-text("New"), [class*="add-portfolio"]')
            ).toBeVisible();
        }
    });

});

// ─────────────────────────────────────────────────────────────────────────────
// 8. PRIVACY & TERMS
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Static Pages', () => {

    test('privacy page loads', async ({ page }) => {
        await page.goto(`${BASE}/privacy.html`);
        await expect(page.locator('h1')).toBeVisible();
        await expect(page).toHaveTitle(/Privacy|SkillBridge/);
    });

    test('terms page loads', async ({ page }) => {
        await page.goto(`${BASE}/terms.html`);
        await expect(page.locator('h1')).toBeVisible();
        await expect(page).toHaveTitle(/Terms|SkillBridge/);
    });

});
