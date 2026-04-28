package com.rvce.scas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RefreshRequest {
    @NotNull
    private UUID userId;

    @NotBlank
    private String refreshToken;
}
