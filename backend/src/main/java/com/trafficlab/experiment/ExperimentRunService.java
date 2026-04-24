package com.trafficlab.experiment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trafficlab.domain.Experiment;
import com.trafficlab.domain.ExperimentRun;
import com.trafficlab.domain.Reservation;
import com.trafficlab.domain.RunEventType;
import com.trafficlab.domain.Seat;
import com.trafficlab.metrics.MetricCalculator;
import com.trafficlab.metrics.RunMetrics;
import com.trafficlab.repository.ExperimentRepository;
import com.trafficlab.repository.ExperimentRunRepository;
import com.trafficlab.repository.ReservationRepository;
import com.trafficlab.repository.SeatRepository;
import com.trafficlab.reservation.ReservationCommand;
import com.trafficlab.reservation.ReservationResult;
import com.trafficlab.reservation.ReservationStrategy;
import com.trafficlab.reservation.ReservationStrategyRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExperimentRunService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentRunRepository runRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationStrategyRegistry strategyRegistry;
    private final ReservationRecorder reservationRecorder;
    private final RunEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public ExperimentRunService(
            ExperimentRepository experimentRepository,
            ExperimentRunRepository runRepository,
            SeatRepository seatRepository,
            ReservationRepository reservationRepository,
            ReservationStrategyRegistry strategyRegistry,
            ReservationRecorder reservationRecorder,
            RunEventPublisher eventPublisher,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper
    ) {
        this.experimentRepository = experimentRepository;
        this.runRepository = runRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.strategyRegistry = strategyRegistry;
        this.reservationRecorder = reservationRecorder;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    public ExperimentRun startRun(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found: " + experimentId));
        ExperimentRun run = transactionTemplate.execute(status -> runRepository.save(ExperimentRun.create(experiment.getId(), experiment.getTotalRequests())));
        if (run == null) {
            throw new IllegalStateException("Failed to create experiment run");
        }

        Thread runner = Thread.ofVirtual().name("trafficlab-run-" + run.getId()).start(() -> executeRun(run.getId()));
        runner.setUncaughtExceptionHandler((thread, throwable) -> markFailed(run.getId(), throwable));
        return run;
    }

    public ExperimentRun getRun(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
    }

    private void executeRun(Long runId) {
        long started = System.nanoTime();
        try {
            ExperimentRun run = markRunning(runId);
            Experiment experiment = experimentRepository.findById(run.getExperimentId())
                    .orElseThrow(() -> new IllegalArgumentException("Experiment not found: " + run.getExperimentId()));

            eventPublisher.emit(runId, RunEventType.RUN_STARTED, "Experiment run started with strategy=" + experiment.getStrategyType());
            seedSeats(runId, experiment.getTargetSeatCount());
            eventPublisher.emit(runId, RunEventType.SEATS_SEEDED, "Seeded " + experiment.getTargetSeatCount() + " concert seats for this run.");

            executeLoad(runId, experiment, started);

            List<Reservation> reservations = reservationRepository.findByRunIdOrderByCreatedAtAsc(runId);
            long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            RunMetrics metrics = MetricCalculator.calculate(reservations, elapsedMs);
            complete(runId, metrics);
            eventPublisher.emit(runId, RunEventType.RUN_COMPLETED, progressMessage(metrics.totalRequests(), metrics.totalRequests(), metrics.successCount(), metrics.failureCount(), metrics.throughput()));
        } catch (Exception exception) {
            markFailed(runId, exception);
        }
    }

    private ExperimentRun markRunning(Long runId) {
        return transactionTemplate.execute(status -> {
            ExperimentRun run = getRun(runId);
            run.markRunning();
            return run;
        });
    }

    private void seedSeats(Long runId, int targetSeatCount) {
        transactionTemplate.executeWithoutResult(status -> {
            List<Seat> seats = new ArrayList<>();
            for (int seatNumber = 1; seatNumber <= targetSeatCount; seatNumber++) {
                seats.add(Seat.create(runId, seatNumber));
            }
            seatRepository.saveAll(seats);
        });
    }

    private void executeLoad(Long runId, Experiment experiment, long startedNanos) throws Exception {
        int totalRequests = experiment.getTotalRequests();
        int poolSize = Math.max(1, Math.min(experiment.getConcurrentUsers(), totalRequests));
        ReservationStrategy strategy = strategyRegistry.get(experiment.getStrategyType());
        AtomicInteger completed = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        int progressStep = Math.max(1, totalRequests / 20);

        try (var executor = Executors.newFixedThreadPool(poolSize)) {
            ExecutorCompletionService<ReservationResult> completionService = new ExecutorCompletionService<>(executor);
            for (int requestNumber = 1; requestNumber <= totalRequests; requestNumber++) {
                int seatNumber = selectSeatNumber(experiment, requestNumber);
                String userKey = "user-%05d".formatted(requestNumber);
                ReservationCommand command = new ReservationCommand(
                        runId,
                        userKey,
                        seatNumber,
                        experiment.getArtificialDelayMs(),
                        experiment.getStrategyType()
                );
                completionService.submit(() -> strategy.reserve(command));
            }

            for (int i = 0; i < totalRequests; i++) {
                ReservationResult result = completionService.take().get();
                reservationRecorder.record(runId, result);
                int completedCount = completed.incrementAndGet();
                if (result.success()) {
                    success.incrementAndGet();
                } else {
                    failure.incrementAndGet();
                }

                if (completedCount == totalRequests || completedCount % progressStep == 0) {
                    double elapsedSeconds = Math.max(0.001, Duration.ofNanos(System.nanoTime() - startedNanos).toMillis() / 1000.0);
                    double throughput = completedCount / elapsedSeconds;
                    eventPublisher.emit(runId, RunEventType.PROGRESS, progressMessage(completedCount, totalRequests, success.get(), failure.get(), throughput));
                }
            }
        }
    }

    private int selectSeatNumber(Experiment experiment, int requestNumber) {
        int targetSeatCount = experiment.getTargetSeatCount();
        if (targetSeatCount <= 1) {
            return 1;
        }
        if (!experiment.isHotspotMode()) {
            return ((requestNumber - 1) % targetSeatCount) + 1;
        }

        int hotspotSize = Math.max(1, Math.min(3, targetSeatCount));
        if (ThreadLocalRandom.current().nextDouble() < 0.85) {
            return ThreadLocalRandom.current().nextInt(1, hotspotSize + 1);
        }
        return ThreadLocalRandom.current().nextInt(1, targetSeatCount + 1);
    }

    private void complete(Long runId, RunMetrics metrics) {
        transactionTemplate.executeWithoutResult(status -> {
            ExperimentRun run = getRun(runId);
            run.markCompleted(
                    metrics.totalRequests(),
                    metrics.successCount(),
                    metrics.failureCount(),
                    metrics.duplicateReservationCount(),
                    metrics.invariantViolationCount(),
                    metrics.throughput(),
                    metrics.p50LatencyMs(),
                    metrics.p95LatencyMs(),
                    metrics.p99LatencyMs(),
                    metrics.elapsedMs()
            );
        });
    }

    private void markFailed(Long runId, Throwable throwable) {
        transactionTemplate.executeWithoutResult(status -> {
            ExperimentRun run = getRun(runId);
            run.markFailed(throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage());
        });
        eventPublisher.emit(runId, RunEventType.RUN_FAILED, "Run failed: " + throwable.getClass().getSimpleName());
    }

    private String progressMessage(int completed, int total, int success, int failure, double throughput) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "completedRequests", completed,
                    "totalRequests", total,
                    "successCount", success,
                    "failureCount", failure,
                    "throughput", throughput
            ));
        } catch (JsonProcessingException exception) {
            return "completed=%d,total=%d,success=%d,failure=%d,throughput=%.2f".formatted(completed, total, success, failure, throughput);
        }
    }
}
