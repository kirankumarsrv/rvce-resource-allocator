package com.rvce.scas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request payload for placing a single student in a bench seat.
 */
@Data
public class SeatPlacementRequest {

    @NotNull
    private UUID studentId;

    @NotNull
    private UUID hallId;

    @Min(1)
    private Short benchRow;

    @Min(1)
    private Short benchCol;

    @Min(0)
    private Short benchSeatIndex;
}