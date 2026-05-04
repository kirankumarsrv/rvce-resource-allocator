package com.rvce.scas.service;

import com.rvce.scas.cache.RoomAvailabilityCache;
import com.rvce.scas.dto.request.OverrideRequest;
import com.rvce.scas.dto.response.OverrideDto;
import com.rvce.scas.entity.DayOverride;
import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.event.ClassCancelledEvent;
import com.rvce.scas.exception.OverrideNotFoundException;
import com.rvce.scas.exception.SlotConflictException;
import com.rvce.scas.mapper.TimetableMapper;
import com.rvce.scas.repository.DayOverrideRepository;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h3>Purpose</h3>
 * Service for day override operations (T-104).
 * Manages cancellations and bookings that override the canonical schedule.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Create and validate day overrides</li>
 *   <li>Enforce ownership and role-based permissions</li>
 *   <li>Publish events for cache invalidation and notifications</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on DayOverrideRepository, TimetableSlotRepository, RoomAvailabilityCache.
 *
 * <h3>Transaction Behaviour</h3>
 * createOverride() and deleteOverride() are @Transactional.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DayOverrideService {

    private final DayOverrideRepository overrideRepository;
    private final TimetableSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final RoomAvailabilityCache cache;
    private final TimetableMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a day override for cancelling or booking a slot.
     * Validates permissions, checks for conflicts, and invalidates cache.
     *
     * <p>Decision DD-13: Role-based access control for override operations.</p>
     *
     * @param req the override request
     * @param actor the authenticated user making the request
     * @return OverrideDto for the created override
     * @throws SlotConflictException if override already exists
     * @throws AccessDeniedException if actor lacks permission
     */
    @Transactional
    public OverrideDto createOverride(OverrideRequest req, JwtPrincipal actor) {
        log.info("Creating override for slot {} on {} by user {}",
                req.getSlotId(), req.getDate(), actor.getUserId());

        // Fetch and validate slot
        TimetableSlot slot = slotRepository.findById(req.getSlotId())
            .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

        // Check ownership (teacher can cancel own slots, admin can cancel any)
        validateOwnership(slot, actor);

        // Check for existing override
        if (overrideRepository.existsBySlotIdAndDate(req.getSlotId(), req.getDate())) {
            throw new SlotConflictException("Override already exists for this slot and date");
        }

        // Create override
        DayOverride override = new DayOverride();
        override.setSlot(slot);
        override.setDate(req.getDate());
        override.setStatus(req.getStatus());
        override.setReason(req.getReason());
        override.setCreatedBy(actor.getUserId());
        override.setCreatedAt(LocalDateTime.now());

        DayOverride saved = overrideRepository.save(override);

        // Invalidate cache for the specific date
        cache.invalidateByDate(req.getDate());

        // Publish event if cancelled
        if (req.getStatus() == DayOverride.OverrideStatus.CANCELLED) {
            eventPublisher.publishEvent(new ClassCancelledEvent(
                this, req.getSlotId(), req.getDate(), actor.getUserId(), req.getReason()
            ));
        }

        log.info("Override created: {}", saved.getId());
        return mapper.toDto(saved);
    }

    /**
     * Retrieves overrides for a specific date, optionally filtered by room.
     *
     * @param date the date to query
     * @param roomId optional room filter
     * @return list of overrides
     */
    @Transactional(readOnly = true)
    public List<OverrideDto> getOverrides(LocalDate date, java.util.UUID roomId) {
        List<DayOverride> overrides = overrideRepository.findOverridesByDate(date, roomId);
        return mapper.toDtoList(overrides);
    }

    /**
     * Deletes an override, restoring the canonical schedule.
     * Validates ownership and invalidates cache.
     *
     * @param overrideId the override to delete
     * @param actor the authenticated user
     * @throws OverrideNotFoundException if override doesn't exist
     * @throws AccessDeniedException if actor lacks permission
     */
    @Transactional
    public void deleteOverride(java.util.UUID overrideId, JwtPrincipal actor) {
        log.info("Deleting override {} by user {}", overrideId, actor.getUserId());

        DayOverride override = overrideRepository.findById(overrideId)
            .orElseThrow(() -> new OverrideNotFoundException("Override not found"));

        // Check ownership
        validateOwnership(override.getSlot(), actor);

        // Delete override
        overrideRepository.delete(override);

        // Invalidate cache for the date
        cache.invalidateByDate(override.getDate());

        log.info("Override deleted: {}", overrideId);
    }

    /**
     * Validates that the actor has permission to modify the slot.
     * Teachers can modify their own slots, admins can modify any.
     *
     * @param slot the slot being modified
     * @param actor the user attempting the action
     */
    private void validateOwnership(TimetableSlot slot, JwtPrincipal actor) {
        boolean isOwner = slot.getTeacher().getUserId().equals(actor.getUserId());
        boolean isAdmin = actor.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException(
                "User does not own this slot and is not an admin"
            );
        }
    }

}