package com.rvce.scas.service;

import com.rvce.scas.dto.response.DirectionStepDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.RoomDirection;
import com.rvce.scas.exception.DirectionsNotFoundException;
import com.rvce.scas.exception.RoomNotFoundException;
import com.rvce.scas.mapper.RoomLocationMapper;
import com.rvce.scas.repository.RoomDirectionRepository;
import com.rvce.scas.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for reading pre-seeded walking directions for a room.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RoomDirectionsService {

    private final RoomRepository roomRepository;
    private final RoomDirectionRepository roomDirectionRepository;
    private final RoomLocationMapper roomLocationMapper;

    /**
     * Returns deterministic walking directions for a room from a given start point.
     *
     * @param roomId destination room id
     * @param fromLocationTag start location tag such as MAIN_GATE or LIBRARY
     * @return ordered list of direction steps
     */
    public List<DirectionStepDto> getDirections(UUID roomId, String fromLocationTag) {
        Room room = roomRepository.findById(roomId)
            .filter(Room::getIsActive)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        String normalizedTag = normalizeTag(fromLocationTag);
        if (!roomDirectionRepository.existsByRoomIdAndFromLocationTag(room.getId(), normalizedTag)) {
            throw new DirectionsNotFoundException(
                "No directions found for room " + roomId + " from " + normalizedTag
            );
        }

        List<RoomDirection> directions = roomDirectionRepository.findDirectionsByRoomAndStart(room.getId(), normalizedTag);
        if (directions.isEmpty()) {
            throw new DirectionsNotFoundException(
                "No directions found for room " + roomId + " from " + normalizedTag
            );
        }

        return roomLocationMapper.toDirectionStepDtoList(
            directions.stream()
                .sorted(Comparator.comparing(RoomDirection::getStepOrder))
                .toList()
        );
    }

    private String normalizeTag(String fromLocationTag) {
        if (fromLocationTag == null || fromLocationTag.trim().isEmpty()) {
            throw new IllegalArgumentException("Direction start point must not be blank");
        }
        return fromLocationTag.trim().toUpperCase(Locale.ROOT);
    }
}