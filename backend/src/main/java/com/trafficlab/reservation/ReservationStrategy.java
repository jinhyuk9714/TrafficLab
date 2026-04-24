package com.trafficlab.reservation;

import com.trafficlab.domain.StrategyType;

public interface ReservationStrategy {
    StrategyType type();

    ReservationResult reserve(ReservationCommand command);
}
