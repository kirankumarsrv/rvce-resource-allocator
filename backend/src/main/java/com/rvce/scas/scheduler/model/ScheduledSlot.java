    package com.rvce.scas.scheduler.model;

    import com.fasterxml.jackson.annotation.JsonCreator;
    import com.fasterxml.jackson.annotation.JsonProperty;

    /**
     * One scheduled entry: subject X is in room Y on day D at time T.
     * isLabSecondHour=true means this slot is the 2nd hour of a 2-hour lab block —
     * it's stored separately so the grid renders correctly, but is never used
     * as a "start" slot when checking constraints.
     */
    public class ScheduledSlot {

        private final Subject subject;
        private final Room room;
        private final Day day;
        private final TimeSlot timeSlot;
        private final boolean isLabSecondHour; // true = continuation of previous slot

        @JsonCreator
        public ScheduledSlot(@JsonProperty("subject") Subject subject,
                            @JsonProperty("room") Room room,
                            @JsonProperty("day") Day day,
                            @JsonProperty("timeSlot") TimeSlot timeSlot,
                            @JsonProperty("isLabSecondHour") boolean isLabSecondHour) {
            this.subject = subject;
            this.room = room;
            this.day = day;
            this.timeSlot = timeSlot;
            this.isLabSecondHour = isLabSecondHour;
        }

        public Subject getSubject() { return subject; }
        public Room getRoom() { return room; }
        public Day getDay() { return day; }
        public TimeSlot getTimeSlot() { return timeSlot; }
        public boolean isLabSecondHour() { return isLabSecondHour; }

        // Key used for conflict detection: "MONDAY_0" (day + slot index)
        public String timeKey() { return day.name() + "_" + timeSlot.getIndex(); }

        @Override
        public String toString() {
            return String.format("[%s %s] %s in %s%s",
                day, timeSlot.getDisplay(), subject.getName(), room.getId(),
                isLabSecondHour ? " (lab-2nd-hr)" : "");
        }
    }
