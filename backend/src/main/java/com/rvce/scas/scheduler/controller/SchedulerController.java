package com.rvce.scas.scheduler.controller;

import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.scheduler.algorithm.SchedulerResult;
import com.rvce.scas.scheduler.dto.DepartmentInput;
import com.rvce.scas.scheduler.mapper.SchedulerOutputAdapter;
import com.rvce.scas.scheduler.service.SchedulerService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final SchedulerService schedulerService;
    private final SchedulerOutputAdapter adapter;
    private final TimetableSlotRepository timetableSlotRepository;

    public SchedulerController(SchedulerService schedulerService,
                               SchedulerOutputAdapter adapter,
                               TimetableSlotRepository timetableSlotRepository) {
        this.schedulerService = schedulerService;
        this.adapter = adapter;
        this.timetableSlotRepository = timetableSlotRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<SchedulerResult> generate(@RequestBody DepartmentInput input) {
        SchedulerResult result = schedulerService.generate(input);
        List<TimetableSlot> entities = adapter.convert(result.getScheduledSlots());
        timetableSlotRepository.saveAll(entities);
        return ResponseEntity.ok(result);
    }
}