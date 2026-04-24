package com.trafficlab.repository;

import com.trafficlab.domain.Seat;
import com.trafficlab.domain.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    Optional<Seat> findByRunIdAndSeatNumber(Long runId, int seatNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.runId = :runId and s.seatNumber = :seatNumber")
    Optional<Seat> findByRunIdAndSeatNumberForUpdate(@Param("runId") Long runId, @Param("seatNumber") int seatNumber);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seat s set s.status = :status where s.runId = :runId and s.seatNumber = :seatNumber")
    int markStatusUnsafe(@Param("runId") Long runId, @Param("seatNumber") int seatNumber, @Param("status") SeatStatus status);

    void deleteByRunId(Long runId);
}
