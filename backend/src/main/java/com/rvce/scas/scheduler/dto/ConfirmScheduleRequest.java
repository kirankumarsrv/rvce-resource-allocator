package com.rvce.scas.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rvce.scas.scheduler.model.ScheduledSlot;

import java.util.List;

public class ConfirmScheduleRequest {

    private final String department;
    private final List<ScheduledSlot> scheduledSlots;

    @JsonCreator
    public ConfirmScheduleRequest(@JsonProperty("department") String department,
                                   @JsonProperty("scheduledSlots") List<ScheduledSlot> scheduledSlots) {
        this.department = department;
        this.scheduledSlots = scheduledSlots;
    }

    public String getDepartment() { return department; }
    public List<ScheduledSlot> getScheduledSlots() { return scheduledSlots; }
}