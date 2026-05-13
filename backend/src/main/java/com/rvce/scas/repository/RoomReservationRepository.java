package com.rvce.scas.repository;

import com.rvce.scas.entity.RoomReservation;
import com.rvce.scas.entity.RoomReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@NoRepositoryBean
public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    @Query("""
            select case when count(rr) > 0 then true else false end
            from RoomReservation rr
            where rr.room.id = :roomId
              and rr.reservationDate = :reservationDate
              and rr.status = :status
              and rr.startTime < :endTime
              and rr.endTime > :startTime
            """)
    boolean existsActiveConflict(@Param("roomId") java.util.UUID roomId,
                                 @Param("reservationDate") LocalDate reservationDate,
                                 @Param("startTime") LocalTime startTime,
                                 @Param("endTime") LocalTime endTime,
                                 @Param("status") ReservationStatus status);

    List<RoomReservation> findByReservationDateOrderByStartTimeAsc(LocalDate reservationDate);

    List<RoomReservation> findByReservationDateAndRoom_IdOrderByStartTimeAsc(LocalDate reservationDate, java.util.UUID roomId);

    List<RoomReservation> findByReservationDateAndCreatedByOrderByStartTimeAsc(LocalDate reservationDate, UUID createdBy);

    List<RoomReservation> findByReservationDateAndCreatedByAndRoom_IdOrderByStartTimeAsc(
        LocalDate reservationDate,
        UUID createdBy,
        UUID roomId);
}
