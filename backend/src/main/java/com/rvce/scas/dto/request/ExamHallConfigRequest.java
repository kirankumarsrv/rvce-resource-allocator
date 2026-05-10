package com.rvce.scas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request payload for assigning a room as an exam hall.
 */
@Data
public class ExamHallConfigRequest {

    @NotNull
    private UUID roomId;

    @NotNull
    @Min(0)
    private Integer twoSeaterCount;

    @NotNull
    @Min(0)
    private Integer threeSeaterCount;

    private String invigilatorId;
}