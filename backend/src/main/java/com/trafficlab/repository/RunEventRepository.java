package com.trafficlab.repository;

import com.trafficlab.domain.RunEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunEventRepository extends JpaRepository<RunEvent, Long> {
    List<RunEvent> findByRunIdOrderByCreatedAtAsc(Long runId);

    void deleteByRunId(Long runId);
}
