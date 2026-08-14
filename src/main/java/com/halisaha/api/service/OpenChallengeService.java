package com.halisaha.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.halisaha.api.entity.Match;
import com.halisaha.api.entity.OpenChallenge;
import com.halisaha.api.entity.OpenChallengeStatus;
import com.halisaha.api.entity.Reservation;
import com.halisaha.api.entity.Team;
import com.halisaha.api.entity.User;
import com.halisaha.api.exception.BadRequestException;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.ForbiddenException;
import com.halisaha.api.exception.ResourceNotFoundException;
import com.halisaha.api.repository.MatchRepository;
import com.halisaha.api.repository.OpenChallengeRepository;
import com.halisaha.api.repository.OpenMatchRepository;
import com.halisaha.api.repository.ReservationRepository;
import com.halisaha.api.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenChallengeService {

    private final OpenChallengeRepository openChallengeRepository;
    private final ReservationRepository reservationRepository;
    private final TeamRepository teamRepository;
    private final OpenMatchRepository openMatchRepository;
    private final MatchRepository matchRepository;
    private final AvailabilityService availabilityService;

    @Transactional
    public OpenChallenge createChallenge(Long reservationId, Long teamId, Integer teamSize, User currentUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasyon bulunamadı"));

        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Sadece kendi rezervasyonun için meydan okuma açabilirsin");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Takım bulunamadı"));

        if (!team.getCaptain().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Sadece kaptanı olduğun takımla meydan okuma açabilirsin");
        }

        if (openChallengeRepository.existsByReservationId(reservationId)) {
            throw new ConflictException("Bu rezervasyon için zaten bir meydan okuma var");
        }

        if (openMatchRepository.existsByReservationId(reservationId)) {
            throw new ConflictException("Bu rezervasyon zaten bir eksik oyuncu ilanı için kullanılıyor");
        }

        if (reservation.getEffectiveEndDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Geçmiş bir rezervasyon için meydan okuma açılamaz");
        }

        OpenChallenge challenge = OpenChallenge.builder()
                .team(team)
                .reservation(reservation)
                .status(OpenChallengeStatus.OPEN)
                .teamSize(teamSize)
                .build();

        return openChallengeRepository.save(challenge);
    }

    @Transactional
    public Match acceptChallenge(Long challengeId, Long challengerTeamId, User currentUser) {
        Team challengerTeam = teamRepository.findById(challengerTeamId)
                .orElseThrow(() -> new ResourceNotFoundException("Takım bulunamadı"));

        if (!challengerTeam.getCaptain().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Sadece kaptanı olduğun takımla meydan okumayı kabul edebilirsin");
        }

        OpenChallenge challenge = openChallengeRepository.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Meydan okuma bulunamadı"));

        if (challenge.getStatus() != OpenChallengeStatus.OPEN) {
            throw new ConflictException("Bu meydan okuma artık kapalı");
        }

        if (challenge.getTeam().getId().equals(challengerTeam.getId())) {
            throw new ForbiddenException("Kendi meydan okumana kabul veremezsin");
        }

        Reservation reservation = challenge.getReservation();
        availabilityService.assertAvailable(currentUser, reservation.getDate(),
                reservation.getStartTime(), reservation.getEndTime());

        challenge.setStatus(OpenChallengeStatus.CLOSED);
        openChallengeRepository.save(challenge);

        Match match = Match.builder()
                .reservation(reservation)
                .homeTeam(challenge.getTeam())
                .awayTeam(challengerTeam)
                .build();

        return matchRepository.save(match);
    }

    public List<OpenChallenge> getOpenChallenges() {
        return openChallengeRepository.findByStatus(OpenChallengeStatus.OPEN);
    }
}
