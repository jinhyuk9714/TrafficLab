package com.trafficlab.api;

import com.trafficlab.experiment.LabResetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final LabResetService resetService;

    public LabController(LabResetService resetService) {
        this.resetService = resetService;
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> reset() {
        resetService.reset();
        return Map.of("status", "reset");
    }
}
