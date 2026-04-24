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
@Table(name = "experiment_runs")
public class ExperimentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long experimentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    private Instant startedAt;
    private Instant finishedAt;

    @Column(nullable = false)
    private int totalRequests;

    @Column(nullable = false)
    private int successCount;

    @Column(nullable = false)
    private int failureCount;

    @Column(nullable = false)
    private int duplicateReservationCount;

    @Column(nullable = false)
    private int invariantViolationCount;

    @Column(nullable = false)
    private double throughput;

    @Column(nullable = false)
    private long p50LatencyMs;

    @Column(nullable = false)
    private long p95LatencyMs;

    @Column(nullable = false)
    private long p99LatencyMs;

    @Column(nullable = false)
    private long elapsedMs;

    @Column(length = 2000)
    private String errorMessage;

    protected ExperimentRun() {
    }

    private ExperimentRun(Long experimentId, int totalRequests) {
        this.experimentId = experimentId;
        this.status = RunStatus.PENDING;
        this.totalRequests = totalRequests;
    }

    public static ExperimentRun create(Long experimentId, int totalRequests) {
        return new ExperimentRun(experimentId, totalRequests);
    }

    public void markRunning() {
        this.status = RunStatus.RUNNING;
        this.startedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markCompleted(
            int totalRequests,
            int successCount,
            int failureCount,
            int duplicateReservationCount,
            int invariantViolationCount,
            double throughput,
            long p50LatencyMs,
            long p95LatencyMs,
            long p99LatencyMs,
            long elapsedMs
    ) {
        this.status = RunStatus.COMPLETED;
        this.finishedAt = Instant.now();
        this.totalRequests = totalRequests;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.duplicateReservationCount = duplicateReservationCount;
        this.invariantViolationCount = invariantViolationCount;
        this.throughput = throughput;
        this.p50LatencyMs = p50LatencyMs;
        this.p95LatencyMs = p95LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
        this.elapsedMs = elapsedMs;
    }

    public void markFailed(String errorMessage) {
        this.status = RunStatus.FAILED;
        this.finishedAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public RunStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getDuplicateReservationCount() {
        return duplicateReservationCount;
    }

    public int getInvariantViolationCount() {
        return invariantViolationCount;
    }

    public double getThroughput() {
        return throughput;
    }

    public long getP50LatencyMs() {
        return p50LatencyMs;
    }

    public long getP95LatencyMs() {
        return p95LatencyMs;
    }

    public long getP99LatencyMs() {
        return p99LatencyMs;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
