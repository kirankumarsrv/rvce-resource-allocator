package com.rvce.scas.scheduler.algorithm;

import com.rvce.scas.scheduler.dto.DepartmentInput;
import com.rvce.scas.scheduler.model.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * GREEDY TIMETABLE SCHEDULER — v2
 *
 * CHANGES FROM v1:
 *   1. Lab batches: all batches of the same section run SIMULTANEOUSLY.
 *      Scheduler assigns the whole batch group atomically so every batch uses
 *      the same (day, period) in a different free lab room.
 *
 *   2. Fixed theory room: if subject.getFixedRoomId() != null, scheduler uses
 *      that room for theory slots (if free). Falls back to any free room with a warning.
 *
 *   3. Year-wide electives: subjects with section=null are expanded to all sections
 *      of that year before scheduling. TTO enters one elective row, scheduler clones it.
 *
 *   4. Semester removed from logic. year drives everything.
 *
 * SORT PRIORITY (unchanged):
 *   1. Lab batches first (hardest to place — need 2 consecutive slots + lab room)
 *   2. Higher credits
 *   3. More total hours pending
 *
 * CONSTRAINTS:
 *   C1. No teacher double-booked across any year/section
 *   C2. No section in two places at once (batch-aware)
 *   C3. No room used twice at same time
 *   C4. Max 1 lab SESSION per section per day (both batches count as one session)
 *   C5. Lab = 2 physically consecutive slots (no crossing break/lunch)
 *   C6. Same subject not twice on same day for a section
 *   C7. Elective slots pre-assigned
 *   C8. Year 2&3: morning preference (soft)
 *   C9. Batch labs for the same section are scheduled as one atomic group:
 *       a batch is only committed when every batch in the group can also be
 *       placed at the same time in a different lab room.
 */
public class TimetableScheduler {

    private static final Logger LOG = Logger.getLogger(TimetableScheduler.class.getName());

    private final DepartmentInput input;

    public TimetableScheduler(DepartmentInput input) {
        this.input = input;
    }

