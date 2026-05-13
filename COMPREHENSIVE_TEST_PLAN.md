# COMPREHENSIVE TEST PLAN FOR EDGE CASES

## Test Strategy
- **Unit Tests**: SubstitutionService, clash detection logic
- **Integration Tests**: End-to-end substitution flows
- **Frontend Tests**: UI rendering, error handling
- **Edge Case Tests**: All 15 scenarios

---

## TEST SUITE 1: SUBSTITUTION CLASH DETECTION

### TC-1.1: Time Overlap Edge Cases
```gherkin
Given: Original teacher has slot 10:00-11:00 on Monday
Given: Replacement teacher has various slots

Scenario 1: Perfect overlap
  When: Replacement has 10:00-11:00
  Then: CLASH ✅

Scenario 2: Partial overlap (later)
  When: Replacement has 10:30-11:30
  Then: CLASH ✅

Scenario 3: Partial overlap (earlier)
  When: Replacement has 09:30-10:15
  Then: CLASH ✅

Scenario 4: Adjacent (no overlap)
  When: Replacement has 11:00-12:00
  Then: NO CLASH ✅

Scenario 5: Before (no overlap)
  When: Replacement has 09:00-10:00
  Then: NO CLASH ✅
```

**Test Method**: `testTimeOverlapLogic()`
- Test all 5 scenarios
- Verify correct interval overlap algorithm

---

### TC-1.2: Semester Substitution Recurring Dates
```gherkin
Given: Substitution SEMESTER scope
Given: Start: 2026-05-18 (Monday), End: 2026-06-01
Given: Original teacher: Monday 10-11
Given: Replacement teacher: Monday 10-11

When: detectClashes(substitutionRequest)
Then: Return 3 clashing dates:
  - 2026-05-18 (Monday)
  - 2026-05-25 (Monday)
  - 2026-06-01 (Monday)
```

**Test Method**: `testSemesterRecurringClashDetection()`

---

### TC-1.3: Self-Substitution Validation
```gherkin
Given: originalTeacherId == replacementTeacherId
When: validateRequest()
Then: Throw IllegalArgumentException("Original and replacement teacher must differ")
And: HTTP 400 Bad Request returned
```

**Test Method**: `testSelfSubstitutionRejected()`

---

### TC-1.4: Inactive Teacher Substitution
```gherkin
Given: Replacement teacher has status = INACTIVE
When: substitute(request)
Then: Throw IllegalArgumentException("Teacher is inactive")
And: No database updates occur
```

**Test Method**: `testInactiveTeacherRejected()`

---

### TC-1.5: Cross-Department Substitution (Business Rule)
```gherkin
Given: Original teacher: CSE Department
Given: Replacement teacher: ME Department
When: substitute(request)
Then: Either:
  A) Allow substitution (current behavior)
  B) Reject with message
  C) Warn and require approval
```

**Test Method**: `testCrossDepartmentSubstitution()`
**Decision**: Document business rule

---

### TC-1.6: Invalid Date Range
```gherkin
Given: startDate = 2026-05-20
Given: endDate = 2026-05-18
When: validateRequest()
Then: Throw IllegalArgumentException("Start date must be on or before end date")
```

**Test Method**: `testInvalidDateRangeRejected()`

---

## TEST SUITE 2: TRANSACTIONAL INTEGRITY

### TC-2.1: Partial CSV Upload Rollback
```gherkin
Given: CSV with 5 valid rows + 1 invalid row (invalid room ID)
When: uploadTimetable(csvFile)
Then: No rows inserted (all-or-nothing)
And: Error message returned
```

**Test Method**: `testPartialCsvRollback()`

---

### TC-2.2: Concurrent Substitution Conflict
```gherkin
Given: Two TTO users simultaneously substitute same teacher
Given: version = 1 on all slots

When: User A saves substitution (success)
And: User B saves substitution (concurrent)
Then: User A: HTTP 200 (success)
And: User B: HTTP 409 (Optimistic Lock Conflict)
```

**Test Method**: `testConcurrentSubstitutionOptimisticLock()`

---

## TEST SUITE 3: AUTHORIZATION & PERMISSIONS

### TC-3.1: Student Dashboard Access
```gherkin
Given: User is STUDENT role
When: GET /exam/student/exams
Then: HTTP 200
And: Returns StudentPublishedExamDto[]
```

**Test Method**: `testStudentCanViewPublishedExams()`

---

### TC-3.2: Exam Controller Teacher List Access
```gherkin
Given: User is EXAM_CONTROLLER role
When: GET /exam/teachers
Then: HTTP 200
And: Returns TeacherListDto[]
```

**Test Method**: `testExamControllerCanFetchTeachers()`

---

## TEST SUITE 4: CSV UPLOAD EDGE CASES

### TC-4.1: Empty File Upload
```gherkin
Given: Empty CSV file
When: uploadTimetable(emptyFile)
Then: HTTP 400
And: Message: "CSV is empty"
```

