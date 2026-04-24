package com.trafficlab.reservation;

final class ReservationSupport {

    private ReservationSupport() {
    }

    static long elapsedMillis(long startedNanos) {
        return Math.max(1, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    static void artificialDelay(int artificialDelayMs) {
        if (artificialDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(artificialDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reservation attempt interrupted", exception);
        }
    }
}
