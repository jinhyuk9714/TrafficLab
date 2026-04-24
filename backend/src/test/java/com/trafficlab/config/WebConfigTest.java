package com.trafficlab.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    void usesConfiguredCorsAllowedOrigins() {
        WebConfig webConfig = new WebConfig(
                "http://localhost:3000, http://127.0.0.1:3000, https://trafficlab-web-jinhyuk9714.onrender.com"
        );
        InspectableCorsRegistry registry = new InspectableCorsRegistry();

        webConfig.addCorsMappings(registry);

        assertThat(registry.configurations().get("/api/**").getAllowedOrigins())
                .containsExactly(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "https://trafficlab-web-jinhyuk9714.onrender.com"
                );
    }

    private static class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }
}
