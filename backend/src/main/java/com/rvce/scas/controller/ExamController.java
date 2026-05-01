package com.rvce.scas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Provides exam-related endpoints used by controllers and reviewers.
 */
@RestController
@RequestMapping("/api/exam")
public class ExamController {

    /**
     * Marks the requested exam as published.
     *
     * @param examId the exam identifier to publish
     * @return a simple confirmation payload with the exam id and status
     */
    @PostMapping("/{examId}/publish")
    public ResponseEntity<Map<String, String>> publish(@PathVariable UUID examId) {
        return ResponseEntity.ok(Map.of("examId", examId.toString(), "status", "published"));
    }
}
