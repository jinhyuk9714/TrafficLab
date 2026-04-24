package com.trafficlab.experiment;

import com.trafficlab.domain.Scenario;
import com.trafficlab.domain.ScenarioType;
import com.trafficlab.repository.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScenarioSeeder {

    private final ScenarioRepository scenarioRepository;

    public ScenarioSeeder(ScenarioRepository scenarioRepository) {
        this.scenarioRepository = scenarioRepository;
    }

    @Transactional
    public void seedIfMissing() {
        scenarioRepository.findByType(ScenarioType.CONCERT_BOOKING)
                .orElseGet(() -> scenarioRepository.save(Scenario.create(
                        ScenarioType.CONCERT_BOOKING,
                        "Concert Booking",
                        "동시 접속자가 한정된 콘서트 좌석을 동시에 예약하는 실험입니다.",
                        true
                )));
    }
}
