package com.rvce.scas.service;

import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating Excel exports
 * Handles seating lists, invigilator sheets, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {
    private final ExamSessionRepository examSessionRepository;
    private final ExamSeatRepository examSeatRepository;
    private final ExamStudentRepository examStudentRepository;
    private final AuditService auditService;

    /**
     * Generate seating Excel file (CSV format)
     */
    @Transactional(readOnly = true)
    public byte[] generateSeatingExcel(UUID examId, String scope, UUID actorId) throws Exception {
        examSessionRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        List<ExamSeat> seats = examSeatRepository.findByExamSession_ExamIdOrderByHall_SortOrderAscBenchRowAscBenchColAscBenchSeatIndexAsc(examId);

        if (scope != null && scope.startsWith("hall:")) {
            UUID hallId = UUID.fromString(scope.substring(5));
            seats = seats.stream()
                .filter(s -> s.getHall().getHallId().equals(hallId))
                .toList();
        }

        List<ExamStudent> students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId);
        Map<UUID, ExamStudent> studentById = students.stream()
            .collect(Collectors.toMap(ExamStudent::getStudentId, s -> s));

        StringBuilder csv = new StringBuilder();
        csv.append("Bench,USN,Name,Branch\n");
        
        for (ExamSeat seat : seats) {
            ExamStudent student = studentById.get(seat.getStudentId());
            csv.append(seat.getBenchNumber()).append(",");
            csv.append(student != null ? escapeCsv(student.getUsn()) : "").append(",");
            csv.append(student != null ? escapeCsv(student.getStudentName()) : "").append(",");
            csv.append(student != null ? student.getBranchCode() : "").append("\n");
        }

        auditService.log(actorId, "EXPORT_EXCEL", "EXAM", examId);
        log.info("Generated Excel export for exam: {} with {} seats", examId, seats.size());

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Generate invigilator sheet
     */
    @Transactional(readOnly = true)
    public byte[] generateInvigilatorSheet(UUID examId, UUID hallId, UUID actorId) throws Exception {
        List<ExamSeat> seats = examSeatRepository.findByExamSession_ExamIdAndHall_HallIdOrderByBenchRowAscBenchColAscBenchSeatIndexAsc(
            examId, hallId
        );

        List<ExamStudent> students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId);
        Map<UUID, ExamStudent> studentById = students.stream()
            .collect(Collectors.toMap(ExamStudent::getStudentId, s -> s));

        StringBuilder csv = new StringBuilder();
        csv.append("Bench,USN,Name,Branch,Present\n");
        
        for (ExamSeat seat : seats) {
            ExamStudent student = studentById.get(seat.getStudentId());
            csv.append(seat.getBenchNumber()).append(",");
            csv.append(student != null ? escapeCsv(student.getUsn()) : "").append(",");
            csv.append(student != null ? escapeCsv(student.getStudentName()) : "").append(",");
            csv.append(student != null ? student.getBranchCode() : "").append(",\n");
        }

        auditService.log(actorId, "EXPORT_INVIGILATOR_SHEET", "EXAM", examId);
        log.info("Generated invigilator sheet for hall: {} with {} students", hallId, seats.size());

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
