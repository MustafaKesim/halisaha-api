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

import com.halisaha.api.entity.Match;
import com.halisaha.api.entity.OpenChallenge;
import com.halisaha.api.entity.OpenChallengeStatus;
import com.halisaha.api.entity.Reservation;
import com.halisaha.api.entity.Team;
import com.halisaha.api.entity.User;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.ForbiddenException;
import com.halisaha.api.repository.MatchRepository;
import com.halisaha.api.repository.OpenChallengeRepository;
import com.halisaha.api.repository.OpenMatchRepository;
import com.halisaha.api.repository.ReservationRepository;
import com.halisaha.api.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class OpenChallengeServiceTest {

    @Mock
    private OpenChallengeRepository openChallengeRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private OpenMatchRepository openMatchRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private OpenChallengeService openChallengeService;

    private final User owner = User.builder().id(1L).build();
    private final User rivalCaptain = User.builder().id(2L).build();
    private final Reservation reservation = Reservation.builder().id(10L).user(owner)
            .date(LocalDate.now().plusDays(30)).startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 0)).build();
    private final Team homeTeam = Team.builder().id(50L).name("Ev Sahibi").captain(owner).build();
    private final Team awayTeam = Team.builder().id(60L).name("Rakip").captain(rivalCaptain).build();

    @Test
    void createChallenge_throwsForbidden_whenNotReservationOwner() {
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> openChallengeService.createChallenge(10L, 50L, 7, rivalCaptain))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createChallenge_throwsForbidden_whenNotTeamCaptain() {
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(teamRepository.findById(60L)).thenReturn(Optional.of(awayTeam));

        assertThatThrownBy(() -> openChallengeService.createChallenge(10L, 60L, 7, owner))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createChallenge_throwsConflict_whenReservationHasOpenMatch() {
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(teamRepository.findById(50L)).thenReturn(Optional.of(homeTeam));
        when(openChallengeRepository.existsByReservationId(10L)).thenReturn(false);
        when(openMatchRepository.existsByReservationId(10L)).thenReturn(true);

        assertThatThrownBy(() -> openChallengeService.createChallenge(10L, 50L, 7, owner))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createChallenge_savesOpenChallenge_whenValid() {
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(teamRepository.findById(50L)).thenReturn(Optional.of(homeTeam));
        when(openChallengeRepository.existsByReservationId(10L)).thenReturn(false);
        when(openMatchRepository.existsByReservationId(10L)).thenReturn(false);
        when(openChallengeRepository.save(any(OpenChallenge.class))).thenAnswer(inv -> inv.getArgument(0));

        OpenChallenge result = openChallengeService.createChallenge(10L, 50L, 7, owner);

        assertThat(result.getStatus()).isEqualTo(OpenChallengeStatus.OPEN);
        assertThat(result.getTeam()).isEqualTo(homeTeam);
        assertThat(result.getTeamSize()).isEqualTo(7);
    }

    @Test
    void acceptChallenge_throwsForbidden_whenAcceptingOwnChallenge() {
        OpenChallenge challenge = OpenChallenge.builder().id(200L).team(homeTeam)
                .reservation(reservation).status(OpenChallengeStatus.OPEN).teamSize(7).build();
        when(teamRepository.findById(50L)).thenReturn(Optional.of(homeTeam));
        when(openChallengeRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> openChallengeService.acceptChallenge(200L, 50L, owner))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptChallenge_throwsForbidden_whenAcceptingOwnChallengeWithADifferentCaptainedTeam() {
        Team secondTeamSameOwner = Team.builder().id(70L).name("Ikinci Takim").captain(owner).build();
        OpenChallenge challenge = OpenChallenge.builder().id(200L).team(homeTeam)
                .reservation(reservation).status(OpenChallengeStatus.OPEN).teamSize(7).build();
        when(teamRepository.findById(70L)).thenReturn(Optional.of(secondTeamSameOwner));
        when(openChallengeRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> openChallengeService.acceptChallenge(200L, 70L, owner))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptChallenge_throwsConflict_whenAlreadyClosed() {
        OpenChallenge challenge = OpenChallenge.builder().id(200L).team(homeTeam)
                .reservation(reservation).status(OpenChallengeStatus.CLOSED).teamSize(7).build();
        when(teamRepository.findById(60L)).thenReturn(Optional.of(awayTeam));
        when(openChallengeRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> openChallengeService.acceptChallenge(200L, 60L, rivalCaptain))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptChallenge_createsMatchAndClosesChallenge_whenValid() {
        OpenChallenge challenge = OpenChallenge.builder().id(200L).team(homeTeam)
                .reservation(reservation).status(OpenChallengeStatus.OPEN).teamSize(7).build();
        when(teamRepository.findById(60L)).thenReturn(Optional.of(awayTeam));
        when(openChallengeRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(challenge));
        when(openChallengeRepository.save(any(OpenChallenge.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        Match result = openChallengeService.acceptChallenge(200L, 60L, rivalCaptain);

        assertThat(challenge.getStatus()).isEqualTo(OpenChallengeStatus.CLOSED);
        assertThat(result.getHomeTeam()).isEqualTo(homeTeam);
        assertThat(result.getAwayTeam()).isEqualTo(awayTeam);
        assertThat(result.getReservation()).isEqualTo(reservation);
    }
}
