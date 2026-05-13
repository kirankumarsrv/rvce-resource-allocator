/**
 * Shared helper utilities for Playwright end-to-end tests.
 *
 * Provides reusable functions for login, debug action logging, browser event capture,
 * and advanced Playwright features like manual attachments and test steps.
 *
 * @packageDocumentation
 */
import { Page, expect, test } from '@playwright/test';

/**
 * Base URL for API calls in tests.
 */
export const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Logs into the application using the provided credentials.
 *
 * @param page - The Playwright page instance.
 * @param username - The email to authenticate with (e.g., priya.sharma@rvce.edu.in).
 * @param password - The password to authenticate with (e.g., Test@1234).
 *
 * @example
 * ```typescript
 * await login(page, 'teacher1@rvce.edu.in', 'Test@1234');
 * ```
 */
export async function login(page: Page, username: string, password: string) {
  await test.step(`Login with user ${username}`, async () => {
    console.log(`Login start: ${username}`);
    const loginError = page.locator('[data-test-id="login-error"]');
    let lastErrorMessage = 'Login failed';

    for (let attempt = 1; attempt <= 2; attempt += 1) {
      await page.goto('/login');
      console.log(`Login page loaded (attempt ${attempt})`);

      await page.fill('input[data-test-id="login-email"]', username);
      await page.fill('input[data-test-id="login-password"]', password);
      await page.click('button[data-test-id="login-submit"]');

      try {
        await expect(page).toHaveURL(/.*\/(teacher|student|admin|exam-ctrl|tto)(\/|$)/, {
          timeout: 15000,
        });
        console.log(`Login successful: ${username}`);
        return;
      } catch {
        if (await loginError.isVisible({ timeout: 1000 }).catch(() => false)) {
          lastErrorMessage = (await loginError.textContent())?.trim() || lastErrorMessage;
          console.warn(`Login failed on attempt ${attempt}: ${lastErrorMessage}`);
        } else {
          lastErrorMessage = `No redirect after login attempt ${attempt}`;
          console.warn(lastErrorMessage);
        }

        if (attempt < 2) {
          await page.waitForTimeout(2000);
        }
      }
    }

    throw new Error(`Login failed for ${username}: ${lastErrorMessage}`);
  });
}

/**
 * Logs a named action for easier test tracing and debugging.
 *
 * Wraps action in test.step() for structured test reporting.
 *
 * @param actionName - The friendly name of the action being performed.
 * @param action - Optional async function to execute within the step.
 *
 * @example
 * ```typescript
 * await debugAction('Creating exam', async () => {
 *   await page.click('text=Create Exam');
 * });
 * ```
 */
export async function debugAction(
  actionName: string,
  action?: () => Promise<void>
): Promise<void> {
  await test.step(`Action: ${actionName}`, async () => {
    console.log(`ACTION: ${actionName}`);
    if (action) {
      await action();
    }
  });
}

/**
 * Attaches a debug snapshot to the current test for inspection.
 *
 * Captures page state for manual review in test report.
 *
 * @param page - The Playwright page instance.
 * @param name - Friendly name for the attachment (e.g., 'after-login').
 *
 * @example
 * ```typescript
 * await attachPageSnapshot(page, 'after-login-success');
 * ```
 */
export async function attachPageSnapshot(page: Page, name: string): Promise<void> {
  const html = await page.content();
  test.info().attach(`${name}-snapshot`, {
    body: html,
    contentType: 'text/html',
  });
  console.log(`Snapshot attached: ${name}`);
}

/**
 * Adds browser-level logging handlers for console events, page errors, and failed requests.
 *
 * Enables comprehensive debug output for diagnosing test failures.
 *
 * @param page - The Playwright page instance.
 *
 * @example
 * ```typescript
 * test.beforeEach(async ({ page }) => {
 *   enableBrowserLogs(page);
 * });
 * ```
 */
export function enableBrowserLogs(page: Page) {
  page.on('console', (message) => {
    console.log(`BROWSER CONSOLE [${message.type()}]: ${message.text()}`);
  });

  page.on('pageerror', (error) => {
    console.log(`BROWSER PAGE ERROR: ${error.message}`);
  });

  page.on('requestfailed', async (request) => {
    const failure = request.failure();
    const response = await request.response();
    const statusCode = response?.status() ?? 'N/A';
    console.log(
      `REQUEST FAILED: ${request.method()} ${request.url()} status=${statusCode} error=${failure?.errorText ?? 'unknown'}`
    );
  });

  page.on('response', (response) => {
    // Log only error responses for clarity
    if (!response.ok()) {
      console.log(
        `RESPONSE ERROR: ${response.request().method()} ${response.url()} status=${response.status()}`
      );
    }
  });
}

/**
 * Asserts that a network request completes successfully.
 *
 * Waits for a specific request and validates its response status.
 *
 * @param page - The Playwright page instance.
 * @param urlPattern - URL pattern to match (regex or string).
 * @param expectedStatus - Expected HTTP status code (default: 200).
 *
 * @example
 * ```typescript
 * await assertNetworkRequest(page, '/api/exam/sessions', 200);
 * ```
 */
export async function assertNetworkRequest(
  page: Page,
  urlPattern: string | RegExp,
  expectedStatus: number = 200
): Promise<void> {
  await test.step(`Assert network request: ${urlPattern}`, async () => {
    const response = await page.waitForResponse((resp) => {
      const matches = typeof urlPattern === 'string' 
        ? resp.url().includes(urlPattern) 
        : urlPattern.test(resp.url());
      return matches && resp.status() === expectedStatus;
    });
    console.log(
      `✓ Network request successful: ${response.request().method()} ${response.url()} (${response.status()})`
    );
  });
}

