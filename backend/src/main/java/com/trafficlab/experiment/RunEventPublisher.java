package com.trafficlab.experiment;

import com.trafficlab.api.ApiDtos.RunEventResponse;
import com.trafficlab.domain.RunEvent;
import com.trafficlab.domain.RunEventType;
import com.trafficlab.repository.RunEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RunEventPublisher {

    private final RunEventRepository runEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public RunEventPublisher(RunEventRepository runEventRepository, TransactionTemplate transactionTemplate) {
        this.runEventRepository = runEventRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public SseEmitter stream(Long runId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(runId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(runId, emitter));
        emitter.onTimeout(() -> remove(runId, emitter));
        emitter.onError(ignored -> remove(runId, emitter));

        List<RunEvent> existingEvents = runEventRepository.findByRunIdOrderByCreatedAtAsc(runId);
        existingEvents.forEach(event -> send(emitter, event));
        return emitter;
    }

    public RunEvent emit(Long runId, RunEventType type, String message) {
        RunEvent event = transactionTemplate.execute(status -> runEventRepository.save(RunEvent.create(runId, type, message)));
        if (event != null) {
            emitters.getOrDefault(runId, new CopyOnWriteArrayList<>())
                    .forEach(emitter -> send(emitter, event));
        }
        return event;
    }

    private void send(SseEmitter emitter, RunEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType().name())
                    .id(String.valueOf(event.getId()))
                    .data(RunEventResponse.from(event)));
        } catch (IOException | IllegalStateException exception) {
            remove(event.getRunId(), emitter);
        }
    }

    private void remove(Long runId, SseEmitter emitter) {
        List<SseEmitter> runEmitters = emitters.get(runId);
        if (runEmitters != null) {
            runEmitters.remove(emitter);
        }
    }
}