**Test Method**: `testEmptyFileRejected()`

---

### TC-4.2: Missing Headers
```gherkin
Given: CSV without "room_id,teacher_id,day_of_week" columns
When: uploadTimetable(invalidHeaders)
Then: HTTP 400
And: Message: "Missing required columns"
```

**Test Method**: `testMissingHeadersRejected()`

---

### TC-4.3: Duplicate Upload Handling
```gherkin
Given: First upload succeeds (5 rows inserted)
When: Upload identical CSV again
Then: Either:
  A) Deduplicate (0 new rows)
  B) Version (5 new rows in v2)
  C) Reject (HTTP 409 Conflict)
```

**Test Method**: `testDuplicateUploadHandling()`

---

## TEST SUITE 5: PERFORMANCE

### TC-5.1: Large CSV Performance
```gherkin
Given: CSV with 500 rows
When: uploadTimetable(largeCSV)
Then: Completes in < 5 seconds
And: No memory issues
And: All validations run
```

**Test Method**: `testLargeCSVPerformance()`

---

### TC-5.2: Semester Substitution Performance
```gherkin
Given: SEMESTER substitution over 90 days
Given: 10+ teacher slots per day
When: detectClashes()
Then: Completes in < 2 seconds
```

**Test Method**: `testSemesterClashPerformance()`

---

## TEST SUITE 6: FRONTEND UI

### TC-6.1: Clash Display Rendering
```gherkin
Given: Substitution returns 3 clashes
When: User views SubstitutePage
Then: Display shows:
  - Clash count: 3
  - List of clashing slots
  - Date, time, room for each clash
  - No blank screen 🚨
```

**Test Method**: `testClashDisplayRendersCorrectly()` (E2E)

---

### TC-6.2: Teachers Dropdown Loading
```gherkin
Given: User opens ExamHallModal
When: Modal loads
Then: Teachers dropdown populated
And: No "Failed to fetch teachers" error 🚨
```

**Test Method**: `testTeachersDropdownLoads()` (E2E)

---

### TC-6.3: Student Exam List Display
```gherkin
Given: Student user logs in
When: Navigate to StudentPage
Then: Display shows published exams
And: No "You do not have permission" error 🚨
```

**Test Method**: `testStudentExamListLoads()` (E2E)

---

## TEST EXECUTION MATRIX

| Test Suite | Type | Count | Priority | Status |
|-----------|------|-------|----------|---------|
| Clash Detection | Unit | 5 | 🔴 Critical | ❌ To-Do |
| Recurring Dates | Unit | 1 | 🔴 Critical | ❌ To-Do |
| Self-Substitution | Unit | 1 | 🟠 High | ❌ To-Do |
| Inactive Teacher | Unit | 1 | 🟠 High | ❌ To-Do |
| Cross-Department | Unit | 1 | 🟡 Medium | ❌ To-Do |
| Date Validation | Unit | 1 | 🟠 High | ❌ To-Do |
| CSV Rollback | Integration | 1 | 🔴 Critical | ❌ To-Do |
| Concurrent Subs | Integration | 1 | 🟠 High | ❌ To-Do |
| Auth Student | Integration | 1 | 🔴 Critical | ❌ To-Do |
| Auth Teachers | Integration | 1 | 🔴 Critical | ❌ To-Do |
| Empty CSV | Unit | 1 | 🟡 Medium | ❌ To-Do |
| Missing Headers | Unit | 1 | 🟡 Medium | ❌ To-Do |
| Duplicate Upload | Integration | 1 | 🟡 Medium | ❌ To-Do |
| Large CSV | Performance | 1 | 🟡 Medium | ❌ To-Do |
| Semester Performance | Performance | 1 | 🟡 Medium | ❌ To-Do |
| Clash Display (E2E) | Frontend | 1 | 🔴 Critical | ❌ To-Do |
| Teachers Dropdown (E2E) | Frontend | 1 | 🔴 Critical | ❌ To-Do |
| Student Exams (E2E) | Frontend | 1 | 🔴 Critical | ❌ To-Do |

**Total Tests: 20 (Must add to codebase)**

---

## Test Implementation Files

### Backend Tests
- `SubstitutionServiceTest.java` - Add new test methods (TC-1.1 through TC-2.2)
- `TimetableUploadServiceTest.java` - Add CSV validation tests (TC-4.x, TC-5.1)
- `AuthorizationTest.java` - Add auth tests (TC-3.x)

### Frontend Tests (Playwright/Vitest)
- `substitution.spec.ts` - Add clash display tests
- `exam-control.spec.ts` - Add teachers dropdown test
- `student.spec.ts` - Add dashboard access test

---

## Manual Testing Checklist

After automation, manual verify:
- [ ] Substitution with no clashes updates database
- [ ] Substitution with clashes does NOT update database
- [ ] Student can see published exams on mobile
- [ ] Exam controller sees complete teachers list
- [ ] TTO can complete semester substitution without error
- [ ] Error messages are clear and actionable

---
