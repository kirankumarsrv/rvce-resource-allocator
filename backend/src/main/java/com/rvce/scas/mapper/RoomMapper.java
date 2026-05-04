package com.rvce.scas.mapper;

import com.rvce.scas.dto.response.RoomAvailabilityDto;
import com.rvce.scas.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

/**
 * <h3>Purpose</h3>
 * MapStruct mapper for converting Room entities to RoomAvailabilityDto responses.
 * Ensures clean separation between DB entities and API contracts.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Convert Room entities to DTOs for availability responses</li>
 *   <li>Handle field mapping and type conversions</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Uses MapStruct for compile-time mapping generation.
 *
 * <h3>Transaction Behaviour</h3>
 * Called during read operations in service layer.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Mapper(componentModel = "spring")
public interface RoomMapper {

    /**
     * Converts a Room entity to RoomAvailabilityDto.
     *
     * @param room the room entity
     * @return the corresponding DTO
     */
    @Mapping(target = "floor", source = "floorNumber")
    RoomAvailabilityDto toDto(Room room);

    /**
     * Converts a list of Room entities to RoomAvailabilityDto list.
     *
     * @param rooms the list of room entities
     * @return the list of corresponding DTOs
     */
    List<RoomAvailabilityDto> toDtoList(List<Room> rooms);

}