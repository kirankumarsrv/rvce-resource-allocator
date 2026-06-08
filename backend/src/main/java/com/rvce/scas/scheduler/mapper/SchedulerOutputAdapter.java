package com.rvce.scas.scheduler.mapper;

import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.entity.User;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.scheduler.model.ScheduledSlot;
import com.rvce.scas.scheduler.model.TimeSlot;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class SchedulerOutputAdapter {

    private static final Logger log = LoggerFactory.getLogger(SchedulerOutputAdapter.class);

    // Layer 2 placeholder — real teacher lookup comes in Layer 3
    private static final UUID PLACEHOLDER_TEACHER_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444004");


    @PersistenceContext
    private EntityManager entityManager;


    // Existing timetable version from seed data
    private static final UUID TIMETABLE_VERSION_ID =
            UUID.fromString("66666666-6666-6666-6666-666666666001");

    private final RoomRepository roomRepository;


   public SchedulerOutputAdapter(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<TimetableSlot> convert(List<ScheduledSlot> scheduledSlots) {
        List<TimetableSlot> result = new ArrayList<>();

        // Load placeholder teacher once — same for all slots in Layer 2
        User teacher = entityManager.getReference(User.class, PLACEHOLDER_TEACHER_ID);

        for (ScheduledSlot slot : scheduledSlots) {

            // Look up Room entity by name — scheduler room ID must match DB room name
            Optional<Room> roomOpt = roomRepository.findByName(slot.getRoom().getId());
            if (roomOpt.isEmpty()) {
                log.warn("Room '{}' not found in DB, skipping slot: {}",
                        slot.getRoom().getId(), slot);
                continue;
            }

            TimetableSlot entity = new TimetableSlot();

            // Day: 0=Monday in enum, 1=Monday in DB
            entity.setDayOfWeek(slot.getDay().getIndex() + 1);

            // Time mapping
            entity.setStartTime(toStartTime(slot.getTimeSlot()));
            entity.setEndTime(toEndTime(slot.getTimeSlot()));
            entity.setPeriodNumber(slot.getTimeSlot().getIndex() + 1);

            // Relationships
            entity.setRoom(roomOpt.get());
            entity.setTeacher(teacher);

            // Subject fields
            entity.setSubject(slot.getSubject().getName());
            entity.setSubjectCode(slot.getSubject().getId());
            entity.setSection(slot.getSubject().getSection() != null
                    ? slot.getSubject().getSection() : "ALL");
            entity.setDepartment(slot.getSubject().getDepartment());
            entity.setSemester(parseSemester(slot.getSubject().getSemester()));

            // Metadata
            entity.setVersionId(TIMETABLE_VERSION_ID);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setIsActive(true);

            result.add(entity);
        }

        log.info("Adapter: converted {}/{} slots",
                result.size(), scheduledSlots.size());
        return result;
    }

    private LocalTime toStartTime(TimeSlot slot) {
        return switch (slot) {
            case SLOT_9AM    -> LocalTime.of(9,  0);
            case SLOT_10AM   -> LocalTime.of(10, 0);
            case SLOT_1130AM -> LocalTime.of(11, 30);
            case SLOT_1230PM -> LocalTime.of(12, 30);
            case SLOT_230PM  -> LocalTime.of(14, 30);
            case SLOT_330PM  -> LocalTime.of(15, 30);
        };
    }

    private LocalTime toEndTime(TimeSlot slot) {
        return switch (slot) {
            case SLOT_9AM    -> LocalTime.of(10, 0);
            case SLOT_10AM   -> LocalTime.of(11, 0);
            case SLOT_1130AM -> LocalTime.of(12, 30);
            case SLOT_1230PM -> LocalTime.of(13, 30);
            case SLOT_230PM  -> LocalTime.of(15, 30);
            case SLOT_330PM  -> LocalTime.of(16, 30);
        };
    }

    private int parseSemester(String semester) {
        if (semester == null || semester.isBlank()) return 0;
        try { return Integer.parseInt(semester.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}