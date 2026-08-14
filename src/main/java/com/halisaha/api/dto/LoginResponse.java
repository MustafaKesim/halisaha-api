package com.halisaha.api.dto;

public record LoginResponse(
        String token,
        Long userId,
        String email,
        String fullName
) {}
