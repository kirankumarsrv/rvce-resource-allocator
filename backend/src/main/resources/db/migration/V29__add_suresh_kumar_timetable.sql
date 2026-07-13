INSERT INTO timetable_slots (
    slot_id, version_id, room_id, teacher_id, department, 
    subject_code, subject_name, section, semester, 
    day_of_week, period_number, start_time, end_time
) VALUES 
-- ==========================================================
-- MONDAY (Day 1) - Suresh Kumar Exclusive Slots
-- ==========================================================
-- Suresh Kumar: 08:00 - 09:00 -> DAA (Sec C, Room A205)
(
    101, '66666666-6666-6666-6666-666666666001', 
    '55555555-5555-5555-5555-555500000005', '44444444-4444-4444-4444-444444444024', 
    'Computer Science & Engineering', '21CS51', 'Design & Analysis of Algorithms', 'C', 5, 
    1, 1, '08:00', '09:00'
),
-- Suresh Kumar: 09:00 - 10:00 -> OS (Sec C, Room A205)
(
    102, '66666666-6666-6666-6666-666666666001', 
    '55555555-5555-5555-5555-555500000005', '44444444-4444-4444-4444-444444444024', 
    'Computer Science & Engineering', '21CS52', 'Operating Systems', 'C', 5, 
    1, 2, '09:00', '10:00'
),

-- ==========================================================
-- WEDNESDAY (Day 3)
-- ==========================================================
-- Suresh Kumar: 09:00 - 10:00 -> OS (Sec C, Room A205)
(
    103, '66666666-6666-6666-6666-666666666001', 
    '55555555-5555-5555-5555-555500000005', '44444444-4444-4444-4444-444444444024', 
    'Computer Science & Engineering', '21CS52', 'Operating Systems', 'C', 5, 
    3, 2, '09:00', '10:00'
),

-- ==========================================================
-- THURSDAY (Day 4)
-- ==========================================================
-- Suresh Kumar: 14:00 - 15:00 -> DAA (Sec C, Room A205)
(
    104, '66666666-6666-6666-6666-666666666001', 
    '55555555-5555-5555-5555-555500000005', '44444444-4444-4444-4444-444444444024', 
    'Computer Science & Engineering', '21CS51', 'Design & Analysis of Algorithms', 'C', 5, 
    4, 6, '14:00', '15:00'
),

-- ==========================================================
-- FRIDAY (Day 5) - Conflict Test Setup against Ramesh's existing Lab
-- ==========================================================
-- Suresh Kumar: 11:15 - 12:15 -> DAA (Sec C, Room A205)
(
    105, '66666666-6666-6666-6666-666666666001', 
    '55555555-5555-5555-5555-555500000005', '44444444-4444-4444-4444-444444444024', 
    'Computer Science & Engineering', '21CS51', 'Design & Analysis of Algorithms', 'C', 5, 
    5, 4, '11:15', '12:15'
)
ON CONFLICT DO NOTHING;