const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();
  page.on('console', msg => console.log('CONSOLE:', msg.type(), msg.text()));
  page.on('pageerror', err => console.log('PAGEERROR:', err.message));
  page.on('requestfailed', req => console.log('REQUESTFAILED:', req.url(), req.failure()?.errorText));
  await page.goto('http://localhost:5173/admin/users', { waitUntil: 'networkidle' });
  console.log('URL', page.url());
  console.log('TITLE', await page.title());
  console.log('BODYCOUNT', await page.evaluate(() => document.body.children.length));
  console.log('ROOTHTML', await page.evaluate(() => document.querySelector('#root')?.innerHTML));
  await browser.close();
})();
