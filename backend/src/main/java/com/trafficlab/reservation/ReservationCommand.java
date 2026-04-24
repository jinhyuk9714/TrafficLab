package com.trafficlab.reservation;

import com.trafficlab.domain.StrategyType;

public record ReservationCommand(
        Long runId,
        String userKey,
        int seatNumber,
        int artificialDelayMs,
        StrategyType strategyType
) {
}
