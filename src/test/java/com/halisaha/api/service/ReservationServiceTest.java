package com.halisaha.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.halisaha.api.entity.Reservation;
import com.halisaha.api.entity.User;
import com.halisaha.api.entity.Venue;
import com.halisaha.api.exception.BadRequestException;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.ResourceNotFoundException;
import com.halisaha.api.repository.MatchRepository;
import com.halisaha.api.repository.OpenChallengeRepository;
import com.halisaha.api.repository.OpenMatchRepository;
import com.halisaha.api.repository.ReservationRepository;
import com.halisaha.api.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private OpenMatchRepository openMatchRepository;

    @Mock
    private OpenChallengeRepository openChallengeRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private ReservationService reservationService;

    private final User user = User.builder().id(1L).email("test@example.com").build();
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @Test
    void createReservation_throwsBadRequest_whenDateInPast() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> reservationService.createReservation(
                1L, user, yesterday, LocalTime.of(17, 0), LocalTime.of(18, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Geçmiş");
    }

    @Test
    void createReservation_throwsBadRequest_whenStartTimeNotOnTheHour() {
        assertThatThrownBy(() -> reservationService.createReservation(
                1L, user, tomorrow, LocalTime.of(17, 30), LocalTime.of(18, 30)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("saat başı");
    }

    @Test
    void createReservation_throwsBadRequest_whenDurationNotOneHour() {
        assertThatThrownBy(() -> reservationService.createReservation(
                1L, user, tomorrow, LocalTime.of(17, 0), LocalTime.of(19, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1 saat");
    }

    @Test
    void createReservation_throwsBadRequest_whenOutsideOperatingHours() {
        assertThatThrownBy(() -> reservationService.createReservation(
                1L, user, tomorrow, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("çalışma saatleri");
    }

    @Test
    void createReservation_throwsResourceNotFound_whenVenueMissing() {
        when(venueRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(
                1L, user, tomorrow, LocalTime.of(17, 0), LocalTime.of(18, 0)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReservation_throwsConflict_whenSlotAlreadyTaken() {
        Venue venue = Venue.builder().id(1L).name("Saha 1").build();
        when(venueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(venue));
        when(reservationRepository.existsConflict(1L, tomorrow, LocalTime.of(17, 0), LocalTime.of(18, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(
                1L, user, tomorrow, LocalTime.of(17, 0), LocalTime.of(18, 0)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createReservation_savesReservation_whenValidAndAvailable() {
        Venue venue = Venue.builder().id(1L).name("Saha 1").build();
        when(venueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(venue));
        when(reservationRepository.existsConflict(1L, tomorrow, LocalTime.of(17, 0), LocalTime.of(18, 0)))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.createReservation(
                1L, user, tomorrow, LocalTime.of(17, 0), LocalTime.of(18, 0));

        assertThat(result.getVenue()).isEqualTo(venue);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(result.getEndTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void createReservation_rollsDateForward_whenStartTimeIsAfterMidnight() {
        LocalDate expectedActualDate = tomorrow.plusDays(1);

        Venue venue = Venue.builder().id(1L).name("Saha 1").build();
        when(venueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(venue));
        when(reservationRepository.existsConflict(1L, expectedActualDate, LocalTime.of(1, 0), LocalTime.of(2, 0)))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.createReservation(
                1L, user, tomorrow, LocalTime.of(1, 0), LocalTime.of(2, 0));

        assertThat(result.getDate()).isEqualTo(expectedActualDate);
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(1, 0));
    }
}
