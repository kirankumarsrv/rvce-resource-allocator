package com.rvce.scas.scheduler.service;

import com.rvce.scas.scheduler.algorithm.SchedulerResult;
import com.rvce.scas.scheduler.algorithm.TimetableScheduler;
import com.rvce.scas.scheduler.dto.DepartmentInput;
import org.springframework.stereotype.Service;

@Service
public class SchedulerService {
    public SchedulerResult generate(DepartmentInput input) {
        TimetableScheduler scheduler = new TimetableScheduler(input);
        return scheduler.schedule();
    }
}