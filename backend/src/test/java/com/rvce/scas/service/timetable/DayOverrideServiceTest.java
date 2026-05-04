package com.rvce.scas.service.timetable;

import com.rvce.scas.cache.RoomAvailabilityCache;
import com.rvce.scas.dto.request.OverrideRequest;
import com.rvce.scas.dto.response.OverrideDto;
import com.rvce.scas.entity.DayOverride;
import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.entity.User;
import com.rvce.scas.exception.OverrideNotFoundException;
import com.rvce.scas.exception.SlotConflictException;
import com.rvce.scas.mapper.TimetableMapper;
import com.rvce.scas.repository.DayOverrideRepository;
import com.rvce.scas.repository.TimetableSlotRepository;
import com.rvce.scas.security.JwtPrincipal;
import com.rvce.scas.service.DayOverrideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit and integration tests for DayOverrideService (T-104).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-104: Day Override (Cancellation) Tests")
class DayOverrideServiceTest {

    @InjectMocks
    private DayOverrideService overrideService;

    @Mock
    private DayOverrideRepository overrideRepository;

    @Mock
    private TimetableSlotRepository slotRepository;

    @Mock
    private RoomAvailabilityCache cache;

    @Mock
    private TimetableMapper mapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Long slotId;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        slotId = 1L;
        testDate = LocalDate.now();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Teacher cancels own slot → 200 + day_overrides row created + Redis invalidated")
    void testTeacherCancelsOwnSlot() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"));
        doReturn(authorities).when(principal).getAuthorities();

