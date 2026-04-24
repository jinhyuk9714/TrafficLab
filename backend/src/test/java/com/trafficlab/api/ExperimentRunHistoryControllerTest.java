package com.trafficlab.api;

import com.trafficlab.TrafficLabApplication;
import com.trafficlab.domain.Experiment;
import com.trafficlab.domain.ExperimentRun;
import com.trafficlab.domain.ScenarioType;
import com.trafficlab.domain.StrategyType;
import com.trafficlab.repository.ExperimentRepository;
import com.trafficlab.repository.ExperimentRunRepository;
import com.trafficlab.repository.ReservationRepository;
import com.trafficlab.repository.RunEventRepository;
import com.trafficlab.repository.SeatRepository;
import com.trafficlab.reservation.DistributedLockClient;
import com.trafficlab.support.InMemoryDistributedLockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        TrafficLabApplication.class,
        ExperimentRunHistoryControllerTest.TestLockConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExperimentRunHistoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ExperimentRepository experimentRepository;

    @Autowired
    ExperimentRunRepository runRepository;

    @Autowired
    RunEventRepository runEventRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    SeatRepository seatRepository;

    @BeforeEach
    void setUp() {
        runEventRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        runRepository.deleteAll();
        experimentRepository.deleteAll();
    }

    @Test
    void listsRunsForExperimentInNewestFirstOrder() throws Exception {
        Experiment experiment = experimentRepository.save(Experiment.create(
                "Run history demo",
                ScenarioType.CONCERT_BOOKING,
                StrategyType.REDIS_LOCK,
                20,
                100,
                10,
                true,
                15
        ));
        ExperimentRun older = runRepository.save(completedRun(experiment.getId(), 100, 0, 0, 40));
        ExperimentRun newer = runRepository.save(completedRun(experiment.getId(), 100, 2, 1, 80));

        mockMvc.perform(get("/api/experiments/{experimentId}/runs", experiment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(newer.getId()))
                .andExpect(jsonPath("$[0].duplicateReservationCount").value(2))
                .andExpect(jsonPath("$[0].p95LatencyMs").value(80))
                .andExpect(jsonPath("$[1].id").value(older.getId()));
    }

    private ExperimentRun completedRun(Long experimentId, int totalRequests, int duplicateCount, int invariantCount, long p95LatencyMs) {
        ExperimentRun run = ExperimentRun.create(experimentId, totalRequests);
        run.markRunning();
        run.markCompleted(totalRequests, 10, totalRequests - 10, duplicateCount, invariantCount, 200.0, 10, p95LatencyMs, 120, 500);
        return run;
    }

    static class TestLockConfiguration {
        @Bean
        @Primary
        DistributedLockClient distributedLockClient() {
            return new InMemoryDistributedLockClient();
        }
    }
}
