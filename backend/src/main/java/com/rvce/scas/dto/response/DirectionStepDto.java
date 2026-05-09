package com.rvce.scas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>Purpose</h3>
 * Represents a single step in a walking direction sequence.
 * Returned by GET /api/rooms/{id}/directions?from=MAIN_GATE (T-302).
 *
 * <h3>Usage</h3>
 * Steps are always returned in ascending order of stepOrder.
 * The frontend (T-303 directions panel, T-304 exam navigation page)
 * renders each step as a numbered list item with the instruction text
 * and distance marker.
 *
 * <h3>Example</h3>
 * <pre>
 * Step 1: "From main gate, walk 50m straight towards the library" (distance: 50m)
 * Step 2: "Turn left at the library entrance" (distance: 50m)
 * Step 3: "Enter Block A through the blue doors on your right" (distance: 100m)
 * </pre>
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectionStepDto {

    private Short stepOrder;        // 1, 2, 3, ... (position in sequence)
    private String instruction;     // "Walk 50m straight towards the library"
    private Short distanceMeters;   // Cumulative distance from start point
    private String landmark;        // Optional landmark reference in the instruction

}
