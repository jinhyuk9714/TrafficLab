package com.trafficlab.reservation;

import com.trafficlab.domain.Seat;
import com.trafficlab.domain.StrategyType;
import com.trafficlab.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.UUID;

@Service
public class RedisLockReservationStrategy implements ReservationStrategy {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final SeatRepository seatRepository;
    private final TransactionTemplate transactionTemplate;
    private final DistributedLockClient lockClient;

    public RedisLockReservationStrategy(
            SeatRepository seatRepository,
            TransactionTemplate transactionTemplate,
            DistributedLockClient lockClient
    ) {
        this.seatRepository = seatRepository;
        this.transactionTemplate = transactionTemplate;
        this.lockClient = lockClient;
    }

    @Override
    public StrategyType type() {
        return StrategyType.REDIS_LOCK;
    }

    @Override
    public ReservationResult reserve(ReservationCommand command) {
        long started = System.nanoTime();
        String key = "trafficlab:run:%d:seat:%d".formatted(command.runId(), command.seatNumber());
        String token = UUID.randomUUID().toString();

        if (!lockClient.tryLock(key, token, LOCK_TTL)) {
            return ReservationResult.failure(command, "LOCK_NOT_ACQUIRED", ReservationSupport.elapsedMillis(started));
        }

        try {
            return transactionTemplate.execute(status -> {
                Seat seat = seatRepository.findByRunIdAndSeatNumber(command.runId(), command.seatNumber())
                        .orElse(null);
                if (seat == null) {
                    return ReservationResult.failure(command, "SEAT_NOT_FOUND", ReservationSupport.elapsedMillis(started));
                }
                if (!seat.isAvailable()) {
                    return ReservationResult.failure(command, "SOLD_OUT", ReservationSupport.elapsedMillis(started));
                }

                ReservationSupport.artificialDelay(command.artificialDelayMs());
                seat.reserve();
                seatRepository.saveAndFlush(seat);
                return ReservationResult.success(command, seat.getId(), ReservationSupport.elapsedMillis(started));
            });
        } catch (RuntimeException exception) {
            return ReservationResult.failure(command, "ERROR: " + exception.getClass().getSimpleName(), ReservationSupport.elapsedMillis(started));
        } finally {
            lockClient.releaseIfOwner(key, token);
        }
    }
}
