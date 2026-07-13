package com.rvce.scas.service;

import com.rvce.scas.dto.request.CreateExamSessionRequest;
import com.rvce.scas.dto.response.ExamSessionDto;
import com.rvce.scas.dto.response.ExamStudentDto;
import com.rvce.scas.dto.response.ExamStudentUploadErrorDto;
import com.rvce.scas.dto.response.ExamStudentUploadResultDto;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.entity.User;
import com.rvce.scas.entity.Department;
import com.rvce.scas.repository.DepartmentRepository;
import com.rvce.scas.exception.ExamSessionNotFoundException;
import com.rvce.scas.exception.ExamStudentCsvValidationException;
import com.rvce.scas.mapper.ExamMapper;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.validation.UsnValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Service for creating exam sessions and uploading enrolled students.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamUploadService {

    private static final Set<String> ALLOWED_BRANCH_CODES = Set.of("CSE", "ISE", "ECE", "MECH", "CIVIL", "EEE");

    private final ExamSessionRepository examSessionRepository;
    private final ExamStudentRepository examStudentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ExamMapper examMapper;
    private final UsnValidator usnValidator;
    private final AuditService auditService;

    @Transactional
    public ExamSessionDto createExamSession(CreateExamSessionRequest request, UUID actorId) {
        Objects.requireNonNull(request, "request must not be null");
        validateTimeRange(request.getStartTime(), request.getEndTime());

        ExamSession examSession = new ExamSession();
        examSession.setName(trimRequired(request.getName(), "name"));
        examSession.setSubjectCode(trimRequired(request.getSubjectCode(), "subjectCode").toUpperCase(Locale.ROOT));
        examSession.setSubjectName(trimRequired(request.getSubjectName(), "subjectName"));
        examSession.setSection(normalizeOptional(request.getSection()));
        examSession.setSemester(request.getSemester() == null ? null : request.getSemester().shortValue());
        UUID deptId = request.getDepartmentId();
        if (deptId == null) {
            String deptName = normalizeOptional(request.getDepartmentName());
            if (deptName != null) {
                deptId = findOrCreateDepartmentByName(deptName);
            } else {
                deptId = departmentRepository.findByCodeIgnoreCase("ADMIN")
                        .map(Department::getDepartmentId)
                        .orElseGet(() -> findOrCreateDepartmentByName("Administration"));
            }
        }
        examSession.setDepartmentId(deptId);
        examSession.setExamDate(request.getExamDate());
        examSession.setStartTime(request.getStartTime());
        examSession.setEndTime(request.getEndTime());
        examSession.setStatus(ExamSession.ExamStatus.DRAFT);
        examSession.setCreatedBy(actorId);

        ExamSession saved = examSessionRepository.save(examSession);
        auditService.log(actorId, "CREATE_EXAM_SESSION", "exam_sessions", saved.getExamId());

        ExamSessionDto response = examMapper.toDto(saved);
        response.setStudentCount(0L);
        return response;
    }

    @Transactional
    public ExamStudentUploadResultDto uploadStudents(UUID examId, MultipartFile file, UUID actorId) {
        ExamSession examSession = examSessionRepository.findById(Objects.requireNonNull(examId))
                .orElseThrow(() -> new ExamSessionNotFoundException("Exam session not found: " + examId));

        if (examSession.getStatus() == ExamSession.ExamStatus.CANCELLED
                || examSession.getStatus() == ExamSession.ExamStatus.PUBLISHED
                || examSession.getStatus() == ExamSession.ExamStatus.COMPLETED
                || examSession.getStatus() == ExamSession.ExamStatus.GENERATED) {
            throw new IllegalArgumentException("Students can only be uploaded while the exam is in DRAFT or CONFIGURED status.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required for student upload.");
        }

        String batchId = UUID.randomUUID().toString();
        List<PendingStudentRow> pendingRows = new ArrayList<>();
        List<ExamStudentUploadErrorDto> errors = new ArrayList<>();
        Set<String> seenUsns = new HashSet<>();
        Set<String> existingUsns = new HashSet<>(examStudentRepository.findUsnsByExamId(examId));
        int totalRows = 0;

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                totalRows++;

                String rawUsn = getRequired(record, "usn");
                String rawName = getRequired(record, "name");
                String rawBranch = getRequired(record, "branch_code");

                String usn = usnValidator.normalize(rawUsn);
                String studentName = rawName == null ? null : rawName.trim();
                String branchCode = rawBranch == null ? null : rawBranch.trim().toUpperCase(Locale.ROOT);

                if (!usnValidator.isValid(usn)) {
                    errors.add(error(rowNumber, usn, "Invalid USN format"));
                    continue;
                }

                if (studentName == null || studentName.isBlank()) {
                    errors.add(error(rowNumber, usn, "Student name is required"));
                    continue;
                }

                if (branchCode == null || !ALLOWED_BRANCH_CODES.contains(branchCode)) {
                    errors.add(error(rowNumber, usn, "Invalid branch code"));
                    continue;
                }

                if (!seenUsns.add(usn)) {
                    errors.add(error(rowNumber, usn, "Duplicate USN in this CSV upload"));
                    continue;
                }

                if (existingUsns.contains(usn)) {
                    errors.add(error(rowNumber, usn, "Duplicate USN in this exam"));
                    continue;
                }

                pendingRows.add(new PendingStudentRow(usn, studentName, branchCode));
            }
        } catch (IOException ex) {
            errors.add(error(1, "", "CSV parsing error: " + ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            errors.add(error(1, "", "CSV parsing error: " + ex.getMessage()));
        }

        if (pendingRows.isEmpty() && totalRows == 0 && errors.isEmpty()) {
            errors.add(error(1, "", "CSV contains no student rows"));
        }

        if (!errors.isEmpty()) {
            throw new ExamStudentCsvValidationException(buildResult(totalRows, 0, totalRows, errors));
        }

        Set<String> usnsToLink = new HashSet<>();
        for (PendingStudentRow row : pendingRows) {
            usnsToLink.add(row.usn());
        }

        Map<String, UUID> studentIdsByUsn = new HashMap<>();
        if (!usnsToLink.isEmpty()) {
            // USN is encrypted at rest with a random IV, so equality lookups on the column are not reliable.
            // Resolve active students in memory using the decrypted entity value instead.
            for (User user : userRepository.findAllByRoleName("STUDENT")) {
                if (user.getUsn() != null) {
                    studentIdsByUsn.put(usnValidator.normalize(user.getUsn()), user.getUserId());
                }
            }
        }

        List<ExamStudent> students = new ArrayList<>(pendingRows.size());
        for (PendingStudentRow row : pendingRows) {
            ExamStudent student = new ExamStudent();
            student.setExamId(examId);
            student.setUsn(row.usn());
            student.setStudentName(row.studentName());
            student.setBranchCode(row.branchCode());
            student.setUploadBatchId(batchId);
            student.setStudentId(studentIdsByUsn.get(row.usn()));
            students.add(student);
        }

        examStudentRepository.saveAll(students);
        auditService.log(actorId, "UPLOAD_EXAM_STUDENTS", "exam_students", examId);

        return buildResult(totalRows, students.size(), 0, List.of());
    }

    @Transactional(readOnly = true)
    public ExamSessionDto getExamSession(@NonNull UUID examId) {
        ExamSession examSession = examSessionRepository.findById(examId)
                .orElseThrow(() -> new ExamSessionNotFoundException("Exam session not found: " + examId));

        ExamSessionDto response = examMapper.toDto(examSession);
        response.setStudentCount(examStudentRepository.countByExamId(examId));
        return response;
    }

    @Transactional(readOnly = true)
    public Page<ExamSessionDto> getExamSessions(@NonNull Pageable pageable) {
        return examSessionRepository.findAll(pageable)
                .map(examMapper::toDto)
                .map(dto -> {
                    dto.setStudentCount(examStudentRepository.countByExamId(dto.getExamId()));
                    return dto;
                });
    }

    @Transactional(readOnly = true)
    public Page<ExamStudentDto> getExamStudents(@NonNull UUID examId, String branchCode, String usn, @NonNull Pageable pageable) {
        if (!examSessionRepository.existsById(examId)) {
            throw new ExamSessionNotFoundException("Exam session not found: " + examId);
        }

        String normalizedBranchCode = normalizeOptional(branchCode);
        String normalizedUsn = normalizeOptional(usn);

        return examStudentRepository.searchExamStudents(examId, normalizedBranchCode, normalizedUsn, pageable)
                .map(examMapper::toDto);
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Exam start and end time are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Exam end time must be after start time.");
        }
    }

    private String trimRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getRequired(CSVRecord record, String column) {
        String value = record.get(column);
        return value == null ? null : value.trim();
    }

    private UUID findOrCreateDepartmentByName(String name) {
        String trimmed = name.trim();
        return departmentRepository.findByNameIgnoreCase(trimmed)
                .map(d -> d.getDepartmentId())
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setDepartmentId(UUID.randomUUID());
                    dept.setName(trimmed);
                    dept.setCode(generateDepartmentCode(trimmed));
                    Department saved = departmentRepository.save(dept);
                    return saved.getDepartmentId();
                });
    }

    private String generateDepartmentCode(String name) {
        String candidate = name.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (candidate.length() > 6) candidate = candidate.substring(0, 6);
        if (candidate.isEmpty()) candidate = "DEPT";
        String code = candidate;
        int suffix = 0;
        while (departmentRepository.existsByCode(code)) {
            suffix++;
            code = (candidate + suffix).substring(0, Math.min(10, candidate.length() + String.valueOf(suffix).length()));
        }
        return code;
    }

    private ExamStudentUploadErrorDto error(int row, String usn, String message) {
        ExamStudentUploadErrorDto error = new ExamStudentUploadErrorDto();
        error.setRow(row);
        error.setUsn(usn);
        error.setError(message);
        return error;
    }

    private ExamStudentUploadResultDto buildResult(int totalRows, int inserted, int skipped, List<ExamStudentUploadErrorDto> errors) {
        ExamStudentUploadResultDto result = new ExamStudentUploadResultDto();
        result.setTotalRows(totalRows);
        result.setInserted(inserted);
        result.setSkipped(skipped);
        result.setErrors(errors);
        return result;
    }

    private record PendingStudentRow(String usn, String studentName, String branchCode) {
    }
}
