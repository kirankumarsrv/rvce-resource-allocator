package com.rvce.scas.mapper;

import com.rvce.scas.dto.response.DirectionStepDto;
import com.rvce.scas.dto.response.RoomLocationDto;
import com.rvce.scas.dto.response.RoomSearchResultDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.RoomDirection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * <h3>Purpose</h3>
 * MapStruct mapper for converting Room and RoomDirection entities to
 * navigation-specific DTOs. Ensures clean separation between DB entities
 * and API contracts for Epic 2 (Campus Navigation System).
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Convert Room entities to RoomLocationDto (with signed S3 URLs)</li>
 *   <li>Convert Room entities to RoomSearchResultDto (minimal fields for search results)</li>
 *   <li>Convert RoomDirection entities to DirectionStepDto</li>
 * </ul>
 *
 * <h3>Security Notes</h3>
 * <ul>
 *   <li>RoomLocationDto excludes internal fields (bench_rows, dept_owner_id)</li>
 *   <li>floorPlanUrl field is populated by the service layer (after S3 pre-sign)</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Uses MapStruct for compile-time mapping generation.
 *
 * <h3>Transaction Behaviour</h3>
 * Called during read operations in service layer.
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Mapper(componentModel = "spring")
public interface RoomLocationMapper {

    /**
     * Converts a Room entity to RoomLocationDto.
     * Note: floorPlanUrl is NOT set by this mapper — it is populated
     * by the service layer after generating a pre-signed S3 URL.
     *
     * @param room the room entity
     * @return the RoomLocationDto
     */
    @Mapping(target = "floorNumber", source = "floorNumber")
    @Mapping(target = "floorPlanUrl", ignore = true)
    RoomLocationDto toLocationDto(Room room);

    /**
     * Converts a Room entity to RoomSearchResultDto.
     * Includes only fields needed for the search Combobox dropdown and map.panTo.
     *
     * @param room the room entity
     * @return the RoomSearchResultDto
     */
    @Mapping(target = "floorNumber", source = "floorNumber")
    RoomSearchResultDto toSearchResultDto(Room room);

    /**
     * Converts a list of Room entities to RoomSearchResultDto list.
     *
     * @param rooms the list of room entities
     * @return the list of corresponding DTOs
     */
    List<RoomSearchResultDto> toSearchResultDtoList(List<Room> rooms);

    /**
     * Converts a RoomDirection entity to DirectionStepDto.
     *
     * @param direction the room direction entity
     * @return the DirectionStepDto
     */
    @Mapping(target = "landmark", ignore = true)
    DirectionStepDto toDirectionStepDto(RoomDirection direction);

    /**
     * Converts a list of RoomDirection entities to DirectionStepDto list.
     *
     * @param directions the list of room direction entities
     * @return the list of corresponding DTOs
     */
    List<DirectionStepDto> toDirectionStepDtoList(List<RoomDirection> directions);

}
