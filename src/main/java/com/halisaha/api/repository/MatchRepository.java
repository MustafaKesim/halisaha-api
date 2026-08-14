package com.halisaha.api.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.halisaha.api.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("SELECT m FROM Match m WHERE m.homeTeam.captain.id = :userId OR m.awayTeam.captain.id = :userId")
    List<Match> findByCaptainId(@Param("userId") Long userId);

    Optional<Match> findByReservationId(Long reservationId);

    @Query("SELECT COUNT(m) > 0 FROM Match m " +
           "WHERE (m.homeTeam.captain.id = :userId OR m.awayTeam.captain.id = :userId) " +
           "AND m.reservation.date = :date " +
           "AND m.reservation.startTime < :endTime " +
           "AND m.reservation.endTime > :startTime")
    boolean existsCaptainConflict(@Param("userId") Long userId,
                                   @Param("date") LocalDate date,
                                   @Param("startTime") LocalTime startTime,
                                   @Param("endTime") LocalTime endTime);
}
