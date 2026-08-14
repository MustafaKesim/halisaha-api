package com.halisaha.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.halisaha.api.entity.OpenMatch;
import com.halisaha.api.entity.OpenMatchStatus;
import com.halisaha.api.entity.Reservation;
import com.halisaha.api.entity.User;
import com.halisaha.api.exception.BadRequestException;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.ForbiddenException;
import com.halisaha.api.exception.ResourceNotFoundException;
import com.halisaha.api.repository.OpenChallengeRepository;
import com.halisaha.api.repository.OpenMatchRepository;
import com.halisaha.api.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenMatchService {

    private final OpenMatchRepository openMatchRepository;
    private final ReservationRepository reservationRepository;
    private final OpenChallengeRepository openChallengeRepository;

    @Transactional
    public OpenMatch createOpenMatch(Long reservationId, User currentUser, int missingPlayerCount) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasyon bulunamadı"));

        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Sadece kendi rezervasyonun için ilan açabilirsin");
        }

        if (openMatchRepository.existsByReservationId(reservationId)) {
            throw new ConflictException("Bu rezervasyon için zaten bir ilan var");
        }

        if (openChallengeRepository.existsByReservationId(reservationId)) {
            throw new ConflictException("Bu rezervasyon zaten bir meydan okuma için kullanılıyor");
        }

        if (reservation.getEffectiveEndDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Geçmiş bir rezervasyon için ilan açılamaz");
        }

        if (missingPlayerCount <= 0 || missingPlayerCount > 10) {
            throw new BadRequestException("Eksik oyuncu sayısı 1 ile 10 arasında olmalı");
        }

        OpenMatch openMatch = OpenMatch.builder()
                .reservation(reservation)
                .missingPlayerCount(missingPlayerCount)
                .acceptedCount(0)
                .status(OpenMatchStatus.OPEN)
                .build();

        return openMatchRepository.save(openMatch);
    }

    public List<OpenMatch> getOpenMatches() {
        LocalDateTime now = LocalDateTime.now();
        return openMatchRepository.findByStatus(OpenMatchStatus.OPEN).stream()
                .filter(om -> om.getReservation().getEffectiveEndDateTime().isAfter(now))
                .toList();
    }
}
