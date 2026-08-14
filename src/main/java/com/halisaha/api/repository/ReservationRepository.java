package com.halisaha.api.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.halisaha.api.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.venue.id = :venueId " +
           "AND r.date = :date " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    boolean existsConflict(@Param("venueId") Long venueId,
                            @Param("date") LocalDate date,
                            @Param("startTime") LocalTime startTime,
                            @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.user.id = :userId " +
           "AND r.date = :date " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    boolean existsUserConflict(@Param("userId") Long userId,
                                @Param("date") LocalDate date,
                                @Param("startTime") LocalTime startTime,
                                @Param("endTime") LocalTime endTime);

    List<Reservation> findByVenueIdAndDate(Long venueId, LocalDate date);

    List<Reservation> findByUserIdOrderByDateAscStartTimeAsc(Long userId);
}
