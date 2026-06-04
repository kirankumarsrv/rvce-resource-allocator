package com.rvce.scas.scheduler.algorithm;

import com.rvce.scas.scheduler.model.ScheduledSlot;
import com.rvce.scas.scheduler.model.Room;

import java.util.List;
import java.util.Map;

/**
 * Output from TimetableScheduler.
 * Contains both the successful assignments and any subjects that couldn't be
 * fully scheduled (so TTO can manually resolve).
 */
public class SchedulerResult {

    private final List<ScheduledSlot> scheduledSlots;

    // Full room inventory and schedule horizon, used for utilization reporting.
    private final List<Room> rooms;
    private final int daysInWeek;

    // subjectId -> how many hours were NOT assigned (0 = fully scheduled)
    private final Map<String, Integer> unscheduledHours;

    // Teacher load summary for fairness check
    private final Map<String, Integer> teacherLoadSummary;

    public SchedulerResult(List<ScheduledSlot> scheduledSlots,
                           List<Room> rooms,
                           int daysInWeek,
                           Map<String, Integer> unscheduledHours,
                           Map<String, Integer> teacherLoadSummary) {
        this.scheduledSlots = scheduledSlots;
        this.rooms = rooms;
        this.daysInWeek = daysInWeek;
        this.unscheduledHours = unscheduledHours;
        this.teacherLoadSummary = teacherLoadSummary;
    }

    public List<ScheduledSlot> getScheduledSlots() { return scheduledSlots; }
    public List<Room> getRooms() { return rooms; }
    public int getDaysInWeek() { return daysInWeek; }
    public Map<String, Integer> getUnscheduledHours() { return unscheduledHours; }
    public Map<String, Integer> getTeacherLoadSummary() { return teacherLoadSummary; }

    public boolean isFullyScheduled() {
        return unscheduledHours.values().stream().allMatch(h -> h == 0);
    }

    public void printSummary() {
        System.out.println("=== SCHEDULER RESULT ===");
        System.out.println("Total slots assigned : " + scheduledSlots.size());
        long failed = unscheduledHours.values().stream().filter(h -> h > 0).count();
        System.out.println("Subjects with gaps   : " + failed);
        if (failed > 0) {
            System.out.println("--- Unscheduled Hours ---");
            unscheduledHours.forEach((id, h) -> {
                if (h > 0) System.out.println("  " + id + " : " + h + " hr(s) unassigned");
            });
        }
        System.out.println("--- Teacher Load ---");
        teacherLoadSummary.forEach((t, h) -> System.out.println("  " + t + " : " + h + " hrs/week"));
    }
}
