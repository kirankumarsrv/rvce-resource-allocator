import { test, expect } from './fixtures';
import { login, enableBrowserLogs, attachPageSnapshot } from './helpers';
import fs from 'fs';

test.describe('Admin Substitution Flow', () => {
  test.beforeEach(async ({ page, resetBackend }) => {
    await resetBackend();
    enableBrowserLogs(page);
    await login(page, 'admin@rvce.edu.in', 'Test@1234');
  });

  test('Admin substitution reports clash when replacement teacher already has a conflicting slot', async ({ page }) => {
    // Upload timetable with conflicting slots
    await page.goto('/tto/upload');
    const csvContent = `room_id,teacher_id,day_of_week,start_time,end_time,subject,department
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444004,1,09:00:00,10:00:00,Math,CSE
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444018,1,09:00:00,10:00:00,Physics,CSE
`;
    const tempFile = 'temp_timetable.csv';
    fs.writeFileSync(tempFile, csvContent);
    await page.setInputFiles('input[type="file"]', tempFile);
    await page.click('button:has-text("Upload")');
    await page.waitForSelector('[data-test-id="upload-result"]', { timeout: 10000 });

    const teacherListResponse = page.waitForResponse((response) =>
      response.url().includes('/api/timetable/teachers') && response.status() === 200
    );

    await page.goto('/admin/substitute');
    await teacherListResponse;
    await expect(page.locator('[data-test-id="substitute-page"]')).toBeVisible({ timeout: 30000 });

    await page.selectOption('[data-test-id="substitute-original-teacher"]', {
      label: 'Dr. Ramesh Kumar (ramesh.kumar@rvce.edu.in)',
    });
    await page.selectOption('[data-test-id="substitute-replacement-teacher"]', {
      label: 'Dr. Vikram Singh (vikram.singh@rvce.edu.in)',
    });

    await page.fill('[data-test-id="substitute-start-date"]', '2026-05-13');
    await page.fill('[data-test-id="substitute-end-date"]', '2026-05-13');
    await page.selectOption('[data-test-id="substitute-scope"]', 'ONE_DAY');

    await Promise.all([
      page.waitForResponse((response) =>
        response.url().includes('/api/timetable/substitute') && response.status() === 200
      ),
      page.click('[data-test-id="substitute-submit"]'),
    ]);

    await expect(page.locator('[data-test-id="substitution-summary"]')).toBeVisible();
    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Clashes detected: 1');
    await expect(page.locator('[data-test-id="substitution-clash-list"] li')).toHaveCount(1);
    await attachPageSnapshot(page, 'admin-substitution-clash');

    // Clean up
    fs.unlinkSync(tempFile);
  });
});
