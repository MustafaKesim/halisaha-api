package com.halisaha.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ReservationTest {

    @Test
    void getEffectiveEndDateTime_staysSameDay_whenNoMidnightCrossing() {
        Reservation reservation = Reservation.builder()
                .date(LocalDate.of(2026, 8, 14))
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        assertThat(reservation.getEffectiveEndDateTime())
                .isEqualTo(LocalDateTime.of(2026, 8, 14, 18, 0));
    }

    @Test
    void getEffectiveEndDateTime_rollsToNextDay_whenEndTimeCrossesMidnight() {
        Reservation reservation = Reservation.builder()
                .date(LocalDate.of(2026, 8, 14))
                .startTime(LocalTime.of(23, 0))
                .endTime(LocalTime.of(0, 0))
                .build();

        assertThat(reservation.getEffectiveEndDateTime())
                .isEqualTo(LocalDateTime.of(2026, 8, 15, 0, 0));
    }
}
