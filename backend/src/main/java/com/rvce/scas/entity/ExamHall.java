package com.rvce.scas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Exam hall configuration snapshot for one exam session.
 */
@Getter
@Setter
@Entity
@Table(
        name = "exam_halls",
        uniqueConstraints = @UniqueConstraint(name = "uq_exam_room", columnNames = {"exam_id", "room_id"})
)
public class ExamHall {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hall_id", nullable = false, updatable = false)
    private UUID hallId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.MERGE)
    @JoinColumn(name = "exam_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.MERGE)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "assigned_capacity", nullable = false)
    private Short assignedCapacity;

    @Column(name = "total_benches", nullable = false)
    private Short totalBenches;

    @Column(name = "two_seater_count", nullable = false)
    private Short twoSeaterCount;

    @Column(name = "three_seater_count", nullable = false)
    private Short threeSeaterCount;

    @Column(name = "total_capacity", nullable = false)
    private Short totalCapacity;

    @Column(name = "bench_rows", nullable = false)
    private Short benchRows;

    @Column(name = "bench_cols", nullable = false)
    private Short benchCols;

    @ManyToOne(fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.MERGE)
    @JoinColumn(name = "invigilator_id")
    private User invigilator;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;
}