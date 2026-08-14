package com.halisaha.api.dto;

import jakarta.validation.constraints.NotNull;

public record AcceptChallengeRequest(
        @NotNull Long challengerTeamId
) {}
