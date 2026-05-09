package com.rvce.scas.repository;

import com.rvce.scas.entity.RoomDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Repository for querying pre-seeded walking directions from known start points
 * to each room on the RVCE campus.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Fetch step-by-step directions for a specific room and start point</li>
 *   <li>Return steps in deterministic order (by step_order)</li>
 *   <li>Support enumeration of available start points for a room</li>
 * </ul>
 *
 * <h3>Transaction Behaviour</h3>
 * Read-only queries. Used by T-302 (directions API) and T-303 (map directions panel).
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Repository
public interface RoomDirectionRepository extends JpaRepository<RoomDirection, UUID> {

    /**
     * Fetches all direction steps from a specific start point to a room.
     * Steps are returned in ascending order of step_order for deterministic rendering.
     *
     * @param roomId the destination room
     * @param fromLocationTag the starting point (e.g., MAIN_GATE, LIBRARY, CANTEEN)
     * @return list of direction steps ordered by step_order; empty list if no directions exist
     */
    @Query("""
        SELECT rd FROM RoomDirection rd
        WHERE rd.roomId = :roomId
        AND rd.fromLocationTag = :fromLocationTag
        ORDER BY rd.stepOrder ASC
        """)
    List<RoomDirection> findDirectionsByRoomAndStart(
        @Param("roomId") UUID roomId,
        @Param("fromLocationTag") String fromLocationTag
    );

    /**
     * Checks if direction steps exist for a room and start point.
     *
     * @param roomId the destination room
     * @param fromLocationTag the starting point
     * @return true if at least one step exists; false otherwise
     */
    boolean existsByRoomIdAndFromLocationTag(UUID roomId, String fromLocationTag);

}
