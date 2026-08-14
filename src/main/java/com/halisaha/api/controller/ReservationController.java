package com.halisaha.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.halisaha.api.dto.CreateReservationRequest;
import com.halisaha.api.dto.ReservationLinkInfo;
import com.halisaha.api.dto.ReservationResponse;
import com.halisaha.api.entity.Reservation;
import com.halisaha.api.entity.User;
import com.halisaha.api.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Rezervasyonlar", description = "Saha rezervasyon işlemleri")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "Belirtilen saha için yeni rezervasyon oluşturur (saat başı, tam 1 saatlik dilimler, 17:00-02:00 arası)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rezervasyon oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz tarih/saat (geçmiş tarih, saat başı olmayan, 1 saatten farklı süre, çalışma saatleri dışı)"),
            @ApiResponse(responseCode = "404", description = "Saha bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Bu saha, seçilen tarih ve saat aralığında zaten dolu")
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request,
                                                        @AuthenticationPrincipal User user) {
        Reservation reservation = reservationService.createReservation(
                request.venueId(), user, request.date(), request.startTime(), request.endTime());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(reservation));
    }

    @Operation(summary = "Giriş yapmış kullanıcının kendi rezervasyonlarını listeler")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Rezervasyon listesi döndü"))
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> myReservations(@AuthenticationPrincipal User user) {
        List<ReservationResponse> reservations = reservationService.getMyReservations(user).stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(reservations);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        ReservationLinkInfo link = reservationService.getLinkInfo(reservation);

        return new ReservationResponse(
                reservation.getId(),
                reservation.getVenue().getId(),
                reservation.getVenue().getName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                link.type(),
                link.status(),
                link.summary()
        );
    }
}
