package com.rvce.scas.mapper;

import com.rvce.scas.dto.response.OverrideDto;
import com.rvce.scas.entity.DayOverride;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

/**
 * <h3>Purpose</h3>
 * MapStruct mapper for timetable-related entity-DTO conversions.
 * Handles conversions for override operations and CSV parsing.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Convert DayOverride entities to OverrideDto responses</li>
 *   <li>Support CSV row to entity mapping</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Uses MapStruct for compile-time mapping generation.
 *
 * <h3>Transaction Behaviour</h3>
 * Called during CRUD operations in service layer.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Mapper(componentModel = "spring", imports = {java.time.LocalDateTime.class})
public interface TimetableMapper {

    /**
     * Converts a DayOverride entity to OverrideDto.
     *
     * @param override the day override entity
     * @return the corresponding DTO
     */
    @Mapping(target = "slotId", source = "slot.id")
    @Mapping(target = "createdBy", expression = "java(override.getCreatedBy().toString())")
    OverrideDto toDto(DayOverride override);

    /**
     * Converts a list of DayOverride entities to OverrideDto list.
     *
     * @param overrides the list of day override entities
     * @return the list of corresponding DTOs
     */
    List<OverrideDto> toDtoList(List<DayOverride> overrides);

}