package com.rvce.scas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomsController {

    @GetMapping("/availability")
    public ResponseEntity<Map<String, String>> availability() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/{roomId}/verify-occupancy")
    public ResponseEntity<Map<String, String>> verifyOccupancy(@PathVariable UUID roomId) {
        return ResponseEntity.ok(Map.of("roomId", roomId.toString(), "status", "verified"));
    }
}
