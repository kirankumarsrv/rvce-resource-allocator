package com.rvce.scas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, String>> auditLogs() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
