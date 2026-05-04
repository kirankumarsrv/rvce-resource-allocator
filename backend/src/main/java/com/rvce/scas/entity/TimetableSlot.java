package com.rvce.scas.entity;

import com.rvce.scas.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Represents a scheduled class slot in the weekly timetable.
 * This is the canonical schedule that repeats every week, overlaid by day_overrides for exceptions.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Store recurring weekly schedule data</li>
 *   <li>Support teacher substitutions and availability queries</li>
 *   <li>Provide audit trail for scheduling changes</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * References Room and User entities.
 *
 * <h3>Transaction Behaviour</h3>
 * Updated in T-101 uploads and T-103 substitutions; read in T-102 queries.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Entity
@Table(name = "timetable_slots", indexes = {
    @Index(name = "idx_ts_day_time", columnList = "day_of_week, start_time, end_time"),
    @Index(name = "idx_ts_teacher", columnList = "teacher_id"),
    @Index(name = "idx_ts_room", columnList = "room_id")
})
@Data
public class TimetableSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long id;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1=Monday, 7=Sunday

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "subject_name", nullable = false)
    private String subject;

    @Column(nullable = false)
    private String department;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Soft delete flag

    @Version
    @Column(name = "row_version")
    private Integer version; // Optimistic locking for T-103

    // Additional fields from database schema
    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "subject_code", nullable = false)
    private String subjectCode;

    @Column(nullable = false)
    private String section;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}