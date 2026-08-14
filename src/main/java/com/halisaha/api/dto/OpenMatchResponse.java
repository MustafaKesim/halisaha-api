package com.halisaha.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record OpenMatchResponse(
        Long id,
        Long reservationId,
        String venueName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int missingPlayerCount,
        int acceptedCount,
        String status,
        Long ownerId,
        String ownerName,
        long pendingRequestCount
) {}
