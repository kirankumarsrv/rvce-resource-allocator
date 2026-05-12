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
      try {
        // Call backend reset endpoint if available
        const response = await page.request.post(`${API_BASE_URL}/test/reset`, {
          headers: {
            'Content-Type': 'application/json',
          },
        });

        if (response.status() === 200) {
          console.log('Backend reset successful');
        } else {
          console.warn(`Backend reset returned status ${response.status()}`);
        }
      } catch (error) {
        console.warn('Backend reset failed, continuing with test:', error);
      }
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