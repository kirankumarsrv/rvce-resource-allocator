package com.rvce.scas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request payload for creating a room reservation.
 */
@Data
public class RoomReservationRequest {

    @NotNull(message = "Room id is required")
    private @NonNull UUID roomId;

    @NotNull(message = "Reservation date is required")
    private LocalDate reservationDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Size(max = 500, message = "Purpose must be 500 characters or fewer")
    private String purpose;
}
