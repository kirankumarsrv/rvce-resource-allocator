package com.rvce.scas.entity;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a concrete room reservation for a date/time window.
 * Reservations are separate from timetable overrides because they are room-centric,
 * while timetable overrides remain slot-centric.
 */
@Data
public class RoomReservation {

    private Long id;
    private Room room;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;
    private ReservationStatus status;
    private UUID createdBy;
    private Instant createdAt;

    public enum ReservationStatus {
        RESERVED,
        CANCELLED
    }
}
