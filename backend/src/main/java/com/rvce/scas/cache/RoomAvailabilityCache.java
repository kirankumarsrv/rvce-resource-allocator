package com.rvce.scas.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvce.scas.dto.response.RoomAvailabilityDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * <h3>Purpose</h3>
 * Redis-based cache abstraction for room availability queries (T-102).
 * Implements the caching strategy with TTL and pattern-based invalidation.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Cache room availability results with 60-second TTL</li>
 *   <li>Handle cache misses gracefully with DB fallback</li>
 *   <li>Support pattern-based cache invalidation</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on RedisTemplate for Redis operations.
 *
 * <h3>Transaction Behaviour</h3>
 * Cache operations are independent of DB transactions.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomAvailabilityCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "room:avail";
    private static final int CACHE_TTL_SECONDS = 60;

    /**
     * Retrieves cached room availability data for the given key.
     * Returns empty Optional on cache miss or deserialization failure.
     *
     * @param key the cache key
     * @return optional list of room availability DTOs
     */
    public Optional<List<RoomAvailabilityDto>> get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("Cache miss for key: {}", key);
                return Optional.empty();
            }
            List<RoomAvailabilityDto> result = objectMapper.readValue(json,
                new TypeReference<List<RoomAvailabilityDto>>() {});
            log.debug("Cache hit for key: {}, returned {} rooms", key, result.size());
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("Failed to deserialize cached data for key: {}, error: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Stores room availability data in cache with TTL.
     * Serializes the DTO list to JSON before storing.
     *
     * @param key the cache key
     * @param data the room availability data to cache
     */
    public void put(String key, List<RoomAvailabilityDto> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            log.debug("Cached {} rooms for key: {}", data.size(), key);
        } catch (Exception e) {
            log.warn("Failed to serialize and cache data for key: {}, error: {}", key, e.getMessage());
        }
    }

    /**
     * Invalidates all cache entries for a specific date.
     * Uses Redis SCAN for non-blocking pattern matching.
     *
     * @param date the date to invalidate cache for
     */
    public void invalidateByDate(LocalDate date) {
        String pattern = CACHE_PREFIX + ":" + date + ":*";
        invalidatePattern(pattern);
    }

    /**
     * Invalidates all room availability cache entries.
     * Called when timetable data changes globally.
     */
    public void invalidateAll() {
        String pattern = CACHE_PREFIX + ":*";
        invalidatePattern(pattern);
    }

    /**
     * Helper method to invalidate keys matching a pattern.
     * Uses SCAN to avoid blocking Redis with KEYS command.
     *
     * @param pattern the Redis key pattern to match
     */
    private void invalidatePattern(String pattern) {
        var scanParams = org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).build();
        var cursor = redisTemplate.scan(scanParams);
        List<String> keys = new java.util.ArrayList<>();
        cursor.forEachRemaining(keys::add);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Invalidated {} cache keys matching pattern: {}", keys.size(), pattern);
        }
    }

}