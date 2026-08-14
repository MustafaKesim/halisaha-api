package com.halisaha.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.halisaha.api.entity.OpenMatch;
import com.halisaha.api.entity.OpenMatchStatus;

import jakarta.persistence.LockModeType;

public interface OpenMatchRepository extends JpaRepository<OpenMatch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT om FROM OpenMatch om WHERE om.id = :id")
    Optional<OpenMatch> findByIdForUpdate(@Param("id") Long id);

    boolean existsByReservationId(Long reservationId);

    Optional<OpenMatch> findByReservationId(Long reservationId);

    List<OpenMatch> findByStatus(OpenMatchStatus status);
}
