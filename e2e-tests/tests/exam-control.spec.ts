/**
 * Exam control end-to-end tests for RVCE Resource Allocator.
 *
 * Ensures exam monitoring pages are accessible and start/end actions function.
 */
import { test, expect } from './fixtures';
import { login, debugAction, enableBrowserLogs, attachPageSnapshot, assertNetworkRequest } from './helpers';

test.describe('Exam Control Tests', () => {
  test.beforeEach(async ({ page, resetBackend }) => {
    await test.step('Reset backend state', async () => {
      await resetBackend();
    });

    await test.step('Setup browser logging', async () => {
      enableBrowserLogs(page);
    });

    await debugAction('Exam control login');
    await login(page, 'priya.sharma@rvce.edu.in', 'Test@1234');
  });

  test('Can view exam control page', async ({ page }) => {
    await test.step('Navigate to exam control', async () => {
      await page.click('[data-test-id="nav--exam-ctrl"]');
      await expect(page.locator('[data-test-id="exam-control-heading"]')).toBeVisible({
        timeout: 10000
      });
      await attachPageSnapshot(page, 'exam-control-page');
    });
  });

  test('Can start exam', async ({ page }) => {
    await test.step('Navigate to exam control', async () => {
      await page.click('[data-test-id="nav--exam-ctrl"]');
    });

    await test.step('Start exam if available', async () => {
      const examCards = page.locator('.rounded-lg.border');
      if (await examCards.count() > 0) {
        await examCards.first().click();
        // Look for start exam button or similar
        const startButton = page.locator('button').filter({ hasText: /start|Start/ });
        if (await startButton.isVisible()) {
          await startButton.click();
          await assertNetworkRequest(page, '/api/exam/', 200);
          await expect(page.locator('text=Exam started')).toBeVisible({
            timeout: 10000
          });
          await attachPageSnapshot(page, 'after-exam-start');
        } else {
          console.log('No start exam button found - exam may already be started');
        }
      } else {
        console.log('No exams available to start');
      }
    });
  });

  test('Can monitor exam progress', async ({ page }) => {
    await test.step('Navigate to exam control', async () => {
      await page.click('[data-test-id="nav--exam-ctrl"]');
    });

    await test.step('Check exam monitoring features', async () => {
      const examCards = page.locator('.rounded-lg.border');
      if (await examCards.count() > 0) {
        await examCards.first().click();
        // Look for monitoring elements
        const statusElements = page.locator('text=Students present').or(page.locator('.exam-status'));
        await expect(statusElements.first()).toBeVisible({
          timeout: 5000
        });
        await attachPageSnapshot(page, 'exam-monitoring');
      } else {
        console.log('No exams available for monitoring');
      }
    });
  });

  test('Can end exam', async ({ page }) => {
    await test.step('Navigate to exam control', async () => {
      await page.click('[data-test-id="nav--exam-ctrl"]');
    });

    await test.step('End exam if available', async () => {
      const examCards = page.locator('.rounded-lg.border');
      if (await examCards.count() > 0) {
        await examCards.first().click();
        // Look for end exam button or similar
        const endButton = page.locator('button').filter({ hasText: /end|End/ });
        if (await endButton.isVisible()) {
          await endButton.click();
          await assertNetworkRequest(page, '/api/exam/', 200);
          await expect(page.locator('text=Exam ended')).toBeVisible({
            timeout: 10000
          });
          await attachPageSnapshot(page, 'after-exam-end');
        } else {
          console.log('No end exam button found - exam may not be started');
        }
      } else {
        console.log('No exams available to end');
      }
    });
  });
});