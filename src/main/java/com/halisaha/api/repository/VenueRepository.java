package com.halisaha.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.halisaha.api.entity.Venue;

import jakarta.persistence.LockModeType;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venue v WHERE v.id = :id")
    Optional<Venue> findByIdForUpdate(@Param("id") Long id);
}