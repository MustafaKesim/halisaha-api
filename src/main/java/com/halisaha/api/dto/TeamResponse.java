package com.halisaha.api.dto;

public record TeamResponse(
        Long id,
        String name,
        Long captainId,
        String captainName
) {}
