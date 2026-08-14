package com.halisaha.api.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.halisaha.api.entity.Venue;
import com.halisaha.api.repository.VenueRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final VenueRepository venueRepository;

    @Override
    public void run(String... args) {
        if (venueRepository.count() == 0) {
            venueRepository.save(Venue.builder()
                    .name("Saha 1")
                    .location("Yeşil Vadi Halı Saha")
                    .pricePerHour(new BigDecimal("2000"))
                    .build());

            venueRepository.save(Venue.builder()
                    .name("Saha 2")
                    .location("Yeşil Vadi Halı Saha")
                    .pricePerHour(new BigDecimal("2000"))
                    .build());
        }
    }
}