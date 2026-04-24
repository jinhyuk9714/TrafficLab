package com.trafficlab.api;

import com.trafficlab.api.ApiDtos.ExperimentCreateRequest;
import com.trafficlab.api.ApiDtos.ExperimentResponse;
import com.trafficlab.api.ApiDtos.StartRunResponse;
import com.trafficlab.domain.ExperimentRun;
import com.trafficlab.experiment.ExperimentRunService;
import com.trafficlab.experiment.ExperimentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;
    private final ExperimentRunService runService;

    public ExperimentController(ExperimentService experimentService, ExperimentRunService runService) {
        this.experimentService = experimentService;
        this.runService = runService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExperimentResponse create(@Valid @RequestBody ExperimentCreateRequest request) {
        return ExperimentResponse.from(experimentService.create(request));
    }

    @GetMapping
    public List<ExperimentResponse> list() {
        return experimentService.list().stream()
                .map(ExperimentResponse::from)
                .toList();
    }

    @GetMapping("/{experimentId}")
    public ExperimentResponse get(@PathVariable Long experimentId) {
        return ExperimentResponse.from(experimentService.get(experimentId));
    }

    @PostMapping("/{experimentId}/runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public StartRunResponse startRun(@PathVariable Long experimentId) {
        ExperimentRun run = runService.startRun(experimentId);
        return new StartRunResponse(run.getId(), run.getStatus());
    }
}
