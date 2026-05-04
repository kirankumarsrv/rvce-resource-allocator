package com.rvce.scas.repository;

import com.rvce.scas.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * <h3>Purpose</h3>
 * Repository for querying available rooms based on timetable and override exclusions.
 * Implements the double-exclusion JPQL query for T-102 availability engine.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Execute complex availability queries with filters</li>
 *   <li>Support pagination for large result sets</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on Room, TimetableSlot, DayOverride entities.
 *
 * <h3>Transaction Behaviour</h3>
 * Read-only queries for availability checks.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, java.util.UUID> {

    /**
     * Finds rooms that are NOT occupied by timetable slots or day overrides on the given date/time.
     * This is the core double-exclusion query for T-102.
     *
     * <p>The query excludes rooms that have:
     * 1. A timetable slot on the same day_of_week and overlapping time
     * 2. A day override with OCCUPIED status on the exact date
     * (CANCELLED overrides make the room available, so they are not excluded)</p>
     *
     * @param date the date to check availability for
     * @param startTime start of the time window
     * @param endTime end of the time window
     * @param minCapacity minimum room capacity (optional filter)
     * @param building building filter (optional)
     * @return list of available rooms matching criteria
     */
    @Query("""
        SELECT r FROM Room r
        WHERE r.id NOT IN (
            SELECT ts.room.id FROM TimetableSlot ts
            WHERE ts.isActive = true
            AND ts.dayOfWeek = :dayOfWeek
            AND ts.startTime < :endTime
            AND ts.endTime > :startTime
        )
        AND r.id NOT IN (
            SELECT do.slot.room.id FROM DayOverride do
            WHERE do.date = :date
            AND do.status = 'OCCUPIED'
        )
        AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
        AND (:building IS NULL OR r.building = :building)
        """)
    List<Room> findAvailableRooms(
        @Param("date") LocalDate date,
        @Param("dayOfWeek") int dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("minCapacity") Integer minCapacity,
        @Param("building") String building
    );

}