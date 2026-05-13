const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();
  page.on('console', msg => console.log('CONSOLE', msg.type(), msg.text()));
  page.on('pageerror', err => console.log('PAGEERROR', err.message));
  page.on('requestfailed', req => console.log('REQUESTFAILED', req.url(), req.failure()?.errorText));
  await page.goto('http://localhost:5173/login', { waitUntil: 'networkidle' });
  console.log('LOGIN PAGE URL', page.url());
  await page.fill('input[data-test-id="login-email"]', 'admin@rvce.edu.in');
  await page.fill('input[data-test-id="login-password"]', 'Test@1234');
  await Promise.all([
    page.waitForNavigation({ waitUntil: 'networkidle', timeout: 15000 }),
    page.click('button[data-test-id="login-submit"]')
  ]).catch(e => console.log('NAV ERROR', e.message));
  console.log('AFTER LOGIN URL', page.url());
  await page.goto('http://localhost:5173/admin/users', { waitUntil: 'networkidle' });
  console.log('ADMIN USERS URL', page.url());
  console.log('BODY LEN', await page.evaluate(() => document.body.innerHTML.length));
  console.log('ROOT HTML', await page.evaluate(() => document.querySelector('#root')?.innerHTML));
  console.log('TEST IDS', await page.evaluate(() => Array.from(document.querySelectorAll('[data-test-id]')).map(el => el.getAttribute('data-test-id'))));
  await page.screenshot({ path: 'debug-admin-users.png', fullPage: true });
  console.log('Screenshot saved debug-admin-users.png');
  await browser.close();
})();
