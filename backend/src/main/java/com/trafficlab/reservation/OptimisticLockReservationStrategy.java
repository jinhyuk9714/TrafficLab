package com.trafficlab.reservation;

import com.trafficlab.domain.Seat;
import com.trafficlab.domain.StrategyType;
import com.trafficlab.repository.SeatRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OptimisticLockReservationStrategy implements ReservationStrategy {

    private final SeatRepository seatRepository;
    private final TransactionTemplate transactionTemplate;

    public OptimisticLockReservationStrategy(SeatRepository seatRepository, TransactionTemplate transactionTemplate) {
        this.seatRepository = seatRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public StrategyType type() {
        return StrategyType.OPTIMISTIC_LOCK;
    }

    @Override
    public ReservationResult reserve(ReservationCommand command) {
        long started = System.nanoTime();
        try {
            return transactionTemplate.execute(status -> {
                try {
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
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException exception) {
                    status.setRollbackOnly();
                    return ReservationResult.failure(command, "OPTIMISTIC_CONFLICT", ReservationSupport.elapsedMillis(started));
                }
            });
        } catch (RuntimeException exception) {
            return ReservationResult.failure(command, "ERROR: " + exception.getClass().getSimpleName(), ReservationSupport.elapsedMillis(started));
        }
    }
}
