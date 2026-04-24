package com.trafficlab.domain;

public enum StrategyType {
    UNSAFE,
    OPTIMISTIC_LOCK,
    PESSIMISTIC_LOCK,
    REDIS_LOCK
}
