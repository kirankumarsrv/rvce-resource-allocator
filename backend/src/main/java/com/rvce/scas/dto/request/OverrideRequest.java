package com.rvce.scas.dto.request;

import com.rvce.scas.entity.DayOverride;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * Request DTO for creating day overrides (T-104).
 * Used for cancelling classes or booking rooms.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Data
public class OverrideRequest {

    /**
     * The ID of the timetable slot to override.
     */
    @NotNull
    private Long slotId;

    /**
     * The date of the override.
     */
    @NotNull
    private LocalDate date;

    /**
     * The type of override: CANCELLED or OCCUPIED.
     */
    @NotNull
    private DayOverride.OverrideStatus status;

    /**
     * Optional reason for the override.
     */
    private String reason;

}