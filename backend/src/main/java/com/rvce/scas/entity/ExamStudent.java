package com.rvce.scas.entity;

import com.rvce.scas.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Student enrollment row for an exam session.
 */
@Getter
@Setter
@Entity
@Table(
        name = "exam_students",
        uniqueConstraints = @UniqueConstraint(name = "uq_exam_student", columnNames = {"exam_id", "usn"})
)
public class ExamStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id", nullable = false, updatable = false)
    private UUID entryId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "student_id")
    private UUID studentId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "usn", nullable = false, length = 100)
    private String usn;

    @Column(name = "student_name", nullable = false, length = 150)
    private String studentName;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "upload_batch_id", length = 36)
    private String uploadBatchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
