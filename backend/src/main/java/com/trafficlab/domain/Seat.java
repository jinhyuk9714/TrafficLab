package com.trafficlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(
        name = "seats",
        indexes = {
                @Index(name = "idx_seats_run_seat", columnList = "runId, seatNumber")
        }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long runId;

    @Column(nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant createdAt;

    protected Seat() {
    }

    private Seat(Long runId, int seatNumber) {
        this.runId = runId;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
        this.createdAt = Instant.now();
    }

    public static Seat create(Long runId, int seatNumber) {
        return new Seat(runId, seatNumber);
    }

    public void reserve() {
        this.status = SeatStatus.RESERVED;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
