package com.halisaha.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.halisaha.api.entity.Match;
import com.halisaha.api.entity.User;
import com.halisaha.api.repository.MatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;

    public List<Match> getMyMatches(User user) {
        return matchRepository.findByCaptainId(user.getId());
    }
}
