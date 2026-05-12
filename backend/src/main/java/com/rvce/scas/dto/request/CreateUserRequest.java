package com.rvce.scas.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new user (teacher or student).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String name;           // User's full name
    private String email;          // Email (must be unique, RVCE domain preferred)
    private String usn;            // USN (only for students)
    private String role;           // Role: TEACHER, STUDENT, DEPT_COORD, etc.
    private String departmentCode; // Department code: CSE, ISE, ECE, MECH, CIVIL, EEE, ADMIN
}
