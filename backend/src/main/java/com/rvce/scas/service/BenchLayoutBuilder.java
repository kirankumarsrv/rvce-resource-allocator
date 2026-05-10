package com.rvce.scas.service;

import com.rvce.scas.dto.response.ExamSeatDto;
import com.rvce.scas.dto.response.HallGridCellDto;
import com.rvce.scas.dto.response.HallGridDto;
import com.rvce.scas.dto.response.SeatWarningDto;
import com.rvce.scas.entity.ExamHall;
import com.rvce.scas.entity.ExamSeat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure layout helper for the manual seating dashboard.
 */
@Component
public class BenchLayoutBuilder {

    public HallGridDto buildHallGrid(ExamHall hall, List<ExamSeat> seats) {
        Map<String, List<ExamSeatDto>> seatsByPosition = seats == null ? Map.of() : seats.stream()
                .map(this::toSeatDto)
                .collect(Collectors.groupingBy(this::positionKey));

        List<List<HallGridCellDto>> grid = new ArrayList<>(hall.getBenchRows());
        int benchIndex = 0;

        for (int row = 1; row <= hall.getBenchRows(); row++) {
            List<HallGridCellDto> rowCells = new ArrayList<>(hall.getBenchCols());
            for (int col = 1; col <= hall.getBenchCols(); col++) {
                int capacity = benchCapacity(hall, benchIndex);
                boolean active = capacity > 0;
                List<ExamSeatDto> benchSeats = new ArrayList<>(seatsByPosition.getOrDefault(positionKey(row, col), List.of()));
                benchSeats.sort(Comparator.comparingInt(ExamSeatDto::getBenchSeatIndex));

                HallGridCellDto cell = new HallGridCellDto();
                cell.setRow(row);
                cell.setCol(col);
                cell.setLabel(benchLabel(row, col));
                cell.setSeatCapacity(capacity);
                cell.setOccupiedCount(benchSeats.size());
                cell.setActive(active);
                cell.setExcluded(!active);
                cell.setSeats(benchSeats);
                cell.setWarnings(buildWarnings(capacity, benchSeats));
                rowCells.add(cell);
                benchIndex++;
            }
            grid.add(rowCells);
        }

        HallGridDto response = new HallGridDto();
        response.setHallId(hall.getHallId());
        response.setRoomName(hall.getRoom().getName());
        response.setRoomDisplayName(hall.getRoom().getDisplayName());
        response.setBenchRows(hall.getBenchRows() == null ? null : hall.getBenchRows().intValue());
        response.setBenchCols(hall.getBenchCols() == null ? null : hall.getBenchCols().intValue());
        response.setGrid(grid);
        return response;
    }

    public String benchLabel(int row, int col) {
        return String.valueOf((char) ('A' + (row - 1))) + "-" + col;
    }

    private int benchCapacity(ExamHall hall, int benchIndex) {
        if (benchIndex < hall.getTwoSeaterCount()) {
            return 2;
        }
        if (benchIndex < hall.getTwoSeaterCount() + hall.getThreeSeaterCount()) {
            return 3;
        }
        return 0;
    }

    private List<SeatWarningDto> buildWarnings(int capacity, List<ExamSeatDto> seats) {
        List<SeatWarningDto> warnings = new ArrayList<>();

        if (capacity == 3 && seats.size() == 2 && seats.stream().noneMatch(seat -> seat.getBenchSeatIndex() == 1)) {
            warnings.add(warning("MIDDLE_SEAT_EMPTY", "Three-seater bench has a gap in the middle seat."));
        }

        if (seats.size() > 1) {
            long distinctBranches = seats.stream()
                    .map(ExamSeatDto::getBranchCode)
                    .filter(branch -> branch != null && !branch.isBlank())
                    .distinct()
                    .count();
            if (distinctBranches < seats.size()) {
                warnings.add(warning("SAME_BRANCH_BENCH", "Multiple students from the same branch share this bench."));
            }
        }

        if (capacity > 0 && seats.size() > capacity) {
            warnings.add(warning("OVER_CAPACITY", "This bench has more occupants than its configured capacity."));
        }

        return warnings;
    }

    private SeatWarningDto warning(String type, String message) {
        SeatWarningDto warning = new SeatWarningDto();
        warning.setType(type);
        warning.setMessage(message);
        return warning;
    }

    private ExamSeatDto toSeatDto(ExamSeat seat) {
        ExamSeatDto dto = new ExamSeatDto();
        dto.setSeatId(seat.getSeatId());
        dto.setExamId(seat.getExamSession().getExamId());
        dto.setHallId(seat.getHall().getHallId());
        dto.setStudentId(seat.getStudentId());
        dto.setBenchRow(seat.getBenchRow());
        dto.setBenchCol(seat.getBenchCol());
        dto.setBenchSeatIndex(seat.getBenchSeatIndex());
        dto.setBenchNumber(seat.getBenchNumber());
        dto.setManualOverride(seat.isManualOverride());
        return dto;
    }

    private String positionKey(ExamSeatDto seat) {
        return positionKey(seat.getBenchRow(), seat.getBenchCol());
    }

    private String positionKey(int row, int col) {
        return row + ":" + col;
    }
}