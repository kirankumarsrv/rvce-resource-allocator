package com.rvce.scas.repository;

import com.rvce.scas.entity.TimetableSlot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;

/**
 * <h3>Purpose</h3>
 * Repository for timetable slot operations, including clash detection and substitution queries.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Fetch slots for teacher substitution operations</li>
 *   <li>Check for scheduling conflicts</li>
 *   <li>Support bulk updates for substitutions</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on TimetableSlot entity.
 *
 * <h3>Transaction Behaviour</h3>
 * Read operations for clash checks; write operations for substitutions.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

    /**
     * Finds all active slots for a teacher within a date range.
     * Used by T-103 substitution engine to identify slots to reassign.
     *
     * @param teacherId the teacher whose slots to find
     * @param startDate start of the date range (inclusive)
     * @param endDate end of the date range (inclusive)
     * @return list of active slots for the teacher in the range
     */
    @Query("""
        SELECT ts FROM TimetableSlot ts
        WHERE ts.isActive = true
        AND ts.teacher.userId = :teacherId
        AND ts.dayOfWeek BETWEEN :startDayOfWeek AND :endDayOfWeek
        """)
    List<TimetableSlot> findSlotsForTeacherInRange(
        @Param("teacherId") UUID teacherId,
        @Param("startDayOfWeek") int startDayOfWeek,
        @Param("endDayOfWeek") int endDayOfWeek
    );

    @Modifying
    @Query("UPDATE TimetableSlot ts SET ts.isActive = false " +
        "WHERE ts.department = :department AND ts.isActive = true")
    int deactivateActiveForDepartment(@Param("department") String department);
    
    @Query("""
        SELECT ts FROM TimetableSlot ts
        WHERE ts.isActive = true
        AND ts.teacher.userId = :teacherId
        AND ts.dayOfWeek IN :dayOfWeeks
        """)
    List<TimetableSlot> findSlotsForTeacherOnDays(
        @Param("teacherId") UUID teacherId,
        @Param("dayOfWeeks") List<Integer> dayOfWeeks
    );

    /**
     * Checks if a teacher has any slot that would conflict with the given time on the given day.
     * Used for clash detection in T-103 substitution.
     *
     * @param teacherId the teacher to check
     * @param dayOfWeek the day of the week
     * @param startTime start time to check
     * @param endTime end time to check
     * @return true if there is a conflicting slot
     */
    @Query("""
        SELECT COUNT(ts) > 0 FROM TimetableSlot ts
        WHERE ts.isActive = true
        AND ts.teacher.userId = :teacherId
        AND ts.dayOfWeek = :dayOfWeek
        AND ts.startTime < :endTime
        AND ts.endTime > :startTime
        """)
    boolean existsConflictingSlot(
        @Param("teacherId") UUID teacherId,
        @Param("dayOfWeek") int dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    @Query("""
        SELECT COUNT(ts) > 0 FROM TimetableSlot ts
        WHERE ts.isActive = true
        AND ts.room.id = :roomId
        AND ts.dayOfWeek = :dayOfWeek
        AND ts.startTime < :endTime
        AND ts.endTime > :startTime
        """)
    boolean existsRoomTimeConflict(
        @Param("roomId") java.util.UUID roomId,
        @Param("dayOfWeek") int dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    @Query("""
        SELECT ts FROM TimetableSlot ts
        WHERE ts.isActive = true
        AND ts.room.id = :roomId
        AND ts.dayOfWeek = :dayOfWeek
        AND ts.startTime < :endTime
        AND ts.endTime > :startTime
        """)
    List<TimetableSlot> findActiveRoomConflicts(
        @Param("roomId") Long roomId,
        @Param("dayOfWeek") int dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        Pageable pageable
    );

    /**
     * Finds all slots for a teacher on a specific day of week (T-105 queries).
     *
     * @param teacherId the teacher ID
     * @param dayOfWeek the day of week (1=Monday, 7=Sunday)
     * @return list of slots for that teacher on that day
     */
    @Query("""
        SELECT ts FROM TimetableSlot ts
        WHERE ts.teacher.userId = :teacherId
        AND ts.dayOfWeek = :dayOfWeek
        """)
    List<TimetableSlot> findByTeacherIdAndDayOfWeek(
        @Param("teacherId") UUID teacherId,
        @Param("dayOfWeek") Integer dayOfWeek
    );

    /**
     * Finds all slots for a room on a specific day of week (T-105 queries).
     *
     * @param roomId the room ID
     * @param dayOfWeek the day of week
     * @return list of slots in that room on that day
     */
    @Query("""
        SELECT ts FROM TimetableSlot ts
        WHERE ts.room.id = :roomId
        AND ts.dayOfWeek = :dayOfWeek
        """)
    List<TimetableSlot> findByRoomIdAndDayOfWeek(
        @Param("roomId") Long roomId,
        @Param("dayOfWeek") Integer dayOfWeek
    );

    /**
     * Counts active slots for analytics (T-105).
     */
    @Query("SELECT COUNT(ts) FROM TimetableSlot ts WHERE ts.isActive = true")
    long countByIsActive(boolean isActive);

    /**
     * Counts distinct teachers for analytics (T-105).
     */
    @Query("SELECT COUNT(DISTINCT ts.teacher.userId) FROM TimetableSlot ts")
    long countDistinctTeachers();

    /**
     * Counts distinct rooms for analytics (T-105).
     */
    @Query("SELECT COUNT(DISTINCT ts.room.id) FROM TimetableSlot ts")
    long countDistinctRooms();

    /**
     * Counts slots for a specific day of week (T-105).
     */
    @Query("SELECT COUNT(ts) FROM TimetableSlot ts WHERE ts.dayOfWeek = :dayOfWeek")
    long countByDayOfWeek(@Param("dayOfWeek") int dayOfWeek);

    /**
     * Counts slots within a time range (T-105 analytics).
     * Used for morning/afternoon/evening utilization metrics.
     */
    @Query("""
        SELECT COUNT(ts) FROM TimetableSlot ts
        WHERE ts.startTime >= :startTime AND ts.startTime < :endTime
        """)
    long countSlotsInTimeRange(
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

}