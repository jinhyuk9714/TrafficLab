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

import java.time.Instant;

@Entity
@Table(
        name = "run_events",
        indexes = @Index(name = "idx_run_events_run", columnList = "runId")
)
public class RunEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunEventType type;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    protected RunEvent() {
    }

    private RunEvent(Long runId, RunEventType type, String message) {
        this.runId = runId;
        this.type = type;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public static RunEvent create(Long runId, RunEventType type, String message) {
        return new RunEvent(runId, type, message);
    }

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }

    public RunEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
