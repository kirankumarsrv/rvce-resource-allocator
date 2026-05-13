import { test, expect } from './fixtures';
import { login, enableBrowserLogs, attachPageSnapshot, assertNetworkRequest } from './helpers';

test.describe('Admin User Management', () => {
  test.beforeEach(async ({ page, resetBackend }) => {
    await resetBackend();
    enableBrowserLogs(page);
    await login(page, 'admin@rvce.edu.in', 'Test@1234');
  });

  test('Create single user via admin UI', async ({ page }) => {
    await page.goto('/admin/users');
    await page.fill('[data-test-id="create-name"]', 'E2E Teacher');
    await page.fill('[data-test-id="create-email"]', `e2e.teacher+${Date.now()}@example.com`);
    await page.selectOption('[data-test-id="create-role"]', 'TEACHER');
    await page.fill('[data-test-id="create-dept"]', 'CSE');

    await Promise.all([
      page.waitForResponse((r) => r.url().includes('/api/admin/users') && r.status() === 201),
      page.click('[data-test-id="create-submit"]')
    ]);

    await expect(page.locator('[data-test-id="user-mgmt-message"]')).toHaveText(/Created/);
    await attachPageSnapshot(page, 'after-create-user');
  });

  test('Bulk create users via admin UI', async ({ page }) => {
    await page.goto('/admin/users');
    const now = Date.now();
    const payload = JSON.stringify([
      { name: 'Bulk A', email: `bulk.a+${now}@example.com`, role: 'STUDENT', departmentCode: 'CSE' },
      { name: 'Bulk B', email: `bulk.b+${now}@example.com`, role: 'TEACHER', departmentCode: 'CSE' }
    ]);
    await page.fill('[data-test-id="bulk-text"]', payload);

    await Promise.all([
      page.waitForResponse((r) => r.url().includes('/api/admin/users/bulk') && r.status() === 201),
      page.click('[data-test-id="bulk-submit"]')
    ]);

    await expect(page.locator('[data-test-id="user-mgmt-message"]')).toHaveText(/Bulk created/);
    await attachPageSnapshot(page, 'after-bulk-create');
  });
});
