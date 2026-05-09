package com.rvce.scas.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>Purpose</h3>
 * Minimal DTO for room search results from GET /api/rooms/search?q=...
 * Returned by T-302 search API.
 *
 * <h3>Design Notes</h3>
 * Includes only the fields needed for the map Combobox dropdown:
 * <ul>
 *   <li>id, name: for display in dropdown and to fetch full location</li>
 *   <li>floor, block, building: for disambiguating (e.g., "AB-201 Block A, Floor 2")</li>
 *   <li>latitude, longitude: for map.panTo() when user clicks a result</li>
 * </ul>
 * Does NOT include floor plan URL, landmark description, or directions —
 * those are fetched separately via GET /api/rooms/{id}/location if needed.
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSearchResultDto {

    private UUID id;
    private String name;
    @JsonProperty("floor")
    private Integer floorNumber;
    private String block;
    private String building;
    private BigDecimal latitude;
    private BigDecimal longitude;

}
