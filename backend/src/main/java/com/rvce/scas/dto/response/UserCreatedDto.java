package com.rvce.scas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO when a user is created.
 * Contains the new user's ID and a temporary password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedDto {
    private UUID userId;
    private String email;
    private String name;
    private String tempPassword;
    private String role;
    private String department;
}
