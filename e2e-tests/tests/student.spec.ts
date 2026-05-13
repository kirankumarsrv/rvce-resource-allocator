/**
 * Student-facing end-to-end tests for RVCE Resource Allocator.
 *
 * Confirms student dashboard access, assigned exam visibility, and seating plans.
 */
import { test, expect } from './fixtures';
import { login, debugAction, enableBrowserLogs } from './helpers';

test.describe('Student Tests', () => {
  test.beforeEach(async ({ page, resetBackend }) => {
    await test.step('Reset backend state', async () => {
      await resetBackend();
    });

    await test.step('Setup browser logging', async () => {
      enableBrowserLogs(page);
    });

    await debugAction('Student login');
    await login(page, 'student1', 'password');
  });

  test('Student can view dashboard', async ({ page }) => {
    await expect(page.locator('text=Student Dashboard')).toBeVisible();
  });

  test('Student can view assigned exam', async ({ page }) => {
    await page.click('text=My Exams');
    await expect(page.locator('text=Math Exam')).toBeVisible();
    await expect(page.locator('text=Hall A')).toBeVisible(); // Seating info
  });

  test('Student can view seating arrangement', async ({ page }) => {
    await page.click('text=Seating Plan');
    await expect(page.locator('.seating-chart')).toBeVisible();
  });
});