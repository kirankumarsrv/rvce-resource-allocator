package com.rvce.scas.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timetable_versions")
@Data
public class TimetableVersion {

    @Id
    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(nullable = false)
    private Integer semester;

    private String label;

    @Column(nullable = false)
    private String status; // DRAFT | ACTIVE | ARCHIVED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}