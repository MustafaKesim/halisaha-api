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

import com.halisaha.api.dto.ApplyRequest;
import com.halisaha.api.dto.JoinRequestResponse;
import com.halisaha.api.entity.JoinRequest;
import com.halisaha.api.entity.User;
import com.halisaha.api.service.JoinRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Maça Katılma Başvuruları", description = "Açık maç ilanlarına başvurma, kabul/red etme")
@RestController
@RequestMapping("/api/join-requests")
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    @Operation(summary = "Bir eksik oyuncu ilanına başvurur")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Başvuru oluşturuldu (PENDING)"),
            @ApiResponse(responseCode = "403", description = "Kendi ilanına başvuramazsın"),
            @ApiResponse(responseCode = "404", description = "İlan bulunamadı"),
            @ApiResponse(responseCode = "409", description = "İlan kapalı ya da zaten başvurmuşsun")
    })
    @PostMapping
    public ResponseEntity<JoinRequestResponse> apply(@Valid @RequestBody ApplyRequest request,
                                                       @AuthenticationPrincipal User user) {
        JoinRequest joinRequest = joinRequestService.apply(request.openMatchId(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(joinRequest));
    }

    @Operation(summary = "Giriş yapmış kullanıcının yaptığı tüm başvuruları ve durumlarını listeler")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Başvuru listesi döndü"))
    @GetMapping("/me")
    public ResponseEntity<List<JoinRequestResponse>> myRequests(@AuthenticationPrincipal User user) {
        List<JoinRequestResponse> requests = joinRequestService.getMyRequests(user).stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Bir başvuruyu kabul eder (kadro dolarsa ilan otomatik kapanır)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Başvuru kabul edildi"),
            @ApiResponse(responseCode = "403", description = "Sadece ilan sahibi kabul/red edebilir"),
            @ApiResponse(responseCode = "404", description = "Başvuru veya ilan bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Başvuru zaten değerlendirilmiş ya da kontenjan dolu")
    })
    @PostMapping("/{id}/accept")
    public ResponseEntity<JoinRequestResponse> accept(@PathVariable Long id, @AuthenticationPrincipal User user) {
        JoinRequest joinRequest = joinRequestService.respond(id, user, true);
        return ResponseEntity.ok(toResponse(joinRequest));
    }

    @Operation(summary = "Bir başvuruyu reddeder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Başvuru reddedildi"),
            @ApiResponse(responseCode = "403", description = "Sadece ilan sahibi kabul/red edebilir"),
            @ApiResponse(responseCode = "404", description = "Başvuru bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Başvuru zaten değerlendirilmiş")
    })
    @PostMapping("/{id}/reject")
    public ResponseEntity<JoinRequestResponse> reject(@PathVariable Long id, @AuthenticationPrincipal User user) {
        JoinRequest joinRequest = joinRequestService.respond(id, user, false);
        return ResponseEntity.ok(toResponse(joinRequest));
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
