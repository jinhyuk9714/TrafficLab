package com.trafficlab.repository;

import com.trafficlab.domain.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {
    List<ExperimentRun> findByExperimentIdOrderByIdDesc(Long experimentId);
}
