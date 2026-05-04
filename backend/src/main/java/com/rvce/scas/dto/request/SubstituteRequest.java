package com.rvce.scas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for teacher substitution operations (T-103).
 * Contains the parameters needed to reassign slots from one teacher to another.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Data
public class SubstituteRequest {

    /**
     * The ID of the teacher whose slots are being reassigned.
     */
    @NotNull
    private UUID originalTeacherId;

    /**
     * The ID of the teacher taking over the slots.
     */
    @NotNull
    private UUID replacementTeacherId;

    /**
     * Start date of the substitution range (inclusive).
     */
    @NotNull
    private LocalDate startDate;

    /**
     * End date of the substitution range (inclusive).
     */
    @NotNull
    private LocalDate endDate;

    /**
     * Scope of substitution: ONE_DAY, SEMESTER, or CUSTOM.
     */
    @NotNull
    private SubstitutionScope scope;

    public enum SubstitutionScope {
        ONE_DAY, SEMESTER
    }

}