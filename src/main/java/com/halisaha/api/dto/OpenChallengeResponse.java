package com.halisaha.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record OpenChallengeResponse(
        Long id,
        Long reservationId,
        String venueName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Long teamId,
        String teamName,
        Integer teamSize,
        String status,
        Long ownerId,
        String ownerName
) {}
