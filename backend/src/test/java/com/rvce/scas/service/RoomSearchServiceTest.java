package com.rvce.scas.service;

import com.rvce.scas.dto.response.RoomSearchResultDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.mapper.RoomLocationMapper;
import com.rvce.scas.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("T-302: Room Search Service Tests")
class RoomSearchServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomLocationMapper roomLocationMapper;

    @InjectMocks
    private RoomSearchService roomSearchService;

    private Room room;
    private RoomSearchResultDto resultDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(roomSearchService, "minQueryLength", 2);
        ReflectionTestUtils.setField(roomSearchService, "maxResults", 10);

        room = new Room();
        room.setId(UUID.randomUUID());
        room.setName("AB-201");
        room.setDisplayName("Academic Block 201");
        room.setFloorNumber(2);
        room.setBlock("AB");
        room.setBuilding("Academic Block");
        room.setLatitude(new BigDecimal("12.9238437"));
        room.setLongitude(new BigDecimal("77.4988752"));
        room.setIsActive(true);

        resultDto = RoomSearchResultDto.builder()
            .id(room.getId())
            .name(room.getName())
            .floorNumber(room.getFloorNumber())
            .block(room.getBlock())
            .building(room.getBuilding())
            .latitude(room.getLatitude())
            .longitude(room.getLongitude())
            .build();
    }

    @Test
    @DisplayName("Exact prefix query returns prefix matches without falling back to full-text")
    void searchRoomsExactPrefix() {
        when(roomRepository.findByNameStartingWithIgnoreCaseAndIsActive(eq("AB-201"), eq(true), any()))
            .thenReturn(List.of(room));
        when(roomLocationMapper.toSearchResultDtoList(List.of(room))).thenReturn(List.of(resultDto));

        List<RoomSearchResultDto> results = roomSearchService.searchRooms("AB-201", 10);

        assertEquals(1, results.size());
        assertEquals("AB-201", results.get(0).getName());
        verify(roomRepository, never()).searchByFullText(anyString(), anyInt());
    }

    @Test
    @DisplayName("Partial query falls back to full-text search")
    void searchRoomsFullTextFallback() {
        when(roomRepository.findByNameStartingWithIgnoreCaseAndIsActive(eq("seminar"), eq(true), any()))
            .thenReturn(List.of());
        when(roomRepository.searchByFullText("seminar", 5)).thenReturn(List.of(room));
        when(roomLocationMapper.toSearchResultDtoList(List.of(room))).thenReturn(List.of(resultDto));

        List<RoomSearchResultDto> results = roomSearchService.searchRooms("seminar", 5);

        assertEquals(1, results.size());
        verify(roomRepository).searchByFullText("seminar", 5);
    }

    @Test
    @DisplayName("Single-character query is rejected")
    void searchRoomsRejectsShortQuery() {
        assertThrows(IllegalArgumentException.class, () -> roomSearchService.searchRooms("A", 10));
        verify(roomRepository, never()).findByNameStartingWithIgnoreCaseAndIsActive(anyString(), anyBoolean(), any());
        verify(roomRepository, never()).searchByFullText(anyString(), anyInt());
    }

    @Test
    @DisplayName("Limit is capped at configured maximum")
    void searchRoomsCapsLimit() {
        when(roomRepository.findByNameStartingWithIgnoreCaseAndIsActive(eq("AB"), eq(true), any()))
            .thenReturn(List.of(room));
        when(roomLocationMapper.toSearchResultDtoList(List.of(room))).thenReturn(List.of(resultDto));

        roomSearchService.searchRooms("AB", 25);

        verify(roomRepository).findByNameStartingWithIgnoreCaseAndIsActive(eq("AB"), eq(true), any());
    }
}