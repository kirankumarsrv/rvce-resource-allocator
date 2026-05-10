package com.rvce.scas.service;

import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating PDF exports
 * Uses simple text-based PDF generation for now (can be upgraded to iText7 later)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {
    private final ExamSessionRepository examSessionRepository;
    private final ExamSeatRepository examSeatRepository;
    private final ExamStudentRepository examStudentRepository;
    private final AuditService auditService;

    /**
     * Generate seating PDF report (CSV format for now, can be upgraded to PDF)
     */
    @Transactional(readOnly = true)
    public byte[] generateSeatingPdf(@NonNull UUID examId, String scope, @NonNull UUID actorId) throws Exception {
        ExamSession examSession = examSessionRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        List<ExamSeat> seats = examSeatRepository.findByExamSession_ExamIdOrderByHall_SortOrderAscBenchRowAscBenchColAscBenchSeatIndexAsc(examId);
        List<ExamStudent> students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId);
        Map<UUID, ExamStudent> studentsByUserId = students.stream()
            .collect(Collectors.toMap(ExamStudent::getStudentId, s -> s, (a, b) -> a));

        if (scope != null && scope.startsWith("hall:")) {
            UUID hallId = UUID.fromString(scope.substring(5));
            seats = seats.stream()
                .filter(s -> s.getHall().getHallId().equals(hallId))
                .toList();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("SEATING ARRANGEMENT\n");
        csv.append("Subject: ").append(examSession.getSubjectCode() != null ? examSession.getSubjectCode() : "N/A").append("\n");
        String examDate = examSession.getExamDate() != null 
            ? examSession.getExamDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
            : "N/A";
        csv.append("Exam Date: ").append(examDate).append("\n");
        csv.append("Total Students: ").append(seats.size()).append("\n\n");
        
        csv.append("Bench,USN,Student Name,Branch\n");
        for (ExamSeat seat : seats) {
            ExamStudent student = studentsByUserId.get(seat.getStudentId());
            csv.append(seat.getBenchNumber()).append(",");
            csv.append(student != null && student.getUsn() != null ? student.getUsn() : "").append(",");
            csv.append(student != null && student.getStudentName() != null ? student.getStudentName() : "").append(",");
            csv.append(student != null && student.getBranchCode() != null ? student.getBranchCode() : "").append("\n");
        }

        csv.append("\nGenerated on: ").append(Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        auditService.log(actorId, "EXPORT_PDF", "EXAM", examId);
        log.info("Generated PDF export for exam: {} with {} seats", examId, seats.size());

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate hall invigilator report (CSV format for now)
     */
    @Transactional(readOnly = true)
    public byte[] generateHallInvigilatorPdf(@NonNull UUID examId, @NonNull UUID hallId, @NonNull UUID actorId) throws Exception {
        ExamSession examSession = examSessionRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        List<ExamSeat> seats = examSeatRepository.findByExamSession_ExamIdAndHall_HallIdOrderByBenchRowAscBenchColAscBenchSeatIndexAsc(
            examId, hallId
        );
        List<ExamStudent> students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId);
        Map<UUID, ExamStudent> studentsByUserId = students.stream()
            .collect(Collectors.toMap(ExamStudent::getStudentId, s -> s, (a, b) -> a));

        StringBuilder csv = new StringBuilder();
        csv.append("INVIGILATOR SHEET\n");
        csv.append("Subject: ").append(examSession.getSubjectCode() != null ? examSession.getSubjectCode() : "N/A").append("\n");
        String examDate = examSession.getExamDate() != null 
            ? examSession.getExamDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
            : "N/A";
        csv.append("Date: ").append(examDate).append("\n");
        csv.append("Total: ").append(seats.size()).append(" students\n\n");
        
        csv.append("Bench,USN,Name,Branch,Present\n");
        for (ExamSeat seat : seats) {
            ExamStudent student = studentsByUserId.get(seat.getStudentId());
            csv.append(seat.getBenchNumber()).append(",");
            csv.append(student != null && student.getUsn() != null ? student.getUsn() : "").append(",");
            csv.append(student != null && student.getStudentName() != null ? student.getStudentName() : "").append(",");
            csv.append(student != null && student.getBranchCode() != null ? student.getBranchCode() : "").append(",\n");
        }

        csv.append("\nInvigilator Signature: ___________  Date: ___________");

        auditService.log(actorId, "EXPORT_INVIGILATOR_PDF", "EXAM", examId);
        log.info("Generated invigilator PDF for hall: {} with {} students", hallId, seats.size());

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}

