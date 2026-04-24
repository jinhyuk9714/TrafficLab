package com.trafficlab.api;

import com.trafficlab.api.ApiDtos.ReservationPageResponse;
import com.trafficlab.api.ApiDtos.ReservationResponse;
import com.trafficlab.api.ApiDtos.RunResponse;
import com.trafficlab.domain.Experiment;
import com.trafficlab.domain.ExperimentRun;
import com.trafficlab.export.MarkdownExportService;
import com.trafficlab.experiment.ExperimentRunService;
import com.trafficlab.experiment.RunEventPublisher;
import com.trafficlab.repository.ExperimentRepository;
import com.trafficlab.repository.ReservationRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final ExperimentRunService runService;
    private final RunEventPublisher eventPublisher;
    private final ReservationRepository reservationRepository;
    private final ExperimentRepository experimentRepository;
    private final MarkdownExportService exportService;

    public RunController(
            ExperimentRunService runService,
            RunEventPublisher eventPublisher,
            ReservationRepository reservationRepository,
            ExperimentRepository experimentRepository,
            MarkdownExportService exportService
    ) {
        this.runService = runService;
        this.eventPublisher = eventPublisher;
        this.reservationRepository = reservationRepository;
        this.experimentRepository = experimentRepository;
        this.exportService = exportService;
    }

    @GetMapping("/{runId}")
    public RunResponse get(@PathVariable Long runId) {
        return RunResponse.from(runService.getRun(runId));
    }

    @GetMapping(path = "/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable Long runId) {
        runService.getRun(runId);
        return eventPublisher.stream(runId);
    }

    @GetMapping("/{runId}/reservations")
    public ReservationPageResponse reservations(
            @PathVariable Long runId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size
    ) {
        runService.getRun(runId);
        var reservationPage = reservationRepository.findByRunIdOrderByCreatedAtAsc(runId, PageRequest.of(page, size));
        return new ReservationPageResponse(
                reservationPage.getContent().stream().map(ReservationResponse::from).toList(),
                reservationPage.getNumber(),
                reservationPage.getSize(),
                reservationPage.getTotalElements(),
                reservationPage.getTotalPages()
        );
    }

    @GetMapping(path = "/{runId}/export", produces = "text/markdown; charset=UTF-8")
    public String export(@PathVariable Long runId) {
        ExperimentRun run = runService.getRun(runId);
        Experiment experiment = experimentRepository.findById(run.getExperimentId())
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found: " + run.getExperimentId()));
        return exportService.render(experiment, run);
    }
}
