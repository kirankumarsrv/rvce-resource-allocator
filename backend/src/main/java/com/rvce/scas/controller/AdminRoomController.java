package com.rvce.scas.controller;

import com.rvce.scas.dto.request.RoomLocationUpdateDto;
import com.rvce.scas.dto.response.RoomLocationDto;
import com.rvce.scas.service.RoomLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * REST controller for admin-only room management endpoints.
 * Implements T-301 admin operations for updating room navigation data.
 *
 * <h3>Authorization</h3>
 * All endpoints in this controller require SUPER_ADMIN role.
 * A TTO (Timetable Officer) cannot accidentally overwrite room GPS coordinates.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>PUT /api/admin/rooms/{id}/location - Update room GPS coordinates and navigation metadata</li>
 * </ul>
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminRoomController {

    private final RoomLocationService roomLocationService;

    /**
     * Updates room location data: GPS coordinates, landmark description, and floor plan S3 key.
     * Restricted to SUPER_ADMIN role only.
     *
     * @param roomId the room ID (UUID)
     * @param updateDto the location update request DTO
     * @return updated RoomLocationDto
     * @throws com.rvce.scas.exception.RoomNotFoundException if room does not exist or is inactive
     */
    @PutMapping("/{roomId}/location")
    public ResponseEntity<RoomLocationDto> updateLocation(
        @PathVariable UUID roomId,
        @Valid @RequestBody RoomLocationUpdateDto updateDto
    ) {
        log.info("Admin updating room location: roomId={}, latitude={}, longitude={}",
            roomId, updateDto.getLatitude(), updateDto.getLongitude());

        RoomLocationDto updated = roomLocationService.updateLocation(roomId, updateDto);

        return ResponseEntity.ok(updated);
    }

}
