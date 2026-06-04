package com.rvce.scas.scheduler.controller;

import com.rvce.scas.scheduler.algorithm.SchedulerResult;
import com.rvce.scas.scheduler.dto.DepartmentInput;
import com.rvce.scas.scheduler.service.SchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/generate")
    public ResponseEntity<SchedulerResult> generate(@RequestBody DepartmentInput input) {
        SchedulerResult result = schedulerService.generate(input);
        return ResponseEntity.ok(result);
    }
}