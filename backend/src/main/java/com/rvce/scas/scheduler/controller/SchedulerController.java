package com.rvce.scas.scheduler.controller;

import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.entity.TimetableVersion;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.repository.TimetableVersionRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.scheduler.algorithm.SchedulerResult;
import com.rvce.scas.scheduler.dto.ConfirmScheduleRequest;
import com.rvce.scas.scheduler.dto.DepartmentInput;
import com.rvce.scas.scheduler.mapper.SchedulerOutputAdapter;
import com.rvce.scas.scheduler.model.ScheduledSlot;
import com.rvce.scas.scheduler.service.SchedulerService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    // Placeholder "confirmed by" user until real auth/session wiring lands (Kiran's auth work).
    // This is the same system/admin UUID already seeded as created_by on the original
    // timetable_versions row in V6__seed_data.sql.
    private static final UUID SYSTEM_USER_ID =
        UUID.fromString("44444444-4444-4444-4444-444444444002");

    // Fixed for now — no academic-year setting exists yet. Matches the seeded version's year.
    private static final String ACADEMIC_YEAR = "2025-26";

    private final SchedulerService schedulerService;
    private final SchedulerOutputAdapter adapter;
    private final TimetableSlotRepository timetableSlotRepository;
    private final TimetableVersionRepository timetableVersionRepository;
    private final UserRepository userRepository;

    public SchedulerController(SchedulerService schedulerService,
                               SchedulerOutputAdapter adapter,
                               TimetableSlotRepository timetableSlotRepository,
                               TimetableVersionRepository timetableVersionRepository,
                               UserRepository userRepository) {
        this.schedulerService = schedulerService;
        this.adapter = adapter;
        this.timetableSlotRepository = timetableSlotRepository;
        this.timetableVersionRepository = timetableVersionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<SchedulerResult> generate(@RequestBody DepartmentInput input) {
        SchedulerResult result = schedulerService.generate(input);
        return ResponseEntity.ok(result); // preview only — no DB write
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<?> confirm(@RequestBody ConfirmScheduleRequest request) {
        int semester = deriveSemester(request.getScheduledSlots());

        // Archive whatever was ACTIVE for this academic year + semester.
        // uq_active_semester is DEFERRABLE, so this update and the insert
        // below are both allowed in the same transaction.
        timetableVersionRepository.archiveActiveVersion(ACADEMIC_YEAR, semester, LocalDateTime.now());

        UUID versionId = UUID.randomUUID();
        TimetableVersion version = new TimetableVersion();
        version.setVersionId(versionId);
        version.setAcademicYear(ACADEMIC_YEAR);
        version.setSemester(semester);
        version.setLabel(request.getDepartment() + " — confirmed " + LocalDateTime.now());
        version.setStatus("ACTIVE");
        version.setCreatedBy(userRepository.getReferenceById(SYSTEM_USER_ID));
        version.setActivatedAt(LocalDateTime.now());
        version.setCreatedAt(LocalDateTime.now());
        timetableVersionRepository.save(version);

        List<TimetableSlot> entities = adapter.convert(request.getScheduledSlots(), versionId);

        timetableSlotRepository.deactivateActiveForDepartment(request.getDepartment());
        timetableSlotRepository.saveAll(entities);

        return ResponseEntity.ok(Map.of(
            "versionId", versionId,
            "savedSlots", entities.size()
        ));
    }

    private int deriveSemester(List<ScheduledSlot> scheduledSlots) {
        if (scheduledSlots == null || scheduledSlots.isEmpty()) return 0;
        String raw = scheduledSlots.get(0).getSubject().getSemester();
        if (raw == null || raw.isBlank()) return 0;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}