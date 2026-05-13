package com.rvce.scas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for configuring an exam hall.
 * Invigilator is mandatory and must reference a valid teacher.
 */
public class ExamHallConfigRequest {

    @NotNull
    private UUID roomId;

    @NotNull
    @Min(0)
    private Integer twoSeaterCount;

    @NotNull
    @Min(0)
    private Integer threeSeaterCount;

    @NotBlank(message = "Invigilator is required")
    private String invigilatorId;

    // Getters and setters
    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public Integer getTwoSeaterCount() {
        return twoSeaterCount;
    }

    public void setTwoSeaterCount(Integer twoSeaterCount) {
        this.twoSeaterCount = twoSeaterCount;
    }

    public Integer getThreeSeaterCount() {
        return threeSeaterCount;
    }

    public void setThreeSeaterCount(Integer threeSeaterCount) {
        this.threeSeaterCount = threeSeaterCount;
    }

    public String getInvigilatorId() {
        return invigilatorId;
    }

    public void setInvigilatorId(String invigilatorId) {
        this.invigilatorId = invigilatorId;
    }
}
