package com.halisaha.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record JoinRequestResponse(
        Long id,
        Long openMatchId,
        String venueName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Long applicantId,
        String applicantName,
        String status
) {}