    /**
    * Build a timetable in four passes:
     * 1. Expand year-wide electives into per-section subjects.
     * 2. Sort by scheduling priority so the hardest items are placed first.
     * 3. Pre-assign electives that already have fixed slots.
    * 4. Greedily place the remaining theory and lab hours while tracking conflicts.
    *
    * Batched labs are scheduled atomically: batch 1 is only committed when the
    * matching batch 2 can also be placed at the same day/period in another room.
     *
     * The returned result contains both the scheduled rows and the remaining
     * unscheduled hours per subject, which makes it easier to understand gaps.
     */
    public SchedulerResult schedule() {
        List<ScheduledSlot> result      = new ArrayList<>();
        Map<String, Integer> unscheduled = new LinkedHashMap<>();
        ConflictTracker tracker         = new ConflictTracker();

        logInfo("Starting schedule with " + input.getSubjects().size() + " subjects and "
                + input.getRooms().size() + " rooms.");

        // Step 1: expand year-wide electives into per-section subjects
        List<Subject> expanded = expandYearWideElectives(input.getSubjects());
        logInfo("Expanded subjects to " + expanded.size() + " scheduling entries.");

        // Step 2: sort by priority
        List<Subject> sorted = sortByPriority(expanded);
        logInfo("Applied scheduling priority order: labs first, then higher-credit and higher-load subjects.");
        
        List<Subject> regularSubjects = sorted;
        
        // Print sorted list
        StringBuilder sortedListStr = new StringBuilder("\n=== SORTED SUBJECTS BY PRIORITY ===\n");
        int index = 1;
        for (Subject sub : sorted) {
            int effectiveHours = getEffectiveTotalHours(sub);
            sortedListStr.append(String.format("%d. ID: %s | Type: %s | Batch: %s | Credits: %d | Hours: %d | Section: %s\n",
                    index++, sub.getId(), sub.getType(), sub.getBatch() != null ? sub.getBatch() : "N/A",
            sub.getCredits(), effectiveHours, sub.getSection() != null ? sub.getSection() : "YEAR-WIDE"));
        }
        sortedListStr.append("===================================\n");
        logInfo(sortedListStr.toString());

        Map<String, List<Subject>> labGroups = new LinkedHashMap<>();
        for (Subject sub : sorted) {
            if (sub.isBatchLab()) {
                String key = sub.getParentSectionKey() + "|" + sub.getName();
                labGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(sub);
            }
        }
        labGroups.values().forEach(list ->
            list.sort(Comparator.comparingInt(s -> Integer.parseInt(s.getBatch()))));

        Map<String, Integer> electiveCommittedHours = new HashMap<>();

        // Step 3: pre-assign fixed elective slots
        for (Subject sub : regularSubjects) {
            if (sub.isElective() && sub.getElectiveSlot() != null) {
                logInfo("Pre-assigning elective " + subjectSummary(sub) + " to fixed slot "
                        + sub.getElectiveSlot() + ".");
                electiveCommittedHours.put(sub.getId(),
                        preAssignElective(sub, tracker, result));
            }
        }

        // Step 3b: explicitly schedule any remaining electives before non-electives.
        // This is the fallback path when TTO did not provide the exact elective slot count.
        for (Subject sub : regularSubjects) {
            if (!sub.isElective()) continue;
            int needed = getEffectiveTotalHours(sub);
            int committed = electiveCommittedHours.getOrDefault(sub.getId(), 0);
            int remaining = Math.max(0, needed - committed);
            int assigned = 0;
            unscheduled.put(sub.getId(), remaining);

            if (remaining == 0) {
                tracker.markSubjectScheduled(sub.getId());
                logInfo("Elective " + subjectSummary(sub) + " already fully pre-assigned.");
                continue;
            }

            logInfo("Explicitly scheduling remaining " + remaining + " hour(s) for elective "
                    + subjectSummary(sub) + " before regular subjects.");

            for (int i = 0; i < remaining; i++) {
                boolean ok = tryAssignTheoryHour(sub, tracker, result);
                if (ok) {
                    assigned++;
                } else {
                    logWarn("Could not assign elective hour " + (i + 1) + " of "
                            + remaining + " for " + subjectSummary(sub) + ".");
                }
            }

            unscheduled.put(sub.getId(), remaining - assigned);
            tracker.markSubjectScheduled(sub.getId());
            logInfo("Finished explicit elective placement for " + subjectSummary(sub)
                    + ": assigned " + assigned + " of " + remaining + " remaining hours.");
        }

        // Step 4: greedy assignment for remaining subjects.
        // Electives were already handled first, either through fixed-slot pre-assignment
        // or through the explicit elective scheduling pass above.
        for (Subject sub : regularSubjects) {
            if (tracker.isSubjectScheduled(sub.getId())) continue;

            int assigned = 0;
            int needed   = getEffectiveTotalHours(sub);
            unscheduled.put(sub.getId(), needed);

            logInfo("Scheduling " + subjectSummary(sub) + " with " + needed + " weekly hours.");

            if (sub.getType() == Subject.Type.LAB) {
                if (sub.isBatchLab()) {
                    String groupKey = sub.getParentSectionKey() + "|" + sub.getName();
                    List<Subject> batches = labGroups.get(groupKey);

                    // Only schedule when we see batch 1 — all other batches are handled atomically
                    if ("1".equals(sub.getBatch())) {
                        boolean ok = tryAssignPairedLabBlock(sub, batches, tracker, result);
                        if (ok) {
                            assigned += 2; // batch1 contributes 2 hours (batch2,3 are auto-committed)
                            // Mark all other batch subjects as having 0 unscheduled hours
                            if (batches != null) {
                                for (Subject b : batches) {
                                    if (!"1".equals(b.getBatch())) {
                                        unscheduled.put(b.getId(), 0);
                                    }
                                }
                            }
                        } else {
                            logWarn("Could not assign " + (batches != null ? batches.size() : "?")
                                    + "-batch lab for " + subjectSummary(sub));
                        }
                    }
                    // Skip batch 2, 3 etc — already handled atomically above when batch 1 ran
                } else {
                    boolean ok = tryAssignLabBlock(sub, tracker, result);
                    if (ok) assigned += 2;
                    else logWarn("Could not assign lab block for " + subjectSummary(sub));
                }
            } else {
                // Theory: assign one slot at a time
                int requiredTheoryHours = getEffectiveTheoryHours(sub);
                for (int i = 0; i < requiredTheoryHours; i++) {
                    boolean ok = tryAssignTheoryHour(sub, tracker, result);
                    if (ok) assigned++;
                    else logWarn("Could not assign theory hour " + (i + 1) + " of "
                            + requiredTheoryHours + " for " + subjectSummary(sub) + ".");
                }
            }

            unscheduled.put(sub.getId(), needed - assigned);
            logInfo("Finished " + subjectSummary(sub) + ": assigned " + assigned + " of "
                    + needed + " hours.");
        }

        logInfo("Scheduling complete: " + result.size() + " timetable entries created.");

        return new SchedulerResult(result, input.getRooms(), input.getDaysInWeek(),
            unscheduled, tracker.getAllTeacherLoads());
    }

