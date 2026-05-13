package com.rvce.scas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for listing teachers (used in dropdown for invigilator assignment).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherListDto {
    private UUID userId;
    private String name;
    private String email;
    private String department;
}
