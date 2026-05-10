package com.rvce.scas.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for opening a manual seating session.
 */
@Data
public class SeatingSessionDto {

    private UUID examId;
    private UUID sessionId;
    private Instant openedAt;
}