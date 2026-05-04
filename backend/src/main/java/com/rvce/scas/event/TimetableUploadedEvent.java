package com.rvce.scas.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <h3>Purpose</h3>
 * Event published after successful timetable upload (T-101).
 * Triggers cache invalidation for room availability queries.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Notify listeners that timetable data has changed</li>
 *   <li>Carry metadata about the upload operation</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Published by TimetableUploadService; consumed by TimetableEventListener.
 *
 * <h3>Transaction Behaviour</h3>
 * Published synchronously after DB commit in T-101.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Getter
public class TimetableUploadedEvent extends ApplicationEvent {

    /**
     * Number of slots inserted in the upload.
     */
    private final int insertedCount;

    /**
     * Timestamp of the upload operation.
     */
    private final java.time.LocalDateTime eventTimestamp;

    public TimetableUploadedEvent(Object source, int insertedCount) {
        super(source);
        this.insertedCount = insertedCount;
        this.eventTimestamp = java.time.LocalDateTime.now();
    }

}