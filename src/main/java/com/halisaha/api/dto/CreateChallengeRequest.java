package com.halisaha.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateChallengeRequest(
        @NotNull Long reservationId,
        @NotNull Long teamId,
        @NotNull @Min(3) @Max(11) Integer teamSize
) {}
