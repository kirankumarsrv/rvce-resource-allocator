# QUICK REFERENCE: ISSUES & SOLUTIONS

## 🔴 CRITICAL ISSUES (Must Fix Immediately)

### Issue #1: Semester Substitution Clash Detection
**Symptom**: Reporting 0 clashes when there should be many
**Location**: `SubstitutionService.java` line 146
**Fix**: Expand clash detection to iterate through date range
**Impact**: Data correctness - substitutions can create schedule conflicts
**Time**: ~2 hours

---

### Issue #2: Student Authorization
**Symptom**: "You do not have permission to perform this action" 
**Location**: `ExamController.java` line 179
**Requires**: `hasRole('STUDENT')`
**Problem**: Students don't have STUDENT role
**Fix**: Add STUDENT role to student users OR change authorization to permission-based
**Impact**: Blocks all students from viewing exams
**Time**: ~1 hour

---

### Issue #3: Exam Teachers Dropdown
**Symptom**: "Failed to fetch teachers" in Add New Hall modal
**Location**: Either `TimetableController.java:112` or `AdminController.java:91`
**Problem**: Role mismatch - EXAM_CONTROLLER can't access `/exam/teachers` endpoint
**Fix**: Unify endpoints or adjust role requirements
**Impact**: Blocks exam hall configuration
**Time**: ~1 hour

---

### Issue #4: Frontend Blank Screen
**Symptom**: Blank screen after clash detection during substitution
**Location**: `SubstitutePage.tsx`
**Problem**: Clash display rendering fails or incomplete data structure
**Fix**: Verify response DTO, add error boundaries, test clash rendering
**Impact**: UX - confuses users during normal operation
**Time**: ~1 hour

---

## 🟠 IMPORTANT FIXES (High Priority)

### Issue #5: Timetable Readability
**Problem**: Shows IDs instead of human-readable names
**Fix**: Include teacher names/emails and room names in responses
**Impact**: Debugging and user experience
**Time**: ~2 hours

---

### Issue #6: Missing Edge Case Tests
**Problem**: 15+ edge cases not covered, high risk of bugs
**Tests Needed**:
- Time overlap algorithm verification
- Self-substitution prevention
- Invalid date range validation
- CSV rollback integrity
- Concurrent substitution handling
- Cross-department business rules
- Inactive teacher checks

**Impact**: System reliability
**Time**: ~6-8 hours

---

## SUMMARY TABLE

| # | Issue | Severity | Component | Fix Time | Blocked By |
|---|-------|----------|-----------|----------|-----------|
| 1 | Semester clash detection | 🔴 Critical | Backend | 2h | Nothing |
| 2 | Student authorization | 🔴 Critical | Backend | 1h | Nothing |
| 3 | Teachers dropdown 403 | 🔴 Critical | Backend | 1h | Nothing |
| 4 | Blank screen on clashes | 🔴 Critical | Frontend | 1h | Issue #1 |
| 5 | Timetable readability | 🟠 High | Backend | 2h | Nothing |
| 6 | Edge case tests | 🟠 High | Tests | 6h | Issues 1-5 |

**Total Critical Path**: 5 hours (Issues 1-4 can run in parallel)
**Total All Fixes**: ~13-15 hours

---

## APPROVAL CHECKLIST

Please confirm:

- [ ] Understand Issue #1 (Semester clash detection broken)
- [ ] Understand Issue #2 (Student 403 error)
- [ ] Understand Issue #3 (Teachers dropdown)
- [ ] Understand Issue #4 (Blank screen UX)
- [ ] Approve Phase 1 critical fixes
- [ ] Want comprehensive edge case tests

---

## NEXT ACTIONS (After Approval)

1. Fix backend issues (1-3) in parallel
2. Deploy and test
3. Fix frontend (4)
4. Improve readability (5)
5. Add comprehensive tests (6)

**Estimated Completion**: 1-2 days (depending on deployment cycles)

---
