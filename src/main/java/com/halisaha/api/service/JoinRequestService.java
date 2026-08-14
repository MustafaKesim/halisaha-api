package com.halisaha.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.halisaha.api.entity.JoinRequest;
import com.halisaha.api.entity.JoinRequestStatus;
import com.halisaha.api.entity.OpenMatch;
import com.halisaha.api.entity.OpenMatchStatus;
import com.halisaha.api.entity.Reservation;
import com.halisaha.api.entity.User;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.ForbiddenException;
import com.halisaha.api.exception.ResourceNotFoundException;
import com.halisaha.api.repository.JoinRequestRepository;
import com.halisaha.api.repository.OpenMatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final OpenMatchRepository openMatchRepository;
    private final AvailabilityService availabilityService;

    @Transactional
    public JoinRequest apply(Long openMatchId, User applicant) {
        OpenMatch openMatch = openMatchRepository.findById(openMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("İlan bulunamadı"));

        if (openMatch.getStatus() != OpenMatchStatus.OPEN) {
            throw new ConflictException("Bu ilan artık kapalı");
        }

        if (openMatch.getReservation().getUser().getId().equals(applicant.getId())) {
            throw new ForbiddenException("Kendi ilanına başvuramazsın");
        }

        if (joinRequestRepository.existsByOpenMatchIdAndApplicantId(openMatchId, applicant.getId())) {
            throw new ConflictException("Bu ilana zaten başvurdun");
        }

        Reservation reservation = openMatch.getReservation();
        availabilityService.assertAvailable(applicant, reservation.getDate(),
                reservation.getStartTime(), reservation.getEndTime());

        JoinRequest joinRequest = JoinRequest.builder()
                .openMatch(openMatch)
                .applicant(applicant)
                .status(JoinRequestStatus.PENDING)
                .build();

        return joinRequestRepository.save(joinRequest);
    }

    @Transactional
    public JoinRequest respond(Long joinRequestId, User currentUser, boolean accept) {
        JoinRequest joinRequest = joinRequestRepository.findById(joinRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Başvuru bulunamadı"));

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new ConflictException("Bu başvuru zaten değerlendirilmiş");
        }

        // .getId() lazy proxy'yi initialize etmez (FK'den okunur), bu yüzden güvenli;
        // OpenMatch'i asıl (kilitli) sorgudan önce başka bir yoldan yüklemiyoruz.
        Long openMatchId = joinRequest.getOpenMatch().getId();

        if (!accept) {
            // Red yolunda sayaç değişmiyor, kilide gerek yok; sahiplik kontrolü burada güvenli.
            Long ownerId = joinRequest.getOpenMatch().getReservation().getUser().getId();
            if (!ownerId.equals(currentUser.getId())) {
                throw new ForbiddenException("Sadece ilan sahibi başvuruları değerlendirebilir");
            }
            joinRequest.setStatus(JoinRequestStatus.REJECTED);
            return joinRequestRepository.save(joinRequest);
        }

        OpenMatch openMatch = openMatchRepository.findByIdForUpdate(openMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("İlan bulunamadı"));

        Long ownerId = openMatch.getReservation().getUser().getId();
        if (!ownerId.equals(currentUser.getId())) {
            throw new ForbiddenException("Sadece ilan sahibi başvuruları değerlendirebilir");
        }

        if (openMatch.getStatus() != OpenMatchStatus.OPEN
                || openMatch.getAcceptedCount() >= openMatch.getMissingPlayerCount()) {
            throw new ConflictException("Kontenjan dolu");
        }

        // Basvuru anindan bu yana basvuranin baska bir taahhude girmis olabilecegi ihtimaline karsi son anda tekrar kontrol.
        Reservation reservation = openMatch.getReservation();
        availabilityService.assertAvailable(joinRequest.getApplicant(), reservation.getDate(),
                reservation.getStartTime(), reservation.getEndTime());

        joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
        joinRequestRepository.save(joinRequest);

        openMatch.setAcceptedCount(openMatch.getAcceptedCount() + 1);
        if (openMatch.getAcceptedCount() >= openMatch.getMissingPlayerCount()) {
            openMatch.setStatus(OpenMatchStatus.CLOSED);
        }
        openMatchRepository.save(openMatch);

        return joinRequest;
    }

    public List<JoinRequest> getRequestsForMatch(Long openMatchId, User currentUser) {
        OpenMatch openMatch = openMatchRepository.findById(openMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("İlan bulunamadı"));

        if (!openMatch.getReservation().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Sadece ilan sahibi başvuruları görebilir");
        }

        return joinRequestRepository.findByOpenMatchId(openMatchId);
    }

    public List<JoinRequest> getMyRequests(User applicant) {
        return joinRequestRepository.findByApplicantId(applicant.getId());
    }

    public long countPending(Long openMatchId) {
        return joinRequestRepository.countByOpenMatchIdAndStatus(openMatchId, JoinRequestStatus.PENDING);
    }
}
