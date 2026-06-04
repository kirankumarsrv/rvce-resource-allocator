package com.rvce.scas.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rvce.scas.scheduler.model.Room;
import com.rvce.scas.scheduler.model.Subject;

import java.util.List;

/**
 * Everything the TTO enters for their department.
 * This is the single input object passed to the scheduler.
 *
 * Decision: One object per department run — matches requirement 8
 * ("each TTO enters details of their department").
 */
public class DepartmentInput {

    private final String department;       // e.g. "CSE"
    private final List<Subject> subjects;  // all courses for all years/sections
    private final List<Room> rooms;        // classrooms + labs available
    private final int daysInWeek;          // usually 5; year-1 sections may get 6

    // Teacher load tracking: teacherId -> total hours already assigned
    // Scheduler uses this to distribute load equally across teachers.
    // Starts empty; scheduler fills it.
    @JsonCreator
    public DepartmentInput(@JsonProperty("department") String department,
        @JsonProperty("subjects") List<Subject> subjects,
        @JsonProperty("rooms") List<Room> rooms,
        @JsonProperty("daysInWeek") int daysInWeek) {
        this.department = department;
        this.subjects = subjects;
        this.rooms = rooms;
        this.daysInWeek = daysInWeek;
    }

    public String getDepartment() { return department; }
    public List<Subject> getSubjects() { return subjects; }
    public List<Room> getRooms() { return rooms; }
    public int getDaysInWeek() { return daysInWeek; }
}
