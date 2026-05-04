package com.rvce.scas.validation;

import com.rvce.scas.exception.RoomNotFoundException;
import com.rvce.scas.exception.SlotConflictException;
import com.rvce.scas.exception.TeacherNotFoundException;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalTime;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Validator for individual CSV rows during timetable upload (T-101).
 * Performs business rule validation against live DB state.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Validate room existence</li>
 *   <li>Validate teacher existence</li>
 *   <li>Check for slot conflicts</li>
 *   <li>Validate time formats and ranges</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on RoomRepository and UserRepository for existence checks.
 *
 * <h3>Transaction Behaviour</h3>
 * Validation queries are read-only and can run outside main transaction.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvRowValidator {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final TimetableSlotRepository slotRepository;

    /**
     * Validates a single CSV row against business rules.
     * Throws exceptions for validation failures.
     *
     * @param row the parsed CSV row data
     * @param rowNumber the row number in the CSV for error reporting
     * @throws RoomNotFoundException if room does not exist
     * @throws TeacherNotFoundException if teacher does not exist
     * @throws IllegalArgumentException for format or range validation failures
     */
    public void validateRow(CsvRowDto row, int rowNumber) {
        // Validate room exists
        if (!roomRepository.existsById(row.getRoomId())) {
            throw new RoomNotFoundException("Room ID " + row.getRoomId() + " not found at row " + rowNumber);
        }

        if (!userRepository.existsById(row.getTeacherId())) {
            throw new TeacherNotFoundException("Teacher ID " + row.getTeacherId() + " not found at row " + rowNumber);
        }

        if (slotRepository.existsRoomTimeConflict(row.getRoomId(), row.getDayOfWeek(), row.getStartTime(), row.getEndTime())) {
            throw new SlotConflictException("Room " + row.getRoomId() + " is already occupied on day " + row.getDayOfWeek() + " between " + row.getStartTime() + " and " + row.getEndTime() + " at row " + rowNumber);
        }

        validateTimeRange(row.getStartTime(), row.getEndTime(), rowNumber);

        // Validate day of week
        if (row.getDayOfWeek() < 1 || row.getDayOfWeek() > 7) {
            throw new IllegalArgumentException("Invalid day of week " + row.getDayOfWeek() + " at row " + rowNumber);
        }

        log.debug("Row {} validation passed", rowNumber);
    }

    /**
     * Validates that start time is before end time and within reasonable bounds.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param rowNumber the row number for error reporting
     */
    private void validateTimeRange(LocalTime startTime, LocalTime endTime, int rowNumber) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time at row " + rowNumber);
        }

        // Optional: Add business hour validation (e.g., 8 AM to 6 PM)
        LocalTime businessStart = LocalTime.of(8, 0);
        LocalTime businessEnd = LocalTime.of(18, 0);

        if (startTime.isBefore(businessStart) || endTime.isAfter(businessEnd)) {
            log.warn("Time range {} - {} at row {} is outside typical business hours", startTime, endTime, rowNumber);
            // Not throwing exception, just warning
        }
    }

    // Placeholder for CsvRowDto - this would be defined elsewhere
    public static class CsvRowDto {
        private UUID roomId;
        private UUID teacherId;
        private Integer dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String subject;
        private String department;

        // getters and setters
        public UUID getRoomId() { return roomId; }
        public void setRoomId(UUID roomId) { this.roomId = roomId; }
        public UUID getTeacherId() { return teacherId; }
        public void setTeacherId(UUID teacherId) { this.teacherId = teacherId; }
        public Integer getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
    }

}