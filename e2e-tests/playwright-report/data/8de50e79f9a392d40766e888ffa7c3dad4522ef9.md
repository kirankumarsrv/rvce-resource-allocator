# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: auth.spec.ts >> Authentication Tests >> Cannot login with invalid credentials
- Location: tests\auth.spec.ts:31:7

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
  1  | /**
  2  |  * Core authentication end-to-end tests for RVCE Resource Allocator.
  3  |  *
  4  |  * Covers valid and invalid login flows and ensures logout works correctly.
  5  |  */
  6  | import { test, expect } from './fixtures';
  7  | import { login, debugAction, enableBrowserLogs, attachPageSnapshot } from './helpers';
  8  | 
  9  | test.describe('Authentication Tests', () => {
  10 |   test.beforeEach(async ({ page, resetBackend }) => {
  11 |     await test.step('Reset backend state', async () => {
  12 |       await resetBackend();
  13 |     });
  14 | 
  15 |     await test.step('Setup browser logging', async () => {
  16 |       enableBrowserLogs(page);
  17 |     });
  18 |   });
  19 | 
  20 |   test('Can login with valid credentials', async ({ page }) => {
  21 |     await debugAction('Valid login test');
  22 |     await login(page, 'admin@rvce.edu.in', 'Test@1234');
  23 |     await test.step('Verify successful login', async () => {
  24 |       await expect(page.locator('[data-test-id="nav--admin"]')).toBeVisible({
  25 |         timeout: 10000
  26 |       });
  27 |       await attachPageSnapshot(page, 'after-valid-login');
  28 |     });
  29 |   });
  30 | 
  31 |   test('Cannot login with invalid credentials', async ({ page }) => {
  32 |     await test.step('Attempt login with invalid credentials', async () => {
> 33 |       await page.goto('/');
     |                  ^ Error: page.goto: NS_ERROR_CONNECTION_REFUSED
  34 |       await page.fill('input[data-test-id="login-email"]', 'invalid@example.com');
  35 |       await page.fill('input[data-test-id="login-password"]', 'invalid');
  36 |       await page.click('button[data-test-id="login-submit"]');
  37 |     });
  38 | 
  39 |     await test.step('Verify error message', async () => {
  40 |       await expect(page.locator('text=Invalid credentials')).toBeVisible({
  41 |         timeout: 5000
  42 |       });
  43 |       await attachPageSnapshot(page, 'invalid-login-error');
  44 |     });
  45 |   });
  46 | 
  47 |   test('Can logout', async ({ page }) => {
  48 |     await test.step('Login first', async () => {
  49 |       await login(page, 'admin@rvce.edu.in', 'Test@1234');
  50 |     });
  51 | 
  52 |     await test.step('Perform logout', async () => {
  53 |       const logoutButton = page.locator('button').filter({ hasText: /logout|Logout/ });
  54 |       if (await logoutButton.isVisible()) {
  55 |         await logoutButton.click();
  56 |       } else {
  57 |         // Try navigation logout
  58 |         await page.click('[data-test-id="nav--logout"]');
  59 |       }
  60 |     });
  61 | 
  62 |     await test.step('Verify logout success', async () => {
  63 |       await expect(page).toHaveURL('/');
  64 |       await expect(page.locator('input[data-test-id="login-email"]')).toBeVisible();
  65 |       await attachPageSnapshot(page, 'after-logout');
  66 |     });
  67 |   });
  68 | });
```