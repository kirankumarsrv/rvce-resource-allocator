package com.rvce.scas.service;

import com.rvce.scas.cache.RoomAvailabilityCache;
import com.rvce.scas.dto.response.RoomAvailabilityDto;
import com.rvce.scas.mapper.RoomMapper;
import com.rvce.scas.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * <h3>Purpose</h3>
 * Service for room availability queries (T-102).
 * Implements the availability query engine with Redis caching.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Execute availability queries with caching</li>
 *   <li>Build cache keys deterministically</li>
 *   <li>Handle cache misses with DB fallback</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on RoomRepository, RoomAvailabilityCache, RoomMapper.
 *
 * <h3>Transaction Behaviour</h3>
 * getAvailable() is @Transactional(readOnly=true) for performance.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomAvailabilityService {

    private final RoomRepository roomRepository;
    private final RoomAvailabilityCache cache;
    private final RoomMapper roomMapper;

    /**
     * Retrieves available rooms for the given query parameters.
     * Implements Redis caching with 60-second TTL for performance.
     *
     * <p>Decision DD-06: Redis caching reduces DB load for repeated queries.</p>
     *
     * @param date the date to check (nullable, defaults to today)
     * @param startTime start of time window
     * @param endTime end of time window
     * @param minCapacity minimum room capacity (optional)
     * @param building building filter (optional)
     * @return list of available rooms, never null
     */
    @Transactional(readOnly = true)
    public List<RoomAvailabilityDto> getAvailable(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Integer minCapacity,
            String building) {

        if (date == null) {
            date = LocalDate.now();
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (minCapacity != null && minCapacity < 0) {
            throw new IllegalArgumentException("minCapacity must be a non-negative integer");
        }

        // Build query object
        AvailabilityQuery query = new AvailabilityQuery(date, startTime, endTime, minCapacity, building);

        // Build cache key
        String cacheKey = buildCacheKey(query);

        // Check cache first
        Optional<List<RoomAvailabilityDto>> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            log.debug("Cache hit for availability query");
            return cached.get();
        }

        // Cache miss: query database
        log.debug("Cache miss, querying database for availability");
        int dayOfWeek = query.date.getDayOfWeek().getValue();
        List<com.rvce.scas.entity.Room> rooms;
        if (query.building == null) {
            rooms = roomRepository.findAvailableRooms(
                query.date, dayOfWeek, query.startTime, query.endTime, query.minCapacity
            );
        } else {
            rooms = roomRepository.findAvailableRoomsByBuilding(
                query.date, dayOfWeek, query.startTime, query.endTime, query.minCapacity, query.building
            );
        }

        // Only exam halls can be added as exam seating halls.
        rooms = rooms.stream()
                .filter(room -> room.getRoomType() != null && room.getRoomType().equalsIgnoreCase("EXAM_HALL"))
                .toList();

        List<RoomAvailabilityDto> result = roomMapper.toDtoList(rooms);

        // Cache the result
        cache.put(cacheKey, result);

        return result;
    }

    /**
     * Builds a deterministic cache key for the availability query.
     * Key format: room:avail:{date}:{startTime}-{endTime}[:{building}][:{minCapacity}]
     *
     * @param query the availability query
     * @return the cache key
     */
    public String buildCacheKey(AvailabilityQuery query) {
        StringBuilder key = new StringBuilder("room:avail:")
            .append(query.date)
            .append(":")
            .append(query.startTime)
            .append("-")
            .append(query.endTime);

        if (query.building != null) {
            key.append(":").append(query.building);
        }
        if (query.minCapacity != null) {
            key.append(":").append(query.minCapacity);
        }

        return key.toString();
    }

    /**
     * Value object for availability queries.
     */
    public record AvailabilityQuery(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer minCapacity,
        String building
    ) {}

}