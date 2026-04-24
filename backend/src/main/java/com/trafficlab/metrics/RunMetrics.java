package com.trafficlab.metrics;

public record RunMetrics(
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
}
