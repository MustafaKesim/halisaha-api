package com.halisaha.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.halisaha.api.entity.JoinRequest;
import com.halisaha.api.entity.JoinRequestStatus;
import com.halisaha.api.entity.OpenMatch;
import com.halisaha.api.entity.OpenMatchStatus;
import com.halisaha.api.entity.Reservation;
import com.halisaha.api.entity.User;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.ForbiddenException;
import com.halisaha.api.repository.JoinRequestRepository;
import com.halisaha.api.repository.OpenMatchRepository;

@ExtendWith(MockitoExtension.class)
class JoinRequestServiceTest {

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private OpenMatchRepository openMatchRepository;

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private JoinRequestService joinRequestService;

    private final User owner = User.builder().id(1L).build();
    private final User applicant = User.builder().id(2L).build();
    private final Reservation reservation = Reservation.builder().id(10L).user(owner).build();

    @Test
    void apply_throwsForbidden_whenApplyingToOwnMatch() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.OPEN).missingPlayerCount(1).acceptedCount(0).build();
        when(openMatchRepository.findById(100L)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> joinRequestService.apply(100L, owner))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void apply_throwsConflict_whenMatchAlreadyClosed() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.CLOSED).missingPlayerCount(1).acceptedCount(1).build();
        when(openMatchRepository.findById(100L)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> joinRequestService.apply(100L, applicant))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void apply_throwsConflict_whenAlreadyApplied() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.OPEN).missingPlayerCount(2).acceptedCount(0).build();
        when(openMatchRepository.findById(100L)).thenReturn(Optional.of(openMatch));
        when(joinRequestRepository.existsByOpenMatchIdAndApplicantId(100L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> joinRequestService.apply(100L, applicant))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void apply_savesPendingRequest_whenValid() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.OPEN).missingPlayerCount(2).acceptedCount(0).build();
        when(openMatchRepository.findById(100L)).thenReturn(Optional.of(openMatch));
        when(joinRequestRepository.existsByOpenMatchIdAndApplicantId(100L, 2L)).thenReturn(false);
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        JoinRequest result = joinRequestService.apply(100L, applicant);

        assertThat(result.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(result.getApplicant()).isEqualTo(applicant);
    }

    @Test
    void respond_closesOpenMatch_whenLastSlotAccepted() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.OPEN).missingPlayerCount(1).acceptedCount(0).build();
        JoinRequest joinRequest = JoinRequest.builder().id(500L).openMatch(openMatch)
                .applicant(applicant).status(JoinRequestStatus.PENDING).build();

        when(joinRequestRepository.findById(500L)).thenReturn(Optional.of(joinRequest));
        when(openMatchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(openMatch));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(openMatchRepository.save(any(OpenMatch.class))).thenAnswer(inv -> inv.getArgument(0));

        joinRequestService.respond(500L, owner, true);

        assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.ACCEPTED);
        assertThat(openMatch.getAcceptedCount()).isEqualTo(1);
        assertThat(openMatch.getStatus()).isEqualTo(OpenMatchStatus.CLOSED);
    }

    @Test
    void respond_throwsConflict_whenAlreadyFull() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.CLOSED).missingPlayerCount(1).acceptedCount(1).build();
        JoinRequest joinRequest = JoinRequest.builder().id(500L).openMatch(openMatch)
                .applicant(applicant).status(JoinRequestStatus.PENDING).build();

        when(joinRequestRepository.findById(500L)).thenReturn(Optional.of(joinRequest));
        when(openMatchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> joinRequestService.respond(500L, owner, true))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void respond_throwsForbidden_whenNonOwnerTriesToAccept() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.OPEN).missingPlayerCount(2).acceptedCount(0).build();
        JoinRequest joinRequest = JoinRequest.builder().id(500L).openMatch(openMatch)
                .applicant(applicant).status(JoinRequestStatus.PENDING).build();

        when(joinRequestRepository.findById(500L)).thenReturn(Optional.of(joinRequest));
        when(openMatchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(openMatch));

        User stranger = User.builder().id(99L).build();

        assertThatThrownBy(() -> joinRequestService.respond(500L, stranger, true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void respond_throwsConflict_whenAlreadyDecided() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.OPEN).missingPlayerCount(2).acceptedCount(1).build();
        JoinRequest joinRequest = JoinRequest.builder().id(500L).openMatch(openMatch)
                .applicant(applicant).status(JoinRequestStatus.ACCEPTED).build();

        when(joinRequestRepository.findById(500L)).thenReturn(Optional.of(joinRequest));

        assertThatThrownBy(() -> joinRequestService.respond(500L, owner, true))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void respond_rejectsRequest_whenAcceptIsFalse() {
        OpenMatch openMatch = OpenMatch.builder().id(100L).reservation(reservation)
                .status(OpenMatchStatus.OPEN).missingPlayerCount(2).acceptedCount(0).build();
        JoinRequest joinRequest = JoinRequest.builder().id(500L).openMatch(openMatch)
                .applicant(applicant).status(JoinRequestStatus.PENDING).build();

        when(joinRequestRepository.findById(500L)).thenReturn(Optional.of(joinRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        JoinRequest result = joinRequestService.respond(500L, owner, false);

        assertThat(result.getStatus()).isEqualTo(JoinRequestStatus.REJECTED);
    }
}
