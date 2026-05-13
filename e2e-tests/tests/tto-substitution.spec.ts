import { test, expect } from './fixtures'
import { login, enableBrowserLogs } from './helpers'
import fs from 'fs'
import path from 'path'

const TTO_EMAIL = 'tto@rvce.edu.in'
const TTO_PASSWORD = 'Test@1234'

async function uploadTimetableCsv(page, csvContent: string, fileName: string) {
  const tempFilePath = path.join(__dirname, '..', fileName)
  fs.writeFileSync(tempFilePath, csvContent)

  await page.goto('/tto/upload')
  await page.setInputFiles('input[type="file"]', tempFilePath)
  await page.click('button:has-text("Upload Timetable")')
  await expect(page.locator('[data-test-id="upload-result"]')).toBeVisible({ timeout: 20000 })

  // Clean up generated file after upload completes
  fs.unlinkSync(tempFilePath)
}

async function submitSubstitution(
  page,
  originalTeacherLabel: string,
  replacementTeacherLabel: string,
  startDate: string,
  endDate: string,
  scope: 'ONE_DAY' | 'SEMESTER'
) {
  await page.goto('/tto/substitute')
  await expect(page.locator('[data-test-id="substitute-page"]')).toBeVisible({ timeout: 20000 })

  await page.selectOption('[data-test-id="substitute-original-teacher"]', {
    label: originalTeacherLabel,
  })
  await page.selectOption('[data-test-id="substitute-replacement-teacher"]', {
    label: replacementTeacherLabel,
  })

  await page.fill('[data-test-id="substitute-start-date"]', startDate)
  await page.fill('[data-test-id="substitute-end-date"]', endDate)
  await page.selectOption('[data-test-id="substitute-scope"]', scope)

  await Promise.all([
    page.waitForResponse((response) =>
      response.url().includes('/api/timetable/substitute') && response.status() === 200
    ),
    page.click('[data-test-id="substitute-submit"]'),
  ])

  await expect(page.locator('[data-test-id="substitution-summary"]')).toBeVisible({ timeout: 20000 })
}

test.describe('TTO substitution scenarios', () => {
  test.beforeEach(async ({ page, resetBackend }) => {
    await resetBackend()
    enableBrowserLogs(page)
    await login(page, TTO_EMAIL, TTO_PASSWORD)
  })

  test('ONE_DAY substitution should block when replacement teacher has a clash', async ({ page }) => {
    await uploadTimetableCsv(
      page,
      `room_id,teacher_id,day_of_week,start_time,end_time,subject,department
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444004,1,10:00:00,11:00:00,Math,CSE
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444018,1,10:00:00,11:00:00,Physics,CSE
`,
      'tto-substitution-clash-one-day.csv'
    )

    await submitSubstitution(
      page,
      'Dr. Ramesh Kumar (ramesh.kumar@rvce.edu.in)',
      'Dr. Vikram Singh (vikram.singh@rvce.edu.in)',
      '2026-05-13',
      '2026-05-13',
      'ONE_DAY'
    )

    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Clashes detected: 1')
    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Reassigned slots: 0')
    await expect(page.locator('[data-test-id="substitution-clash-list"] li')).toHaveCount(1)
  })

  test('ONE_DAY substitution should succeed when replacement teacher has no clash', async ({ page }) => {
    await uploadTimetableCsv(
      page,
      `room_id,teacher_id,day_of_week,start_time,end_time,subject,department
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444003,1,09:00:00,10:00:00,Physics,CSE
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444019,1,10:00:00,11:00:00,English,CSE
`,
      'tto-substitution-no-clash-one-day.csv'
    )

    await submitSubstitution(
      page,
      'Dr. Priya Sharma (priya.sharma@rvce.edu.in)',
      'Prof. Anjali Gupta (anjali.gupta@rvce.edu.in)',
      '2026-05-13',
      '2026-05-13',
      'ONE_DAY'
    )

    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Clashes detected: 0')
    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Reassigned slots: 1')
  })

  test('SEMESTER substitution should block when replacement teacher is busy in the requested range', async ({ page }) => {
    await uploadTimetableCsv(
      page,
      `room_id,teacher_id,day_of_week,start_time,end_time,subject,department
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444005,1,09:00:00,10:00:00,Computer Networks,CSE
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444020,1,09:00:00,10:00:00,Algorithms,CSE
`,
      'tto-substitution-clash-semester.csv'
    )

    await submitSubstitution(
      page,
      'Kiran Reddy (kiran@rvce.edu.in)',
      'Dr. Rajesh Patel (rajesh.patel@rvce.edu.in)',
      '2026-05-13',
      '2026-06-13',
      'SEMESTER'
    )

    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Clashes detected: 1')
    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Reassigned slots: 0')
  })

  test('SEMESTER substitution should succeed when replacement teacher is free for the full range', async ({ page }) => {
    await uploadTimetableCsv(
      page,
      `room_id,teacher_id,day_of_week,start_time,end_time,subject,department
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444007,1,09:00:00,10:00:00,Mathematics,CSE
55555555-5555-5555-5555-555555555001,44444444-4444-4444-4444-444444444021,1,10:00:00,11:00:00,Physics,CSE
`,
      'tto-substitution-no-clash-semester.csv'
    )

    await submitSubstitution(
      page,
      'Diya Rao (diya.rao@rvce.edu.in)',
      'Prof. Swathi Sharma (swathi.sharma@rvce.edu.in)',
      '2026-05-13',
      '2026-06-13',
      'SEMESTER'
    )

    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Clashes detected: 0')
    await expect(page.locator('[data-test-id="substitution-summary"]')).toContainText('Reassigned slots: 1')
  })
})
