package com.rvce.scas.service;

import com.rvce.scas.dto.response.UploadResultDto;
import com.rvce.scas.exception.CsvValidationException;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for timetable upload service.
 * Verifies:
 * - Field-level error reporting (UUID, time format, day_of_week validation)
 * - CSV parsing robustness
 * - Transaction rollback on validation failure
 * - Row-level error collection
 */
@SpringBootTest
@Transactional
@DisplayName("Timetable Upload Service Tests")
class TimetableUploadServiceTest {

    @Autowired
    private TimetableUploadService uploadService;

    @Autowired
    private TimetableSlotRepository timetableSlotRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should provide clear error for invalid room_id UUID format")
    void testErrorMessageForInvalidRoomIdFormat() {
        String csvContent = """
            room_id,teacher_id,day_of_week,start_time,end_time,subject,department
            invalid-uuid,00000000-0000-0000-0000-000000000001,0,10:00:00,12:00:00,DSA,CSE
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        var exception = assertThrows(
            CsvValidationException.class,
            () -> uploadService.upload(file)
        );

        String message = exception.getMessage();
        assertTrue(message.contains("room_id"), "Error should mention field name");
        assertTrue(message.contains("UUID format"), "Error should explain format requirement");
        assertTrue(message.contains("Row 2"), "Error should include row number");
    }

    @Test
    @DisplayName("Should provide clear error for invalid teacher_id UUID format")
    void testErrorMessageForInvalidTeacherIdFormat() {
        String csvContent = """
            room_id,teacher_id,day_of_week,start_time,end_time,subject,department
            00000000-0000-0000-0000-000000000001,not-a-uuid,0,10:00:00,12:00:00,DSA,CSE
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        var exception = assertThrows(
            CsvValidationException.class,
            () -> uploadService.upload(file)
        );

        String message = exception.getMessage();
        assertTrue(message.contains("teacher_id"), "Error should mention field name");
        assertTrue(message.contains("UUID format"), "Error should explain format requirement");
    }

    @Test
    @DisplayName("Should provide clear error for invalid day_of_week value")
    void testErrorMessageForInvalidDayOfWeek() {
        String csvContent = """
            room_id,teacher_id,day_of_week,start_time,end_time,subject,department
            00000000-0000-0000-0000-000000000001,00000000-0000-0000-0000-000000000001,10,10:00:00,12:00:00,DSA,CSE
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        var exception = assertThrows(
            CsvValidationException.class,
            () -> uploadService.upload(file)
        );

        String message = exception.getMessage();
        assertTrue(message.contains("day_of_week"), "Error should mention field name");
        assertTrue(message.contains("0-6") || message.contains("between"), "Error should specify valid range");
    }

    @Test
    @DisplayName("Should provide clear error for invalid time format")
    void testErrorMessageForInvalidTimeFormat() {
        String csvContent = """
            room_id,teacher_id,day_of_week,start_time,end_time,subject,department
            00000000-0000-0000-0000-000000000001,00000000-0000-0000-0000-000000000001,0,10-00-00,12:00:00,DSA,CSE
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        var exception = assertThrows(
            CsvValidationException.class,
            () -> uploadService.upload(file)
        );

        String message = exception.getMessage();
        assertTrue(message.contains("start_time"), "Error should mention field name");
        assertTrue(message.contains("time format"), "Error should explain format requirement");
        assertTrue(message.contains("HH:mm:ss"), "Error should show expected format");
    }

    @Test
    @DisplayName("Should provide clear error for missing required columns")
    void testErrorMessageForMissingColumn() {
        String csvContent = """
            room_id,day_of_week,start_time,end_time,subject,department
            00000000-0000-0000-0000-000000000001,0,10:00:00,12:00:00,DSA,CSE
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        var exception = assertThrows(
            CsvValidationException.class,
            () -> uploadService.upload(file)
        );

        String message = exception.getMessage();
        assertTrue(
            message.contains("teacher_id") || message.contains("Column") || message.contains("parsing error"),
            "Error should indicate missing required field or column"
        );
    }

    @Test
    @DisplayName("Should return error count on validation failure")
    void testErrorCountReportingOnValidationFailure() {
        String csvContent = """
            room_id,teacher_id,day_of_week,start_time,end_time,subject,department
            invalid1,00000000-0000-0000-0000-000000000001,0,10:00:00,12:00:00,DSA,CSE
            invalid2,00000000-0000-0000-0000-000000000001,0,10:00:00,12:00:00,DSA,CSE
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        var exception = assertThrows(
            CsvValidationException.class,
            () -> uploadService.upload(file)
        );

        // Should contain at least 2 row errors
        assertTrue(
            exception.getMessage().contains("Row 2") || exception.getMessage().contains("errors"),
            "Error message should reference specific rows"
        );
    }

    @Test
    @DisplayName("Should rollback transaction if any row fails validation")
    void testTransactionRollbackOnValidationFailure() throws IOException {
        long countBefore = timetableSlotRepository.count();

        // Create 1 valid + 1 invalid row
        String csvContent = """
            room_id,teacher_id,day_of_week,start_time,end_time,subject,department
            00000000-0000-0000-0000-000000000001,00000000-0000-0000-0000-000000000001,0,10:00:00,12:00:00,DSA,CSE
            00000000-0000-0000-0000-000000000002,invalid-uuid,0,10:00:00,12:00:00,DSA,CSE
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        assertThrows(CsvValidationException.class, () -> uploadService.upload(file));

        // Verify no rows were inserted (transaction rolled back)
        long countAfter = timetableSlotRepository.count();
        assertEquals(countBefore, countAfter, "Transaction should rollback on validation failure - no rows should be persisted");
    }

    @Test
    @DisplayName("Should handle empty CSV file")
    void testEmptyCSVFileHandling() {
        String csvContent = "room_id,teacher_id,day_of_week,start_time,end_time,subject,department\n";

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        assertThrows(
            CsvValidationException.class,
            () -> uploadService.upload(file),
            "Should reject empty CSV with no data rows"
        );
    }
}
