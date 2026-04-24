package com.trafficlab.metrics;

import com.trafficlab.domain.Reservation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MetricCalculator {

    private MetricCalculator() {
    }

    public static RunMetrics calculate(List<Reservation> reservations, long elapsedMs) {
        int totalRequests = reservations.size();
        int successCount = (int) reservations.stream().filter(Reservation::isSuccess).count();
        int failureCount = totalRequests - successCount;

        Map<Integer, Long> successfulBySeat = reservations.stream()
                .filter(Reservation::isSuccess)
                .collect(Collectors.groupingBy(Reservation::getSeatNumber, Collectors.counting()));

        int duplicateReservationCount = successfulBySeat.values().stream()
                .filter(count -> count > 1)
                .mapToInt(count -> Math.toIntExact(count - 1))
                .sum();

        int invariantViolationCount = (int) successfulBySeat.values().stream()
                .filter(count -> count > 1)
                .count();

        List<Long> latencies = reservations.stream()
                .map(Reservation::getLatencyMs)
                .sorted(Comparator.naturalOrder())
                .toList();

        double elapsedSeconds = elapsedMs <= 0 ? 0.001 : elapsedMs / 1000.0;
        double throughput = totalRequests / elapsedSeconds;

        return new RunMetrics(
                totalRequests,
                successCount,
                failureCount,
                duplicateReservationCount,
                invariantViolationCount,
                throughput,
                percentile(latencies, 0.50),
                percentile(latencies, 0.95),
                percentile(latencies, 0.99),
                elapsedMs
        );
    }

    private static long percentile(List<Long> sortedLatencies, double percentile) {
        if (sortedLatencies.isEmpty()) {
            return 0;
        }
        int rank = (int) Math.ceil(percentile * sortedLatencies.size());
        int index = Math.min(Math.max(rank - 1, 0), sortedLatencies.size() - 1);
        return sortedLatencies.get(index);
    }
}
