package com.rvce.scas.scheduler.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A physical room — classroom or lab.
 * Capacity is stored but treated as binary: fits one class at a time.
 * (Per requirement: "capacity is considered as can occupy 1 class at a time")
 */
public class Room {

    public enum RoomType { CLASSROOM, LAB }

    private final String id;           // e.g. "CR101", "LAB_CSE1"
    private final String name;
    private final RoomType type;
    private final int capacity;        // stored for future use
    private final String labType;      // null for classrooms, "CS_LAB", "PHY_LAB", etc. for labs

    @JsonCreator
    public Room(@JsonProperty("id") String id,
                @JsonProperty("name") String name,
                @JsonProperty("type") RoomType type,
                @JsonProperty("capacity") int capacity,
                @JsonProperty("labType") String labType) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.labType = labType;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public RoomType getType() { return type; }
    public int getCapacity() { return capacity; }
    public String getLabType() { return labType; }
    public boolean isLab() { return type == RoomType.LAB; }

    /**
     * Check if this lab room matches the required lab type.
     * Returns true if: room is a classroom (no lab type required),
     * or room's labType matches the required type.
     */
    public boolean matchesLabType(String requiredLabType) {
        if (!isLab()) return true;  // classrooms match any requirement
        if (requiredLabType == null || requiredLabType.isBlank()) return true;  // no requirement
        return requiredLabType.equals(labType);
    }

    @Override public String toString() { return id + "(" + type + ")"; }
}
