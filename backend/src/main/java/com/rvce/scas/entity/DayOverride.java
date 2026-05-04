package com.rvce.scas.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Represents date-specific overrides to the canonical timetable schedule.
 * Allows cancellations, occupations, or other exceptions without modifying the weekly slots.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Store temporary schedule changes for specific dates</li>
 *   <li>Support room booking and class cancellations</li>
 *   <li>Maintain referential integrity with timetable_slots</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * References TimetableSlot entity.
 *
 * <h3>Transaction Behaviour</h3>
 * Created/deleted in T-104 operations; read in T-102 queries.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Entity
@Table(name = "day_overrides", uniqueConstraints = {
    @UniqueConstraint(name = "idx_do_slot_date", columnNames = {"slot_id", "override_date"})
}, indexes = {
    @Index(name = "idx_do_date_status", columnList = "override_date, status")
})
@Data
public class DayOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "override_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimetableSlot slot;

    @Column(name = "override_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OverrideStatus status; // CANCELLED, OCCUPIED

    @Column
    private String reason; // Optional reason for the override

    @Column(name = "created_by", nullable = false)
    private UUID createdBy; // User who created the override

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;

    public enum OverrideStatus {
        CANCELLED,    // Class cancelled, room freed
        CLAIMED,      // Room claimed by another teacher
        EXTRA_CLASS   // Extra class scheduled
    }

}