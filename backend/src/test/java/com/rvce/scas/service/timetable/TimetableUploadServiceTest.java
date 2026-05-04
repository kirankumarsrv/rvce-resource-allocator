package com.rvce.scas.service.timetable;

import com.rvce.scas.dto.response.UploadResultDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.event.TimetableUploadedEvent;
import com.rvce.scas.exception.CsvValidationException;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.service.TimetableUploadService;
import com.rvce.scas.validation.CsvRowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

/**
 * Unit and integration tests for TimetableUploadService (T-101).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-101: Timetable Upload Service Tests")
class TimetableUploadServiceTest {

    private TimetableUploadService uploadService;

    private CsvRowValidator csvValidator;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimetableSlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UUID teacherId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        csvValidator = new CsvRowValidator(roomRepository, userRepository, slotRepository);
        uploadService = new TimetableUploadService(csvValidator, slotRepository, roomRepository, userRepository, eventPublisher);
    }

    @Test
    @DisplayName("Valid CSV upload should persist one slot and publish event")
    void testValidCsvUpload() {
        UUID roomId = UUID.randomUUID();
        String csvContent = "room_id,teacher_id,day_of_week,start_time,end_time,subject,department\n" +
                roomId + "," + teacherId + ",1,09:00,10:00,Maths,CSE\n";
        var file = new MockMultipartFile("file", "timetable.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(roomRepository.existsById(roomId)).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(new Room()));
        when(userRepository.existsById(teacherId)).thenReturn(true);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(new com.rvce.scas.entity.User()));
        when(slotRepository.existsRoomTimeConflict(roomId, 1, LocalTime.of(9, 0), LocalTime.of(10, 0))).thenReturn(false);
        when(slotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        UploadResultDto result = uploadService.upload(file);

        assertNotNull(result);
        assertEquals(1, result.getInsertedCount());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getErrors().isEmpty());
        verify(slotRepository, times(1)).saveAll(anyList());
        verify(eventPublisher, times(1)).publishEvent(isA(TimetableUploadedEvent.class));
    }

    @Test
    @DisplayName("Invalid CSV upload should fail when room does not exist")
    void testInvalidCsvWithNonExistentRoom() {
        UUID roomId = UUID.randomUUID();
        String csvContent = "room_id,teacher_id,day_of_week,start_time,end_time,subject,department\n" +
                roomId + "," + teacherId + ",1,09:00,10:00,Maths,CSE\n";
        var file = new MockMultipartFile("file", "timetable.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(roomRepository.existsById(roomId)).thenReturn(false);

        CsvValidationException exception = assertThrows(CsvValidationException.class, () -> uploadService.upload(file));

        assertTrue(exception.getMessage().contains("Room ID " + roomId + " not found"));
        verify(slotRepository, never()).saveAll(anyList());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Empty CSV upload should be rejected with no data rows error")
    void testEmptyCsvUpload() {
        String csvContent = "room_id,teacher_id,day_of_week,start_time,end_time,subject,department\n";
        var file = new MockMultipartFile("file", "timetable.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        CsvValidationException exception = assertThrows(CsvValidationException.class, () -> uploadService.upload(file));

        assertTrue(exception.getMessage().contains("no data rows provided"));
        verify(slotRepository, never()).saveAll(anyList());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Malformed CSV header should produce a validation error")
    void testMalformedCsvHeader() {
        String csvContent = "room_id,day_of_week,start_time\n1,1,09:00\n";
        var file = new MockMultipartFile("file", "timetable.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        CsvValidationException exception = assertThrows(CsvValidationException.class, () -> uploadService.upload(file));

        assertTrue(exception.getMessage().contains("Validation failed"));
        verify(slotRepository, never()).saveAll(anyList());
        verify(eventPublisher, never()).publishEvent(any());
    }

}