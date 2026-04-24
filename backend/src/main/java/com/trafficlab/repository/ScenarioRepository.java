package com.trafficlab.repository;

import com.trafficlab.domain.Scenario;
import com.trafficlab.domain.ScenarioType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    Optional<Scenario> findByType(ScenarioType type);
}
