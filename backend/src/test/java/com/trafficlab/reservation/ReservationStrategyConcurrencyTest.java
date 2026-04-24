package com.trafficlab.reservation;

import com.trafficlab.TrafficLabApplication;
import com.trafficlab.domain.Seat;
import com.trafficlab.domain.StrategyType;
import com.trafficlab.repository.ReservationRepository;
import com.trafficlab.repository.SeatRepository;
import com.trafficlab.support.InMemoryDistributedLockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        TrafficLabApplication.class,
        ReservationStrategyConcurrencyTest.TestLockConfiguration.class
})
@ActiveProfiles("test")
class ReservationStrategyConcurrencyTest {

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    ReservationStrategyRegistry registry;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
    }

    @Test
    void unsafeStrategyCanProduceDuplicateSuccessfulReservationsUnderContention() throws Exception {
        seatRepository.save(Seat.create(1L, 1));

        List<ReservationResult> results = runConcurrentAttempts(StrategyType.UNSAFE, 40, 40);
        results.forEach(result -> reservationRepository.save(result.toReservation(1L)));

        long successfulAttempts = reservationRepository.countSuccessfulByRunAndSeat(1L, 1);

        assertThat(successfulAttempts).isGreaterThan(1);
    }

    @Test
    void pessimisticLockPreventsDuplicateSuccessfulReservationsUnderContention() throws Exception {
        seatRepository.save(Seat.create(2L, 1));

        List<ReservationResult> results = runConcurrentAttempts(StrategyType.PESSIMISTIC_LOCK, 2L, 40, 25);
        results.forEach(result -> reservationRepository.save(result.toReservation(2L)));

        long successfulAttempts = reservationRepository.countSuccessfulByRunAndSeat(2L, 1);

        assertThat(successfulAttempts).isEqualTo(1);
    }

    @Test
    void redisLockStrategyPreventsDuplicateSuccessfulReservationsWithLockClientFallback() throws Exception {
        seatRepository.save(Seat.create(3L, 1));

        List<ReservationResult> results = runConcurrentAttempts(StrategyType.REDIS_LOCK, 3L, 40, 25);
        results.forEach(result -> reservationRepository.save(result.toReservation(3L)));

        long successfulAttempts = reservationRepository.countSuccessfulByRunAndSeat(3L, 1);

        assertThat(successfulAttempts).isEqualTo(1);
    }

    private List<ReservationResult> runConcurrentAttempts(StrategyType strategyType, int requestCount, int delayMs) throws Exception {
        return runConcurrentAttempts(strategyType, 1L, requestCount, delayMs);
    }

    private List<ReservationResult> runConcurrentAttempts(StrategyType strategyType, long runId, int requestCount, int delayMs) throws Exception {
        ReservationStrategy strategy = registry.get(strategyType);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(requestCount)) {
            var futures = IntStream.rangeClosed(1, requestCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        return strategy.reserve(new ReservationCommand(
                                runId,
                                "user-" + index,
                                1,
                                delayMs,
                                strategyType
                        ));
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get(10, TimeUnit.SECONDS);
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();
        }
    }

    static class TestLockConfiguration {
        @Bean
        @Primary
        DistributedLockClient distributedLockClient() {
            return new InMemoryDistributedLockClient();
        }
    }
}
