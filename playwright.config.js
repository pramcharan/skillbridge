
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
    testDir: './tests',
    timeout: 60000,
    expect: { timeout: 15000 },
    retries: 1,
    workers: process.env.CI ? 2 : 2,

    reporter: [
        ['list'],
        ['html', { outputFolder: 'playwright-report', open: 'on-failure' }],
    ],

    use: {
        baseURL: 'http://localhost:8080',
        headless: true,
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        trace: 'retain-on-failure',
        actionTimeout: 15000,
    },

    projects: [
        { name: 'Chromium', use: { ...devices['Desktop Chrome'] } },
        { name: 'Mobile Chrome', use: { ...devices['Pixel 5'] } },
    ],
});