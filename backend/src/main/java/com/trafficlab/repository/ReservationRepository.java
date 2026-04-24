package com.trafficlab.repository;

import com.trafficlab.domain.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByRunIdOrderByCreatedAtAsc(Long runId);

    Page<Reservation> findByRunIdOrderByCreatedAtAsc(Long runId, Pageable pageable);

    void deleteByRunId(Long runId);

    @Query("""
            select count(r)
            from Reservation r
            where r.runId = :runId
              and r.seatNumber = :seatNumber
              and r.success = true
            """)
    long countSuccessfulByRunAndSeat(@Param("runId") Long runId, @Param("seatNumber") int seatNumber);
}
