package com.trafficlab.experiment;

import com.trafficlab.repository.ExperimentRepository;
import com.trafficlab.repository.ExperimentRunRepository;
import com.trafficlab.repository.ReservationRepository;
import com.trafficlab.repository.RunEventRepository;
import com.trafficlab.repository.ScenarioRepository;
import com.trafficlab.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabResetService {

    private final RunEventRepository runEventRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ExperimentRunRepository runRepository;
    private final ExperimentRepository experimentRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioSeeder scenarioSeeder;

    public LabResetService(
            RunEventRepository runEventRepository,
            ReservationRepository reservationRepository,
            SeatRepository seatRepository,
            ExperimentRunRepository runRepository,
            ExperimentRepository experimentRepository,
            ScenarioRepository scenarioRepository,
            ScenarioSeeder scenarioSeeder
    ) {
        this.runEventRepository = runEventRepository;
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.runRepository = runRepository;
        this.experimentRepository = experimentRepository;
        this.scenarioRepository = scenarioRepository;
        this.scenarioSeeder = scenarioSeeder;
    }

    @Transactional
    public void reset() {
        runEventRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        runRepository.deleteAll();
        experimentRepository.deleteAll();
        scenarioRepository.deleteAll();
        scenarioSeeder.seedIfMissing();
    }
}
