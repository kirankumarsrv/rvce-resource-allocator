# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: exam-control.spec.ts >> Exam Control Tests >> Can start exam
- Location: tests\exam-control.spec.ts:33:7

# Error details

```
Error: page.goto: NS_ERROR_CONNECTION_REFUSED
Call log:
  - navigating to "http://localhost:5174/", waiting until "load"

```

# Page snapshot

```yaml
- article "Looks like there’s a problem with this site" [ref=e3]:
  - img "Illustration of a fox looking at disconnected network cables." [ref=e5]
  - generic [ref=e7]:
    - heading "Looks like there’s a problem with this site" [level=1] [ref=e8]
    - paragraph [ref=e9]:
      - text: Nightly can’t connect to the server at
      - strong [ref=e10]: localhost:5174
    - generic [ref=e11]:
      - heading "What can you do about it?" [level=3] [ref=e12]
      - paragraph [ref=e13]: Try connecting on a different device. Check your modem or router. Disconnect and reconnect to Wi-Fi.
    - button "Try Again" [ref=e16]:
      - generic [ref=e18]:
        - generic: Try Again
```

# Test source

```ts
  1   | /**
  2   |  * Shared helper utilities for Playwright end-to-end tests.
  3   |  *
  4   |  * Provides reusable functions for login, debug action logging, browser event capture,
  5   |  * and advanced Playwright features like manual attachments and test steps.
  6   |  *
  7   |  * @packageDocumentation
  8   |  */
  9   | import { Page, test } from '@playwright/test';
  10  | 
  11  | /**
  12  |  * Base URL for API calls in tests.
  13  |  */
  14  | export const API_BASE_URL = 'http://localhost:8080/api';
  15  | 
  16  | /**
  17  |  * Logs into the application using the provided credentials.
  18  |  *
  19  |  * @param page - The Playwright page instance.
  20  |  * @param username - The email to authenticate with (e.g., priya.sharma@rvce.edu.in).
  21  |  * @param password - The password to authenticate with (e.g., Test@1234).
  22  |  *
  23  |  * @example
  24  |  * ```typescript
  25  |  * await login(page, 'teacher1@rvce.edu.in', 'Test@1234');
  26  |  * ```
  27  |  */
  28  | export async function login(page: Page, username: string, password: string) {
  29  |   await test.step(`Login with user ${username}`, async () => {
  30  |     console.log(`Login start: ${username}`);
> 31  |     await page.goto('/');
      |                ^ Error: page.goto: NS_ERROR_CONNECTION_REFUSED
  32  |     console.log('Login page loaded');
  33  |     await page.fill('input[data-test-id="login-email"]', username);
  34  |     await page.fill('input[data-test-id="login-password"]', password);
  35  |     await page.click('button[data-test-id="login-submit"]');
  36  |     await page.waitForURL(/.*\/(teacher|student|admin|exam-ctrl|tto)/);
  37  |     console.log(`Login successful: ${username}`);
  38  |   });
  39  | }
  40  | 
  41  | /**
  42  |  * Logs a named action for easier test tracing and debugging.
  43  |  *
  44  |  * Wraps action in test.step() for structured test reporting.
  45  |  *
  46  |  * @param actionName - The friendly name of the action being performed.
  47  |  * @param action - Optional async function to execute within the step.
  48  |  *
  49  |  * @example
  50  |  * ```typescript
  51  |  * await debugAction('Creating exam', async () => {
  52  |  *   await page.click('text=Create Exam');
  53  |  * });
  54  |  * ```
  55  |  */
  56  | export async function debugAction(
  57  |   actionName: string,
  58  |   action?: () => Promise<void>
  59  | ): Promise<void> {
  60  |   await test.step(`Action: ${actionName}`, async () => {
  61  |     console.log(`ACTION: ${actionName}`);
  62  |     if (action) {
  63  |       await action();
  64  |     }
  65  |   });
  66  | }
  67  | 
  68  | /**
  69  |  * Attaches a debug snapshot to the current test for inspection.
  70  |  *
  71  |  * Captures page state for manual review in test report.
  72  |  *
  73  |  * @param page - The Playwright page instance.
  74  |  * @param name - Friendly name for the attachment (e.g., 'after-login').
  75  |  *
  76  |  * @example
  77  |  * ```typescript
  78  |  * await attachPageSnapshot(page, 'after-login-success');
  79  |  * ```
  80  |  */
  81  | export async function attachPageSnapshot(page: Page, name: string): Promise<void> {
  82  |   const html = await page.content();
  83  |   test.info().attach(`${name}-snapshot`, {
  84  |     body: html,
  85  |     contentType: 'text/html',
  86  |   });
  87  |   console.log(`Snapshot attached: ${name}`);
  88  | }
  89  | 
  90  | /**
  91  |  * Adds browser-level logging handlers for console events, page errors, and failed requests.
  92  |  *
  93  |  * Enables comprehensive debug output for diagnosing test failures.
  94  |  *
  95  |  * @param page - The Playwright page instance.
  96  |  *
  97  |  * @example
  98  |  * ```typescript
  99  |  * test.beforeEach(async ({ page }) => {
  100 |  *   enableBrowserLogs(page);
  101 |  * });
  102 |  * ```
  103 |  */
  104 | export function enableBrowserLogs(page: Page) {
  105 |   page.on('console', (message) => {
  106 |     console.log(`BROWSER CONSOLE [${message.type()}]: ${message.text()}`);
  107 |   });
  108 | 
  109 |   page.on('pageerror', (error) => {
  110 |     console.log(`BROWSER PAGE ERROR: ${error.message}`);
  111 |   });
  112 | 
  113 |   page.on('requestfailed', async (request) => {
  114 |     const failure = request.failure();
  115 |     const response = await request.response();
  116 |     const statusCode = response?.status() ?? 'N/A';
  117 |     console.log(
  118 |       `REQUEST FAILED: ${request.method()} ${request.url()} status=${statusCode} error=${failure?.errorText ?? 'unknown'}`
  119 |     );
  120 |   });
  121 | 
  122 |   page.on('response', (response) => {
  123 |     // Log only error responses for clarity
  124 |     if (!response.ok()) {
  125 |       console.log(
  126 |         `RESPONSE ERROR: ${response.request().method()} ${response.url()} status=${response.status()}`
  127 |       );
  128 |     }
  129 |   });
  130 | }
  131 | 
```