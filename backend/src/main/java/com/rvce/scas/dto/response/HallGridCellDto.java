package com.rvce.scas.dto.response;

import lombok.Data;

import java.util.List;

/**
 * A single cell in the hall grid view.
 */
@Data
public class HallGridCellDto {

    private int row;
    private int col;
    private String label;
    private int seatCapacity;
    private int occupiedCount;
    private boolean active;
    private boolean excluded;
    private List<ExamSeatDto> seats;
    private List<SeatWarningDto> warnings;
}