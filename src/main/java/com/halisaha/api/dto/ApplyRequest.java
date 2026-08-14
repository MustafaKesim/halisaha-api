package com.halisaha.api.dto;

import jakarta.validation.constraints.NotNull;

public record ApplyRequest(
        @NotNull Long openMatchId
) {}
