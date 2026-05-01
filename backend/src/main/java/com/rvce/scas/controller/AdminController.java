package com.rvce.scas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Handles admin-facing endpoints for operational and audit visibility.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /**
     * Returns a lightweight success payload for the audit log endpoint.
     *
     * @return HTTP 200 response with a simple status marker
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, String>> auditLogs() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
