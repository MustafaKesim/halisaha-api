package com.halisaha.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.halisaha.api.dto.MatchResponse;
import com.halisaha.api.entity.Match;
import com.halisaha.api.entity.User;
import com.halisaha.api.service.MatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Maçlar", description = "Kabul edilmiş meydan okumalardan oluşan maçları listeleme")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "Kaptanı olduğun takımların (ev sahibi ya da rakip olarak) yer aldığı maçları listeler")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Maç listesi döndü"))
    @GetMapping("/me")
    public ResponseEntity<List<MatchResponse>> myMatches(@AuthenticationPrincipal User user) {
        List<MatchResponse> matches = matchService.getMyMatches(user).stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(matches);
    }

    private MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getReservation().getId(),
                match.getReservation().getVenue().getName(),
                match.getReservation().getDate(),
                match.getReservation().getStartTime(),
                match.getReservation().getEndTime(),
                match.getHomeTeam().getId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getId(),
                match.getAwayTeam().getName()
        );
    }
}
