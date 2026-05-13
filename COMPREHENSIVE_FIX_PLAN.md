# COMPREHENSIVE FIX PLAN: SUBSTITUTION, EXAM CONTROL & STUDENT DASHBOARD

## Executive Summary
Three main issues identified with root causes and 15+ edge case gaps. This plan provides prioritized fixes and comprehensive test coverage.

---

# ISSUE 1: TEACHER SUBSTITUTION FAILURES

## 1.1 Semester Substitution Clash Detection is Broken ❌

### Problem
- Current logic only checks `day_of_week` directly from timetable slots
- **Does NOT expand recurring weekly slots across the date range**
- Example: Teacher A has Monday 10-11, replacement has Monday 10-11
  - ONE_DAY (May 18) ✅ Correctly detects clash
  - SEMESTER (May 18-June 1) ❌ Reports 0 clashes (should report 3: May 18, 25, June 1)

### Root Cause
Location: [SubstitutionService.java](backend/src/main/java/com/rvce/scas/service/SubstitutionService.java#L146)

```java
public List<ClashDetail> clashCheck(List<TimetableSlot> slots, SubstituteRequest req) {
    // Only checks slot.getDayOfWeek() directly
    // Does NOT iterate through dates to find matching weekdays
    boolean hasConflict = slotRepository.existsConflictingSlot(
        req.getReplacementTeacherId(),
        slot.getDayOfWeek(),  // <-- Checks only this
        slot.getStartTime(),
        slot.getEndTime()
    );
}
```

### Fix Required
Modify clash detection to:
1. For SEMESTER scope: Iterate through every date in startDate..endDate
2. For each date, check if replacement teacher has conflicting slots on matching weekday
3. Accumulate all clashing dates
4. Populate clash details with actual dates

---

## 1.2 Frontend Blank Screen During Conflict Display 🚨

### Problem
- After creating a substitution with clashes, frontend shows blank screen
- UI should display clash details but doesn't

### Suspected Cause
[SubstitutionPage.tsx](frontend/src/pages/SubstitutePage.tsx#L132) - Clash rendering may have:
- Missing error handling
- State update issues
- API response format mismatch

### Fix Required
- Verify response DTO structure matches frontend expectations
- Add proper error boundaries
- Test clash display with actual conflict data

---

## 1.3 "Reassigned slots" Show Empty in Summary ❌

### Problem
- After successful substitution, summary shows:
  ```
  Reassigned slots:
  
  Clashes detected: 0
  ```
- Should list the actual reassigned slot details

### Root Cause
Likely in response DTO or frontend rendering. ClashDetail needs complete information:
- Date
- Room name (not just ID)
- Teacher involved
- Time slot

### Fix Required
- Ensure ClashDetail DTO contains all required fields
- Frontend must format and display reassigned slots list

---

## 1.4 Timetable Display Lacks Readability 📋

### Problem
- Timetable shows teacher IDs instead of names/emails
- Timetable shows room IDs instead of room names
- Makes timetable hard to read and debug

### Fix Required
- Modify TimetableSlot or API response to include:
  - `teacher.name` and `teacher.email`
  - `room.name`
- Update frontend to display human-readable information

---

# ISSUE 2: EXAM CONTROL - "Failed to Fetch Teachers"

## 2.1 Problem
- Opening "Add New Hall" modal shows "Failed to fetch teachers"
- No invigilator/teacher dropdown appears

## 2.2 Root Cause Analysis

Two `/teachers` endpoints exist:
1. **`/api/timetable/teachers`** ✅ 
   - Requires: `hasAnyRole('TTO','ADMIN','SUPER_ADMIN','DEPT_COORD','EXAM_CONTROLLER')`
   - Works for exam coordinators

2. **`/api/exam/teachers`** ⚠️
   - Requires: `hasRole('ADMIN') or hasRole('DEPT_COORD')`
   - More restrictive
   - Returns TeacherListDto (more detailed)

Frontend calls: `${API_BASE}/teachers` 
- Which endpoint gets called? Depends on controller registration order
- **Issue**: If EXAM_CONTROLLER role isn't mapped to ADMIN/DEPT_COORD, they can't fetch exam teachers

## 2.3 Fix Required

**Option A (Recommended): Unify endpoints**
```java
// In TimetableController or ExamHallController
@GetMapping("/teachers")
@PreAuthorize("hasAnyRole('TTO','ADMIN','SUPER_ADMIN','DEPT_COORD','EXAM_CONTROLLER')")
public ResponseEntity<List<TeacherListDto>> listTeachers() {
    // Return consistent DTO with: id, name, email, department
}
```

**Option B: Fix routing**
- Ensure frontend calls explicit `/exam/teachers` endpoint
- Verify response DTO includes required fields

---

# ISSUE 3: STUDENT DASHBOARD - "You Do Not Have Permission"

## 3.1 Problem
- Students can't view published seating assignments
- Endpoint returns 403 Forbidden with "insufficient permissions" message

## 3.2 Root Cause

[ExamController.java](backend/src/main/java/com/rvce/scas/controller/ExamController.java#L179)

```java
@GetMapping("/student/exams")
@PreAuthorize("hasRole('STUDENT')")  // <-- PROBLEM
public ResponseEntity<List<StudentPublishedExamDto>> getStudentPublishedExams(Authentication authentication) {
    // ...
}
```

**Issue**: Authorization checks for `STUDENT` role, but users may have:
- `EXAM_READ` permission
- `NOTIFICATIONS_READ` permission
- But NOT explicit `STUDENT` role

Real user roles: `STUDENT`, `TEACHER`, `ADMIN`, `DEPT_COORD`, `TTO`, `EXAM_CONTROLLER`

Students likely don't have a dedicated "STUDENT" role in UserRole table.

## 3.3 Fix Required

Change from role-based to permission-based or verify role mapping:

```java
@GetMapping("/student/exams")
@PreAuthorize("hasRole('STUDENT')")  // OR add students to STUDENT role
// OR: @PreAuthorize("hasAnyRole('STUDENT','ADMIN') or hasPermission(#authentication, 'EXAM_READ')")
public ResponseEntity<List<StudentPublishedExamDto>> getStudentPublishedExams(Authentication authentication) {
```

**Action**: Either:
1. Assign STUDENT role to all student users during user creation
2. Change authorization to check permissions instead of roles
3. Update role check to include any appropriate role

---

# ISSUE 4: MISSING EDGE CASE COVERAGE 🔥

### Currently Tested ✅
- CSV parsing
- Room validation
- Teacher validation  
- Duplicate room occupancy
- One-day substitution
- Basic clash detection
- Semester substitution (partially buggy)

### NOT Tested ❌

#### 1. Time Overlap Edge Cases
| Slot A | Slot B      | Expected |
|--------|-----------|----------|
| 10–11  | 10:30–11:30 | **CLASH** ⚠️ |
| 10–11  | 11–12       | NO clash |
| 10–11  | 9–10        | NO clash |
| 10–11  | 9:30–10:15  | **CLASH** ⚠️ |

**Risk**: Current overlap logic may be too simplistic (only checks `==`)

#### 2. Self-Substitution  
- Input: Original=Ramesh, Replacement=Ramesh
- Expected: ❌ REJECT
- Risk: System may allow meaningless substitution

#### 3. Cross-Department Substitution
- CSE teacher → ME teacher substitution
- Risk: Business rule not enforced

#### 4. Inactive/Deleted Teacher Handling
- Substituting with deleted teacher
- Expected: ❌ REJECT

#### 5. Invalid Date Ranges
- Start: May 20, End: May 18
- Expected: ❌ Validation error

#### 6. Duplicate Upload Handling
- Upload same timetable CSV twice
- Expected: Either prevent or version properly

#### 7. Transactional Integrity
- CSV with 5 valid + 1 invalid row
- Expected: ❌ 0 rows inserted (not 5)
- Risk: Partial inserts corrupt data

#### 8. Concurrent Substitution
- Two TTO users substitute same teacher simultaneously
- Expected: One succeeds, one gets 409 Conflict

#### 9. Substitution Reversal
- After substitution, can it be undone?
- Expected: Clean rollback

#### 10. Audit Trail
- After substitution, can admin see who/when/what changed?
- Expected: Complete audit log

#### 11. Empty/Malformed CSV
- Blank file, missing columns, wrong headers
- Expected: Clean validation errors

#### 12. Large CSV Performance
- 500-1000 row uploads
- Expected: Fast processing, no OOM

---

# IMPLEMENTATION PRIORITY

## Phase 1: CRITICAL FIXES (Do First) 🚨
1. **Fix semester substitution clash detection** (affects correctness)
2. **Fix student role authorization** (blocks users)
3. **Fix exam teachers endpoint** (blocks UI)
4. **Verify clash display on frontend** (UX issue)

## Phase 2: IMPORTANT FIXES 📌
5. Make timetable readable (teacher/room names)
6. Add comprehensive edge case tests
7. Fix overlap algorithm if incorrect
8. Add self-substitution validation

## Phase 3: NICE-TO-HAVE IMPROVEMENTS 💡
9. Audit trail implementation
10. Performance optimization
11. Substitution reversal feature
12. Advanced business rule enforcement

---

# FILES TO MODIFY

## Backend Java
- `SubstitutionService.java` - Fix clash detection logic
- `ExamController.java` - Fix student authorization  
- `ExamHallController.java` - Fix teachers endpoint
- `TimetableController.java` - Verify teachers endpoint
- Entity/DTO classes - Add fields for readability
- Test files - Add edge case tests

## Frontend TypeScript/React
- `SubstitutePage.tsx` - Fix clash display, add loading states
- `ExamHallModal.tsx` - Fix teachers fetch error handling
- `StudentPage.tsx` - Verify error handling
- Service files - Add proper error boundaries

---

# SUCCESS CRITERIA

✅ Semester substitution correctly detects recurring clashes
✅ Students can view published exams
✅ Exam controllers can add invigilators
✅ UI displays substitution results cleanly
✅ Timetable is human-readable
✅ All 15+ edge cases have passing tests
✅ No 403 errors for authorized users
✅ No blank screens during normal operation

---

# ESTIMATED EFFORT
- Phase 1 (Critical): 4-6 hours
- Phase 2 (Important): 6-8 hours
- Phase 3 (Nice-to-have): 8-10 hours
- Testing: 4-6 hours

**Total: 22-30 hours** (depending on hidden issues)

---

# NEXT STEPS
1. ✅ Review this plan
2. ⏳ Approve changes
3. 🔧 Implement Phase 1 fixes
4. ✔️ Run tests
5. 📋 Repeat for Phase 2, 3

---
