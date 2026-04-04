// ─────────────────────────────────────────────────────────────
// FILE 1: playwright.config.js
// Place at: your project root (same folder as pom.xml)
// ─────────────────────────────────────────────────────────────

import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
    testDir: './tests',
    timeout: 30000,
    retries: 1,               // retry flaky tests once
    workers: 2,               // 2 parallel workers (safe for WebSocket tests)

    reporter: [
        ['list'],             // console output
        ['html', {
            outputFolder: 'playwright-report',
            open: 'on-failure'   // auto-open report if tests fail
        }],
    ],

    use: {
        baseURL: 'http://localhost:8080',
        headless: true,
        screenshot: 'only-on-failure',  // saves screenshot when test fails
        video: 'retain-on-failure',     // saves video when test fails
        trace: 'retain-on-failure',     // saves trace for debugging
        actionTimeout: 10000,
    },

    projects: [
        {
            name: 'Chromium',
            use: { ...devices['Desktop Chrome'] },
        },
        {
            name: 'Mobile Chrome',
            use: { ...devices['Pixel 5'] },
        },
    ],
});


// ─────────────────────────────────────────────────────────────
// FILE 2: package.json
// Place at: your project root (same folder as pom.xml)
// ─────────────────────────────────────────────────────────────

/*
{
  "name": "skillbridge-tests",
  "version": "1.0.0",
  "description": "SkillBridge automated test suite",
  "scripts": {
    "test:ui":       "playwright test",
    "test:ui:headed": "playwright test --headed",
    "test:ui:debug":  "playwright test --ui",
    "test:api":      "newman run postman/SkillBridge.postman_collection.json --reporters cli,html --reporter-html-export reports/api-report.html",
    "test:load":     "k6 run load-tests/load-test.js",
    "test:all":      "npm run test:api && npm run test:ui && npm run test:load",
    "report":        "playwright show-report"
  },
  "devDependencies": {
    "@playwright/test": "^1.44.0"
  }
}
*/
