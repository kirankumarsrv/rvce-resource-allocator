package com.rvce.scas.service;

import com.rvce.scas.cache.RoomAvailabilityCache;
import com.rvce.scas.dto.request.RoomReservationRequest;
import com.rvce.scas.dto.response.RoomReservationDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.RoomReservation;
import com.rvce.scas.entity.RoomReservation.ReservationStatus;
import com.rvce.scas.entity.User;
import com.rvce.scas.exception.RoomNotFoundException;
import com.rvce.scas.exception.RoomReservationConflictException;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.RoomReservationRepository;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public class RoomReservationService {

    private final RoomRepository roomRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final RoomAvailabilityCache roomAvailabilityCache;
    private final UserRepository userRepository;

    @Transactional
    public RoomReservationDto createReservation(@NonNull RoomReservationRequest request, @NonNull JwtPrincipal principal) {
        validate(request);

        @NonNull UUID roomId = Objects.requireNonNull(request.getRoomId(), "Room id is required.");

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + request.getRoomId()));

        if (hasTimetableConflict(room.getId(), request.getReservationDate(), request.getStartTime(), request.getEndTime())) {
            throw new RoomReservationConflictException("Room " + room.getName() + " is already occupied by timetable slots for the selected time window.");
        }

        if (roomReservationRepository.existsActiveConflict(
                room.getId(),
                request.getReservationDate(),
                request.getStartTime(),
                request.getEndTime(),
                ReservationStatus.RESERVED)) {
            throw new RoomReservationConflictException("Room " + room.getName() + " already has a reservation for the selected time window.");
        }

        User currentUser = resolveCurrentUser(principal);
        RoomReservation reservation = new RoomReservation();
        reservation.setRoom(room);
        reservation.setReservationDate(request.getReservationDate());
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPurpose(StringUtils.hasText(request.getPurpose()) ? request.getPurpose().trim() : null);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setCreatedBy(currentUser.getUserId());
        reservation.setCreatedAt(Instant.now());

        RoomReservation saved = roomReservationRepository.save(reservation);
        roomAvailabilityCache.invalidateByDate(request.getReservationDate());
        return map(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomReservationDto> getReservations(LocalDate reservationDate, UUID roomId) {
        List<RoomReservation> reservations = roomId == null
                ? roomReservationRepository.findByReservationDateOrderByStartTimeAsc(reservationDate)
                : roomReservationRepository.findByReservationDateAndRoom_IdOrderByStartTimeAsc(reservationDate, roomId);
        return reservations.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<RoomReservationDto> getMyReservations(LocalDate reservationDate, UUID roomId, @NonNull JwtPrincipal principal) {
        User currentUser = resolveCurrentUser(principal);
        List<RoomReservation> reservations = roomId == null
                ? roomReservationRepository.findByReservationDateAndCreatedByOrderByStartTimeAsc(reservationDate, currentUser.getUserId())
                : roomReservationRepository.findByReservationDateAndCreatedByAndRoom_IdOrderByStartTimeAsc(
                        reservationDate,
                        currentUser.getUserId(),
                        roomId);
        return reservations.stream().map(this::map).toList();
    }

    @Transactional
    public RoomReservationDto cancelReservation(@NonNull Long reservationId, @NonNull JwtPrincipal principal) {
        RoomReservation reservation = roomReservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Room reservation not found: " + reservationId));

        User currentUser = resolveCurrentUser(principal);
        if (!reservation.getCreatedBy().equals(currentUser.getUserId())
                && !hasAnyRole(currentUser, "ADMIN", "TTO")) {
            throw new AccessDeniedException("You do not have permission to cancel this reservation.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        RoomReservation saved = roomReservationRepository.save(reservation);
        roomAvailabilityCache.invalidateByDate(saved.getReservationDate());
        return map(saved);
    }

    private boolean hasTimetableConflict(UUID roomId, LocalDate reservationDate, LocalTime startTime, LocalTime endTime) {
        return timetableSlotRepository.existsRoomTimeConflict(roomId, reservationDate.getDayOfWeek().getValue(), startTime, endTime);
    }

    private void validate(RoomReservationRequest request) {
        if (request.getReservationDate() == null) {
            throw new IllegalArgumentException("Reservation date is required.");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Start and end times are required.");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be earlier than end time.");
        }
    }

    private User resolveCurrentUser(@NonNull JwtPrincipal principal) {
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Unable to resolve the current user."));
    }

    private boolean hasAnyRole(User user, String... roles) {
        return user.getUserRoles().stream()
            .map(userRole -> userRole.getRole())
            .map(role -> role.getName())
                .anyMatch(roleName -> java.util.Arrays.stream(roles).anyMatch(roleName::equalsIgnoreCase));
    }

    private RoomReservationDto map(RoomReservation reservation) {
        return RoomReservationDto.builder()
                .id(reservation.getId())
                .roomId(reservation.getRoom().getId())
                .roomName(reservation.getRoom().getName())
                .reservationDate(reservation.getReservationDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .purpose(reservation.getPurpose())
                .status(reservation.getStatus())
                .createdBy(reservation.getCreatedBy())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
