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
import org.springframework.http.HttpStatus;
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
        // Login flow step 1: receives credentials in request body (not URL query).
        /*
         * Beginner notes:
         * - Credentials (email + password) are sent in JSON body. This avoids logging
         *   secrets in server logs and URLs. Ensure your frontend sends these over HTTPS.
         * - Successful response returns an `accessToken` (short-lived JWT) and a
         *   `refreshToken` (opaque ID). The client must store the refresh token securely
         *   (not in localStorage if XSS risk exists; use secure storage mechanisms).
         */
        TokenPair tokens = authService.login(request.getEmail(), request.getPassword());

        // Login flow step 7: normalized token payload shape consumed by frontend.
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
        // Uses opaque refresh token id validated against Redis server-side state.
        /*
         * Refresh notes:
         * - The refresh endpoint expects a `userId` and the refreshToken id. The server
         *   validates the opaque id against Redis state before issuing a new access token.
         * - The new refresh token is rotated and returned. The old token is deleted server-side.
         */
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

        // FIX: explicit guard helps return stable 401 instead of null-principal runtime failures.
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // REVIEW-RISK (medium): refreshToken is currently sent as a URL query parameter.
        // URLs can end up in server access logs, browser history, proxies, and Referer headers.
        // A request body DTO would reduce accidental leakage, especially for a revocable token.
        // Robust parsing: check for presence and prefix to avoid runtime exceptions.
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            accessToken = authHeader.substring(7);
        }
        // If accessToken is null we still attempt logout of refresh token (best-effort).
        authService.logout(accessToken, principal.getUserId(), refreshToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices")
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestHeader("Authorization") String authHeader) {

        // FIX: defensive fallback in case security config is changed later by mistake.
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            accessToken = authHeader.substring(7);
        }
        authService.logoutAllDevices(accessToken, principal.getUserId());
        return ResponseEntity.ok().build();
    }
}
