package com.rvce.scas.scheduler.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * RVCE time slots — 6 schedulable periods per day.
 *
 * RVCE day:
 *   09:00–10:00  SLOT_9AM    (period 1)
 *   10:00–11:00  SLOT_10AM   (period 2)
 *   [BREAK 11:00–11:30 — not a slot]
 *   11:30–12:30  SLOT_1130AM (period 3)
 *   12:30–13:30  SLOT_1230PM (period 4)
 *   [LUNCH 13:30–14:30 — not a slot]
 *   14:30–15:30  SLOT_230PM  (period 5)
 *   15:30–16:30  SLOT_330PM  (period 6)
 *
 * CRITICAL FIX: next() returns physically consecutive slot only.
 * SLOT_10AM.next() = null  — break at 11:00, no lab can start at 10AM.
 * SLOT_1230PM.next() = null — lunch at 13:30, no lab can start at 12:30.
 *
 * Valid lab pairs: (9AM,10AM), (11:30,12:30), (2:30,3:30)
 */
public enum TimeSlot {

    SLOT_9AM   (0, "9:00 AM",  false),
    SLOT_10AM  (1, "10:00 AM", false),
    SLOT_1130AM(2, "11:30 AM", false),
    SLOT_1230PM(3, "12:30 PM", false),
    SLOT_230PM (4, "2:30 PM",  true),
    SLOT_330PM (5, "3:30 PM",  true);

    private final int index;
    private final String display;
    private final boolean isAfternoon;

    TimeSlot(int index, String display, boolean isAfternoon) {
        this.index = index;
        this.display = display;
        this.isAfternoon = isAfternoon;
    }

    public int getIndex()        { return index; }
    public String getDisplay()   { return display; }
    public boolean isAfternoon() { return isAfternoon; }

    /**
     * Physically consecutive next slot. null if break/lunch/end-of-day follows.
     * SLOT_10AM → null (break), SLOT_1230PM → null (lunch), SLOT_330PM → null (end).
     */
    public TimeSlot next() {
        return switch (this) {
            case SLOT_9AM    -> SLOT_10AM;
            case SLOT_10AM   -> null;       // break follows — cannot be lab start
            case SLOT_1130AM -> SLOT_1230PM;
            case SLOT_1230PM -> null;       // lunch follows — cannot be lab start
            case SLOT_230PM  -> SLOT_330PM;
            case SLOT_330PM  -> null;       // end of day
        };
    }

    /** Only slots that have a valid consecutive next slot — lab can start here. */
    public static TimeSlot[] labStartSlots() {
        return new TimeSlot[]{ SLOT_9AM, SLOT_1130AM, SLOT_230PM };
    }

    public static TimeSlot fromIndex(int i) {
        for (TimeSlot s : values()) if (s.index == i) return s;
        throw new IllegalArgumentException("No TimeSlot for index " + i);
    }

    @JsonCreator
    public static TimeSlot fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        for (TimeSlot slot : values()) {
            if (slot.name().equalsIgnoreCase(value) || slot.display.equalsIgnoreCase(value)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("No TimeSlot for value " + value);
    }
}
