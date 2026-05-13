import { test, expect } from '@playwright/test';

test('basic navigation and visual flow with delays', async ({ page }) => {
  // Change URL if frontend served on different port
  await page.goto('http://localhost:5173');
  await page.waitForTimeout(1500); // let page settle visually

  // Example: click "Login" (adjust selector to match your app)
  const loginBtn = await page.$('text=Login');
  if (loginBtn) {
    await loginBtn.click();
    await page.waitForTimeout(1200);
  }

  // Fill demo credentials if login form exists
  const email = await page.$('input[type="email"]');
  if (email) {
    await email.fill('admin@rvce.edu.in');
    await page.waitForTimeout(600);
  }
  const pass = await page.$('input[type="password"]');
  if (pass) {
    await pass.fill('Test@1234');
    await page.waitForTimeout(600);
  }

  const submit = await page.$('button:has-text("Sign in")');
  if (submit) {
    await submit.click();
    await page.waitForTimeout(2000);
  }

  // Navigate to timetable or exams page as a smoke check
  const examsLink = await page.$('text=Exams');
  if (examsLink) {
    await examsLink.click();
    await page.waitForTimeout(1500);
    await expect(page).toHaveURL(/.*exams.*/i);
  }

  // Visual pause so you can observe final state
  await page.waitForTimeout(2500);
});
