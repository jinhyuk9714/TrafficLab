package com.trafficlab.metrics;

import com.trafficlab.domain.Reservation;
import com.trafficlab.domain.StrategyType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricCalculatorTest {

    @Test
    void calculatesLatencyPercentilesAndDuplicateInvariantsFromRecordedReservations() {
        List<Reservation> reservations = List.of(
                success("A", 1, 10),
                success("B", 1, 20),
                success("C", 1, 30),
                success("D", 2, 40),
                failure("E", 2, 50),
                failure("F", 3, 60)
        );

        RunMetrics metrics = MetricCalculator.calculate(reservations, 1200);

        assertThat(metrics.totalRequests()).isEqualTo(6);
        assertThat(metrics.successCount()).isEqualTo(4);
        assertThat(metrics.failureCount()).isEqualTo(2);
        assertThat(metrics.duplicateReservationCount()).isEqualTo(2);
        assertThat(metrics.invariantViolationCount()).isEqualTo(1);
        assertThat(metrics.throughput()).isEqualTo(5.0);
        assertThat(metrics.p50LatencyMs()).isEqualTo(30);
        assertThat(metrics.p95LatencyMs()).isEqualTo(60);
        assertThat(metrics.p99LatencyMs()).isEqualTo(60);
    }

    private static Reservation success(String userKey, int seatNumber, long latencyMs) {
        return Reservation.createAttempt(1L, 1L, seatNumber, userKey, StrategyType.UNSAFE, true, null, latencyMs);
    }

    private static Reservation failure(String userKey, int seatNumber, long latencyMs) {
        return Reservation.createAttempt(1L, null, seatNumber, userKey, StrategyType.UNSAFE, false, "SOLD_OUT", latencyMs);
    }
}
