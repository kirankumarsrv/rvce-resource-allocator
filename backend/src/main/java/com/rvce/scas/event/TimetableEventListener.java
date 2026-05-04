package com.rvce.scas.event;

import com.rvce.scas.cache.RoomAvailabilityCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h3>Purpose</h3>
 * Event listener for timetable-related events.
 * Handles cache invalidation and notifications asynchronously.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Invalidate room availability cache on timetable changes</li>
 *   <li>Process events asynchronously to avoid blocking HTTP responses</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on RoomAvailabilityCache for invalidation operations.
 *
 * <h3>Transaction Behaviour</h3>
 * Listens to events published after transaction commits.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimetableEventListener {

    private final RoomAvailabilityCache roomAvailabilityCache;

    /**
     * Handles timetable upload events by invalidating all cached availability data.
     * This ensures that new schedule data is immediately reflected in queries.
     *
     * @param event the timetable uploaded event
     */
    @EventListener
    @Async
    public void handleTimetableUploaded(TimetableUploadedEvent event) {
        log.info("Processing TimetableUploadedEvent: invalidating all room availability cache");
        roomAvailabilityCache.invalidateAll();
        log.debug("Cache invalidation completed for {} inserted slots", event.getInsertedCount());
    }

    /**
     * Handles class cancellation events for notification processing.
     * Currently logs the event; future epics will add notification logic.
     *
     * @param event the class cancelled event
     */
    @EventListener
    @Async
    public void handleClassCancelled(ClassCancelledEvent event) {
        log.info("Processing ClassCancelledEvent: slot {} cancelled on {} by user {}",
                event.getSlotId(), event.getDate(), event.getCancelledBy());
        // TODO: Integrate with notification system in future epic
    }

}