package com.halisaha.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.halisaha.api.entity.Venue;
import com.halisaha.api.repository.VenueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    public List<Venue> getAllVenues() {
        return venueRepository.findAll();
    }
}
