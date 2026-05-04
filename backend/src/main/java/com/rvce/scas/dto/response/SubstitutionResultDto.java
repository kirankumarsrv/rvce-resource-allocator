package com.rvce.scas.dto.response;

import lombok.Data;
import java.util.List;

/**
 * Response DTO for teacher substitution operations (T-103).
 * Contains the outcome of the substitution attempt.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Data
public class SubstitutionResultDto {

    /**
     * Number of slots successfully reassigned.
     */
    private int autoReassigned;

    /**
     * Number of slots that could not be reassigned due to clashes.
     */
    private int clashCount;

    /**
     * List of detailed clash information, if any.
     */
    private List<ClashDetail> clashes;

}