package com.rvce.scas.dto.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the 2D hall grid.
 */
@Data
public class HallGridDto {

    private UUID hallId;
    private String roomName;
    private String roomDisplayName;
    private Integer benchRows;
    private Integer benchCols;
    private List<List<HallGridCellDto>> grid;
}