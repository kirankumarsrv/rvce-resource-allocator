package com.rvce.scas.service;

import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.event.ExamPublishedEvent;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notification Orchestrator
 * Listens to ExamPublishedEvent and sends notifications to students
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestrator {
    private final NotificationService notificationService;
    private final ExamStudentRepository examStudentRepository;
    private final ExamSeatRepository examSeatRepository;

    /**
     * Handle ExamPublishedEvent — send notifications to all students
     */
    @EventListener
    @Async
    @Transactional
    public void onExamPublished(ExamPublishedEvent event) {
        log.info("Exam published event received: examId={}, examName={}", event.getExamId(), event.getExamName());

        try {
            // Get all students for this exam
            List<ExamStudent> students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(event.getExamId());
            log.info("Found {} students for exam {}", students.size(), event.getExamId());

            // For each student, send a notification
            for (ExamStudent student : students) {
                if (student.getStudentId() == null) {
                    // Skip external students (no user account)
                    log.debug("Skipping external student: usn={}", student.getUsn());
                    continue;
                }

                try {
                    sendExamPublishedNotification(event.getExamId(), event.getExamName(), student);
                } catch (Exception e) {
                    log.error("Failed to send notification for student: usn={}, error={}", 
                        student.getUsn(), e.getMessage());
                    // Continue with other students
                }
            }

            log.info("Exam published notifications completed for exam: {}", event.getExamId());
        } catch (Exception e) {
            log.error("Error handling exam published event: {}", e.getMessage(), e);
        }
    }

    /**
     * Send exam published notification to a student
     */
    private void sendExamPublishedNotification(UUID examId, String examName, ExamStudent student) {
        // Get student's seat assignment
        ExamSeat seat = examSeatRepository.findByExamSession_ExamIdAndStudentId(examId, student.getStudentId())
            .orElse(null);

        if (seat == null) {
            log.warn("Student {} has no seat assignment for exam {}", student.getUsn(), examId);
            return;
        }

        // Build notification payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("exam_id", examId.toString());
        payload.put("exam_name", examName);
        payload.put("usn", student.getUsn());
        payload.put("student_name", student.getStudentName());
        payload.put("bench", seat.getBenchNumber());
        payload.put("hall_id", seat.getHall().getHallId().toString());
        payload.put("timestamp", System.currentTimeMillis());

        // Create notification
        String title = "Your Exam Seating Assigned";
        String body = String.format("Your seat for %s is ready. Bench: %s",
            examName,
            seat.getBenchNumber()
        );

        notificationService.createNotification(
            student.getStudentId(),
            "SEAT_PUBLISHED",
            title,
            body,
            payload,
            examId,
            "EXAM",
            null // No batch ID for now (could be added if implementing SQS batching)
        );

        log.info("Notification sent to student: usn={}, seatId={}", student.getUsn(), seat.getSeatId());
    }
}
