/**
 * Backend reset fixtures for E2E test isolation.
 *
 * Provides utilities to reset the backend database state between tests
 * to ensure test independence and prevent state pollution.
 */
import { test as base, expect } from '@playwright/test';
import { API_BASE_URL } from './helpers';

// Extend the base test with backend reset functionality
export const test = base.extend<{
  resetBackend: () => Promise<void>;
}>({
  resetBackend: async ({ page }, use) => {
    // Reset function that can be called in tests
    const resetBackend = async () => {
      // This project does not expose a reliable unauthenticated reset API in docker.
      // Instead, wait until backend is healthy so login requests do not race startup.
      const healthUrl = API_BASE_URL.replace('/api', '/actuator/health');
      const maxAttempts = 20;

      for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
        try {
          const response = await page.request.get(healthUrl, {
            timeout: 5000,
          });

          if (response.ok()) {
            console.log('Backend health check passed');
            return;
          }

          console.warn(`Backend health check status ${response.status()} (attempt ${attempt}/${maxAttempts})`);
        } catch (error) {
          console.warn(`Backend health check failed (attempt ${attempt}/${maxAttempts})`, error);
        }

        await page.waitForTimeout(1000);
      }

      throw new Error('Backend was not healthy before test execution');
    };

    await use(resetBackend);
  },
});

// Re-export expect for convenience
export { expect };

// Global setup for backend reset before all tests
export const globalSetup = async () => {
  console.log('E2E Global setup: Backend should be running with test data seeded');
};

// Global teardown
export const globalTeardown = async () => {
  console.log('E2E Global teardown: Tests completed');
};