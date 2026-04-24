package com.trafficlab.reservation;

import com.trafficlab.domain.Reservation;
import com.trafficlab.domain.StrategyType;

public record ReservationResult(
        boolean success,
        String failureReason,
        long latencyMs,
        Long seatId,
        int seatNumber,
        String userKey,
        StrategyType strategyType
) {

    public static ReservationResult success(ReservationCommand command, Long seatId, long latencyMs) {
        return new ReservationResult(true, null, latencyMs, seatId, command.seatNumber(), command.userKey(), command.strategyType());
    }

    public static ReservationResult failure(ReservationCommand command, String failureReason, long latencyMs) {
        return new ReservationResult(false, failureReason, latencyMs, null, command.seatNumber(), command.userKey(), command.strategyType());
    }

    public Reservation toReservation(Long runId) {
        return Reservation.createAttempt(runId, seatId, seatNumber, userKey, strategyType, success, failureReason, latencyMs);
    }
}
