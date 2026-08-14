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

import com.halisaha.api.dto.CreateOpenMatchRequest;
import com.halisaha.api.dto.JoinRequestResponse;
import com.halisaha.api.dto.OpenMatchResponse;
import com.halisaha.api.entity.JoinRequest;
import com.halisaha.api.entity.OpenMatch;
import com.halisaha.api.entity.User;
import com.halisaha.api.service.JoinRequestService;
import com.halisaha.api.service.OpenMatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Eksik Oyuncu İlanları", description = "Bir rezervasyona bağlı, eksik oyuncu arayan ilan oluşturma")
@RestController
@RequestMapping("/api/open-matches")
@RequiredArgsConstructor
public class OpenMatchController {

    private final OpenMatchService openMatchService;
    private final JoinRequestService joinRequestService;

    @Operation(summary = "Sahibi olduğun bir rezervasyon için eksik oyuncu ilanı açar")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "İlan oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Eksik oyuncu sayısı geçersiz"),
            @ApiResponse(responseCode = "403", description = "Bu rezervasyonun sahibi değilsin"),
            @ApiResponse(responseCode = "404", description = "Rezervasyon bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Bu rezervasyon için zaten bir ilan/meydan okuma var")
    })
    @PostMapping
    public ResponseEntity<OpenMatchResponse> create(@Valid @RequestBody CreateOpenMatchRequest request,
                                                      @AuthenticationPrincipal User user) {
        OpenMatch openMatch = openMatchService.createOpenMatch(
                request.reservationId(), user, request.missingPlayerCount());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(openMatch));
    }

    @Operation(summary = "Açık (OPEN durumdaki) eksik oyuncu ilanlarını listeler")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "İlan listesi döndü"))
    @GetMapping
    public ResponseEntity<List<OpenMatchResponse>> list() {
        List<OpenMatchResponse> matches = openMatchService.getOpenMatches().stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(matches);
    }

    @Operation(summary = "Bir ilana gelen başvuruları listeler (sadece ilan sahibi görebilir)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Başvuru listesi döndü"),
            @ApiResponse(responseCode = "403", description = "İlan sahibi değilsin"),
            @ApiResponse(responseCode = "404", description = "İlan bulunamadı")
    })
    @GetMapping("/{id}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> joinRequests(@PathVariable Long id,
                                                                    @AuthenticationPrincipal User user) {
        List<JoinRequestResponse> requests = joinRequestService.getRequestsForMatch(id, user).stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(requests);
    }

    private OpenMatchResponse toResponse(OpenMatch openMatch) {
        return new OpenMatchResponse(
                openMatch.getId(),
                openMatch.getReservation().getId(),
                openMatch.getReservation().getVenue().getName(),
                openMatch.getReservation().getDate(),
                openMatch.getReservation().getStartTime(),
                openMatch.getReservation().getEndTime(),
                openMatch.getMissingPlayerCount(),
                openMatch.getAcceptedCount(),
                openMatch.getStatus().name(),
                openMatch.getReservation().getUser().getId(),
                openMatch.getReservation().getUser().getFullName(),
                joinRequestService.countPending(openMatch.getId())
        );
    }

    private JoinRequestResponse toResponse(JoinRequest joinRequest) {
        return new JoinRequestResponse(
                joinRequest.getId(),
                joinRequest.getOpenMatch().getId(),
                joinRequest.getOpenMatch().getReservation().getVenue().getName(),
                joinRequest.getOpenMatch().getReservation().getDate(),
                joinRequest.getOpenMatch().getReservation().getStartTime(),
                joinRequest.getOpenMatch().getReservation().getEndTime(),
                joinRequest.getApplicant().getId(),
                joinRequest.getApplicant().getFullName(),
                joinRequest.getStatus().name()
        );
    }
}
