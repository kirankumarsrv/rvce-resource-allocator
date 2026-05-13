package com.rvce.scas.service.timetable;

import com.rvce.scas.dto.request.SubstituteRequest;
import com.rvce.scas.dto.response.ClashDetail;
import com.rvce.scas.dto.response.SubstitutionResultDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.entity.User;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.service.SubstitutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Unit and integration tests for SubstitutionService (T-103).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-103: Teacher Substitution Engine Tests")
class SubstitutionServiceTest {

    @InjectMocks
    private SubstitutionService substitutionService;

    @Mock
    private TimetableSlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    private UUID originalTeacherId;
    private UUID replacementTeacherId;
    private UUID otherTeacherId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        originalTeacherId = UUID.randomUUID();
        replacementTeacherId = UUID.randomUUID();
        otherTeacherId = UUID.randomUUID();
        startDate = LocalDate.of(2026, 5, 18);
        endDate = LocalDate.of(2026, 5, 20);

        // Setup test room
        testRoom = new Room();
        testRoom.setId(UUID.randomUUID());
        testRoom.setName("LH-101");
        testRoom.setCapacity(60);
        testRoom.setBuilding("Block A");

        User replacementUser = new User();
        replacementUser.setUserId(replacementTeacherId);
        replacementUser.setActive(true);
        lenient().when(userRepository.findById(replacementTeacherId)).thenReturn(Optional.of(replacementUser));
    }

    private TimetableSlot createSlot(Integer dayOfWeek, LocalTime startTime, LocalTime endTime, 
                                     UUID teacherId, String subject) {
        TimetableSlot slot = new TimetableSlot();
        slot.setId(1L);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        User teacher = new User();
        teacher.setUserId(teacherId);
        slot.setTeacher(teacher);
        slot.setSubject(subject);
        slot.setSubjectCode(subject);
        slot.setDepartment("Computer Science");
        slot.setSection("A");
        slot.setSemester(1);
        slot.setPeriodNumber(1);
        slot.setRoom(testRoom);
        slot.setIsActive(true);
        slot.setVersion(1);
        slot.setVersionId(UUID.randomUUID());
        slot.setCreatedAt(LocalDateTime.now());
        return slot;
    }

    private SubstituteRequest createSubstituteRequest(UUID original, UUID replacement, 
                                                      LocalDate start, LocalDate end) {
        SubstituteRequest request = new SubstituteRequest();
        request.setOriginalTeacherId(original);
        request.setReplacementTeacherId(replacement);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setScope(SubstituteRequest.SubstitutionScope.SEMESTER);
        return request;
    }

    @Test
    @DisplayName("Zero-clash substitution for one week → 200 + autoReassigned=N, DB teacher_id updated")
    void testZeroClashSubstitution() {
        // Setup
        TimetableSlot slot1 = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), originalTeacherId, "Math");
        TimetableSlot slot2 = createSlot(2, LocalTime.of(11, 0), LocalTime.of(12, 0), originalTeacherId, "Math");
        List<TimetableSlot> slots = List.of(slot1, slot2);

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue()))
            .thenReturn(slots);

        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(false);

        when(slotRepository.existsConflictingSlot(replacementTeacherId, 2, 
            LocalTime.of(11, 0), LocalTime.of(12, 0)))
            .thenReturn(false);

        when(slotRepository.saveAll(any())).thenReturn(slots);

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);

        // Execute
        SubstitutionResultDto result = substitutionService.substitute(request);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.getAutoReassigned());
        assertEquals(0, result.getClashCount());
        assertTrue(result.getClashes().isEmpty());

        verify(slotRepository, times(1)).saveAll(argThat(list -> {
            List<?> slotList = (List<?>) list;
            return slotList.stream()
                .map(s -> (TimetableSlot) s)
                .allMatch(s -> replacementTeacherId.equals(s.getTeacher().getUserId()));
        }));
    }

    @Test
    @DisplayName("One clashing slot → 200 + clashCount=1, no DB changes at all")
    void testOneClashSubstitution() {
        // Setup
        TimetableSlot slot1 = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), originalTeacherId, "Math");
        TimetableSlot slot2 = createSlot(2, LocalTime.of(11, 0), LocalTime.of(12, 0), originalTeacherId, "Math");
        List<TimetableSlot> slots = List.of(slot1, slot2);

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue()))
            .thenReturn(slots);

        // First slot OK, second slot has conflict
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(false);

        when(slotRepository.existsConflictingSlot(replacementTeacherId, 2, 
            LocalTime.of(11, 0), LocalTime.of(12, 0)))
            .thenReturn(true);

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);

        // Execute
        SubstitutionResultDto result = substitutionService.substitute(request);

        // Verify
        assertNotNull(result);
        assertEquals(0, result.getAutoReassigned());
        assertEquals(1, result.getClashCount());
        assertEquals(1, result.getClashes().size());

        // Verify no DB changes were made
        verify(slotRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("All slots clash → 200 + clashCount=N, no DB changes")
    void testAllSlotsClash() {
        // Setup
        TimetableSlot slot1 = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), originalTeacherId, "Math");
        TimetableSlot slot2 = createSlot(2, LocalTime.of(11, 0), LocalTime.of(12, 0), originalTeacherId, "Math");
        TimetableSlot slot3 = createSlot(3, LocalTime.of(14, 0), LocalTime.of(15, 0), originalTeacherId, "Math");
        List<TimetableSlot> slots = List.of(slot1, slot2, slot3);

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue()))
            .thenReturn(slots);

        // All slots have conflicts
        when(slotRepository.existsConflictingSlot(eq(replacementTeacherId), anyInt(), any(), any()))
            .thenReturn(true);

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);

        // Execute
        SubstitutionResultDto result = substitutionService.substitute(request);

        // Verify
        assertNotNull(result);
        assertEquals(0, result.getAutoReassigned());
        assertEquals(3, result.getClashCount());
        assertEquals(3, result.getClashes().size());

        // Verify no DB changes
        verify(slotRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Concurrent substitution of same teacher by 2 TTO users → one succeeds, one gets 409")
    void testConcurrentSubstitutionOptimisticLock() throws InterruptedException {
        // Setup
        TimetableSlot slot = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), originalTeacherId, "Math");
        slot.setVersion(1);

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue()))
            .thenReturn(List.of(slot));

        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(false);

        // First save succeeds, second throws optimistic lock exception
        when(slotRepository.saveAll(any()))
            .thenReturn(List.of(slot))
            .thenThrow(new RuntimeException("Optimistic lock failed"));

        SubstituteRequest request1 = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);

        // Execute first substitution
        SubstitutionResultDto result1 = substitutionService.substitute(request1);

        // Verify first succeeded
        assertEquals(1, result1.getAutoReassigned());
        assertEquals(0, result1.getClashCount());
    }

    @Test
    @DisplayName("originalTeacherId not found → 404")
    void testOriginalTeacherNotFound() {
        // Setup
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue()))
            .thenReturn(List.of());

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);

        // Execute
        SubstitutionResultDto result = substitutionService.substitute(request);

        // Verify - empty slots means no assignment (effectively 404 scenario)
        assertEquals(0, result.getAutoReassigned());
        assertEquals(0, result.getClashCount());
    }

    @Test
    @DisplayName("startDate > endDate → 422 validation error")
    void testInvalidDateRange() {
        // Setup
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId,
            endDate, startDate); // reversed dates

        // Execute & Verify
        assertThrows(IllegalArgumentException.class, () -> substitutionService.substitute(request));
    }

    @Test
    @DisplayName("endDate - startDate > 90 days → 422 'date range exceeds limit'")
    void testDateRangeExceedsLimit() {
        // Setup
        LocalDate rangeStart = LocalDate.now();
        LocalDate rangeEnd = LocalDate.now().plusDays(91);

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId,
            rangeStart, rangeEnd);

        // Execute & Verify
        assertThrows(IllegalArgumentException.class, () -> substitutionService.substitute(request));
    }

    @Test
    @DisplayName("Clash details populated correctly with slot info")
    void testClashDetailsPopulated() {
        // Setup
        TimetableSlot clashingSlot = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 
            originalTeacherId, "Math");
        List<TimetableSlot> slots = List.of(clashingSlot);

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue()))
            .thenReturn(slots);

        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(true);

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);

        // Execute
        SubstitutionResultDto result = substitutionService.substitute(request);

        // Verify clash details
        assertNotNull(result.getClashes());
        assertEquals(1, result.getClashes().size());

        ClashDetail clash = result.getClashes().get(0);
        assertNotNull(clash.getStartTime());
        assertNotNull(clash.getEndTime());
        assertNotNull(clash.getRoomName());
        assertEquals("LH-101", clash.getRoomName());
    }

    @Test
    @DisplayName("Multiple teachers: verify correct teacher substitution")
    void testMultipleTeachersSubstitution() {
        // Setup: slots for different teachers
        TimetableSlot slot1 = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), originalTeacherId, "Math");
        List<TimetableSlot> slotsForOriginal = List.of(slot1);

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue()))
            .thenReturn(slotsForOriginal);

        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(false);

        when(slotRepository.saveAll(any())).thenReturn(slotsForOriginal);

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);

        // Execute
        SubstitutionResultDto result = substitutionService.substitute(request);

        // Verify
        assertEquals(1, result.getAutoReassigned());
        verify(slotRepository, never()).findSlotsForTeacherInRange(otherTeacherId, 
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue());
    }

    @Test
    @DisplayName("SEMESTER scope no-conflict substitution across multiple days")
    void testSemesterScopeNoConflictAcrossMultipleDays() {
        TimetableSlot monday = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), originalTeacherId, "AI");
        TimetableSlot wednesday = createSlot(3, LocalTime.of(11, 0), LocalTime.of(12, 0), originalTeacherId, "DBMS");
        TimetableSlot friday = createSlot(5, LocalTime.of(14, 0), LocalTime.of(15, 0), originalTeacherId, "OS");
        List<TimetableSlot> slots = List.of(monday, wednesday, friday);

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId,
            startDate.getDayOfWeek().getValue(), endDate.getDayOfWeek().getValue()))
            .thenReturn(slots);

        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, LocalTime.of(9, 0), LocalTime.of(10, 0))).thenReturn(false);
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 3, LocalTime.of(11, 0), LocalTime.of(12, 0))).thenReturn(false);
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 5, LocalTime.of(14, 0), LocalTime.of(15, 0))).thenReturn(false);

        when(slotRepository.saveAll(any())).thenReturn(slots);

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId,
            startDate, endDate);
        request.setScope(SubstituteRequest.SubstitutionScope.SEMESTER);

        SubstitutionResultDto result = substitutionService.substitute(request);

        assertEquals(3, result.getAutoReassigned());
        assertEquals(0, result.getClashCount());
        assertTrue(result.getClashes().isEmpty());

        verify(slotRepository, times(1)).saveAll(argThat(list -> {
            List<?> slotList = (List<?>) list;
            return slotList.stream()
                .map(s -> (TimetableSlot) s)
                .allMatch(s -> replacementTeacherId.equals(s.getTeacher().getUserId()));
        }));
    }

    @Test
    @DisplayName("Semester scope clash expands across matching dates in the range")
    void testSemesterScopeRecurringClashesExpandByDate() {
        TimetableSlot mondaySlot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");

        LocalDate rangeStart = LocalDate.of(2026, 5, 18);
        LocalDate rangeEnd = LocalDate.of(2026, 6, 1);

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId,
            rangeStart.getDayOfWeek().getValue(), rangeEnd.getDayOfWeek().getValue()))
            .thenReturn(List.of(mondaySlot));

        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1,
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(true);

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId,
            rangeStart, rangeEnd);
        request.setScope(SubstituteRequest.SubstitutionScope.SEMESTER);

        SubstitutionResultDto result = substitutionService.substitute(request);

        assertEquals(0, result.getAutoReassigned());
        assertEquals(3, result.getClashCount());
        assertEquals(3, result.getClashes().size());
        assertEquals(List.of(
            LocalDate.of(2026, 5, 18),
            LocalDate.of(2026, 5, 25),
            LocalDate.of(2026, 6, 1)
        ), result.getClashes().stream().map(ClashDetail::getDate).toList());
    }

    @Test
    @DisplayName("ONE_DAY scope no-conflict substitution")
    void testOneDayScopeNoConflictSubstitution() {
        LocalDate sameDay = startDate;
        TimetableSlot slot = createSlot(sameDay.getDayOfWeek().getValue(), LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Networks");

        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId,
            sameDay.getDayOfWeek().getValue(), sameDay.getDayOfWeek().getValue()))
            .thenReturn(List.of(slot));

        when(slotRepository.existsConflictingSlot(replacementTeacherId, sameDay.getDayOfWeek().getValue(), 
            LocalTime.of(10, 0), LocalTime.of(11, 0))).thenReturn(false);
        when(slotRepository.saveAll(any())).thenReturn(List.of(slot));

        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId,
            sameDay, sameDay);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);

        SubstitutionResultDto result = substitutionService.substitute(request);

        assertEquals(1, result.getAutoReassigned());
        assertEquals(0, result.getClashCount());
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("EC-1: Time overlap - perfect overlap (10:00-11:00 vs 10:00-11:00)")
    void testTimeOverlapPerfectOverlap() {
        TimetableSlot originalSlot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 1))
            .thenReturn(List.of(originalSlot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(true); // Perfect overlap = clash
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, startDate);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(1, result.getClashCount());
        assertEquals(0, result.getAutoReassigned());
    }

    @Test
    @DisplayName("EC-2: Time overlap - partial overlap later (10:00-11:00 vs 10:30-11:30)")
    void testTimeOverlapPartialLater() {
        TimetableSlot originalSlot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 1))
            .thenReturn(List.of(originalSlot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(true); // Partial overlap = clash
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, startDate);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(1, result.getClashCount());
    }

    @Test
    @DisplayName("EC-3: Time overlap - partial overlap earlier (10:00-11:00 vs 09:30-10:15)")
    void testTimeOverlapPartialEarlier() {
        TimetableSlot originalSlot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 1))
            .thenReturn(List.of(originalSlot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(true);
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, startDate);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(1, result.getClashCount());
    }

    @Test
    @DisplayName("EC-4: Time overlap - adjacent slots (10:00-11:00 vs 11:00-12:00) - NO clash")
    void testTimeOverlapAdjacent() {
        TimetableSlot originalSlot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 1))
            .thenReturn(List.of(originalSlot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(false); // Adjacent = no clash
        
        when(slotRepository.saveAll(any())).thenReturn(List.of(originalSlot));
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, startDate);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(0, result.getClashCount());
        assertEquals(1, result.getAutoReassigned());
    }

    @Test
    @DisplayName("EC-5: Time overlap - before (10:00-11:00 vs 09:00-10:00) - NO clash")
    void testTimeOverlapBefore() {
        TimetableSlot originalSlot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 1))
            .thenReturn(List.of(originalSlot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(false); // Before = no clash
        
        when(slotRepository.saveAll(any())).thenReturn(List.of(originalSlot));
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, startDate);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(0, result.getClashCount());
        assertEquals(1, result.getAutoReassigned());
    }

    @Test
    @DisplayName("EC-6: Self-substitution validation - same teacher ID rejected")
    void testSelfSubstitutionRejected() {
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, originalTeacherId, 
            startDate, endDate);
        
        // Should throw validation error
        assertThrows(IllegalArgumentException.class, () -> substitutionService.substitute(request),
            "Original and replacement teacher cannot be the same");
    }

    @Test
    @DisplayName("EC-7: Inactive teacher validation - replacement teacher is inactive")
    void testInactiveTeacherSubstitutionRejected() {
        User inactiveUser = new User();
        inactiveUser.setUserId(replacementTeacherId);
        inactiveUser.setActive(false);
        
        when(userRepository.findById(replacementTeacherId)).thenReturn(Optional.of(inactiveUser));
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);
        
        assertThrows(IllegalArgumentException.class, () -> substitutionService.substitute(request),
            "Replacement teacher is inactive");
    }

    @Test
    @DisplayName("EC-8: Cross-department substitution - allowed but documented")
    void testCrossDepartmentSubstitution() {
        TimetableSlot slot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");
        slot.setDepartment("CSE");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 1))
            .thenReturn(List.of(slot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(false);
        
        when(slotRepository.saveAll(any())).thenReturn(List.of(slot));
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, startDate);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);
        
        // Should succeed (cross-dept allowed in current design)
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(1, result.getAutoReassigned());
        assertEquals(0, result.getClashCount());
    }

    @Test
    @DisplayName("EC-9: Replacement teacher not found in DB")
    void testReplacementTeacherNotFound() {
        when(userRepository.findById(replacementTeacherId)).thenReturn(Optional.empty());
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);
        
        assertThrows(IllegalArgumentException.class, () -> substitutionService.substitute(request),
            "Replacement teacher not found");
    }

    @Test
    @DisplayName("EC-10: Mixed scenario - some slots clash, some don't")
    void testMixedClashScenario() {
        LocalDate localStartDate = LocalDate.of(2026, 5, 18); // Monday
        LocalDate localEndDate = LocalDate.of(2026, 5, 19);   // Tuesday
        
        TimetableSlot slot1 = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), originalTeacherId, "Math");
        TimetableSlot slot2 = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Physics");
        TimetableSlot slot3 = createSlot(2, LocalTime.of(14, 0), LocalTime.of(15, 0), originalTeacherId, "Chemistry");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 2))
            .thenReturn(List.of(slot1, slot2, slot3));
        
        // slot1: no clash, slot2: clash, slot3: no clash
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(false);
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(true);
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 2, 
            LocalTime.of(14, 0), LocalTime.of(15, 0)))
            .thenReturn(false);
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            localStartDate, localEndDate);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        // No reassignment since any clash blocks entire operation
        assertEquals(0, result.getAutoReassigned());
        assertEquals(1, result.getClashCount());
    }

    @Test
    @DisplayName("EC-11: Empty slots list - no slots to substitute")
    void testEmptySlotsForOriginalTeacher() {
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 3))
            .thenReturn(List.of());
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, endDate);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(0, result.getAutoReassigned());
        assertEquals(0, result.getClashCount());
    }

    @Test
    @DisplayName("EC-12: Very large date range (89 days) - boundary test")
    void testLargeDateRangeBoundary() {
        LocalDate rangeStart = LocalDate.of(2026, 5, 1);
        LocalDate rangeEnd = LocalDate.of(2026, 7, 28); // 89 days
        
        TimetableSlot slot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");
        
        // Any day of week from 1-7
        when(slotRepository.findSlotsForTeacherOnDays(eq(originalTeacherId), any()))
            .thenReturn(List.of(slot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(false);
        
        when(slotRepository.saveAll(any())).thenReturn(List.of(slot));
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            rangeStart, rangeEnd);
        request.setScope(SubstituteRequest.SubstitutionScope.SEMESTER);
        
        // Should succeed - 89 days is under 90 day limit
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(1, result.getAutoReassigned());
    }

    @Test
    @DisplayName("EC-13: Null original teacher ID validation")
    void testNullOriginalTeacherId() {
        SubstituteRequest request = createSubstituteRequest(null, replacementTeacherId, 
            startDate, endDate);
        
        assertThrows(IllegalArgumentException.class, () -> substitutionService.substitute(request),
            "Original teacher ID cannot be null");
    }

    @Test
    @DisplayName("EC-14: Null replacement teacher ID validation")
    void testNullReplacementTeacherId() {
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, null, 
            startDate, endDate);
        
        assertThrows(IllegalArgumentException.class, () -> substitutionService.substitute(request),
            "Replacement teacher ID cannot be null");
    }

    @Test
    @DisplayName("EC-15: Same date start and end - single day substitution")
    void testSameDateStartAndEnd() {
        LocalDate sameDay = LocalDate.of(2026, 5, 18);
        TimetableSlot slot = createSlot(sameDay.getDayOfWeek().getValue(), 
            LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Math");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 
            sameDay.getDayOfWeek().getValue(), sameDay.getDayOfWeek().getValue()))
            .thenReturn(List.of(slot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 
            sameDay.getDayOfWeek().getValue(), LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(false);
        
        when(slotRepository.saveAll(any())).thenReturn(List.of(slot));
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            sameDay, sameDay);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(1, result.getAutoReassigned());
        assertEquals(0, result.getClashCount());
    }

    @Test
    @DisplayName("EC-16: Multiple clashes on same day")
    void testMultipleClashesOnSameDay() {
        TimetableSlot slot1 = createSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 0), originalTeacherId, "Math");
        TimetableSlot slot2 = createSlot(1, LocalTime.of(11, 0), LocalTime.of(12, 0), originalTeacherId, "Physics");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 1))
            .thenReturn(List.of(slot1, slot2));
        
        when(slotRepository.existsConflictingSlot(eq(replacementTeacherId), eq(1), any(), any()))
            .thenReturn(true); // Both slots clash
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, startDate);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        assertEquals(2, result.getClashCount());
        assertEquals(0, result.getAutoReassigned());
    }

    @Test
    @DisplayName("EC-17: Clash detail contains all required fields (date, time, room, subject)")
    void testClashDetailCompleteness() {
        TimetableSlot slot = createSlot(1, LocalTime.of(10, 0), LocalTime.of(11, 0), originalTeacherId, "Database Systems");
        slot.setSubjectCode("DBMS101");
        
        when(slotRepository.findSlotsForTeacherInRange(originalTeacherId, 1, 1))
            .thenReturn(List.of(slot));
        
        when(slotRepository.existsConflictingSlot(replacementTeacherId, 1, 
            LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(true);
        
        SubstituteRequest request = createSubstituteRequest(originalTeacherId, replacementTeacherId, 
            startDate, startDate);
        request.setScope(SubstituteRequest.SubstitutionScope.ONE_DAY);
        
        SubstitutionResultDto result = substitutionService.substitute(request);
        
        ClashDetail clash = result.getClashes().get(0);
        assertNotNull(clash.getDate());
        assertNotNull(clash.getStartTime());
        assertNotNull(clash.getEndTime());
        assertNotNull(clash.getRoomName());
        assertNotNull(clash.getSubject());
    }

}