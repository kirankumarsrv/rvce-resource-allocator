/**
 * Admin end-to-end tests for RVCE Resource Allocator.
 *
 * These tests verify the admin workflow including login, dashboard access,
 * substitution tool access, room availability access, and logout.
 */
import { test, expect } from './fixtures';
import { login, debugAction, enableBrowserLogs, attachPageSnapshot } from './helpers';

test.describe('Admin Tests', () => {
  test.setTimeout(120000);

  test.beforeEach(async ({ page, resetBackend }) => {
    await test.step('Reset backend state', async () => {
      await resetBackend();
    });

    await test.step('Setup browser logging', async () => {
      enableBrowserLogs(page);
    });

    await debugAction('Admin login');
    await login(page, 'admin@rvce.edu.in', 'Test@1234');
  });

  test('Admin can view dashboard', async ({ page }) => {
    await test.step('Verify admin dashboard access', async () => {
      await expect(page).toHaveURL(/.*\/admin(\/|$)/, { timeout: 15000 });
      await expect(page.getByRole('heading', { name: 'Admin Portal' })).toBeVisible({ timeout: 10000 });
      await attachPageSnapshot(page, 'admin-dashboard');
    });
  });

  test('Admin can open substitution tool', async ({ page }) => {
    await test.step('Open substitution page from admin dashboard', async () => {
      await page.getByRole('link', { name: 'Teacher Substitution' }).first().click();
      await expect(page).toHaveURL(/.*\/admin\/substitute(\/|$)/, { timeout: 10000 });
      await expect(page.getByRole('heading', { name: 'Teacher Substitution' })).toBeVisible({ timeout: 10000 });
      await attachPageSnapshot(page, 'admin-substitute-page');
    });
  });

  test('Admin can open room availability', async ({ page }) => {
    await test.step('Open room availability from admin dashboard', async () => {
      await page.locator('[data-test-id="nav--tto-rooms"]').first().click();
      await expect(page).toHaveURL(/.*\/tto\/rooms(\/|$)/, { timeout: 10000 });
      await expect(page.getByRole('heading', { name: 'Room Availability' })).toBeVisible({ timeout: 10000 });
      await attachPageSnapshot(page, 'admin-room-availability');
    });
  });

  test('Admin can logout', async ({ page }) => {
    await test.step('Logout from authenticated layout', async () => {
      await page.locator('[data-test-id="logout-button"]').click();
      await expect(page).toHaveURL(/.*\/login(\/|$)?/, { timeout: 10000 });
      await expect(page.locator('[data-test-id="login-email"]')).toBeVisible({ timeout: 10000 });
      await attachPageSnapshot(page, 'admin-logout');
    });
  });
});