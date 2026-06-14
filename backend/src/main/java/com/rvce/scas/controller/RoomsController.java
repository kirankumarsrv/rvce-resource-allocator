package com.rvce.scas.controller;

import com.rvce.scas.entity.Room;
import com.rvce.scas.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomsController {

    private final RoomRepository roomRepository;

    /**
     * Returns all active rooms for the scheduler room picker.
     * Public endpoint — no auth required (room list is not sensitive).
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRooms() {
        List<Map<String, Object>> result = roomRepository.findAll().stream()
            .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getName());           // scheduler uses room name as ID
                m.put("name", r.getDisplayName());
                m.put("type", r.getRoomType());
                m.put("capacity", r.getCapacity());
                m.put("labType", r.getLabType());
                return m;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/availability")
    public ResponseEntity<Map<String, String>> availability() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/{roomId}/verify-occupancy")
    public ResponseEntity<Map<String, String>> verifyOccupancy(@PathVariable UUID roomId) {
        return ResponseEntity.ok(Map.of("roomId", roomId.toString(), "status", "verified"));
    }
}
