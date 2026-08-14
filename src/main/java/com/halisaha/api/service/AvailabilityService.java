package com.halisaha.api.service;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import com.halisaha.api.entity.User;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.repository.JoinRequestRepository;
import com.halisaha.api.repository.MatchRepository;
import com.halisaha.api.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ReservationRepository reservationRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final MatchRepository matchRepository;

    public void assertAvailable(User user, LocalDate date, LocalTime startTime, LocalTime endTime) {
        boolean busy = reservationRepository.existsUserConflict(user.getId(), date, startTime, endTime)
                || joinRequestRepository.existsAcceptedConflict(user.getId(), date, startTime, endTime)
                || matchRepository.existsCaptainConflict(user.getId(), date, startTime, endTime);

        if (busy) {
            throw new ConflictException("Bu tarih ve saatte zaten başka bir taahhüdün var");
        }
    }
}
