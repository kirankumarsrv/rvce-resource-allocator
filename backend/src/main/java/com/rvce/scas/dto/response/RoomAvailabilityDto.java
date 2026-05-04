package com.rvce.scas.dto.response;

import lombok.Data;
import java.util.UUID;

/**
 * Response DTO for room availability queries (T-102).
 * Represents a room that is available for booking.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Data
public class RoomAvailabilityDto {

    /**
     * Unique identifier of the room.
     */
    private UUID id;

    /**
     * Human-readable name of the room (e.g., "LH-101").
     */
    private String name;

    /**
     * Maximum capacity of the room.
     */
    private Integer capacity;

    /**
     * Building where the room is located.
     */
    private String building;

    /**
     * Floor number of the room.
     */
    private Integer floor;

}