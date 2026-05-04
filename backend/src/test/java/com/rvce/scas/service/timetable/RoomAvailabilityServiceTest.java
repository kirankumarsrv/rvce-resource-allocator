package com.rvce.scas.service.timetable;

import com.rvce.scas.cache.RoomAvailabilityCache;
import com.rvce.scas.dto.response.RoomAvailabilityDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.mapper.RoomMapper;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.service.RoomAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Unit and integration tests for RoomAvailabilityService (T-102).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-102: Availability Query Engine Tests")
class RoomAvailabilityServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomAvailabilityCache cache;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomAvailabilityService availabilityService;

    private LocalDate testDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Room room1;
    private Room room2;
    private Room room3;
    private RoomAvailabilityDto dto1;
    private RoomAvailabilityDto dto2;
    private RoomAvailabilityDto dto3;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now();
        startTime = LocalTime.of(9, 0);
        endTime = LocalTime.of(10, 0);

        // Setup test rooms
        room1 = new Room();
        room1.setId(UUID.randomUUID());
        room1.setName("LH-101");
        room1.setCapacity(60);
        room1.setBuilding("Block A");
        room1.setFloorNumber(1);

        room2 = new Room();
        room2.setId(UUID.randomUUID());
        room2.setName("LH-102");
        room2.setCapacity(50);
        room2.setBuilding("Block B");
        room2.setFloorNumber(1);

        room3 = new Room();
        room3.setId(UUID.randomUUID());
        room3.setName("LH-103");
        room3.setCapacity(40);
        room3.setBuilding("Block A");
        room3.setFloorNumber(2);

        // Setup DTOs
        dto1 = new RoomAvailabilityDto();
        dto1.setId(room1.getId());
        dto1.setName("LH-101");
        dto1.setCapacity(60);
        dto1.setBuilding("Block A");
        dto1.setFloor(1);

        dto2 = new RoomAvailabilityDto();
        dto2.setId(room2.getId());
        dto2.setName("LH-102");
        dto2.setCapacity(50);
        dto2.setBuilding("Block B");
        dto2.setFloor(1);

        dto3 = new RoomAvailabilityDto();
        dto3.setId(room3.getId());
        dto3.setName("LH-103");
        dto3.setCapacity(40);
        dto3.setBuilding("Block A");
        dto3.setFloor(2);
    }

    @Test
    @DisplayName("Query with all params → correct rooms returned, none occupied in that slot")
    void testQueryWithAllParams() {
        // Setup
        int dayOfWeek = testDate.getDayOfWeek().getValue();
        List<Room> availableRooms = List.of(room1, room2);
        List<RoomAvailabilityDto> expectedDtos = List.of(dto1, dto2);

        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(roomRepository.findAvailableRooms(eq(testDate), eq(dayOfWeek), eq(startTime), eq(endTime), eq(null), eq(null)))
            .thenReturn(availableRooms);
        when(roomMapper.toDtoList(eq(availableRooms))).thenReturn(expectedDtos);
        doNothing().when(cache).put(anyString(), eq(expectedDtos));

        // Execute
        List<RoomAvailabilityDto> result = availabilityService.getAvailable(testDate, startTime, endTime, null, null);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("LH-101", result.get(0).getName());
        assertEquals("LH-102", result.get(1).getName());

        verify(roomRepository, times(1)).findAvailableRooms(testDate, dayOfWeek, startTime, endTime, null, null);
        verify(cache, times(1)).put(anyString(), anyList());
    }

    @Test
    @DisplayName("Query with no params → all rooms returned (no filters)")
    void testQueryWithNoParams() {
        // Setup
        LocalDate today = LocalDate.now();
        LocalTime earlyMorning = LocalTime.of(6, 0);
        LocalTime midnight = LocalTime.of(23, 59);
        int dayOfWeek = today.getDayOfWeek().getValue();

        List<Room> allRooms = List.of(room1, room2, room3);
        List<RoomAvailabilityDto> allDtos = List.of(dto1, dto2, dto3);

        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(roomRepository.findAvailableRooms(eq(today), eq(dayOfWeek), eq(earlyMorning), eq(midnight), eq(null), eq(null)))
            .thenReturn(allRooms);
        when(roomMapper.toDtoList(eq(allRooms))).thenReturn(allDtos);
        doNothing().when(cache).put(anyString(), eq(allDtos));

        // Execute
        List<RoomAvailabilityDto> result = availabilityService.getAvailable(today, earlyMorning, midnight, null, null);

        // Verify
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("Room occupied by timetable slot on that day → excluded from result")
    void testRoomExcludedByTimetableSlot() {
        // Setup: Only room2 and room3 are available (room1 is occupied)
        int dayOfWeek = testDate.getDayOfWeek().getValue();
        List<Room> availableRooms = List.of(room2, room3);
        List<RoomAvailabilityDto> expectedDtos = List.of(dto2, dto3);

        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(roomRepository.findAvailableRooms(eq(testDate), eq(dayOfWeek), eq(startTime), eq(endTime), eq(null), eq(null)))
            .thenReturn(availableRooms);
        when(roomMapper.toDtoList(eq(availableRooms))).thenReturn(expectedDtos);
        doNothing().when(cache).put(anyString(), eq(expectedDtos));

        // Execute
        List<RoomAvailabilityDto> result = availabilityService.getAvailable(testDate, startTime, endTime, null, null);

        // Verify
        assertEquals(2, result.size());
        assertFalse(result.stream().anyMatch(r -> "LH-101".equals(r.getName())));
    }

    @Test
    @DisplayName("First call: DB query runs, result written to Redis")
    void testCacheMiss() {
        // Setup: Cache miss, so repository should be called
        int dayOfWeek = testDate.getDayOfWeek().getValue();
        List<Room> roomList = List.of(room1);
        List<RoomAvailabilityDto> dtoList = List.of(dto1);

        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(roomRepository.findAvailableRooms(eq(testDate), eq(dayOfWeek), eq(startTime), eq(endTime), eq(null), eq(null)))
            .thenReturn(roomList);
        when(roomMapper.toDtoList(eq(roomList))).thenReturn(dtoList);
        doNothing().when(cache).put(anyString(), eq(dtoList));

        // Execute
        List<RoomAvailabilityDto> result = availabilityService.getAvailable(testDate, startTime, endTime, null, null);

        // Verify
        assertEquals(1, result.size());
        verify(cache, times(1)).get(anyString());
        verify(roomRepository, times(1)).findAvailableRooms(eq(testDate), eq(dayOfWeek), eq(startTime), eq(endTime), eq(null), eq(null));
        verify(cache, times(1)).put(anyString(), eq(dtoList));
    }

    @Test
    @DisplayName("Second identical call: Redis hit, no DB query, response < 50ms")
    void testCacheHit() {
        // Setup: Cache hit
        List<RoomAvailabilityDto> cachedDtos = List.of(dto1, dto2);
        String cacheKey = availabilityService.buildCacheKey(
            new RoomAvailabilityService.AvailabilityQuery(testDate, startTime, endTime, null, null)
        );

        when(cache.get(cacheKey)).thenReturn(Optional.of(cachedDtos));

        // Execute
        long startMillis = System.currentTimeMillis();
        List<RoomAvailabilityDto> result = availabilityService.getAvailable(testDate, startTime, endTime, null, null);
        long elapsedMillis = System.currentTimeMillis() - startMillis;

        // Verify
        assertEquals(2, result.size());
        verify(cache, times(1)).get(cacheKey);
        verify(roomRepository, never()).findAvailableRooms(any(), anyInt(), any(), any(), any(), any());
        assertTrue(elapsedMillis < 50, "Cache hit should be faster than 50ms");
    }

    @Test
    @DisplayName("minCapacity=50: rooms with capacity < 50 excluded")
    void testCapacityFilter() {
        // Setup: Only room1 and room2 have capacity >= 50
        int dayOfWeek = testDate.getDayOfWeek().getValue();
        int minCapacity = 50;
        List<Room> availableRooms = List.of(room1, room2);
        List<RoomAvailabilityDto> expectedDtos = List.of(dto1, dto2);

        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(roomRepository.findAvailableRooms(eq(testDate), eq(dayOfWeek), eq(startTime), eq(endTime), eq(minCapacity), eq(null)))
            .thenReturn(availableRooms);
        when(roomMapper.toDtoList(eq(availableRooms))).thenReturn(expectedDtos);
        doNothing().when(cache).put(anyString(), eq(expectedDtos));

        // Execute
        List<RoomAvailabilityDto> result = availabilityService.getAvailable(testDate, startTime, endTime, minCapacity, null);

        // Verify
        assertEquals(2, result.size());
        assertFalse(result.stream().anyMatch(r -> r.getCapacity() < minCapacity));
        verify(roomRepository, times(1)).findAvailableRooms(testDate, dayOfWeek, startTime, endTime, minCapacity, null);
    }

    @Test
    @DisplayName("building='Block A': rooms in other buildings excluded")
    void testBuildingFilter() {
        // Setup: Only rooms in Block A
        int dayOfWeek = testDate.getDayOfWeek().getValue();
        String building = "Block A";
        List<Room> availableRooms = List.of(room1, room3);
        List<RoomAvailabilityDto> expectedDtos = List.of(dto1, dto3);

        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(roomRepository.findAvailableRooms(eq(testDate), eq(dayOfWeek), eq(startTime), eq(endTime), eq(null), eq(building)))
            .thenReturn(availableRooms);
        when(roomMapper.toDtoList(eq(availableRooms))).thenReturn(expectedDtos);
        doNothing().when(cache).put(anyString(), eq(expectedDtos));

        // Execute
        List<RoomAvailabilityDto> result = availabilityService.getAvailable(testDate, startTime, endTime, null, building);

        // Verify
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> "Block A".equals(r.getBuilding())));
        verify(roomRepository, times(1)).findAvailableRooms(testDate, dayOfWeek, startTime, endTime, null, building);
    }

    @Test
    @DisplayName("Cache key generation: deterministic format for query params")
    void testCacheKeyGeneration() {
        // Setup
        RoomAvailabilityService.AvailabilityQuery query1 = 
            new RoomAvailabilityService.AvailabilityQuery(testDate, startTime, endTime, null, null);
        RoomAvailabilityService.AvailabilityQuery query2 = 
            new RoomAvailabilityService.AvailabilityQuery(testDate, startTime, endTime, null, null);
        RoomAvailabilityService.AvailabilityQuery queryWithFilters = 
            new RoomAvailabilityService.AvailabilityQuery(testDate, startTime, endTime, 50, "Block A");

        // Execute
        String key1 = availabilityService.buildCacheKey(query1);
        String key2 = availabilityService.buildCacheKey(query2);
        String keyWithFilters = availabilityService.buildCacheKey(queryWithFilters);

        // Verify
        assertEquals(key1, key2, "Same query params should generate same cache key");
        assertNotEquals(key1, keyWithFilters, "Different query params should generate different cache keys");
        assertTrue(key1.contains(testDate.toString()));
        assertTrue(key1.contains(startTime.toString()));
        assertTrue(keyWithFilters.contains("Block A"));
        assertTrue(keyWithFilters.contains("50"));
    }

}