package com.trafficlab.experiment;

import com.trafficlab.domain.Reservation;
import com.trafficlab.repository.ReservationRepository;
import com.trafficlab.reservation.ReservationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationRecorder {

    private final ReservationRepository reservationRepository;

    public ReservationRecorder(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation record(Long runId, ReservationResult result) {
        return reservationRepository.save(result.toReservation(runId));
    }
}
