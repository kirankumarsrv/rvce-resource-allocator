/**
 * Core authentication end-to-end tests for RVCE Resource Allocator.
 *
 * Covers valid and invalid login flows and ensures logout works correctly.
 */
import { test, expect } from './fixtures';
import { login, debugAction, enableBrowserLogs, attachPageSnapshot } from './helpers';

test.describe('Authentication Tests', () => {
  test.beforeEach(async ({ page, resetBackend }) => {
    await test.step('Reset backend state', async () => {
      await resetBackend();
    });

    await test.step('Setup browser logging', async () => {
      enableBrowserLogs(page);
    });
  });

  test('Can login with valid credentials', async ({ page }) => {
    await debugAction('Valid login test');
    await login(page, 'admin@rvce.edu.in', 'Test@1234');
    await test.step('Verify successful login', async () => {
      await expect(page.locator('[data-test-id="nav--admin"]')).toBeVisible({
        timeout: 10000
      });
      await attachPageSnapshot(page, 'after-valid-login');
    });
  });

  test('Cannot login with invalid credentials', async ({ page }) => {
    await test.step('Attempt login with invalid credentials', async () => {
      await page.goto('/');
      await page.fill('input[data-test-id="login-email"]', 'invalid@example.com');
      await page.fill('input[data-test-id="login-password"]', 'invalid');
      await page.click('button[data-test-id="login-submit"]');
    });

    await test.step('Verify error message', async () => {
      await expect(page.locator('text=Invalid credentials')).toBeVisible({
        timeout: 5000
      });
      await attachPageSnapshot(page, 'invalid-login-error');
    });
  });

  test('Can logout', async ({ page }) => {
    await test.step('Login first', async () => {
      await login(page, 'admin@rvce.edu.in', 'Test@1234');
    });

    await test.step('Perform logout', async () => {
      const logoutButton = page.locator('button').filter({ hasText: /logout|Logout/ });
      if (await logoutButton.isVisible()) {
        await logoutButton.click();
      } else {
        // Try navigation logout
        await page.click('[data-test-id="nav--logout"]');
      }
    });

    await test.step('Verify logout success', async () => {
      await expect(page).toHaveURL('/');
      await expect(page.locator('input[data-test-id="login-email"]')).toBeVisible();
      await attachPageSnapshot(page, 'after-logout');
    });
  });
});