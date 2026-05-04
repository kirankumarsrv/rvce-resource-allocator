package com.rvce.scas.dto.response;

import com.rvce.scas.entity.DayOverride;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for day override operations (T-104).
 * Represents an existing override.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Data
public class OverrideDto {

    /**
     * Unique identifier of the override.
     */
    private Long id;

    /**
     * ID of the slot being overridden.
     */
    private Long slotId;

    /**
     * Date of the override.
     */
    private LocalDate date;

    /**
     * Status of the override.
     */
    private DayOverride.OverrideStatus status;

    /**
     * Reason for the override.
     */
    private String reason;

    /**
     * User who created the override.
     */
    private String createdBy;

    /**
     * Timestamp when the override was created.
     */
    private LocalDateTime createdAt;

}