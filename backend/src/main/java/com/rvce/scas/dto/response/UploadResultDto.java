package com.rvce.scas.dto.response;

import lombok.Data;
import java.util.List;

/**
 * Response DTO for timetable upload operations (T-101).
 * Contains the results of parsing and persisting CSV data.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Data
public class UploadResultDto {

    /**
     * Number of rows successfully inserted.
     */
    private int insertedCount;

    /**
     * Number of rows that failed validation.
     */
    private int errorCount;

    /**
     * List of validation errors, if any.
     */
    private List<String> errors;

}