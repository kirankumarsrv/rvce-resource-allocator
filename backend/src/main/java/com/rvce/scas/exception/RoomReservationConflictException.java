package com.rvce.scas.exception;

/**
 * Thrown when a requested room reservation overlaps with timetable or reservation data.
 */
public class RoomReservationConflictException extends RuntimeException {

    public RoomReservationConflictException(String message) {
        super(message);
    }
}
