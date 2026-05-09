package com.rvce.scas.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rvce.scas.dto.response.DirectionStepDto;
import com.rvce.scas.dto.response.RoomLocationDto;
import com.rvce.scas.dto.response.RoomSearchResultDto;
import com.rvce.scas.service.RoomDirectionsService;
import com.rvce.scas.service.RoomLocationService;
import com.rvce.scas.service.RoomSearchService;

import lombok.RequiredArgsConstructor;

/**
 * <h3>Purpose</h3>
 * REST controller for room-related endpoints including availability checks,
 * occupancy verification, and navigation (T-301 location endpoints).
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>GET /api/rooms/availability - Room availability check</li>
 *   <li>POST /api/rooms/{roomId}/verify-occupancy - Verify room occupancy</li>
 *   <li>GET /api/rooms/{id}/location - [T-301] Get room location for navigation (public)</li>
 * </ul>
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomsController {

    private final RoomLocationService roomLocationService;
    private final RoomSearchService roomSearchService;
    private final RoomDirectionsService roomDirectionsService;

    @GetMapping("/availability")
    public ResponseEntity<Map<String, String>> availability() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/{roomId}/verify-occupancy")
    public ResponseEntity<Map<String, String>> verifyOccupancy(@PathVariable UUID roomId) {
        return ResponseEntity.ok(Map.of("roomId", roomId.toString(), "status", "verified"));
    }

    /**
     * Fetches room location details for campus navigation.
     * This endpoint is PUBLIC — no JWT required.
     * Students need this on exam day even with expired sessions.
     *
     * @param id the room ID (UUID)
     * @return RoomLocationDto with GPS coordinates, building info, and floor plan URL
     * @throws com.rvce.scas.exception.RoomNotFoundException if room does not exist or is inactive
     */
    @GetMapping("/{id}/location")
    public ResponseEntity<RoomLocationDto> getLocation(@PathVariable UUID id) {
        RoomLocationDto location = roomLocationService.getLocationById(id);
        return ResponseEntity.ok(location);
    }

    /**
     * Searches rooms for the campus navigation combobox.
     *
     * @param query search text from the user
     * @param limit optional cap on returned results
     * @return matching rooms with coordinates
     */
    @GetMapping("/search")
    public ResponseEntity<List<RoomSearchResultDto>> searchRooms(
        @RequestParam("q") String query,
        @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(roomSearchService.searchRooms(query, limit));
    }

    /**
     * Returns pre-seeded walking directions for a room from a known start point.
     *
     * @param id room identifier
     * @param from starting location tag
     * @return ordered direction steps
     */
    @GetMapping("/{id}/directions")
    public ResponseEntity<List<DirectionStepDto>> getDirections(
        @PathVariable UUID id,
        @RequestParam("from") String from
    ) {
        return ResponseEntity.ok(roomDirectionsService.getDirections(id, from));
    }

}
