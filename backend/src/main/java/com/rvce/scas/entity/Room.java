package com.rvce.scas.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Represents a physical room in the campus that can be booked for classes or events.
 * This entity is used in availability queries to filter and display rooms.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Store room metadata: name, capacity, building, floor, GPS coordinates</li>
 *   <li>Track room type, department ownership, and availability status</li>
 *   <li>Provide unique identification for timetable slots and exams</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * References Department entity (optional, can be null).
 *
 * <h3>Transaction Behaviour</h3>
 * Read-only in Epic 1 context; created via T-003 migrations.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Entity
@Table(name = "rooms")
@Data
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "room_id")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "LH-101"

    @Column(nullable = false)
    private String displayName; // e.g., "Lecture Hall 101"

    @Column(nullable = false)
    private String roomType; // CLASSROOM, LAB, SEMINAR_HALL, EXAM_HALL, CONFERENCE_ROOM

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = true)
    private String labType; //may be this is error -> i added this

    @Column(nullable = true)
    private Integer benchRows; // For exam halls only

    @Column(nullable = true)
    private Integer benchCols; // For exam halls only

    @Column(nullable = false)
    private Integer floorNumber;

    @Column(nullable = false)
    private String block; // A, B, C, D, Admin, Library

    @Column(nullable = true)
    private String building; // e.g., "Main Block", "PG Block"

    @Column(nullable = true)
    private java.math.BigDecimal latitude; // NUMERIC(10,7) for GPS

    @Column(nullable = true)
    private java.math.BigDecimal longitude; // NUMERIC(10,7) for GPS

    @Column(nullable = true)
    private String directionsText; // Navigation instructions

    @Column(nullable = true)
    private UUID deptOwnerId; // Department owner reference

    @Column(nullable = false)
    private Boolean isActive = true; // Soft delete flag

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}