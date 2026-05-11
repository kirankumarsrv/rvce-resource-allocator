package com.rvce.scas.service;

import com.rvce.scas.dto.response.UploadResultDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.entity.User;
import com.rvce.scas.event.TimetableUploadedEvent;
import com.rvce.scas.exception.CsvValidationException;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.validation.CsvRowValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Service for timetable upload operations (T-101).
 * Handles CSV parsing, validation, and transactional persistence of schedule data.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Parse and validate CSV timetable data</li>
 *   <li>Transactionally persist valid slots</li>
 *   <li>Publish events for cache invalidation</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on CsvRowValidator, TimetableMapper, ApplicationEventPublisher.
 *
 * <h3>Transaction Behaviour</h3>
 * upload() is @Transactional — full rollback on any validation failure.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableUploadService {

    private final CsvRowValidator csvRowValidator;
    private final TimetableSlotRepository slotRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Processes a CSV file upload, validates all rows, and persists valid timetable slots.
     * Implements all-or-nothing upload: any validation failure causes complete rollback.
     *
     * <p>Decision DD-10: All-or-nothing validation prevents partial inconsistent schedules.</p>
     *
     * @param file the uploaded CSV file (must be text/csv)
     * @return UploadResultDto with success/error counts
     * @throws CsvValidationException if any row fails validation (triggers rollback)
     */
    @Transactional
    public UploadResultDto upload(@NonNull MultipartFile file) {
        log.info("Starting timetable upload for file: {}", file.getOriginalFilename());

        List<CsvRowValidator.CsvRowDto> validRows;
        List<String> errors = new ArrayList<>();

        try {
            ParseResult result = parseAndValidate(file.getInputStream());
            validRows = result.validRows();
            errors = result.errors();

            if (!errors.isEmpty()) {
                log.warn("CSV validation failed with {} errors", errors.size());
                throw new CsvValidationException("Validation failed: " + String.join("; ", errors));
            }

            if (validRows.isEmpty()) {
                log.warn("Timetable CSV upload contains no data rows");
                throw new CsvValidationException("Validation failed: no data rows provided");
            }

            // Persist all valid rows
            int insertedCount = persistSlots(validRows);

            // Publish event for cache invalidation
            eventPublisher.publishEvent(new TimetableUploadedEvent(this, insertedCount));

            UploadResultDto resultDto = new UploadResultDto();
            resultDto.setInsertedCount(insertedCount);
            resultDto.setErrorCount(0);
            resultDto.setErrors(List.of());

            log.info("Timetable upload completed successfully: {} slots inserted", insertedCount);
            return resultDto;

        } catch (Exception e) {
            log.error("Timetable upload failed", e);
            throw new CsvValidationException("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Parses the CSV file and validates each row.
     * Collects all errors before deciding to proceed or reject.
     *
     * @param inputStream the CSV file input stream
     * @return ParseResult containing valid rows and errors
     */
    public ParseResult parseAndValidate(java.io.InputStream inputStream) {
        List<CsvRowValidator.CsvRowDto> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            int rowNumber = 1; // Start after header
            for (CSVRecord record : parser) {
                rowNumber++;
                try {
                    CsvRowValidator.CsvRowDto row = parseRecord(record);
                    csvRowValidator.validateRow(row, rowNumber);
                    validRows.add(row);
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("CSV parsing error: " + e.getMessage());
        }

        return new ParseResult(validRows, errors);
    }

    /**
     * Parses a single CSV record into a CsvRowDto.
     *
     * @param record the CSV record
     * @return the parsed row DTO
     */
    private CsvRowValidator.CsvRowDto parseRecord(CSVRecord record) {
        CsvRowValidator.CsvRowDto row = new CsvRowValidator.CsvRowDto();
        row.setRoomId(UUID.fromString(record.get("room_id")));
        row.setTeacherId(UUID.fromString(record.get("teacher_id")));
        row.setDayOfWeek(Integer.parseInt(record.get("day_of_week")));
        row.setStartTime(LocalTime.parse(record.get("start_time")));
        row.setEndTime(LocalTime.parse(record.get("end_time")));
        row.setSubject(record.get("subject"));
        row.setDepartment(record.get("department"));
        return row;
    }

    /**
     * Persists the validated CSV rows as TimetableSlot entities.
     *
     * @param rows the valid CSV rows
     * @return number of slots inserted
     */
    private int persistSlots(List<CsvRowValidator.CsvRowDto> rows) {
        List<TimetableSlot> slots = new ArrayList<>();
        UUID activeVersionId = resolveActiveVersionId();
        for (CsvRowValidator.CsvRowDto row : rows) {
            Room room = roomRepository.findById(row.getRoomId())
                    .orElseThrow(() -> new CsvValidationException("Room not found during persistence: " + row.getRoomId()));

            User teacher = userRepository.findById(row.getTeacherId())
                    .orElseThrow(() -> new CsvValidationException("Teacher not found during persistence: " + row.getTeacherId()));

            TimetableSlot slot = new TimetableSlot();
            slot.setRoom(room);
            slot.setTeacher(teacher);
            slot.setVersionId(activeVersionId);
            slot.setDayOfWeek(row.getDayOfWeek());
            slot.setStartTime(row.getStartTime());
            slot.setEndTime(row.getEndTime());
            slot.setSubject(row.getSubject());
            slot.setDepartment(row.getDepartment());
            slot.setIsActive(true);
            slots.add(slot);
        }

        slotRepository.saveAll(slots);
        log.debug("Persisted {} timetable slots", slots.size());
        return slots.size();
    }

    private UUID resolveActiveVersionId() {
        UUID activeVersionId = jdbcTemplate.queryForObject(
                "SELECT version_id FROM timetable_versions WHERE status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1",
                UUID.class);

        if (activeVersionId == null) {
            throw new CsvValidationException("No ACTIVE timetable version found. Upload cannot continue.");
        }

        return activeVersionId;
    }

    /**
     * Record class for parse results.
     */
    public record ParseResult(List<CsvRowValidator.CsvRowDto> validRows, List<String> errors) {}

}