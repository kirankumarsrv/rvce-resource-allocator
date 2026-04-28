package com.rvce.scas.controller;

import com.rvce.scas.dto.LoginRequest;
import com.rvce.scas.dto.LoginResponse;
import com.rvce.scas.dto.RefreshRequest;
import com.rvce.scas.dto.TokenPair;
import com.rvce.scas.security.JwtPrincipal;
import com.rvce.scas.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and get JWT tokens")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokens = authService.login(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get new access token using refresh token")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenPair tokens = authService.refresh(request.getUserId(), request.getRefreshToken());

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and blacklist tokens")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestHeader(value = "Authorization") String authHeader,
            @RequestParam(required = false) String refreshToken) {

        String accessToken = authHeader.substring(7);
        authService.logout(accessToken, principal.getUserId(), refreshToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices")
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestHeader("Authorization") String authHeader) {

        String accessToken = authHeader.substring(7);
        authService.logoutAllDevices(accessToken, principal.getUserId());
        return ResponseEntity.ok().build();
    }
}
