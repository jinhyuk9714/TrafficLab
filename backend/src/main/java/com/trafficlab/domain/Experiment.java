package com.trafficlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "experiments")
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrategyType strategyType;

    @Column(nullable = false)
    private int concurrentUsers;

    @Column(nullable = false)
    private int totalRequests;

    @Column(nullable = false)
    private int targetSeatCount;

    @Column(nullable = false)
    private boolean hotspotMode;

    @Column(nullable = false)
    private int artificialDelayMs;

    @Column(nullable = false)
    private Instant createdAt;

    protected Experiment() {
    }

    private Experiment(
            String name,
            ScenarioType scenarioType,
            StrategyType strategyType,
            int concurrentUsers,
            int totalRequests,
            int targetSeatCount,
            boolean hotspotMode,
            int artificialDelayMs
    ) {
        this.name = name;
        this.scenarioType = scenarioType;
        this.strategyType = strategyType;
        this.concurrentUsers = concurrentUsers;
        this.totalRequests = totalRequests;
        this.targetSeatCount = targetSeatCount;
        this.hotspotMode = hotspotMode;
        this.artificialDelayMs = artificialDelayMs;
        this.createdAt = Instant.now();
    }

    public static Experiment create(
            String name,
            ScenarioType scenarioType,
            StrategyType strategyType,
            int concurrentUsers,
            int totalRequests,
            int targetSeatCount,
            boolean hotspotMode,
            int artificialDelayMs
    ) {
        return new Experiment(name, scenarioType, strategyType, concurrentUsers, totalRequests, targetSeatCount, hotspotMode, artificialDelayMs);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ScenarioType getScenarioType() {
        return scenarioType;
    }

    public StrategyType getStrategyType() {
        return strategyType;
    }

    public int getConcurrentUsers() {
        return concurrentUsers;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getTargetSeatCount() {
        return targetSeatCount;
    }

    public boolean isHotspotMode() {
        return hotspotMode;
    }

    public int getArtificialDelayMs() {
        return artificialDelayMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
