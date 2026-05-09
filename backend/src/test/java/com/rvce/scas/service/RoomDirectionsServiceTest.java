package com.rvce.scas.service;

import com.rvce.scas.dto.response.DirectionStepDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.RoomDirection;
import com.rvce.scas.exception.DirectionsNotFoundException;
import com.rvce.scas.exception.RoomNotFoundException;
import com.rvce.scas.mapper.RoomLocationMapper;
import com.rvce.scas.repository.RoomDirectionRepository;
import com.rvce.scas.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("T-302: Room Directions Service Tests")
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class RoomDirectionsServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomDirectionRepository roomDirectionRepository;

    @Mock
    private RoomLocationMapper roomLocationMapper;

    @InjectMocks
    private RoomDirectionsService roomDirectionsService;

    private UUID roomId;
    private Room room;
    private RoomDirection step1;
    private RoomDirection step2;
    private DirectionStepDto dto1;
    private DirectionStepDto dto2;

    private void initialize() {
        roomId = UUID.randomUUID();
        room = new Room();
        room.setId(roomId);
        room.setName("AB-201");
        room.setIsActive(true);

        step1 = new RoomDirection();
        step1.setId(UUID.randomUUID());
        step1.setRoomId(roomId);
        step1.setFromLocationTag("MAIN_GATE");
        step1.setStepOrder((short) 1);
        step1.setInstruction("Walk straight for 50m");
        step1.setDistanceMeters((short) 50);

        step2 = new RoomDirection();
        step2.setId(UUID.randomUUID());
        step2.setRoomId(roomId);
        step2.setFromLocationTag("MAIN_GATE");
        step2.setStepOrder((short) 2);
        step2.setInstruction("Turn left at the library");
        step2.setDistanceMeters((short) 100);

        dto1 = DirectionStepDto.builder()
            .stepOrder(step1.getStepOrder())
            .instruction(step1.getInstruction())
            .distanceMeters(step1.getDistanceMeters())
            .build();
        dto2 = DirectionStepDto.builder()
            .stepOrder(step2.getStepOrder())
            .instruction(step2.getInstruction())
            .distanceMeters(step2.getDistanceMeters())
            .build();
    }

    @Test
    @DisplayName("Valid room and start tag returns ordered direction steps")
    void getDirectionsReturnsOrderedSteps() {
        initialize();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomDirectionRepository.existsByRoomIdAndFromLocationTag(roomId, "MAIN_GATE")).thenReturn(true);
        when(roomDirectionRepository.findDirectionsByRoomAndStart(roomId, "MAIN_GATE"))
            .thenReturn(List.of(step2, step1));
        when(roomLocationMapper.toDirectionStepDtoList(List.of(step1, step2))).thenReturn(List.of(dto1, dto2));

        List<DirectionStepDto> results = roomDirectionsService.getDirections(roomId, "main_gate");

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getStepOrder().intValue());
        assertEquals(2, results.get(1).getStepOrder().intValue());
    }

    @Test
    @DisplayName("Unknown start tag returns directions not found")
    void getDirectionsUnknownTag() {
        initialize();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomDirectionRepository.existsByRoomIdAndFromLocationTag(roomId, "UNKNOWN_LOCATION"))
            .thenReturn(false);

        DirectionsNotFoundException exception = assertThrows(
            DirectionsNotFoundException.class,
            () -> roomDirectionsService.getDirections(roomId, "UNKNOWN_LOCATION")
        );
        assertEquals("No directions found for room " + roomId + " from UNKNOWN_LOCATION", exception.getMessage());
    }

    @Test
    @DisplayName("Inactive or missing room returns room not found")
    void getDirectionsMissingRoom() {
        initialize();
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        RoomNotFoundException exception = assertThrows(
            RoomNotFoundException.class,
            () -> roomDirectionsService.getDirections(roomId, "MAIN_GATE")
        );
        assertEquals("Room not found: " + roomId, exception.getMessage());
    }
}