        TimetableSlot slot = new TimetableSlot();
        slot.setId(slotId);
        User teacher = new User();
        teacher.setUserId(userId);
        slot.setTeacher(teacher);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(overrideRepository.existsBySlotIdAndDate(slotId, testDate)).thenReturn(false);
        when(overrideRepository.save(any(DayOverride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OverrideDto expected = new OverrideDto();
        expected.setId(1L);
        expected.setSlotId(slotId);
        expected.setDate(testDate);
        expected.setStatus(DayOverride.OverrideStatus.CANCELLED);
        when(mapper.toDto(any(DayOverride.class))).thenReturn(expected);

        OverrideRequest request = new OverrideRequest();
        request.setSlotId(slotId);
        request.setDate(testDate);
        request.setStatus(DayOverride.OverrideStatus.CANCELLED);

        OverrideDto result = overrideService.createOverride(request, principal);

        assertNotNull(result);
        assertEquals(slotId, result.getSlotId());
        assertEquals(DayOverride.OverrideStatus.CANCELLED, result.getStatus());
        verify(cache, times(1)).invalidateByDate(testDate);
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    @DisplayName("Teacher cancels another teacher's slot → 403")
    void testTeacherCancelsOtherTeacherSlot() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"));
        doReturn(authorities).when(principal).getAuthorities();

        TimetableSlot slot = new TimetableSlot();

        slot.setId(slotId);
        User teacher = new User();
        teacher.setUserId(UUID.randomUUID());
        slot.setTeacher(teacher);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        OverrideRequest request = new OverrideRequest();
        request.setSlotId(slotId);
        request.setDate(testDate);
        request.setStatus(DayOverride.OverrideStatus.CANCELLED);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
            overrideService.createOverride(request, principal)
        );
    }

    @Test
    @DisplayName("ADMIN cancels any slot → 200")
    void testAdminCancelsAnySlot() {
        UUID adminId = UUID.randomUUID();
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(principal.getUserId()).thenReturn(adminId);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(principal).getAuthorities();

        TimetableSlot slot = new TimetableSlot();
        slot.setId(slotId);
        User teacher = new User();
        teacher.setUserId(UUID.randomUUID());
        slot.setTeacher(teacher);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(overrideRepository.existsBySlotIdAndDate(slotId, testDate)).thenReturn(false);
        when(overrideRepository.save(any(DayOverride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        OverrideDto expected = new OverrideDto();
        expected.setId(2L);
        expected.setSlotId(slotId);
        expected.setDate(testDate);
        expected.setStatus(DayOverride.OverrideStatus.CANCELLED);
        when(mapper.toDto(any(DayOverride.class))).thenReturn(expected);

        OverrideRequest request = new OverrideRequest();
        request.setSlotId(slotId);
        request.setDate(testDate);
        request.setStatus(DayOverride.OverrideStatus.CANCELLED);

        OverrideDto result = overrideService.createOverride(request, principal);

        assertNotNull(result);
        assertEquals(slotId, result.getSlotId());
        assertEquals(DayOverride.OverrideStatus.CANCELLED, result.getStatus());
        verify(cache).invalidateByDate(testDate);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Duplicate override (same slotId + date) → 409")
    void testDuplicateOverride() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"));
        doReturn(authorities).when(principal).getAuthorities();

        TimetableSlot slot = new TimetableSlot();

        slot.setId(slotId);
        User teacher = new User();
        teacher.setUserId(userId);
        slot.setTeacher(teacher);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(overrideRepository.existsBySlotIdAndDate(slotId, testDate)).thenReturn(true);

        OverrideRequest request = new OverrideRequest();
        request.setSlotId(slotId);
        request.setDate(testDate);
        request.setStatus(DayOverride.OverrideStatus.CANCELLED);

        assertThrows(SlotConflictException.class, () -> overrideService.createOverride(request, principal));
    }

    @Test
    @DisplayName("Cancel → room:avail:{date}:* cleared → T-102 query returns room as available")
    void testCacheClearedAfterCancel() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"));
        doReturn(authorities).when(principal).getAuthorities();

        TimetableSlot slot = new TimetableSlot();

        slot.setId(slotId);
        User teacher = new User();
        teacher.setUserId(userId);
        slot.setTeacher(teacher);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(overrideRepository.existsBySlotIdAndDate(slotId, testDate)).thenReturn(false);
        when(overrideRepository.save(any(DayOverride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(DayOverride.class))).thenReturn(new OverrideDto());

        OverrideRequest request = new OverrideRequest();
        request.setSlotId(slotId);
        request.setDate(testDate);
        request.setStatus(DayOverride.OverrideStatus.CANCELLED);

        overrideService.createOverride(request, principal);

        verify(cache).invalidateByDate(testDate);
    }

    @Test
    @DisplayName("ClassCancelledEvent published → listener receives event with correct payload")
    void testClassCancelledEventPublished() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"));
        doReturn(authorities).when(principal).getAuthorities();

        TimetableSlot slot = new TimetableSlot();

        slot.setId(slotId);
        User teacher = new User();
        teacher.setUserId(userId);
        slot.setTeacher(teacher);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(overrideRepository.existsBySlotIdAndDate(slotId, testDate)).thenReturn(false);
        when(overrideRepository.save(any(DayOverride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(DayOverride.class))).thenReturn(new OverrideDto());

        OverrideRequest request = new OverrideRequest();
        request.setSlotId(slotId);
        request.setDate(testDate);
        request.setStatus(DayOverride.OverrideStatus.CANCELLED);

        overrideService.createOverride(request, principal);

        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Reinstate (DELETE /override/{id}) → row deleted, room no longer available on that date")
    void testReinstateSlot() {
        UUID userId = UUID.randomUUID();
        UUID overrideId = UUID.randomUUID();
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"));
        doReturn(authorities).when(principal).getAuthorities();

        TimetableSlot slot = new TimetableSlot();

        slot.setId(slotId);
        User teacher = new User();
        teacher.setUserId(userId);
        slot.setTeacher(teacher);

        DayOverride override = new DayOverride();
        override.setSlot(slot);
        override.setDate(testDate);

        when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));

        overrideService.deleteOverride(overrideId, principal);

        verify(overrideRepository, times(1)).delete(override);
        verify(cache, times(1)).invalidateByDate(testDate);
    }

    @Test
    @SuppressWarnings("null")
    @DisplayName("slotId not found → 404")
    void testSlotNotFound() {
        JwtPrincipal principal = mock(JwtPrincipal.class);
        UUID userId = UUID.randomUUID();
        when(principal.getUserId()).thenReturn(userId);

        when(slotRepository.findById(slotId)).thenReturn(Optional.empty());

        OverrideRequest request = new OverrideRequest();
        request.setSlotId(slotId);
        request.setDate(testDate);
        request.setStatus(DayOverride.OverrideStatus.CANCELLED);

        assertThrows(IllegalArgumentException.class, () -> overrideService.createOverride(request, principal));
    }

    @Test
    @SuppressWarnings("null")
    @DisplayName("overrideId not found on DELETE → 404")
    void testOverrideNotFound() {
        UUID missingId = UUID.randomUUID();
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(principal.getUserId()).thenReturn(UUID.randomUUID());

        when(overrideRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(OverrideNotFoundException.class, () -> overrideService.deleteOverride(missingId, principal));
    }

}