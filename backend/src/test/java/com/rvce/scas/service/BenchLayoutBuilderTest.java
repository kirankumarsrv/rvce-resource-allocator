package com.rvce.scas.service;

import com.rvce.scas.dto.response.ExamSeatDto;
import com.rvce.scas.dto.response.HallGridDto;
import com.rvce.scas.dto.response.HallGridCellDto;
import com.rvce.scas.entity.ExamHall;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.Room;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchLayoutBuilderTest {

    private final BenchLayoutBuilder builder = new BenchLayoutBuilder();

    @Test
    void buildHallGridAddsMiddleSeatEmptyWarningForThreeSeaterBenchWithTwoNonMiddleOccupants() {
        ExamHall hall = createExamHall((short) 0, (short) 1);

        ExamSeatDto seatA = buildSeat(hall, 1, 1, (short) 0, "CSE");
        ExamSeatDto seatB = buildSeat(hall, 1, 1, (short) 2, "ECE");

        HallGridDto grid = builder.buildHallGrid(hall, List.of(seatA, seatB));
        HallGridCellDto cell = grid.getGrid().get(0).get(0);

        assertEquals(1, cell.getWarnings().size());
        assertEquals("MIDDLE_SEAT_EMPTY", cell.getWarnings().get(0).getType());
    }

    @Test
    void buildHallGridAddsSameBranchWarningWhenTwoSameBranchStudentsShareABench() {
        ExamHall hall = createExamHall((short) 1, (short) 0);

        ExamSeatDto seatA = buildSeat(hall, 1, 1, (short) 0, "CSE");
        ExamSeatDto seatB = buildSeat(hall, 1, 1, (short) 1, "CSE");

        HallGridDto grid = builder.buildHallGrid(hall, List.of(seatA, seatB));
        HallGridCellDto cell = grid.getGrid().get(0).get(0);

        assertEquals(1, cell.getWarnings().size());
        assertEquals("SAME_BRANCH_BENCH", cell.getWarnings().get(0).getType());
        assertTrue(cell.getWarnings().get(0).getDetail().contains("CSE"));
    }

    @Test
    void buildHallGridDoesNotAddSameBranchWarningForMixedBranches() {
        ExamHall hall = createExamHall((short) 1, (short) 0);

        ExamSeatDto seatA = buildSeat(hall, 1, 1, (short) 0, "CSE");
        ExamSeatDto seatB = buildSeat(hall, 1, 1, (short) 1, "ECE");

        HallGridDto grid = builder.buildHallGrid(hall, List.of(seatA, seatB));
        HallGridCellDto cell = grid.getGrid().get(0).get(0);

        assertTrue(cell.getWarnings().stream().noneMatch(warning -> "SAME_BRANCH_BENCH".equals(warning.getType())));
    }

    @Test
    void buildHallGridAddsOverCapacityWarningWhenSeatsExceedBenchCapacity() {
        ExamHall hall = createExamHall((short) 1, (short) 0);

        ExamSeatDto seatA = buildSeat(hall, 1, 1, (short) 0, null);
        ExamSeatDto seatB = buildSeat(hall, 1, 1, (short) 1, null);
        ExamSeatDto seatC = buildSeat(hall, 1, 1, (short) 2, null);

        HallGridDto grid = builder.buildHallGrid(hall, List.of(seatA, seatB, seatC));
        HallGridCellDto cell = grid.getGrid().get(0).get(0);

        assertEquals(1, cell.getWarnings().size());
        assertEquals("OVER_CAPACITY", cell.getWarnings().get(0).getType());
    }

    private ExamHall createExamHall(short twoSeaterCount, short threeSeaterCount) {
        ExamSession session = new ExamSession();
        session.setExamId(UUID.randomUUID());
        session.setName("Midterm");
        session.setExamDate(LocalDate.of(2026, 5, 12));
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(12, 0));

        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setName("D101");
        room.setDisplayName("Block D - Examination Block");

        ExamHall hall = new ExamHall();
        hall.setHallId(UUID.randomUUID());
        hall.setRoom(room);
        hall.setExamSession(session);
        hall.setBenchRows((short) 1);
        hall.setBenchCols((short) 1);
        hall.setTwoSeaterCount(twoSeaterCount);
        hall.setThreeSeaterCount(threeSeaterCount);
        hall.setTotalCapacity((short) (twoSeaterCount * 2 + threeSeaterCount * 3));
        return hall;
    }

    private ExamSeatDto buildSeat(ExamHall hall, int row, int col, short index, String branchCode) {
        ExamSeatDto seat = new ExamSeatDto();
        seat.setExamId(hall.getExamSession().getExamId());
        seat.setHallId(hall.getHallId());
        seat.setBenchRow(row);
        seat.setBenchCol(col);
        seat.setBenchSeatIndex(index);
        seat.setBranchCode(branchCode);
        return seat;
    }
}
