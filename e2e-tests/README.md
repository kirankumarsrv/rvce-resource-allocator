# E2E Tests for RVCE Resource Allocator

This folder contains end-to-end tests using Playwright for the RVCE Resource Allocator application.

## Setup

1. Ensure the application is running:
   - Backend on http://localhost:8080
   - Frontend on http://localhost:5174

2. Install dependencies:
   ```bash
   cd e2e-tests
   npm install
   npx playwright install
   ```

## Running Tests

- Run all tests: `npm test`
- Run in headed mode: `npm run test:headed`
- Run with UI: `npm run test:ui`

## Debugging Support

- Playwright saves screenshots and video recordings on failure.
- A trace is collected on the first retry to help inspect failures.
- There is a shared `tests/helpers.ts` helper for login and action logging.

## Test Coverage

- **Admin Tests**: Dashboard, user creation, password reset, teacher listing.
- **Teacher Tests**: Exam creation, student upload, invigilator assignment, exam publishing.
- **Student Tests**: Dashboard, exam viewing, seating arrangement.
- **Exam Control**: Starting/ending exams, monitoring progress.
- **Room Allocation**: Room assignment, timetable upload.
- **Authentication**: Login/logout, invalid credentials.

## Notes

- Tests assume seeded data (admin, teachers, students).
- File paths for CSV uploads need to be adjusted to actual locations.
- Tests are designed for the current UI structure; update selectors as needed.