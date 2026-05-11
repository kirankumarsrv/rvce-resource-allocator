package com.rvce.scas.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDate;

/**
 * <h3>Purpose</h3>
 * Event for class cancellations (deprecated - no longer used).
 * Was published when a class was cancelled via day override (T-104).
 * Override feature has been removed as of this version.
 *
 * <h3>Status</h3>
 * DEPRECATED: Override feature removed. This event is not published or used.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 * @deprecated Override feature has been removed
 */
@Deprecated(since = "2.0", forRemoval = true)
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