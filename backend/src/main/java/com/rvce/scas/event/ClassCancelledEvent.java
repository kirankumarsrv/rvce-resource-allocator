package com.rvce.scas.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDate;

/**
 * <h3>Purpose</h3>
 * Event published when a class is cancelled via day override (T-104).
 * Triggers notifications to affected students and faculty.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Notify notification subsystem of class cancellations</li>
 *   <li>Carry details about the cancelled slot</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Published by DayOverrideService; consumed by notification listeners.
 *
 * <h3>Transaction Behaviour</h3>
 * Published synchronously after DB commit in T-104.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Getter
public class ClassCancelledEvent extends ApplicationEvent {

    /**
     * ID of the cancelled slot.
     */
    private final Long slotId;

    /**
     * Date of the cancellation.
     */
    private final LocalDate date;

    /**
     * User who performed the cancellation.
     */
    private final java.util.UUID cancelledBy;

    /**
     * Reason for cancellation, if provided.
     */
    private final String reason;

    public ClassCancelledEvent(Object source, Long slotId, LocalDate date, java.util.UUID cancelledBy, String reason) {
        super(source);
        this.slotId = slotId;
        this.date = date;
        this.cancelledBy = cancelledBy;
        this.reason = reason;
    }

}