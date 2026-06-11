package com.rvce.scas.scheduler.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One schedulable unit — a course assigned to a section (and optionally a batch).
 *
 * CHANGES FROM v1:
 *   - Added batch field: null = full section, "1", "2", "3", ... = lab batch
 *   - Added labTeacherId: may differ from teacherId for batch lab rows
 *   - Added fixedRoomId: TTO-pinned theory classroom (null = scheduler chooses)
 *   - section is now nullable: null means year-wide (used for electives)
 *   - semester removed from logic — year drives all scheduling decisions
 *     semester is kept only as an optional display label
 *
 * BATCH DESIGN DECISION:
 *   A 4-credit course with lab produces N CSV rows:
 *     Row 1: type=THEORY, batch=null  → 3 theory slots for full section
 *     Rows 2..N: type=LAB, batch=1..N → 1 lab session per batch
 *   All batch rows MUST be scheduled at the same (day, period) — the scheduler
 *   treats them as one atomic unit. They go in different lab rooms simultaneously.
 *
 * ELECTIVE DESIGN DECISION:
 *   section=null means "apply to all sections of this year."
 *   The scheduler expands year-wide electives into per-section entries before running.
 *   This means TTO enters ONE elective row instead of one per section.
 *
 * FIXED ROOM DECISION:
 *   fixedRoomId is only honoured for THEORY slots.
 *   Lab rooms are always scheduler-assigned — TTO never pins labs.
 *   If the fixed room is occupied (another section is there), scheduler logs a warning
 *   but does NOT fail — it falls back to free room selection.
 *   (Hard failure would block the whole timetable for one room conflict.)
 */
public class Subject {

    public enum Type { THEORY, LAB }

    private final String id;
    private final String name;
    private final String department;
    private final int year;
    private final String section;       // null = year-wide (electives)
    private final String batch;         // null = full section, "1", "2", ... = lab batch
    private final int credits;
    private final Type type;
    private final String teacherId;     // theory teacher OR first batch lab teacher
    private final String labTeacherId;  // alternate lab teacher (null if same as teacherId)
    private final String fixedRoomId;   // TTO-pinned theory room (null = scheduler chooses)
    private final boolean isElective;
    private final String electiveSlot;  // "WEDNESDAY_2" format, null if not elective
    private final String semester;      // optional display label only — not used in logic
    private final String requiredLabType; // "CS_LAB", "PHY_LAB", etc., only for LAB rows
    private final boolean theoryOnlyFourCredit; // tto flag: true when 4-credit course has only theory

    private final int theoryHoursPerWeek;
    private final int labHoursPerWeek;

    @JsonCreator
    public Subject(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("department") String department,
                   @JsonProperty("year") int year,
                   @JsonProperty("section") String section,
                   @JsonProperty("batch") String batch,
                   @JsonProperty("credits") int credits,
                   @JsonProperty("type") Type type,
                   @JsonProperty("teacherId") String teacherId,
                   @JsonProperty("labTeacherId") String labTeacherId,
                   @JsonProperty("fixedRoomId") String fixedRoomId,
                   @JsonProperty("isElective") boolean isElective,
                   @JsonProperty("electiveSlot") String electiveSlot,
                   @JsonProperty("semester") String semester,
                   @JsonProperty("requiredLabType") String requiredLabType,
                   @JsonProperty("theoryOnlyFourCredit") boolean theoryOnlyFourCredit) {
        this.id            = id;
        this.name          = name;
        this.department    = department;
        this.year          = year;
        this.section       = section;
        this.batch         = batch;
        this.credits       = credits;
        this.type          = type;
        this.teacherId     = teacherId;
        this.labTeacherId  = labTeacherId;
        this.fixedRoomId   = fixedRoomId;
        this.isElective    = isElective;
        this.electiveSlot  = electiveSlot;
        this.semester      = semester;
        this.requiredLabType = requiredLabType;
        this.theoryOnlyFourCredit = theoryOnlyFourCredit;

        // Hours derived from type + credits — never stored as input
        if (type == Type.LAB) {
            // A batch lab row = 2 continuous hours, regardless of credits
            this.theoryHoursPerWeek = 0;
            this.labHoursPerWeek    = 2;
        } else if (credits == 4) {
            // 4-credit theory component = 3 hours (lab is separate rows)
            this.theoryHoursPerWeek = 3;
            this.labHoursPerWeek    = 0;
        } else {
            // 3-credit=3h, 2-credit=2h, 1-credit=1h
            this.theoryHoursPerWeek = credits;
            this.labHoursPerWeek    = 0;
        }
    }

    // ── GETTERS ──────────────────────────────────────────────────────────

    public String getId()            { return id; }
    public String getName()          { return name; }
    public String getDepartment()    { return department; }
    public int    getYear()          { return year; }
    public String getSection()       { return section; }   // null = year-wide
    public String getBatch()         { return batch; }     // null, "1", or "2"
    public int    getCredits()       { return credits; }
    public Type   getType()          { return type; }
    public String getTeacherId()     { return teacherId; }
    public String getFixedRoomId()   { return fixedRoomId; }
    @JsonProperty("isElective")
    public boolean isElective()      { return isElective; }
    public String getElectiveSlot()  { return electiveSlot; }
    public String getSemester()      { return semester; }
    public String getRequiredLabType() { return requiredLabType; }
    @JsonProperty("isTheoryOnlyFourCredit")
    public boolean isTheoryOnlyFourCredit() { return theoryOnlyFourCredit; }

    public int getTheoryHoursPerWeek() { return theoryHoursPerWeek; }
    public int getLabHoursPerWeek()    { return labHoursPerWeek; }
    public int getTotalHoursPerWeek()  { return theoryHoursPerWeek + labHoursPerWeek; }

    public boolean isYearWide() { return section == null || section.isBlank(); }

    public boolean isBatchLab() { return type == Type.LAB && batch != null && !batch.isBlank(); }

    /**
     * The effective teacher for this subject.
     * For batch lab rows: use labTeacherId if specified, otherwise teacherId.
     */
    public String getEffectiveTeacher() {
        if (isBatchLab() && labTeacherId != null && !labTeacherId.isBlank()) {
            return labTeacherId;
        }
        return teacherId;
    }

    /**
    * Section key for conflict tracking.
    * For batched labs: includes batch to allow parallel scheduling.
    * "CSE_Y3_A_B1", "CSE_Y3_A_B2", ... are tracked independently —
    * they can share the same (day, period) without triggering a section conflict.
     */
    public String getSectionKey() {
        String base = department + "_Y" + year + "_" + (section == null ? "ALL" : section);
        if (isBatchLab()) base += "_B" + batch;
        return base;
    }

    /**
     * The parent section key (without batch suffix).
     * Used to check: "has this section already had a lab today?" across both batches.
     */
    public String getParentSectionKey() {
        return department + "_Y" + year + "_" + (section == null ? "ALL" : section);
    }
    //what is this for??
    @Override
    public String toString() {
        String batchStr = batch != null ? " [Batch " + batch + "]" : "";
        return name + "(" + (section != null ? section : "ALL") + batchStr
             + ", " + credits + "cr, " + type + ")";
    }
}
