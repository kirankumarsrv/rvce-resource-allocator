package com.rvce.scas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Final manual seat assignment for an exam session.
 */
@Getter
@Setter
@Entity
@Table(
        name = "exam_seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_exam_seat_position", columnNames = {"hall_id", "bench_row", "bench_col", "bench_seat_index"}),
                @UniqueConstraint(name = "uq_student_per_exam", columnNames = {"exam_id", "student_id"})
        }
)
public class ExamSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seat_id", nullable = false, updatable = false)
    private UUID seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private ExamSession examSession;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private ExamHall hall;

    @Column(name = "bench_row", nullable = false)
    private Short benchRow;

    @Column(name = "bench_col", nullable = false)
    private Short benchCol;

    @Column(name = "bench_seat_index", nullable = false)
    private Short benchSeatIndex;

    @Column(name = "bench_number", nullable = false, length = 10)
    private String benchNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "is_manual_override", nullable = false)
    private boolean manualOverride;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (assignedAt == null) {
            assignedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = "ASSIGNED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}