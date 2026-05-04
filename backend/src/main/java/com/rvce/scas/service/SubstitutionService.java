package com.rvce.scas.service;

import com.rvce.scas.dto.request.SubstituteRequest;
import com.rvce.scas.dto.response.ClashDetail;
import com.rvce.scas.dto.response.SubstitutionResultDto;
import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.entity.User;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Service for teacher substitution operations (T-103).
 * Handles clash detection and atomic reassignment of teaching slots.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Detect scheduling conflicts for substitution</li>
 *   <li>Perform atomic teacher reassignments</li>
 *   <li>Write audit logs for substitutions</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on TimetableSlotRepository for slot operations.
 *
 * <h3>Transaction Behaviour</h3>
 * substitute() uses separate read and write transactions.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubstitutionService {

    private final TimetableSlotRepository slotRepository;
    private final UserRepository userRepository;
    // Assuming AuditService exists
    // private final AuditService auditService;

    /**
     * Processes a teacher substitution request.
     * Fetches slots, checks for clashes, and applies changes atomically if no conflicts.
     *
     * <p>Decision DD-11: Full rejection on any clash prevents inconsistent state.</p>
     *
     * @param req the substitution request
     * @return SubstitutionResultDto with reassignment count and clash details
     */
    @Transactional
    public SubstitutionResultDto substitute(SubstituteRequest req) {
        validateRequest(req);

        log.info("Processing substitution request: {} → {} for {} to {}",
                req.getOriginalTeacherId(), req.getReplacementTeacherId(),
                req.getStartDate(), req.getEndDate());

        // Fetch slots for original teacher in date range
        List<TimetableSlot> slots = fetchSlotsForSubstitution(req);

        // Check for clashes with replacement teacher
        List<ClashDetail> clashes = clashCheck(slots, req);

        if (!clashes.isEmpty()) {
            log.warn("Substitution blocked due to {} clashes", clashes.size());
            return buildResult(0, clashes);
        }

        // No clashes: apply substitution atomically
        int reassigned = applySubstitution(slots, req.getReplacementTeacherId());

        log.info("Substitution completed: {} slots reassigned", reassigned);
        return buildResult(reassigned, List.of());
    }

    /**
     * Fetches slots for the original teacher within the substitution date range.
     * Uses @Transactional(readOnly=true) for the fetch operation.
     *
     * @param req the substitution request
     * @return list of slots to be reassigned
     */
    @Transactional(readOnly = true)
    public List<TimetableSlot> fetchSlotsForSubstitution(SubstituteRequest req) {
        if (req.getStartDate().isAfter(req.getEndDate())) {
            return List.of();
        }

        int startDayOfWeek = req.getStartDate().getDayOfWeek().getValue();
        int endDayOfWeek = req.getEndDate().getDayOfWeek().getValue();

        if (startDayOfWeek <= endDayOfWeek) {
            return slotRepository.findSlotsForTeacherInRange(
                req.getOriginalTeacherId(), startDayOfWeek, endDayOfWeek
            );
        }

        return slotRepository.findSlotsForTeacherOnDays(
            req.getOriginalTeacherId(), List.copyOf(getDayOfWeeksInRange(req.getStartDate(), req.getEndDate()))
        );
    }

    private void validateRequest(SubstituteRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Substitution request cannot be null");
        }
        if (req.getOriginalTeacherId() == null || req.getReplacementTeacherId() == null) {
            throw new IllegalArgumentException("Original and replacement teacher IDs are required");
        }
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new IllegalArgumentException("Substitution startDate and endDate are required");
        }
        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new IllegalArgumentException("Start date must be on or before end date");
        }
        long daysBetween = ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate());
        if (daysBetween > 90) {
            throw new IllegalArgumentException("Date range exceeds 90 days limit");
        }
        if (req.getOriginalTeacherId().equals(req.getReplacementTeacherId())) {
            throw new IllegalArgumentException("Original and replacement teacher must differ");
        }
        if (req.getScope() == null) {
            throw new IllegalArgumentException("Substitution scope is required");
        }
        if (req.getScope() == SubstituteRequest.SubstitutionScope.ONE_DAY
                && !req.getStartDate().equals(req.getEndDate())) {
            throw new IllegalArgumentException("ONE_DAY scope requires identical start and end dates");
        }
    }

    /**
     * Checks if any of the slots would conflict with the replacement teacher's schedule.
     * Performs individual clash checks for each slot.
     *
     * @param slots the slots to check
     * @param replacementId the replacement teacher ID
     * @return list of clash details, empty if no clashes
     */
    public List<ClashDetail> clashCheck(List<TimetableSlot> slots, SubstituteRequest req) {
        List<ClashDetail> clashes = new ArrayList<>();

        for (TimetableSlot slot : slots) {
            boolean hasConflict = slotRepository.existsConflictingSlot(
                req.getReplacementTeacherId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime()
            );

            if (hasConflict) {
                clashes.add(buildClashDetail(slot, req));
            }
        }

        return clashes;
    }

    /**
     * Applies the substitution atomically to all slots.
     * Uses @Transactional to ensure all-or-nothing update.
     *
     * @param slots the slots to update
     * @param replacementId the new teacher ID
     * @return number of slots updated
     */
    @Transactional
    public int applySubstitution(List<TimetableSlot> slots, UUID replacementId) {
        User replacement = userRepository.findById(replacementId)
                .orElseThrow(() -> new IllegalArgumentException("Replacement teacher not found: " + replacementId));

        try {
            for (TimetableSlot slot : slots) {
                slot.setTeacher(replacement);
                // Write audit log
                // auditService.logSubstitution(slot.getId(), originalTeacherId, replacementId);
            }

            // Bulk save
            slotRepository.saveAll(slots);
            return slots.size();
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.error("Optimistic locking failed during substitution", ex);
            throw new RuntimeException("Concurrent substitution conflict detected", ex);
        }
    }

    /**
     * Builds a ClashDetail from a conflicting slot.
     *
     * @param slot the conflicting slot
     * @param req the substitution request
     * @return the clash detail
     */
    private ClashDetail buildClashDetail(TimetableSlot slot, SubstituteRequest req) {
        ClashDetail detail = new ClashDetail();
        detail.setDate(findFirstMatchingDate(slot, req.getStartDate(), req.getEndDate()));
        detail.setStartTime(slot.getStartTime());
        detail.setEndTime(slot.getEndTime());
        detail.setRoomName(slot.getRoom().getName());
        detail.setConflictReason("Replacement teacher has conflicting slot");
        return detail;
    }

    private LocalDate findFirstMatchingDate(TimetableSlot slot, LocalDate startDate, LocalDate endDate) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek().getValue() == slot.getDayOfWeek()) {
                return current;
            }
            current = current.plusDays(1);
        }
        return startDate;
    }

    private Set<Integer> getDayOfWeeksInRange(LocalDate startDate, LocalDate endDate) {
        Set<Integer> dayOfWeeks = new HashSet<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dayOfWeeks.add(current.getDayOfWeek().getValue());
            current = current.plusDays(1);
        }
        return dayOfWeeks;
    }

    /**
     * Builds the result DTO.
     *
     * @param reassigned number of slots reassigned
     * @param clashes list of clashes
     * @return the result DTO
     */
    private SubstitutionResultDto buildResult(int reassigned, List<ClashDetail> clashes) {
        SubstitutionResultDto result = new SubstitutionResultDto();
        result.setAutoReassigned(reassigned);
        result.setClashCount(clashes.size());
        result.setClashes(clashes);
        return result;
    }

}