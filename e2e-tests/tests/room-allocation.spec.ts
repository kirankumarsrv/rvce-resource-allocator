/**
 * Room allocation end-to-end tests for RVCE Resource Allocator.
 *
 * Validates room allocation pages, allocation actions, and timetable imports.
 */
import { test, expect } from '@playwright/test';
import { login, debugAction, enableBrowserLogs } from './helpers';

test.describe('Room Allocation Tests', () => {
  test.beforeEach(async ({ page }) => {
    enableBrowserLogs(page);
    debugAction('Room allocation login');
    await login(page, 'teacher1', 'password');
  });

  test('Can view room allocation page', async ({ page }) => {
    await page.click('text=Room Allocation');
    await expect(page.locator('text=Room Allocation')).toBeVisible();
  });

  test('Can allocate rooms for exam', async ({ page }) => {
    await page.click('text=Room Allocation');
    await page.selectOption('select[name="exam"]', 'Math Exam');
    await page.click('text=Allocate Rooms');
    await expect(page.locator('text=Rooms allocated successfully')).toBeVisible();
  });

  test('Can view allocated rooms', async ({ page }) => {
    await page.click('text=Room Allocation');
    const roomCount = await page.locator('.allocated-room').count();
    expect(roomCount).toBeGreaterThan(0);
  });

  test('Can upload timetable CSV', async ({ page }) => {
    await page.click('text=Upload Timetable');
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('path/to/timetable-upload-template.csv'); // Adjust path
    await page.click('button[type="submit"]');
    await expect(page.locator('text=Timetable uploaded successfully')).toBeVisible();
  });
});