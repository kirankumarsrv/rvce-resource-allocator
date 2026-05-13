import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: 'e2e',
  timeout: 60_000,
  expect: { timeout: 5000 },
  fullyParallel: false,
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  use: {
    headless: false,
    viewport: { width: 1280, height: 800 },
    actionTimeout: 0,
    launchOptions: { slowMo: 400 },
  },
});
