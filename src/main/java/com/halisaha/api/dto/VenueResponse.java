package com.halisaha.api.dto;

import java.math.BigDecimal;

public record VenueResponse(
        Long id,
        String name,
        String location,
        BigDecimal pricePerHour
) {}
