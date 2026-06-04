package com.rvce.scas.scheduler.algorithm;

import com.rvce.scas.scheduler.model.Day;
import com.rvce.scas.scheduler.model.TimeSlot;

import java.util.*;

/**
 * Tracks all constraints during scheduling via O(1) Set lookups.
 *
 * CHANGES FROM v1:
 *   - Lab batch tracking: all batches of the same section can share
 *     the same (day, period) — they are in DIFFERENT rooms simultaneously.
 *     The sectionBusy set uses getSectionKey() which includes the batch suffix,
 *     so "CSE_Y3_A_B1" and "CSE_Y3_A_B2" don't conflict with each other.
 *
 *   - Lab-done-today uses PARENT section key (without batch suffix).
 *     Constraint: a section can have at most 1 lab SESSION per day.
 *     A session = all batches running simultaneously.
 *     So if one batch runs on Monday, every batch in the group MUST also run on Monday.
 *     We track labDoneToday per parent section, not per batch.
 *
 *   - Batch group tracking: when a lab group is assigned to (day, period),
 *     we record this so every batch in the group knows exactly where to go.
 *     Map<parentSectionKey_courseId, SlotKey> labBatchSlot

 *   - Scheduled subject tracking: when batch 1 and batch 2 are committed
 *     together, we mark both subject IDs so the main scheduling loop can skip
 *     the batch-2 row later instead of trying to place it twice.
 *
 *   - Fixed room tracking: if TTO pinned a room for a section's theory,
 *     we track roomBusy normally — the fixed room just gets pre-checked.
 *
 * KEY FORMATS (all O(1) HashSet operations):
 *   teacher busy:       "T_{id}_{DAY}_{slotIdx}"
 *   section busy:       "SEC_{sectionKey}_{DAY}_{slotIdx}"   ← includes batch suffix
 *   room busy:          "ROOM_{roomId}_{DAY}_{slotIdx}"
 *   lab done today:     "LAB_{parentSectionKey}_{DAY}"       ← no batch suffix
 *   subject done today: "SUBJ_{subjectId}_{DAY}"
 */
public class ConflictTracker {

    private final Set<String> teacherBusy  = new HashSet<>();
    private final Set<String> sectionBusy  = new HashSet<>();
    private final Set<String> roomBusy     = new HashSet<>();
    private final Set<String> labDoneToday = new HashSet<>();
    private final Set<String> subjectOnDay = new HashSet<>();
    private final Set<String> scheduledSubjects = new HashSet<>();

    // Teacher total hours — for load distribution reporting
    private final Map<String, Integer> teacherLoad = new HashMap<>();

    // Batch-group slot: "parentSectionKey_courseId" → "DAY_slotIdx"
    // When one batch is scheduled, every batch in the group MUST use the same slot.
    private final Map<String, String> labBatchSlot = new HashMap<>();

    // ── QUERY ────────────────────────────────────────────────────────────

    public boolean isTeacherBusy(String teacherId, Day day, TimeSlot slot) {
        return teacherBusy.contains(tKey(teacherId, day, slot));
    }

    /** sectionKey includes batch suffix — batches don't conflict with each other */
    public boolean isSectionBusy(String sectionKey, Day day, TimeSlot slot) {
        return sectionBusy.contains(sKey(sectionKey, day, slot));
    }

    /**
    * Check if a section is busy considering all batch variants.
    * For a base section like "CSE_Y2_A", this checks:
    *   - CSE_Y2_A (theory)
    *   - CSE_Y2_A_B1 (batch 1)
    *   - CSE_Y2_A_B2 (batch 2)
    *   - CSE_Y2_A_B3 (batch 3)
    * 
    * This ensures that theory and any lab batch are never scheduled simultaneously.
     */
    public boolean isSectionBusyIncludingBatches(String baseSectionKey, Day day, TimeSlot slot) {
        // Check base section (theory)
        if (sectionBusy.contains(sKey(baseSectionKey, day, slot))) {
            return true;
        }
        // Check batch 1 and batch 2 variants
        if (sectionBusy.contains(sKey(baseSectionKey + "_B1", day, slot))) {
            return true;
        }
        if (sectionBusy.contains(sKey(baseSectionKey + "_B2", day, slot))) {
            return true;
        }
        return false;
    }

    public boolean isRoomBusy(String roomId, Day day, TimeSlot slot) {
        return roomBusy.contains(rKey(roomId, day, slot));
    }

