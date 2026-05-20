package com.rvce.scas.mapper;

import com.rvce.scas.dto.response.ExamSessionDto;
import com.rvce.scas.dto.response.ExamHallDto;
import com.rvce.scas.dto.response.ExamSeatDto;
import com.rvce.scas.dto.response.ExamStudentDto;
import com.rvce.scas.entity.ExamHall;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamStudent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for exam session and student DTOs.
 */
@Mapper(componentModel = "spring")
public interface ExamMapper {

    @Mapping(target = "studentCount", ignore = true)
    ExamSessionDto toDto(ExamSession examSession);

    @Mapping(target = "examId", source = "examSession.examId")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomName", source = "room.name")
    @Mapping(target = "roomDisplayName", source = "room.displayName")
    @Mapping(target = "building", source = "room.building")
    @Mapping(target = "floor", source = "room.floorNumber")
    @Mapping(target = "invigilatorId", source = "invigilator.userId")
    @Mapping(target = "invigilatorName", source = "invigilator.name")
    ExamHallDto toDto(ExamHall examHall);

    @Mapping(target = "examId", source = "examSession.examId")
    @Mapping(target = "hallId", source = "hall.hallId")
    @Mapping(target = "usn", ignore = true)
    @Mapping(target = "studentName", ignore = true)
    @Mapping(target = "branchCode", ignore = true)
    @Mapping(target = "needsFrontRow", ignore = true)
    ExamSeatDto toDto(ExamSeat examSeat);

    ExamStudentDto toDto(ExamStudent examStudent);

    List<ExamStudentDto> toDtoList(List<ExamStudent> examStudents);

    List<ExamHallDto> toHallDtoList(List<ExamHall> examHalls);
}