    // ── YEAR-WIDE ELECTIVE EXPANSION ─────────────────────────────────────

    /**
        * Expand year-wide electives (section=null) into one subject per section.
        *
        * The scheduler derives the available sections from the non-elective subjects
        * already present for the same year, then clones the elective once per section.
        * This keeps the input compact while still scheduling one row per section.
     *
     * Example: IE Elective for year=3 with section=null
     *   → IE Elective for year=3, section=A
     *   → IE Elective for year=3, section=B
     *   → IE Elective for year=3, section=C
     */
    private List<Subject> expandYearWideElectives(List<Subject> subjects) {
        // Collect distinct sections per year from non-elective subjects
        Map<Integer, Set<String>> yearSections = new HashMap<>();
        for (Subject s : subjects) {
            if (!s.isYearWide() && s.getSection() != null) {
                yearSections.computeIfAbsent(s.getYear(), k -> new LinkedHashSet<>())
                            .add(s.getSection());
            }
        }

        List<Subject> result = new ArrayList<>();
        for (Subject sub : subjects) {
            if (sub.isYearWide()) {
                // Clone one per section of this year
                Set<String> sections = yearSections.getOrDefault(sub.getYear(), Set.of());
                if (sections.isEmpty()) {
                    logWarn("Year-wide elective " + subjectSummary(sub)
                            + " has no section context to expand into; keeping the original subject.");
                    result.add(sub); // keep as-is
                } else {
                    logInfo("Expanding year-wide elective " + subjectSummary(sub)
                            + " into sections " + sections + ".");
                    for (String sec : sections) {
                        result.add(cloneWithSection(sub, sec));
                    }
                }
            } else {
                result.add(sub);
            }
        }
        return result;
    }

    /**
     * Create a copy of a year-wide subject with a specific section assigned.
     * The cloned subject gets a unique id suffix so the scheduler can track
     * each section independently.
     */
    private Subject cloneWithSection(Subject sub, String section) {
        return new Subject(
            sub.getId() + "_" + section,   // unique ID per cloned section
            sub.getName(),
            sub.getDepartment(),
            sub.getYear(),
            section,                        // assigned section
            sub.getBatch(),
            sub.getCredits(),
            sub.getType(),
            sub.isElective() ? null : sub.getTeacherId(),
            null,                           // labTeacherId
            sub.getFixedRoomId(),
            sub.isElective(),
            sub.getElectiveSlot(),
            sub.getSemester(),
            sub.getRequiredLabType(),       // propagate lab type requirement
            sub.isTheoryOnlyFourCredit()
        );
    }

    // ── SORT ─────────────────────────────────────────────────────────────
    /**
     * Sort subjects by scheduling priority.
     *
     * The order is intentionally biased toward the most constrained rows first:
     * labs, then higher-credit rows, then rows with more total weekly hours,
     * while keeping the two batches for the same section/course adjacent.
     */
    private List<Subject> sortByPriority(List<Subject> subjects) {
        return subjects.stream() // stream for sorting
            .sorted(Comparator // Sort by multiple criteria in order of priority
                // Lab batches next — hardest to place (need 2 consecutive slots + lab room)
                .comparingInt((Subject s) -> s.getType() == Subject.Type.LAB ? 0 : 1)
                // Higher year first — 3rd year before 2nd, then 1st
                .thenComparing(Comparator.comparingInt(Subject::getYear).reversed())
                // Higher credits = more constrained = schedule first
                .thenComparing(Comparator.comparingInt((Subject s) -> s.getCredits()).reversed())
                // More hours = more constrained
                .thenComparing(Comparator.comparingInt((Subject s) -> getEffectiveTotalHours(s)).reversed())
                // Keep the same section/course together so batch 1 and batch 2 stay adjacent
                .thenComparing(Subject::getParentSectionKey)
                .thenComparing(Subject::getName)
                // Batch 2 still comes after batch 1 for the same lab pairing
                .thenComparing(s -> "2".equals(s.getBatch()) ? 1 : 0))
            .collect(Collectors.toList());
    }

    // ── PRE-ASSIGN ELECTIVES ─────────────────────────────────────────────

