package com.rvce.scas.repository;

import com.rvce.scas.entity.DayOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

/**
 * <h3>Purpose</h3>
 * Repository for day override operations, supporting T-104 cancellation and booking features.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Query overrides by date and room</li>
 *   <li>Check for existing overrides to prevent duplicates</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on DayOverride entity.
 *
 * <h3>Transaction Behaviour</h3>
 * CRUD operations for override management.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Repository
public interface DayOverrideRepository extends JpaRepository<DayOverride, java.util.UUID> {

    /**
     * Finds all overrides for a specific date, optionally filtered by room.
     *
     * @param date the date to query
     * @param roomId optional room filter
     * @return list of overrides for the date
     */
    @Query("""
        SELECT do FROM DayOverride do
        WHERE do.date = :date
        AND (:roomId IS NULL OR do.slot.room.id = :roomId)
        """)
    List<DayOverride> findOverridesByDate(
        @Param("date") LocalDate date,
        @Param("roomId") java.util.UUID roomId
    );

    /**
     * Checks if an override already exists for the given slot and date.
     * Used to enforce idempotency in T-104.
     *
     * @param slotId the slot ID
     * @param date the date
     * @return true if override exists
     */
    boolean existsBySlotIdAndDate(Long slotId, LocalDate date);

}