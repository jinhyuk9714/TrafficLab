package com.trafficlab.export;

import com.trafficlab.domain.Experiment;
import com.trafficlab.domain.ExperimentRun;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class MarkdownExportService {

    public String render(Experiment experiment, ExperimentRun run) {
        String interpretation = run.getDuplicateReservationCount() > 0
                ? "This run recorded duplicate successful reservations, which means the booking invariant was violated under concurrent pressure."
                : "This run preserved the booking invariant: each seat had at most one successful reservation.";

        return """
                # TrafficLab Case Study

                ## Experiment

                - name: %s
                - scenarioType: %s
                - strategyType: %s
                - concurrentUsers: %d
                - totalRequests: %d
                - targetSeatCount: %d
                - hotspotMode: %s
                - artificialDelayMs: %d

                ## Measured Results

                - runId: %d
                - status: %s
                - totalRequests: %d
                - successCount: %d
                - failureCount: %d
                - duplicateReservationCount: %d
                - invariantViolationCount: %d
                - throughput: %s requests/sec
                - elapsedMs: %d

                ## Latency Summary

                - p50LatencyMs: %d
                - p95LatencyMs: %d
                - p99LatencyMs: %d

                ## Interpretation

                %s

                The results were computed from persisted reservation attempts for this run. No metric in this case study is generated or mocked.

                ## Limitations

                - The MVP models a single concert booking scenario.
                - Payment, authentication, queueing, and external traffic generation are intentionally out of scope.
                - Local machine CPU, database settings, Redis latency, and Docker resource limits can materially affect measurements.
                """.formatted(
                experiment.getName(),
                experiment.getScenarioType(),
                experiment.getStrategyType(),
                experiment.getConcurrentUsers(),
                experiment.getTotalRequests(),
                experiment.getTargetSeatCount(),
                experiment.isHotspotMode(),
                experiment.getArtificialDelayMs(),
                run.getId(),
                run.getStatus(),
                run.getTotalRequests(),
                run.getSuccessCount(),
                run.getFailureCount(),
                run.getDuplicateReservationCount(),
                run.getInvariantViolationCount(),
                String.format(Locale.US, "%.2f", run.getThroughput()),
                run.getElapsedMs(),
                run.getP50LatencyMs(),
                run.getP95LatencyMs(),
                run.getP99LatencyMs(),
                interpretation
        );
    }
}
