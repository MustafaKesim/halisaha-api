package com.halisaha.api.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.halisaha.api.dto.VenueResponse;
import com.halisaha.api.service.ReservationService;
import com.halisaha.api.service.VenueService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Sahalar", description = "Saha listeleme ve müsaitlik sorgulama")
@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final ReservationService reservationService;

    @Operation(summary = "Tüm sahaları listeler")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Saha listesi döndü"))
    @GetMapping
    public ResponseEntity<List<VenueResponse>> list() {
        List<VenueResponse> venues = venueService.getAllVenues().stream()
                .map(v -> new VenueResponse(v.getId(), v.getName(), v.getLocation(), v.getPricePerHour()))
                .toList();

        return ResponseEntity.ok(venues);
    }

    @Operation(summary = "Belirtilen saha ve tarih için boş saat dilimlerini döner")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Boş saat listesi döndü"))
    @GetMapping("/{id}/available-slots")
    public ResponseEntity<List<LocalTime>> availableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reservationService.getAvailableSlots(id, date));
    }
}
