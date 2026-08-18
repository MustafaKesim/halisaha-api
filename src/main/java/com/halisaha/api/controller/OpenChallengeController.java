package com.halisaha.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.halisaha.api.dto.AcceptChallengeRequest;
import com.halisaha.api.dto.CreateChallengeRequest;
import com.halisaha.api.dto.MatchResponse;
import com.halisaha.api.dto.OpenChallengeResponse;
import com.halisaha.api.entity.Match;
import com.halisaha.api.entity.OpenChallenge;
import com.halisaha.api.entity.User;
import com.halisaha.api.service.OpenChallengeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Rakip Bulma / Meydan Okumalar", description = "Takımı olan kullanıcıların rakip araması ve maç oluşturma")
@RestController
@RequestMapping("/api/open-challenges")
@RequiredArgsConstructor
public class OpenChallengeController {

    private final OpenChallengeService openChallengeService;

    @Operation(summary = "Sahibi olduğun bir rezervasyon için takımınla meydan okuma açar")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Meydan okuma oluşturuldu"),
            @ApiResponse(responseCode = "403", description = "Rezervasyonun ya da takımın sahibi/kaptanı değilsin"),
            @ApiResponse(responseCode = "404", description = "Rezervasyon veya takım bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Bu rezervasyon için zaten bir ilan/meydan okuma var")
    })
    @PostMapping
    public ResponseEntity<OpenChallengeResponse> create(@Valid @RequestBody CreateChallengeRequest request,
                                                          @AuthenticationPrincipal User user) {
        OpenChallenge challenge = openChallengeService.createChallenge(
                request.reservationId(), request.teamId(), request.teamSize(), user);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(challenge));
    }

    @Operation(summary = "Açık (OPEN durumdaki) meydan okumaları listeler")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Meydan okuma listesi döndü"))
    @GetMapping
    public ResponseEntity<List<OpenChallengeResponse>> list() {
        List<OpenChallengeResponse> challenges = openChallengeService.getOpenChallenges().stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(challenges);
    }

    @Operation(summary = "Takımınla bir meydan okumayı kabul eder, otomatik olarak Match oluşturur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meydan okuma kabul edildi, maç oluştu"),
            @ApiResponse(responseCode = "403", description = "Takımın kaptanı değilsin ya da kendi meydan okumana kabul veremezsin"),
            @ApiResponse(responseCode = "404", description = "Meydan okuma veya takım bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Bu meydan okuma artık kapalı")
    })
    @PostMapping("/{id}/accept")
    public ResponseEntity<MatchResponse> accept(@PathVariable Long id,
                                                 @Valid @RequestBody AcceptChallengeRequest request,
                                                 @AuthenticationPrincipal User user) {
        Match match = openChallengeService.acceptChallenge(id, request.challengerTeamId(), user);

        MatchResponse response = new MatchResponse(
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

        return ResponseEntity.ok(response);
    }

    private OpenChallengeResponse toResponse(OpenChallenge challenge) {
        return new OpenChallengeResponse(
                challenge.getId(),
                challenge.getReservation().getId(),
                challenge.getReservation().getVenue().getName(),
                challenge.getReservation().getDate(),
                challenge.getReservation().getStartTime(),
                challenge.getReservation().getEndTime(),
                challenge.getTeam().getId(),
                challenge.getTeam().getName(),
                challenge.getTeamSize(),
                challenge.getStatus().name(),
                challenge.getTeam().getCaptain().getId(),
                challenge.getTeam().getCaptain().getFullName()
        );
    }
}
