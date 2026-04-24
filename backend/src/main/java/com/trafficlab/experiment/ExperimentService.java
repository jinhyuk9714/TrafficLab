package com.trafficlab.experiment;

import com.trafficlab.api.ApiDtos.ExperimentCreateRequest;
import com.trafficlab.domain.Experiment;
import com.trafficlab.domain.ScenarioType;
import com.trafficlab.repository.ExperimentRepository;
import com.trafficlab.repository.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final ScenarioRepository scenarioRepository;

    public ExperimentService(ExperimentRepository experimentRepository, ScenarioRepository scenarioRepository) {
        this.experimentRepository = experimentRepository;
        this.scenarioRepository = scenarioRepository;
    }

    @Transactional
    public Experiment create(ExperimentCreateRequest request) {
        if (request.scenarioType() != ScenarioType.CONCERT_BOOKING) {
            throw new IllegalArgumentException("Only CONCERT_BOOKING is enabled in the MVP.");
        }
        scenarioRepository.findByType(request.scenarioType())
                .filter(scenario -> scenario.isEnabled())
                .orElseThrow(() -> new IllegalArgumentException("Scenario is disabled: " + request.scenarioType()));

        Experiment experiment = Experiment.create(
                request.name(),
                request.scenarioType(),
                request.strategyType(),
                request.concurrentUsers(),
                request.totalRequests(),
                request.targetSeatCount(),
                request.hotspotMode(),
                request.artificialDelayMs()
        );
        return experimentRepository.save(experiment);
    }

    @Transactional(readOnly = true)
    public List<Experiment> list() {
        return experimentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Experiment get(Long experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found: " + experimentId));
    }
}
