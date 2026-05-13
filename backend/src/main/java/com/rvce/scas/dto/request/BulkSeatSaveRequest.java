package com.rvce.scas.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Request payload for saving all dashboard seat placements.
 */
@Data
public class BulkSeatSaveRequest {

    @NotEmpty
    @Valid
    private List<SeatPlacementRequest> assignments;
}