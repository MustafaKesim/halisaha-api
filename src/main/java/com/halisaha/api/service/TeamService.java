package com.halisaha.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.halisaha.api.entity.Team;
import com.halisaha.api.entity.User;
import com.halisaha.api.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    @Transactional
    public Team createTeam(String name, User captain) {
        Team team = Team.builder()
                .name(name)
                .captain(captain)
                .build();

        return teamRepository.save(team);
    }

    public List<Team> getMyTeams(User captain) {
        return teamRepository.findByCaptainId(captain.getId());
    }
}
