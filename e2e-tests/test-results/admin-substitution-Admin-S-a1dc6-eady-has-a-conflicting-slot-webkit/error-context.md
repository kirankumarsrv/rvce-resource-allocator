# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: admin-substitution.spec.ts >> Admin Substitution Flow >> Admin substitution reports clash when replacement teacher already has a conflicting slot
- Location: tests\admin-substitution.spec.ts:12:7

# Error details

```
Test timeout of 30000ms exceeded while running "beforeEach" hook.
```

```
Error: Backend was not healthy before test execution
```

# Test source

```ts
  1  | /**
  2  |  * Backend reset fixtures for E2E test isolation.
  3  |  *
  4  |  * Provides utilities to reset the backend database state between tests
  5  |  * to ensure test independence and prevent state pollution.
  6  |  */
  7  | import { test as base, expect } from '@playwright/test';
  8  | import { API_BASE_URL } from './helpers';
  9  | 
  10 | // Extend the base test with backend reset functionality
  11 | export const test = base.extend<{
  12 |   resetBackend: () => Promise<void>;
  13 | }>({
  14 |   resetBackend: async ({ page }, use) => {
  15 |     // Reset function that can be called in tests
  16 |     const resetBackend = async () => {
  17 |       const resetUrl = `${API_BASE_URL}/admin/dev/timetable/reset`;
  18 |       const healthUrl = API_BASE_URL.replace('/api', '/actuator/health');
  19 |       const loginUrl = `${API_BASE_URL}/auth/login`;
  20 |       const maxAttempts = 20;
  21 | 
  22 |       for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
  23 |         try {
  24 |           const response = await page.request.get(healthUrl, {
  25 |             timeout: 10000,
  26 |           });
  27 | 
  28 |           if (response.ok()) {
  29 |             const loginResponse = await page.request.post(loginUrl, {
  30 |               timeout: 10000,
  31 |               data: {
  32 |                 email: 'admin@rvce.edu.in',
  33 |                 password: 'Test@1234',
  34 |               },
  35 |             });
  36 | 
  37 |             if (!loginResponse.ok()) {
  38 |               throw new Error(`Reset login failed with status ${loginResponse.status()}`);
  39 |             }
  40 | 
  41 |             const { accessToken } = (await loginResponse.json()) as { accessToken: string };
  42 |             await page.request.post(resetUrl, {
  43 |               timeout: 10000,
  44 |               headers: {
  45 |                 Authorization: `Bearer ${accessToken}`,
  46 |               },
  47 |             });
  48 |             console.log('Backend health check passed');
  49 |             return;
  50 |           }
  51 | 
  52 |           console.warn(`Backend health check status ${response.status()} (attempt ${attempt}/${maxAttempts})`);
  53 |         } catch (error) {
  54 |           console.warn(`Backend health check failed (attempt ${attempt}/${maxAttempts})`, error);
  55 |         }
  56 | 
  57 |         await page.waitForTimeout(1000);
  58 |       }
  59 | 
> 60 |       throw new Error('Backend was not healthy before test execution');
     |             ^ Error: Backend was not healthy before test execution
  61 |     };
  62 | 
  63 |     await use(resetBackend);
  64 |   },
  65 | });
  66 | 
  67 | // Re-export expect for convenience
  68 | export { expect };
  69 | 
  70 | // Global setup for backend reset before all tests
  71 | export const globalSetup = async () => {
  72 |   console.log('E2E Global setup: Backend should be running with test data seeded');
  73 | };
  74 | 
  75 | // Global teardown
  76 | export const globalTeardown = async () => {
  77 |   console.log('E2E Global teardown: Tests completed');
  78 | };
```