package com.trafficlab.api;

import com.trafficlab.api.ApiDtos.ScenarioResponse;
import com.trafficlab.repository.ScenarioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioRepository scenarioRepository;

    public ScenarioController(ScenarioRepository scenarioRepository) {
        this.scenarioRepository = scenarioRepository;
    }

    @GetMapping
    public List<ScenarioResponse> list() {
        return scenarioRepository.findAll().stream()
                .map(ScenarioResponse::from)
                .toList();
    }
}
