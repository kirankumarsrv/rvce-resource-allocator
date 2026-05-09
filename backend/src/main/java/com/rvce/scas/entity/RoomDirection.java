package com.rvce.scas.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Stores pre-seeded step-by-step walking directions from known start points
 * (MAIN_GATE, LIBRARY, CANTEEN) to each room on the RVCE campus.
 * Used by T-302 (Room Search & Directions API) and T-303 (Interactive Campus Map).
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Store atomic direction steps in order (step_order 1, 2, 3, ...)</li>
 *   <li>Link each step to its destination room via room_id</li>
 *   <li>Identify the start point (from_location_tag: MAIN_GATE, LIBRARY, CANTEEN)</li>
 *   <li>Track cumulative walking distance for each step</li>
 * </ul>
 *
 * <h3>Data Model Notes</h3>
 * <ul>
 *   <li>Pre-seeded: Directions are manually written during a campus walkthrough
 *       by a team member and stored in the DB. No live Google Maps API calls needed.</li>
 *   <li>Deterministic ordering: step_order ensures steps are always returned in the
 *       same sequence, regardless of insertion order.</li>
 *   <li>Offline-friendly: All direction data is downloaded with the room info;
 *       students can view directions without network on exam day.</li>
 * </ul>
 *
 * <h3>Transaction Behaviour</h3>
 * Created and updated by T-301 (admin endpoint) and T-302 (direction seeding).
 * Read by T-303 (map directions panel) and T-304 (exam navigation page).
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Entity
@Table(
    name = "room_directions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_room_directions",
        columnNames = {"room_id", "from_location_tag", "step_order"}
    )
)
@Data
public class RoomDirection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "direction_id")
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId; // References rooms.room_id (not mapped as @ManyToOne to keep entity lightweight)

    @Column(nullable = false, length = 50)
    private String fromLocationTag; // MAIN_GATE, LIBRARY, CANTEEN, etc.

    @Column(nullable = false)
    private Short stepOrder; // 1, 2, 3, ... (enforces deterministic ordering)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instruction; // "Walk 50m straight towards the library"

    @Column(nullable = false)
    private Short distanceMeters; // Cumulative walking distance from start point

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

}
