package com.rvce.scas.rbac;

/**
 * Central list of application permission names used by RBAC checks.
 */
public final class PermissionConstants {
    /**
     * Prevents instantiation of this constants holder.
     */
    private PermissionConstants() {
    }

    public static final String ROOM_READ = "ROOMS_READ";
    public static final String ROOM_WRITE = "ROOMS_WRITE";
    public static final String ROOM_VERIFY = "ROOMS_VERIFY";
    public static final String ROOM_DELETE = "ROOMS_DELETE";

    public static final String TIMETABLE_READ = "TIMETABLE_READ";
    public static final String TIMETABLE_WRITE = "TIMETABLE_WRITE";
    public static final String TIMETABLE_GENERATE = "TIMETABLE_GENERATE";
    public static final String TIMETABLE_DELETE = "TIMETABLE_DELETE";

    public static final String EXAM_READ = "EXAM_READ";
    public static final String EXAM_WRITE = "EXAM_WRITE";
    public static final String EXAM_GENERATE = "EXAM_GENERATE";
    public static final String EXAM_PUBLISH = "EXAM_PUBLISH";
    public static final String EXAM_DELETE = "EXAM_DELETE";

    public static final String USERS_READ = "USERS_READ";
    public static final String USERS_WRITE = "USERS_WRITE";

    public static final String REPORTS_READ = "REPORTS_READ";
    public static final String AUDIT_READ = "AUDIT_READ";
    public static final String ADMIN_ALL = "ADMIN_ALL";
}
