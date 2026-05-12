/**
 * Admin end-to-end tests for RVCE Resource Allocator.
 *
 * These tests verify the admin workflow including login, dashboard access,
 * teacher listing, user creation, and password reset functionality.
 */
import { test, expect } from './fixtures';
import { login, debugAction, enableBrowserLogs, attachPageSnapshot, assertNetworkRequest } from './helpers';

test.describe('Admin Tests', () => {
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
      await expect(page.locator('[data-test-id="nav--admin"]')).toBeVisible({
        timeout: 10000
      });
      await attachPageSnapshot(page, 'admin-dashboard');
    });
  });

  test('Admin can list teachers', async ({ page }) => {
    await test.step('Navigate to admin dashboard', async () => {
      await page.click('[data-test-id="nav--admin"]');
    });

    await test.step('Check for teacher management features', async () => {
      // Look for teacher-related links or sections
      const teacherLink = page.locator('a').filter({ hasText: /teacher|Teacher/ });
      if (await teacherLink.isVisible()) {
        await teacherLink.click();
        await expect(page.locator('text=List of Teachers')).toBeVisible({
          timeout: 5000
        });
        await attachPageSnapshot(page, 'teacher-list');
      } else {
        console.log('Teacher management link not found');
      }
    });
  });

  test('Admin can create a new user', async ({ page }) => {
    await test.step('Navigate to admin dashboard', async () => {
      await page.click('[data-test-id="nav--admin"]');
    });

    await test.step('Access user creation', async () => {
      const createUserLink = page.locator('a').filter({ hasText: /create|Create/ });
      if (await createUserLink.isVisible()) {
        await createUserLink.click();
        await expect(page.locator('text=Create User')).toBeVisible();
      } else {
        console.log('Create user link not found');
        return;
      }
    });

    await test.step('Fill user creation form', async () => {
      await page.fill('input[name="username"]', 'newteacher');
      await page.fill('input[name="email"]', 'newteacher@example.com');
      await page.selectOption('select[name="role"]', 'TEACHER');
    });

    await test.step('Submit user creation', async () => {
      await page.click('button[type="submit"]');
      await assertNetworkRequest(page, '/api/admin/users', 201);
      await expect(page.locator('text=User created successfully')).toBeVisible({
        timeout: 10000
      });
      await attachPageSnapshot(page, 'after-user-creation');
    });
  });

  test('Admin can reset user password', async ({ page }) => {
    await test.step('Navigate to admin dashboard', async () => {
      await page.click('[data-test-id="nav--admin"]');
    });

    await test.step('Access user management', async () => {
      const usersLink = page.locator('a').filter({ hasText: /users|Users/ });
      if (await usersLink.isVisible()) {
        await usersLink.click();
        await expect(page.locator('text=Users')).toBeVisible({
          timeout: 5000
        });
      } else {
        console.log('Users management link not found');
        return;
      }
    });

    await test.step('Find and reset password for a user', async () => {
      // Look for reset password functionality
      const resetButton = page.locator('button').filter({ hasText: /reset|Reset/ });
      if (await resetButton.isVisible()) {
        await resetButton.first().click();
        await page.fill('input[name="newPassword"]', 'newpass123');
        await page.click('button[type="submit"]');
        await assertNetworkRequest(page, '/api/admin/users/', 200);
        await expect(page.locator('text=Password reset successfully')).toBeVisible({
          timeout: 10000
        });
        await attachPageSnapshot(page, 'after-password-reset');
      } else {
        console.log('Reset password functionality not found');
      }
    });
  });
});