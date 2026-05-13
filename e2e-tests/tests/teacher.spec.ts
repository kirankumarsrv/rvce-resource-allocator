/**
 * Teacher workflow end-to-end tests for RVCE Resource Allocator.
 *
 * Verifies exam creation, student upload, invigilator assignment, and publishing.
 */
import { test, expect } from './fixtures';
import { login, debugAction, enableBrowserLogs, attachPageSnapshot, assertNetworkRequest } from './helpers';

test.describe('Teacher Tests', () => {
  test.beforeEach(async ({ page, resetBackend }) => {
    await test.step('Reset backend state', async () => {
      await resetBackend();
    });

    await test.step('Setup browser logging', async () => {
      enableBrowserLogs(page);
    });

    await debugAction('Teacher login');
    await login(page, 'priya.sharma@rvce.edu.in', 'Test@1234');
  });

  test('Teacher can view dashboard', async ({ page }) => {
    await test.step('Verify teacher dashboard access', async () => {
      await expect(page.locator('[data-test-id="nav--teacher"]')).toBeVisible({
        timeout: 10000
      });
      await attachPageSnapshot(page, 'teacher-dashboard');
    });
  });

  test('Teacher can create an exam', async ({ page }) => {
    await test.step('Navigate to create exam page', async () => {
      await page.click('[data-test-id="nav--exam-ctrl"]');
      await page.click('[data-test-id="create-exam-button"]');
      await expect(page.locator('[data-test-id="create-exam-heading"]')).toBeVisible();
    });

    await test.step('Fill exam details', async () => {
      await page.fill('[data-test-id="exam-name"]', 'Dec 2026 CIE-1 - 5th Sem CSE');
      await page.fill('[data-test-id="exam-subject-code"]', '21CS51');
      await page.fill('[data-test-id="exam-subject-name"]', 'Design and Analysis of Algorithms');
      await page.fill('[data-test-id="exam-semester"]', '5');
      await page.fill('[data-test-id="exam-section"]', 'A');
      await page.fill('[data-test-id="exam-date"]', '2026-12-15');
      await page.fill('[data-test-id="exam-start-time"]', '10:00');
      await page.fill('[data-test-id="exam-end-time"]', '12:00');
    });

    await test.step('Submit exam creation', async () => {
      await page.click('[data-test-id="create-exam-submit"]');
      await assertNetworkRequest(page, '/api/exam/sessions', 201);
      await expect(page.locator('text=Exam created successfully')).toBeVisible({
        timeout: 10000
      });
      await attachPageSnapshot(page, 'after-exam-creation');
    });
  });

  test('Teacher can upload students CSV', async ({ page }) => {
    await test.step('Navigate to exam control', async () => {
      await page.click('[data-test-id="nav--exam-ctrl"]');
      await expect(page.locator('[data-test-id="exam-control-heading"]')).toBeVisible();
    });

    await test.step('Select first exam and upload students', async () => {
      // Click on the first exam card (assuming it exists from previous test)
      await page.locator('.rounded-lg.border').first().click();
      await expect(page.locator('text=Upload Students CSV')).toBeVisible();
    });

    await test.step('Upload CSV file', async () => {
      const fileInput = page.locator('input[type="file"]');
      await fileInput.setInputFiles('frontend/public/students-test.csv');
      await page.click('button:has-text("Upload")');
      await assertNetworkRequest(page, '/api/exam/students/upload', 200);
      await expect(page.locator('text=Students uploaded successfully')).toBeVisible({
        timeout: 15000
      });
      await attachPageSnapshot(page, 'after-student-upload');
    });
  });

  test('Teacher can add invigilator to exam', async ({ page }) => {
    await test.step('Navigate to exam seating page', async () => {
      await page.click('[data-test-id="nav--exam-ctrl"]');
      await page.locator('.rounded-lg.border').first().click();
      await expect(page.locator('text=Add Hall')).toBeVisible();
    });

    await test.step('Open add hall modal', async () => {
      await page.click('text=Add Hall');
      await expect(page.locator('text=Add New Hall')).toBeVisible();
    });

    await test.step('Configure hall settings', async () => {
      // Select first available room
      await page.locator('[data-test-id="add-hall-room"]').selectOption({ index: 0 });
      // Select first available teacher as invigilator
      await page.locator('[data-test-id="add-hall-invigilator"]').selectOption({ index: 0 });
      // Set bench counts
      await page.fill('[data-test-id="add-hall-two-seater"]', '5');
      await page.fill('[data-test-id="add-hall-three-seater"]', '2');
    });

    await test.step('Submit hall configuration', async () => {
      await page.click('[data-test-id="add-hall-submit"]');
      await assertNetworkRequest(page, '/api/exam/', 200);
      await expect(page.locator('text=Invigilator added successfully')).toBeVisible({
        timeout: 10000
      });
      await attachPageSnapshot(page, 'after-hall-addition');
    });
  });

  test('Teacher can publish exam', async ({ page }) => {
    await test.step('Navigate to exam control', async () => {
      await page.click('[data-test-id="nav--exam-ctrl"]');
      await page.locator('.rounded-lg.border').first().click();
    });

    await test.step('Publish exam', async () => {
      await page.click('text=Publish Exam');
      await assertNetworkRequest(page, '/api/exam/', 200);
      await expect(page.locator('text=Exam published successfully')).toBeVisible({
        timeout: 10000
      });
      await attachPageSnapshot(page, 'after-exam-publish');
    });
  });
});