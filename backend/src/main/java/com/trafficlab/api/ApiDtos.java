package com.trafficlab.api;

import com.trafficlab.domain.Experiment;
import com.trafficlab.domain.ExperimentRun;
import com.trafficlab.domain.Reservation;
import com.trafficlab.domain.RunEvent;
import com.trafficlab.domain.RunStatus;
import com.trafficlab.domain.Scenario;
import com.trafficlab.domain.ScenarioType;
import com.trafficlab.domain.StrategyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record ScenarioResponse(
            Long id,
            ScenarioType type,
            String name,
            String description,
            boolean enabled
    ) {
        public static ScenarioResponse from(Scenario scenario) {
            return new ScenarioResponse(
                    scenario.getId(),
                    scenario.getType(),
                    scenario.getName(),
                    scenario.getDescription(),
                    scenario.isEnabled()
            );
        }
    }

    public record ExperimentCreateRequest(
            @NotBlank String name,
            @NotNull ScenarioType scenarioType,
            @NotNull StrategyType strategyType,
            @Min(1) @Max(500) int concurrentUsers,
            @Min(1) @Max(10000) int totalRequests,
            @Min(1) @Max(1000) int targetSeatCount,
            boolean hotspotMode,
            @Min(0) @Max(5000) int artificialDelayMs
    ) {
    }

    public record ExperimentResponse(
            Long id,
            String name,
            ScenarioType scenarioType,
            StrategyType strategyType,
            int concurrentUsers,
            int totalRequests,
            int targetSeatCount,
            boolean hotspotMode,
            int artificialDelayMs,
            Instant createdAt
    ) {
        public static ExperimentResponse from(Experiment experiment) {
            return new ExperimentResponse(
                    experiment.getId(),
                    experiment.getName(),
                    experiment.getScenarioType(),
                    experiment.getStrategyType(),
                    experiment.getConcurrentUsers(),
                    experiment.getTotalRequests(),
                    experiment.getTargetSeatCount(),
                    experiment.isHotspotMode(),
                    experiment.getArtificialDelayMs(),
                    experiment.getCreatedAt()
            );
        }
    }

    public record StartRunResponse(Long runId, RunStatus status) {
    }

    public record RunResponse(
            Long id,
            Long experimentId,
            RunStatus status,
            Instant startedAt,
            Instant finishedAt,
            int totalRequests,
            int successCount,
            int failureCount,
            int duplicateReservationCount,
            int invariantViolationCount,
            double throughput,
            long p50LatencyMs,
            long p95LatencyMs,
            long p99LatencyMs,
            long elapsedMs,
            String errorMessage
    ) {
        public static RunResponse from(ExperimentRun run) {
            return new RunResponse(
                    run.getId(),
                    run.getExperimentId(),
                    run.getStatus(),
                    run.getStartedAt(),
                    run.getFinishedAt(),
                    run.getTotalRequests(),
                    run.getSuccessCount(),
                    run.getFailureCount(),
                    run.getDuplicateReservationCount(),
                    run.getInvariantViolationCount(),
                    run.getThroughput(),
                    run.getP50LatencyMs(),
                    run.getP95LatencyMs(),
                    run.getP99LatencyMs(),
                    run.getElapsedMs(),
                    run.getErrorMessage()
            );
        }
    }

    public record RunEventResponse(
            Long id,
            Long runId,
            String type,
            String message,
            Instant createdAt
    ) {
        public static RunEventResponse from(RunEvent event) {
            return new RunEventResponse(
                    event.getId(),
                    event.getRunId(),
                    event.getType().name(),
                    event.getMessage(),
                    event.getCreatedAt()
            );
        }
    }

    public record ReservationResponse(
            Long id,
            Long runId,
            Long seatId,
            int seatNumber,
            String userKey,
            StrategyType strategyType,
            boolean success,
            String failureReason,
            long latencyMs,
            Instant createdAt
    ) {
        public static ReservationResponse from(Reservation reservation) {
            return new ReservationResponse(
                    reservation.getId(),
                    reservation.getRunId(),
                    reservation.getSeatId(),
                    reservation.getSeatNumber(),
                    reservation.getUserKey(),
                    reservation.getStrategyType(),
                    reservation.isSuccess(),
                    reservation.getFailureReason(),
                    reservation.getLatencyMs(),
                    reservation.getCreatedAt()
            );
        }
    }

    public record ReservationPageResponse(
            List<ReservationResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
