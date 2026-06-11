package com.rvce.scas.scheduler.model;

/**
 * Days of the week.
 * First-year students can use Saturday when the department runs a 6-day week.
 * 2nd/3rd year: Mon-Fri only.
 */
public enum Day {   
    MONDAY(0), TUESDAY(1), WEDNESDAY(2), THURSDAY(3), FRIDAY(4), SATURDAY(5);

    private final int index;
    Day(int index) { this.index = index; }
    public int getIndex() { return index; }

    // Returns days for a given year and department week length.
    public static Day[] daysForYear(int year, int daysInWeek) {
        if (year == 1 && daysInWeek >= 6) return values();       // Mon-Sat
        return new Day[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}; // Mon-Fri
    }

    /**
     * Backward-compatible default for callers that do not know the department week length.
     * Keeps the legacy year-1 Saturday behavior.
     */
    @Deprecated
    public static Day[] daysForYear(int year) {
        return daysForYear(year, 6);
    }
}
