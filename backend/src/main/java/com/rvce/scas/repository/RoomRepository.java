package com.rvce.scas.repository;

import com.rvce.scas.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * <h3>Purpose</h3>
 * Repository for querying available rooms based on timetable exclusions.
 * Implements the JPQL query for T-102 availability engine.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Execute complex availability queries with filters</li>
 *   <li>Support pagination for large result sets</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on Room and TimetableSlot entities.
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
     * Finds rooms that are NOT occupied by timetable slots on the given date/time.
     * This is the core query for T-102 availability checks.
     *
     * <p>The query excludes rooms that have a timetable slot on the same day_of_week 
     * and overlapping time window.</p>
     *
     * @param date the date to check availability for
     * @param dayOfWeek the day of week (1-7)
     * @param startTime start of the time window
     * @param endTime end of the time window
     * @param minCapacity minimum room capacity (optional filter)
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
        AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
        """)
    List<Room> findAvailableRooms(
        @Param("date") LocalDate date,
        @Param("dayOfWeek") int dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("minCapacity") Integer minCapacity
    );

    @Query("""
        SELECT r FROM Room r
        WHERE r.id NOT IN (
            SELECT ts.room.id FROM TimetableSlot ts
            WHERE ts.isActive = true
            AND ts.dayOfWeek = :dayOfWeek
            AND ts.startTime < :endTime
            AND ts.endTime > :startTime
        )
        AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
        AND r.building = :building
        """)
    List<Room> findAvailableRoomsByBuilding(
        @Param("date") LocalDate date,
        @Param("dayOfWeek") int dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("minCapacity") Integer minCapacity,
        @Param("building") String building
    );

    Optional<Room> findByName(String name);

}