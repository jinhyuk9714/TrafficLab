package com.trafficlab.export;

import com.trafficlab.domain.Experiment;
import com.trafficlab.domain.ExperimentRun;
import com.trafficlab.domain.ScenarioType;
import com.trafficlab.domain.StrategyType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownExportServiceTest {

    @Test
    void includesMeasuredSettingsResultsAndInterpretation() {
        Experiment experiment = Experiment.create(
                "Hot seat rush - Redis Lock",
                ScenarioType.CONCERT_BOOKING,
                StrategyType.REDIS_LOCK,
                100,
                1000,
                10,
                true,
                20
        );
        ExperimentRun run = ExperimentRun.create(7L, 1000);
        run.markCompleted(1000, 10, 990, 0, 0, 512.2, 8, 31, 44, 1952);

        String markdown = new MarkdownExportService().render(experiment, run);

        assertThat(markdown).contains("# TrafficLab Case Study");
        assertThat(markdown).contains("Hot seat rush - Redis Lock");
        assertThat(markdown).contains("REDIS_LOCK");
        assertThat(markdown).contains("totalRequests: 1000");
        assertThat(markdown).contains("duplicateReservationCount: 0");
        assertThat(markdown).contains("p95LatencyMs: 31");
        assertThat(markdown).contains("## Interpretation");
        assertThat(markdown).contains("## Limitations");
    }
}
