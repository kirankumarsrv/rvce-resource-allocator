package com.rvce.scas.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rvce.scas.dto.response.RoomSearchResultDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.mapper.RoomLocationMapper;
import com.rvce.scas.repository.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <h3>Purpose</h3>
 * Service for searching rooms by name, block, or building. Implements T-302 room
 * search endpoint logic using PostgreSQL full-text search with a GIN index.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Execute fast room searches using PostgreSQL GIN indexes</li>
 *   <li>Enforce query length minimum (2 characters) to prevent noise</li>
 *   <li>Limit results to prevent bloated Combobox dropdowns</li>
 *   <li>Support both exact prefix matches (B-Tree) and multi-word searches (GIN)</li>
 * </ul>
 *
 * <h3>Search Strategy</h3>
 * The search uses a two-level strategy for optimal performance:
 * <ol>
 *   <li><strong>Exact prefix match</strong> (B-Tree index on rooms.name):
 *       Fast path for simple exact matches like 'AB-201'. Uses LIKE 'AB-201%'
 *       with a B-Tree index. Runs first; if results found, returns immediately.</li>
 *   <li><strong>Full-text search</strong> (GIN index on to_tsvector):
 *       Fallback for multi-word and partial queries like 'seminar hall A'.
 *       Uses to_tsvector and plainto_tsquery with 'simple' dictionary
 *       (no stemming, to preserve room codes like 'A201').</li>
 * </ol>
 *
 * <h3>Performance Notes</h3>
 * <ul>
 *   <li>GIN index on (name, display_name, block, building) reduces search from O(n) to O(log n)</li>
 *   <li>Results are limited to configurable max (default 10) to keep dropdown manageable</li>
 *   <li>Minimum query length (2 chars) prevents excessive matches (e.g., searching for 'A')</li>
 * </ul>
 *
 * <h3>Transaction Behaviour</h3>
 * Read-only; @Transactional(readOnly=true) optimizes for read performance.
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RoomSearchService {

    private final RoomRepository roomRepository;
    private final RoomLocationMapper roomLocationMapper;

    @Value("${room.search.min-query-length:2}")
    private int minQueryLength;

    @Value("${room.search.max-results:10}")
    private int maxResults;

    /**
     * Searches for rooms by name, block, building, or display name.
     * Results are limited and returned in search relevance order.
     *
     * @param query the search query (must be at least 2 characters)
     * @param limit maximum number of results to return (capped at maxResults)
     * @return list of matching RoomSearchResultDto objects
     * @throws IllegalArgumentException if query length is less than minQueryLength
     */
    public List<RoomSearchResultDto> searchRooms(String query, int limit) {
        if (query == null || query.trim().length() < minQueryLength) {
            throw new IllegalArgumentException(
                "Search query must be at least " + minQueryLength + " characters"
            );
        }

        String trimmedQuery = query.trim();
        int requestedLimit = limit > 0 ? limit : maxResults;
        int limitCapped = Math.max(1, Math.min(requestedLimit, maxResults));

        // Level 1: Exact prefix match (fast path)
        Pageable pageable = PageRequest.of(0, limitCapped, Sort.by(Sort.Direction.ASC, "name"));
        List<Room> exactMatches = roomRepository.findByNameStartingWithIgnoreCaseAndIsActive(
            trimmedQuery, true, pageable
        );

        if (!exactMatches.isEmpty()) {
            log.debug("Room search [exact]: query='{}' found {} results", trimmedQuery, exactMatches.size());
            return roomLocationMapper.toSearchResultDtoList(exactMatches);
        }

        // Level 2: Full-text search (fallback)
        List<Room> fullTextMatches = roomRepository.searchByFullText(trimmedQuery, limitCapped);
        log.debug("Room search [full-text]: query='{}' found {} results", trimmedQuery, fullTextMatches.size());

        return roomLocationMapper.toSearchResultDtoList(fullTextMatches);
    }

}