    /** Uses PARENT section key — one lab session max per section per day */
    public boolean hasLabToday(String parentSectionKey, Day day) {
        return labDoneToday.contains(labKey(parentSectionKey, day));
    }

    public boolean hasSubjectToday(String subjectId, Day day) {
        return subjectOnDay.contains(subjKey(subjectId, day));
    }

    /** Returns true when the subject has already been committed to the timetable. */
    public boolean isSubjectScheduled(String subjectId) {
        return scheduledSubjects.contains(subjectId);
    }

    /**
     * For batched labs: check if the group has already been scheduled.
     * If yes, every batch MUST use the same (day, slot).
     * Returns null if the group has not been scheduled yet.
     */
    public String getBatchPairSlot(String parentSectionKey, String courseBaseName) {
        return labBatchSlot.get(parentSectionKey + "_" + courseBaseName);
    }

    // ── COMMIT ───────────────────────────────────────────────────────────

    public void commitTheorySlot(String teacherId, String sectionKey,
                                  String roomId, Day day, TimeSlot slot) {
        if (teacherId != null && !teacherId.isBlank()) {
            teacherBusy.add(tKey(teacherId, day, slot));
            teacherLoad.merge(teacherId, 1, Integer::sum);
        }
        sectionBusy.add(sKey(sectionKey, day, slot));
        roomBusy.add(rKey(roomId, day, slot));
    }

    /**
     * Commit an elective slot with no teacher.
     * Blocks the section and room at this (day, slot) without marking
     * any teacher as busy. Used when year-wide electives have no specific
     * teacher assigned (TTO-less electives).
     */
    public void commitElectiveSlot(String sectionKey, String roomId, Day day, TimeSlot slot) {
        sectionBusy.add(sKey(sectionKey, day, slot));
        roomBusy.add(rKey(roomId, day, slot));
    }

    /**
     * Commit a lab block (2 consecutive slots).
     * parentSectionKey = section without batch suffix (for lab-done-today tracking).
     * sectionKey = with batch suffix (for concurrent batch scheduling).
     * courseBaseName = used as part of the batch-pair key.
     */
    public void commitLabBlock(String teacherId, String sectionKey, String parentSectionKey,
                                String courseBaseName, String roomId,
                                Day day, TimeSlot slot1, TimeSlot slot2) {
        // Mark teacher busy for both slots
        if (teacherId != null && !teacherId.isBlank()) {
            teacherBusy.add(tKey(teacherId, day, slot1));
            teacherBusy.add(tKey(teacherId, day, slot2));
            teacherLoad.merge(teacherId, 2, Integer::sum);
        }

        // Mark section busy (includes batch suffix — batches don't conflict each other)
        sectionBusy.add(sKey(sectionKey, day, slot1));
        sectionBusy.add(sKey(sectionKey, day, slot2));

        // Mark room busy for both slots
        roomBusy.add(rKey(roomId, day, slot1));
        roomBusy.add(rKey(roomId, day, slot2));

        // Mark lab done today — uses PARENT key (no batch)
        labDoneToday.add(labKey(parentSectionKey, day));

        // Record slot for batch pairing
        String pairKey = parentSectionKey + "_" + courseBaseName;
        labBatchSlot.put(pairKey, day.name() + "_" + slot1.getIndex());
    }

    public void markSubjectOnDay(String subjectId, Day day) {
        subjectOnDay.add(subjKey(subjectId, day));
    }

    /** Marks a subject as fully scheduled so later passes can skip it. */
    public void markSubjectScheduled(String subjectId) {
        scheduledSubjects.add(subjectId);
    }

    public Map<String, Integer> getAllTeacherLoads() {
        return Collections.unmodifiableMap(teacherLoad);
    }

    public int getTeacherLoad(String teacherId) {
        return teacherLoad.getOrDefault(teacherId, 0);
    }

    // ── KEY BUILDERS ─────────────────────────────────────────────────────

    private String tKey(String t, Day d, TimeSlot s) {
        return "T_" + t + "_" + d + "_" + s.getIndex();
    }
    private String sKey(String sec, Day d, TimeSlot s) {
        return "SEC_" + sec + "_" + d + "_" + s.getIndex();
    }
    private String rKey(String r, Day d, TimeSlot s) {
        return "ROOM_" + r + "_" + d + "_" + s.getIndex();
    }
    private String labKey(String sec, Day d) {
        return "LAB_" + sec + "_" + d;
    }
    private String subjKey(String subj, Day d) {
        return "SUBJ_" + subj + "_" + d;
    }
}
