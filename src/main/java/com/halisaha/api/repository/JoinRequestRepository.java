package com.halisaha.api.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.halisaha.api.entity.JoinRequest;
import com.halisaha.api.entity.JoinRequestStatus;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    boolean existsByOpenMatchIdAndApplicantId(Long openMatchId, Long applicantId);

    List<JoinRequest> findByOpenMatchId(Long openMatchId);

    long countByOpenMatchIdAndStatus(Long openMatchId, JoinRequestStatus status);

    List<JoinRequest> findByApplicantId(Long applicantId);

    @Query("SELECT COUNT(jr) > 0 FROM JoinRequest jr " +
           "WHERE jr.applicant.id = :userId " +
           "AND jr.status = com.halisaha.api.entity.JoinRequestStatus.ACCEPTED " +
           "AND jr.openMatch.reservation.date = :date " +
           "AND jr.openMatch.reservation.startTime < :endTime " +
           "AND jr.openMatch.reservation.endTime > :startTime")
    boolean existsAcceptedConflict(@Param("userId") Long userId,
                                    @Param("date") LocalDate date,
                                    @Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime);
}
