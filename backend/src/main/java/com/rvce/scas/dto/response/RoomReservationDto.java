package com.rvce.scas.dto.response;

import com.rvce.scas.entity.RoomReservation;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response payload for room reservation operations.
 */
@Value
@Builder
public class RoomReservationDto {
    Long id;
    UUID roomId;
    String roomName;
    LocalDate reservationDate;
    LocalTime startTime;
    LocalTime endTime;
    String purpose;
    RoomReservation.ReservationStatus status;
    UUID createdBy;
    Instant createdAt;
}