    /**
     * Place an elective into its pre-approved slot.
     *
     * If the configured slot string is invalid or the room is already occupied,
     * the method logs a warning and leaves the subject unscheduled instead of
     * failing the entire run.
     */
    private int preAssignElective(Subject sub, ConflictTracker tracker, List<ScheduledSlot> result) {
        // electiveSlot supports comma-separated entries now, e.g. "MONDAY_1,WEDNESDAY_2,FRIDAY_3"
        if (sub.getElectiveSlot() == null || sub.getElectiveSlot().isBlank()) {
            logWarn("Elective " + subjectSummary(sub) + " has no electiveSlot configured.");
            return 0;
        }

        // strip surrounding quotes that may come from naive CSV splitting
        String slotInput = sub.getElectiveSlot().replace("\"", "");
        String[] slotStrs = Arrays.stream(slotInput.split(","))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .toArray(String[]::new);

        int expected = sub.getTheoryHoursPerWeek();
        if (slotStrs.length != expected) {
            logWarn("Elective " + subjectSummary(sub) + " expected " + expected
                    + " slot(s) but got " + slotStrs.length + "; using the valid provided slots and "
                    + "filling the remaining hours explicitly.");
        }

        int committed = 0;
        Set<Day> daysUsed = new HashSet<>();
        for (String s : slotStrs) {
            if (committed == expected) break;
            try {
                String[] parts = s.split("_");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Bad slot format: '" + s + "'");
                }
                Day day = Day.valueOf(parts[0].toUpperCase());
                TimeSlot slot = TimeSlot.fromIndex(Integer.parseInt(parts[1].trim()));

                // C6: do not allow same subject twice on the same day
                if (daysUsed.contains(day) || tracker.hasSubjectToday(sub.getId(), day)) {
                    throw new IllegalStateException("Subject " + sub.getId() + " already has a slot on " + day);
                }

                // For year-wide elective slot fixing, allow the same configured
                // slot to be applied across all sections of the year.
                // Teacher overlap is intentionally not treated as a blocker here.
                if (tracker.isSectionBusyIncludingBatches(sub.getParentSectionKey(), day, slot)) {
                    throw new IllegalStateException("Section busy for slot " + s);
                }

                Room room = findFreeRoom(sub, false, day, slot, tracker);
                if (room == null) {
                    throw new IllegalStateException("No free classroom available for slot " + s);
                }

                result.add(new ScheduledSlot(sub, room, day, slot, false));
                tracker.commitTheorySlot(sub.getEffectiveTeacher(), sub.getSectionKey(),
                                         room.getId(), day, slot);
                tracker.markSubjectOnDay(sub.getId(), day);
                daysUsed.add(day);
                committed++;
                logInfo("Placed elective " + subjectSummary(sub) + " in room " + room.getId()
                        + " at " + day + " " + slot.getDisplay() + ".");
            } catch (Exception e) {
                logWarn("Skipping elective slot '" + s + "' for " + subjectSummary(sub) + ": "
                        + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        return committed;
    }

    // ── LAB BLOCK ASSIGNMENT ─────────────────────────────────────────────

    /**
     * Assign a 2-hour consecutive lab block.
     *
     * This is the single-batch fallback path used for non-batched labs.
     *
     * For batched labs, batch 1 should be scheduled through the paired path so the
     * scheduler only commits the row when batch 2 can also be placed.
     *
     * Returns true when the lab block was committed.
     */
    private boolean tryAssignLabBlock(Subject sub, ConflictTracker tracker,
                                       List<ScheduledSlot> result) {
        if (sub.isBatchLab()) {
            logWarn("Batch lab " + subjectSummary(sub)
                    + " reached the single-lab path. Use paired scheduling instead.");
            return false;
        }

        // Course base name = name without section suffix for batch pairing
        String courseBase = sub.getName();
        String parentKey  = sub.getParentSectionKey();
        String teacher    = sub.getEffectiveTeacher();

        logInfo("Trying lab placement for " + subjectSummary(sub) + " using course base "
                + courseBase + ".");

        // BATCH 2: must use same slot as batch 1
        // BATCH 1 or no-batch: find a valid slot
        Day[] days = daysForYear(sub.getYear());
        boolean morningOnly = isAfternoonRestricted(sub.getYear());

        // Try morning slots first (soft preference for year 2&3)
        boolean assigned = tryLabInDays(sub, tracker, result, days, teacher,
                         parentKey, courseBase, morningOnly);
        // Fallback to afternoon if morning failed
        if (!assigned && morningOnly) {
            logInfo("Lab fallback to afternoon for " + subjectSummary(sub) + ".");
            assigned = tryLabInDays(sub, tracker, result, days, teacher,
                                     parentKey, courseBase, false);
        }
        return assigned;
    }

    private boolean tryLabInDays(Subject sub, ConflictTracker tracker,
                                  List<ScheduledSlot> result, Day[] days,
                                  String teacher, String parentKey,
                                  String courseBase, boolean morningOnly) {
        for (Day day : orderDaysByLoad(days, result, parentKey)) {
            // C4: only 1 lab session per section per day (parent key, no batch)
            if (tracker.hasLabToday(parentKey, day)) continue;

            for (TimeSlot start : TimeSlot.labStartSlots()) {
                TimeSlot next = start.next();
                if (next == null) continue;  // should not happen with labStartSlots()

                if (morningOnly && (start.isAfternoon() || next.isAfternoon())) continue;

                if (tracker.isTeacherBusy(teacher, day, start)) continue;
                if (tracker.isTeacherBusy(teacher, day, next))  continue;
                // Check if theory or any batch is busy — prevents lab/theory conflict
                if (tracker.isSectionBusyIncludingBatches(parentKey, day, start)) continue;
                if (tracker.isSectionBusyIncludingBatches(parentKey, day, next))  continue;

                Room lab = findFreeLabRoom(sub, day, start, next, tracker);
                if (lab == null) continue;

                // All checks passed
                result.add(new ScheduledSlot(sub, lab, day, start, false));
                result.add(new ScheduledSlot(sub, lab, day, next,  true));
                tracker.commitLabBlock(teacher, sub.getSectionKey(), parentKey,
                                        courseBase, lab.getId(), day, start, next);
                tracker.markSubjectOnDay(sub.getId(), day);
                logInfo("Placed lab block for " + subjectSummary(sub) + " in room "
                        + lab.getId() + " on " + day + " starting " + start.getDisplay() + ".");
                return true;
            }
        }
        return false;
    }

    /**
     * Assign batch 1 and batch 2 together for the same section/course.
     *
     * The method searches for a single day/start slot where:
     * - batch 1 is free,
     * - batch 2 is free,
     * - both teachers are free,
     * - the section can take a lab session that day,
     * - and two different lab rooms are available for the two simultaneous rows.
     *
     * Nothing is committed unless the full pair fits.
     */
    private boolean tryAssignPairedLabBlock(Subject batch1, List<Subject> allBatches,
                                            ConflictTracker tracker,
                                            List<ScheduledSlot> result) {
        if (allBatches == null || allBatches.isEmpty()) {
            logWarn("No batches found for " + subjectSummary(batch1));
            return false;
        }

        int n = allBatches.size();
        String courseBase = batch1.getName();
        String parentKey   = batch1.getParentSectionKey();

        logInfo("Trying " + n + "-batch lab placement for " + subjectSummary(batch1) + ".");

        Day[] days = daysForYear(batch1.getYear());
        boolean morningOnly = isAfternoonRestricted(batch1.getYear());

        for (Day day : orderDaysByLoad(days, result, parentKey)) {
            if (tracker.hasLabToday(parentKey, day)) continue;

            for (TimeSlot start : TimeSlot.labStartSlots()) {
                TimeSlot next = start.next();
                if (next == null) continue;

                if (morningOnly && (start.isAfternoon() || next.isAfternoon())) continue;

                boolean allTeachersFree = true;
                for (Subject batch : allBatches) {
                    String teacher = batch.getEffectiveTeacher();
                    if (teacher != null && !teacher.isBlank()) {
                        if (tracker.isTeacherBusy(teacher, day, start)
                                || tracker.isTeacherBusy(teacher, day, next)) {
                            allTeachersFree = false;
                            break;
                        }
                    }
                }
                if (!allTeachersFree) {
                    continue;
                }

                // Check if theory or any batch is busy — prevents lab/theory conflict
                if (tracker.isSectionBusyIncludingBatches(parentKey, day, start)
                        || tracker.isSectionBusyIncludingBatches(parentKey, day, next)) {
                    continue;
                }

                Room[] rooms = findNFreeLabRooms(batch1, n, day, start, next, tracker);
                if (rooms == null) continue;

                for (int i = 0; i < n; i++) {
                    Subject batch = allBatches.get(i);
                    Room room = rooms[i];
                    String teacher = batch.getEffectiveTeacher();

                    result.add(new ScheduledSlot(batch, room, day, start, false));
                    result.add(new ScheduledSlot(batch, room, day, next, true));
                    tracker.commitLabBlock(teacher, batch.getSectionKey(), parentKey,
                            courseBase, room.getId(), day, start, next);
                    tracker.markSubjectOnDay(batch.getId(), day);
                    tracker.markSubjectScheduled(batch.getId());
                }

                logInfo("Placed " + n + "-batch lab blocks for " + parentKey
                        + " on " + day + " starting " + start.getDisplay()
                        + " in rooms: " + Arrays.stream(rooms)
                        .map(Room::getId)
                        .collect(Collectors.joining(", ")) + ".");
                return true;
            }
        }

        return false;
    }

    /**
     * Find rooms for an N-batch lab block.
     *
     * If a free lab has capacity >= 60, all batches may share that same lab.
     * Otherwise, batches must be placed in different free labs.
     */
    private Room[] findNFreeLabRooms(Subject sub, int n, Day day, TimeSlot s1, TimeSlot s2,
                                     ConflictTracker tracker) {
        String requiredLabType = sub.getRequiredLabType();
        List<Room> distinctFreeRooms = new ArrayList<>();
        Room sharedLargeLab = null;

        for (Room room : input.getRooms()) {
            if (!room.isLab()) continue;
            if (!room.matchesLabType(requiredLabType)) continue;
            if (tracker.isRoomBusy(room.getId(), day, s1)) continue;
            if (tracker.isRoomBusy(room.getId(), day, s2)) continue;

            if (room.getCapacity() >= 60) {
                sharedLargeLab = room;
                break;
            }

            distinctFreeRooms.add(room);
            if (distinctFreeRooms.size() == n) {
                return distinctFreeRooms.toArray(new Room[0]);
            }
        }

        if (sharedLargeLab != null) {
            Room[] rooms = new Room[n];
            Arrays.fill(rooms, sharedLargeLab);
            return rooms;
        }

        // No suitable set of rooms found — log diagnostics for debugging
        logLabRoomDiagnostics(sub, day, s1, s2, tracker);
        return null;
    }

    /** Print per-room diagnostics explaining why a room is unsuitable for the requested lab block. */
    private void logLabRoomDiagnostics(Subject sub, Day day, TimeSlot s1, TimeSlot s2,
                                       ConflictTracker tracker) {
        String required = sub.getRequiredLabType();
        StringBuilder sb = new StringBuilder();
        sb.append("No free lab rooms for " + subjectSummary(sub) + " at " + day + " " + s1.getDisplay() + "-" + s2.getDisplay() + "\n");
        for (Room room : input.getRooms()) {
            sb.append(" - ").append(room.getId()).append(" (type=")
              .append(room.getType()).append(", labType=")
              .append(room.getLabType()).append(", cap=")
              .append(room.getCapacity()).append(") : ");
            if (!room.isLab()) {
                sb.append("not a lab");
            } else if (required != null && !room.matchesLabType(required)) {
                sb.append("labType mismatch (needs=").append(required).append(")");
            } else if (tracker.isRoomBusy(room.getId(), day, s1) || tracker.isRoomBusy(room.getId(), day, s2)) {
                sb.append("busy at slot");
            } else if (room.getCapacity() >= 60) {
                sb.append("large (can host all batches) — but should have been accepted earlier");
            } else {
                sb.append("available candidate (unexpected) ");
            }
            sb.append("\n");
        }
        logInfo(sb.toString());
    }

    // ── THEORY ASSIGNMENT ─────────────────────────────────────────────────

    /**
     * Assign a single theory hour.
     *
     * Year 2 and 3 are attempted in morning slots first, then fall back to the
     * full day range if no morning slot can satisfy all constraints.
     */
    private boolean tryAssignTheoryHour(Subject sub, ConflictTracker tracker,
                                         List<ScheduledSlot> result) {
        Day[] days     = daysForYear(sub.getYear());
        boolean morning = isAfternoonRestricted(sub.getYear());
        String teacher  = sub.getEffectiveTeacher();

        logInfo("Trying theory placement for " + subjectSummary(sub) + ". Morning-only preference="
                + morning + ".");

        boolean ok = tryTheoryInDays(sub, tracker, result, days, teacher, morning);
        if (!ok && morning) {
            logInfo("Theory fallback to afternoon for " + subjectSummary(sub) + ".");
            ok = tryTheoryInDays(sub, tracker, result, days, teacher, false);
        }
        return ok;
    }

    private boolean tryTheoryInDays(Subject sub, ConflictTracker tracker,
                                     List<ScheduledSlot> result, Day[] days,
                                     String teacher, boolean morningOnly) {
        for (Day day : orderDaysByLoad(days, result, sub.getParentSectionKey())) {
            // C6: no same subject twice on same day
            if (tracker.hasSubjectToday(sub.getId(), day)) continue;

            for (TimeSlot slot : orderSlotsForSectionDay(sub, day, result)) {
                if (morningOnly && slot.isAfternoon()) continue;
                if (teacher != null && tracker.isTeacherBusy(teacher, day, slot))           continue;
                // Check if section (including any batches) is busy — prevents theory/lab conflict
                if (tracker.isSectionBusyIncludingBatches(sub.getParentSectionKey(), day, slot)) continue;

                Room room = findFreeRoom(sub, false, day, slot, tracker);
                if (room == null) continue;

                result.add(new ScheduledSlot(sub, room, day, slot, false));
                tracker.commitTheorySlot(teacher, sub.getSectionKey(),
                                          room.getId(), day, slot);
                tracker.markSubjectOnDay(sub.getId(), day);
                logInfo("Placed theory hour for " + subjectSummary(sub) + " in room "
                        + room.getId() + " on " + day + " at " + slot.getDisplay() + ".");
                return true;
            }
        }
        return false;
    }

    /**
     * Prefer filling earlier holes for a section/day before placing later periods.
     * This reduces intra-day gaps in section timetables.
     */
    private List<TimeSlot> orderSlotsForSectionDay(Subject sub, Day day,
                                                   List<ScheduledSlot> result) {
        Set<Integer> occupied = new HashSet<>();
        int maxOccupied = -1;

        for (ScheduledSlot scheduled : result) {
            if (scheduled.getDay() != day) continue;
            if (!Objects.equals(scheduled.getSubject().getParentSectionKey(), sub.getParentSectionKey())) {
                continue;
            }
            int idx = scheduled.getTimeSlot().getIndex();
            occupied.add(idx);
            if (idx > maxOccupied) {
                maxOccupied = idx;
            }
        }

        final int frontier = maxOccupied;
        return Arrays.stream(TimeSlot.values())
                .sorted(Comparator
                        .comparingInt((TimeSlot slot) -> {
                            int idx = slot.getIndex();
                            // Fill internal holes first (e.g., period 2 empty while 3 is used).
                            if (frontier >= 0 && idx < frontier && !occupied.contains(idx)) {
                                return 0;
                            }
                            // Then extend contiguously after the latest occupied slot.
                            if (idx == frontier + 1) {
                                return 1;
                            }
                            // Keep all other slots as last preference.
                            return 2;
                        })
                        .thenComparingInt(TimeSlot::getIndex))
                .collect(Collectors.toList());
    }

    /**
     * Return the days ordered by current load (fewest scheduled slots first).
     * Uses the provided `result` list to count already assigned slots per day.
     */
    private List<Day> orderDaysByLoad(Day[] days, List<ScheduledSlot> result, String sectionKey) {
        Map<Day, Integer> load = new EnumMap<>(Day.class);
        for (ScheduledSlot s : result) {
            if (sectionKey.equals(s.getSubject().getParentSectionKey())) {
                load.merge(s.getDay(), 1, Integer::sum);
            }
        }
        return Arrays.stream(days)
                .sorted(Comparator.comparingInt(d -> load.getOrDefault(d, 0)))
                .collect(Collectors.toList());
    }

    private int getEffectiveTheoryHours(Subject sub) {
        if (sub.getType() != Subject.Type.THEORY) {
            return 0;
        }
        if (sub.getCredits() == 4) {
            return sub.isTheoryOnlyFourCredit() ? 4 : 3;
        }
        return sub.getTheoryHoursPerWeek();
    }

    private int getEffectiveTotalHours(Subject sub) {
        if (sub.getType() == Subject.Type.LAB) {
            return sub.getLabHoursPerWeek();
        }
        return getEffectiveTheoryHours(sub);
    }

    // ── ROOM FINDERS ─────────────────────────────────────────────────────

    /**
     * Find a free classroom for theory.
     * If subject has a fixedRoomId: try that room first.
     * Falls back to any free classroom with a warning.
     * Labs are never returned for theory slots.
     */
    private Room findFreeRoom(Subject sub, boolean needLab, Day day,
                               TimeSlot slot, ConflictTracker tracker) {
        if (!needLab && sub.getFixedRoomId() != null) {
            // Try the TTO-pinned room first
            for (Room room : input.getRooms()) {
                if (room.getId().equals(sub.getFixedRoomId())
                        && !room.isLab()
                        && !tracker.isRoomBusy(room.getId(), day, slot)) {
                    logInfo("Using fixed room " + room.getId() + " for " + subjectSummary(sub)
                            + " on " + day + " " + slot.getDisplay() + ".");
                    return room;
                }
            }
            // Fixed room is occupied — warn and fall through to free selection
            logInfo("Fixed room " + sub.getFixedRoomId() + " unavailable for "
                    + subjectSummary(sub) + " on " + day + " " + slot.getDisplay()
                    + "; searching for an alternative room.");
        }

        // General free room search
        for (Room room : input.getRooms()) {
            if (room.isLab()) continue;   // never assign theory to a lab
            if (!tracker.isRoomBusy(room.getId(), day, slot)) return room;
        }
        return null;
    }

    /** Find a free lab room for BOTH consecutive slots. */
    private Room findFreeLabRoom(Subject sub, Day day, TimeSlot s1, TimeSlot s2,
                                  ConflictTracker tracker) {
        String requiredLabType = sub.getRequiredLabType();
        for (Room room : input.getRooms()) {
            if (!room.isLab()) continue;
            if (!room.matchesLabType(requiredLabType)) continue;  // NEW: filter by lab type
            if (tracker.isRoomBusy(room.getId(), day, s1)) continue;
            if (tracker.isRoomBusy(room.getId(), day, s2)) continue;
            return room;
        }
        return null;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    /**
     * Returns the current afternoon-restriction policy for year-specific placement.
     *
     * The current code keeps this disabled, but the hook remains so morning-biased
     * scheduling can be restored without touching the placement logic.
     */
    private boolean isAfternoonRestricted(int year) {
        return year == 2 || year == 3 ;
        // return false;  // disable morning-only preference for testing and flexibility
    }

    private Day[] daysForYear(int year) {
        if (year == 1 && input.getDaysInWeek() >= 6) {
            return Day.values();
        }
        return new Day[]{Day.MONDAY, Day.TUESDAY, Day.WEDNESDAY, Day.THURSDAY, Day.FRIDAY};
    }

    private void logInfo(String message) {
        LOG.log(Level.INFO, "[TimetableScheduler] {0}", message);
    }

    private void logWarn(String message) {
        LOG.log(Level.WARNING, "[TimetableScheduler] {0}", message);
    }

    private String subjectSummary(Subject sub) {
        return sub.getId() + " | year=" + sub.getYear()
                + " | section=" + sub.getSectionKey()
                + " | batch=" + sub.getBatch()
                + " | teacher=" + sub.getEffectiveTeacher();
    }

    /**
     * Fill empty slots with EL gap-filler courses after normal scheduling.
     * EL has no teacher, so it only needs a free room and a free section slot.
     */
    private void fillGapsWithEL(List<Subject> gapFillers, ConflictTracker tracker,
                                List<ScheduledSlot> result, Map<String, Integer> unscheduled) {
        for (Subject elCourse : gapFillers) {
            int needed = getEffectiveTotalHours(elCourse);
            int assigned = 0;

            for (Day day : daysForYear(elCourse.getYear())) {
                if (assigned >= needed) break;

                for (TimeSlot slot : TimeSlot.values()) {
                    if (assigned >= needed) break;
                    if (tracker.hasSubjectToday(elCourse.getId(), day)) continue;
                    if (tracker.isSectionBusyIncludingBatches(elCourse.getParentSectionKey(), day, slot)) continue;

                    Room room = findFreeRoom(elCourse, false, day, slot, tracker);
                    if (room == null) continue;

                    result.add(new ScheduledSlot(elCourse, room, day, slot, false));
                    tracker.commitElectiveSlot(elCourse.getSectionKey(), room.getId(), day, slot);
                    tracker.markSubjectOnDay(elCourse.getId(), day);
                    assigned++;
                }
            }

            unscheduled.put(elCourse.getId(), needed - assigned);
            logInfo("Filled " + assigned + " of " + needed + " EL hour(s) for " + subjectSummary(elCourse));
        }
    }
}

