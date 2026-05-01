package com.rvce.scas.controller;

import com.rvce.scas.dto.LoginRequest;
import com.rvce.scas.dto.LoginResponse;
import com.rvce.scas.dto.LogoutRequest;
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

import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the authentication API for issuing, renewing, and revoking tokens.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns the access and refresh token pair.
     *
     * @param request login credentials submitted by the client
     * @return a login response containing bearer token metadata
     */
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

    /**
     * Rotates the refresh token and returns a new access token.
     *
     * @param request the refresh token request containing the user id and opaque token id
     * @return a new token pair for continued access
     */
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

    /**
     * Revokes the current device session and blacklists the presented tokens.
     *
     * @param principal the authenticated user making the request
     * @param authHeader the bearer token supplied in the Authorization header
     * @param request the logout payload containing the refresh token id
     * @return an empty successful response
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout and blacklist tokens")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestHeader(value = "Authorization") String authHeader,
            @Valid @RequestBody LogoutRequest request) {

        // FIX: refreshToken now comes from request body, not URL query parameter.
        // This prevents token leakage via logs, browser history, Referer headers.
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            accessToken = authHeader.substring(7);
        }
        authService.logout(accessToken, principal.getUserId(), request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    /**
     * Revokes every active session for the authenticated user.
     *
     * @param principal the authenticated user making the request
     * @param authHeader the bearer token supplied in the Authorization header
     * @return an empty successful response
     */
    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices")
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestHeader("Authorization") String authHeader) {

        // Note: logout-all does not need a refresh token since all devices are revoked server-side.
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            accessToken = authHeader.substring(7);
        }
        authService.logoutAllDevices(accessToken, principal.getUserId());
        return ResponseEntity.ok().build();
    }
}
