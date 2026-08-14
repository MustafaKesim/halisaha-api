package com.halisaha.api.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.halisaha.api.dto.ReservationLinkInfo;
import com.halisaha.api.entity.Match;
import com.halisaha.api.entity.OpenChallenge;
import com.halisaha.api.entity.OpenMatch;
import com.halisaha.api.entity.Reservation;
import com.halisaha.api.entity.User;
import com.halisaha.api.entity.Venue;
import com.halisaha.api.exception.BadRequestException;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.ResourceNotFoundException;
import com.halisaha.api.repository.MatchRepository;
import com.halisaha.api.repository.OpenChallengeRepository;
import com.halisaha.api.repository.OpenMatchRepository;
import com.halisaha.api.repository.ReservationRepository;
import com.halisaha.api.repository.VenueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final List<Integer> OPERATING_HOURS = List.of(17, 18, 19, 20, 21, 22, 23, 0, 1);

    private final ReservationRepository reservationRepository;
    private final VenueRepository venueRepository;
    private final OpenMatchRepository openMatchRepository;
    private final OpenChallengeRepository openChallengeRepository;
    private final MatchRepository matchRepository;
    private final AvailabilityService availabilityService;

    // Saha gunu 17:00'de baslayip 02:00'de bitiyor; kullanicinin sectigi "is gunu"
    // tarihinde 00:00/01:00 saatleri aslinda bir sonraki takvim gunune ait.
    // Bu yuzden gercek tarih olarak +1 gun kaydediyoruz.
    private LocalDate resolveActualDate(LocalDate businessDayDate, LocalTime startTime) {
        return startTime.getHour() < 2 ? businessDayDate.plusDays(1) : businessDayDate;
    }

    @Transactional
    public Reservation createReservation(Long venueId, User user, LocalDate businessDayDate, LocalTime startTime,
            LocalTime endTime) {
        if (businessDayDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Geçmiş bir tarihe rezervasyon yapılamaz");
        }
        if (startTime.getMinute() != 0 || startTime.getSecond() != 0) {
            throw new BadRequestException("Rezervasyonlar sadece saat başı başlayabilir (örn. 17:00, 18:00)");
        }
        if (!endTime.equals(startTime.plusHours(1))) {
            throw new BadRequestException("Rezervasyonlar tam olarak 1 saat sürmelidir");
        }
        if (!OPERATING_HOURS.contains(startTime.getHour())) {
            throw new BadRequestException("Saha çalışma saatleri dışında rezervasyon yapılamaz (17:00 - 02:00 arası)");
        }

        LocalDate actualDate = resolveActualDate(businessDayDate, startTime);

        availabilityService.assertAvailable(user, actualDate, startTime, endTime);

        Venue venue = venueRepository.findByIdForUpdate(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Saha bulunamadı"));

        boolean conflict = reservationRepository.existsConflict(venueId, actualDate, startTime, endTime);

        if (conflict) {
            throw new ConflictException("Bu saha, seçilen tarih ve saat aralığında zaten dolu");
        }

        Reservation reservation = Reservation.builder()
                .venue(venue)
                .user(user)
                .date(actualDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        return reservationRepository.save(reservation);
    }

    public List<LocalTime> getAvailableSlots(Long venueId, LocalDate businessDayDate) {
        Set<LocalTime> takenStarts = new HashSet<>();

        reservationRepository.findByVenueIdAndDate(venueId, businessDayDate).stream()
                .map(Reservation::getStartTime)
                .filter(time -> time.getHour() >= 2)
                .forEach(takenStarts::add);

        reservationRepository.findByVenueIdAndDate(venueId, businessDayDate.plusDays(1)).stream()
                .map(Reservation::getStartTime)
                .filter(time -> time.getHour() < 2)
                .forEach(takenStarts::add);

        return OPERATING_HOURS.stream()
                .map(hour -> LocalTime.of(hour, 0))
                .filter(slot -> !takenStarts.contains(slot))
                .toList();
    }

    public List<Reservation> getMyReservations(User user) {
        return reservationRepository.findByUserIdOrderByDateAscStartTimeAsc(user.getId());
    }

    public ReservationLinkInfo getLinkInfo(Reservation reservation) {
        // Match, en kesin/nihai durumu temsil ediyor - once onu kontrol ediyoruz.
        var match = matchRepository.findByReservationId(reservation.getId());
        if (match.isPresent()) {
            Match m = match.get();
            String summary = m.getHomeTeam().getName() + " vs " + m.getAwayTeam().getName();
            return new ReservationLinkInfo("MATCH", null, summary);
        }

        var openChallenge = openChallengeRepository.findByReservationId(reservation.getId());
        if (openChallenge.isPresent()) {
            OpenChallenge c = openChallenge.get();
            String summary = c.getTeamSize() + "'e" + c.getTeamSize() + " rakip aranıyor (" + c.getTeam().getName() + ")";
            return new ReservationLinkInfo("OPEN_CHALLENGE", c.getStatus().name(), summary);
        }

        var openMatch = openMatchRepository.findByReservationId(reservation.getId());
        if (openMatch.isPresent()) {
            OpenMatch m = openMatch.get();
            String summary = "Eksik oyuncu: " + m.getAcceptedCount() + "/" + m.getMissingPlayerCount();
            return new ReservationLinkInfo("OPEN_MATCH", m.getStatus().name(), summary);
        }

        return ReservationLinkInfo.NONE;
    }
}
