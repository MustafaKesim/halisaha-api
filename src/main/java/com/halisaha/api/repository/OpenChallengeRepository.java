package com.halisaha.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.halisaha.api.entity.OpenChallenge;
import com.halisaha.api.entity.OpenChallengeStatus;

import jakarta.persistence.LockModeType;

public interface OpenChallengeRepository extends JpaRepository<OpenChallenge, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oc FROM OpenChallenge oc WHERE oc.id = :id")
    Optional<OpenChallenge> findByIdForUpdate(@Param("id") Long id);

    boolean existsByReservationId(Long reservationId);

    Optional<OpenChallenge> findByReservationId(Long reservationId);

    List<OpenChallenge> findByStatus(OpenChallengeStatus status);
}
