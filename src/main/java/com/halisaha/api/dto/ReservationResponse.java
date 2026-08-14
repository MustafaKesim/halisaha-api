package com.halisaha.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationResponse(
        Long id,
        Long venueId,
        String venueName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String linkType,
        String linkStatus,
        String linkSummary
) {}
