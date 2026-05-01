package com.rvce.scas.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvce.scas.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Serializes authorization failures into the API's JSON error shape.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

        /**
         * Handles a forbidden request by logging the attempt and returning JSON.
         *
         * @param request the current HTTP request
         * @param response the HTTP response to populate
         * @param accessDeniedException the Spring Security access denied exception
         * @throws IOException if the response body cannot be written
         */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";
        String authorities = auth != null ? auth.getAuthorities().toString() : "[]";

        log.warn("ACCESS_DENIED: user={}, path={}, method={}, authorities={}",
                username,
                request.getRequestURI(),
                request.getMethod(),
                authorities
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(403)
                .error("Forbidden")
                .code("INSUFFICIENT_PERMISSIONS")
                .message("You do not have permission to perform this action.")
                .path(request.getRequestURI())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
