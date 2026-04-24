package com.trafficlab.experiment;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final ScenarioSeeder scenarioSeeder;

    public DataInitializer(ScenarioSeeder scenarioSeeder) {
        this.scenarioSeeder = scenarioSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        scenarioSeeder.seedIfMissing();
    }
}
