package com.halisaha.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record MatchResponse(
        Long id,
        Long reservationId,
        String venueName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName
) {}
