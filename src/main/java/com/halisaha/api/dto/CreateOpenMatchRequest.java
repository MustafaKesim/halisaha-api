package com.halisaha.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOpenMatchRequest(
        @NotNull Long reservationId,
        @NotNull @Positive @Max(10) Integer missingPlayerCount
) {}
