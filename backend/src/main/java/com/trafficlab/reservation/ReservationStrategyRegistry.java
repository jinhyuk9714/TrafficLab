package com.trafficlab.reservation;

import com.trafficlab.domain.StrategyType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ReservationStrategyRegistry {

    private final Map<StrategyType, ReservationStrategy> strategies;

    public ReservationStrategyRegistry(List<ReservationStrategy> strategies) {
        this.strategies = new EnumMap<>(StrategyType.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.type(), strategy));
    }

    public ReservationStrategy get(StrategyType strategyType) {
        ReservationStrategy strategy = strategies.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported strategy: " + strategyType);
        }
        return strategy;
    }
}
