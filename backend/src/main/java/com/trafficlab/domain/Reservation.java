package com.trafficlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(name = "idx_reservations_run", columnList = "runId"),
                @Index(name = "idx_reservations_run_seat_success", columnList = "runId, seatNumber, success")
        }
)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long runId;

    private Long seatId;

    @Column(nullable = false)
    private int seatNumber;

    @Column(nullable = false)
    private String userKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrategyType strategyType;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private long latencyMs;

    @Column(nullable = false)
    private Instant createdAt;

    protected Reservation() {
    }

    private Reservation(
            Long runId,
            Long seatId,
            int seatNumber,
            String userKey,
            StrategyType strategyType,
            boolean success,
            String failureReason,
            long latencyMs
    ) {
        this.runId = runId;
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.userKey = userKey;
        this.strategyType = strategyType;
        this.success = success;
        this.failureReason = failureReason;
        this.latencyMs = latencyMs;
        this.createdAt = Instant.now();
    }

    public static Reservation createAttempt(
            Long runId,
            Long seatId,
            int seatNumber,
            String userKey,
            StrategyType strategyType,
            boolean success,
            String failureReason,
            long latencyMs
    ) {
        return new Reservation(runId, seatId, seatNumber, userKey, strategyType, success, failureReason, latencyMs);
    }

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getUserKey() {
        return userKey;
    }

    public StrategyType getStrategyType() {
        return strategyType;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
