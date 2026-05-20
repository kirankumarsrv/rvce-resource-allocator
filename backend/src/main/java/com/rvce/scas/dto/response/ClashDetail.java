package com.rvce.scas.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO representing a scheduling clash detected during substitution (T-103).
 * Provides details about why a slot cannot be reassigned.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Data
public class ClashDetail {

    /**
     * The date of the clashing slot.
     */
    private LocalDate date;

    /**
     * Start time of the clashing slot.
     */
    private LocalTime startTime;

    /**
     * End time of the clashing slot.
     */
    private LocalTime endTime;

    /**
     * Name of the room where the clash occurs.
     */
    private String roomName;

    /**
     * Subject being taught in the clashing slot.
     */
    private String subject;

    /**
     * Description of the conflict reason.
     */
    private String conflictReason;

}