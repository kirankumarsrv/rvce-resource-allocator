package com.rvce.scas.controller;

import com.rvce.scas.dto.request.ExamHallConfigRequest;
import com.rvce.scas.dto.response.ExamHallDto;
import com.rvce.scas.dto.response.HallGridDto;
import com.rvce.scas.security.JwtPrincipal;
import com.rvce.scas.service.ExamHallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamHallController {

    private final ExamHallService examHallService;

    @PostMapping("/{examId}/halls")
    @PreAuthorize("hasAnyRole('DEPT_COORD','ADMIN','TTO','EXAM_CONTROLLER')")
    public ResponseEntity<ExamHallDto> addExamHall(
            @PathVariable UUID examId,
            @Valid @RequestBody ExamHallConfigRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ExamHallDto hall = examHallService.addHall(
                Objects.requireNonNull(examId),
                Objects.requireNonNull(request),
                Objects.requireNonNull(principal.getUserId()));
        return ResponseEntity.status(201).body(hall);
    }

    @GetMapping("/{examId}/halls")
    public ResponseEntity<List<ExamHallDto>> listExamHalls(
            @PathVariable UUID examId) {
        List<ExamHallDto> halls = examHallService.getHalls(Objects.requireNonNull(examId));
        return ResponseEntity.ok(halls);
    }

    @GetMapping("/{examId}/halls/{hallId}/grid")
    public ResponseEntity<HallGridDto> getHallGrid(
            @PathVariable UUID examId,
            @PathVariable UUID hallId) {
        HallGridDto grid = examHallService.getHallGrid(
                Objects.requireNonNull(examId),
                Objects.requireNonNull(hallId));
        return ResponseEntity.ok(grid);
    }

    @DeleteMapping("/{examId}/halls/{hallId}")
    @PreAuthorize("hasAnyRole('DEPT_COORD','ADMIN','TTO','EXAM_CONTROLLER')")
    public ResponseEntity<Void> deleteExamHall(
            @PathVariable UUID examId,
            @PathVariable UUID hallId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        examHallService.deleteHall(
                Objects.requireNonNull(examId),
                Objects.requireNonNull(hallId),
                Objects.requireNonNull(principal.getUserId()));
        return ResponseEntity.noContent().build();
    }
}
