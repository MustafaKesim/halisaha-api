package com.halisaha.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halisaha.api.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByCaptainId(Long captainId);
}
