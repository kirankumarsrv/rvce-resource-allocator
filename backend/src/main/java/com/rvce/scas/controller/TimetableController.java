package com.rvce.scas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadTimetable() {
        return ResponseEntity.ok(Map.of("status", "uploaded"));
    }

    @GetMapping("/versions")
    public ResponseEntity<Map<String, String>> versions() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
