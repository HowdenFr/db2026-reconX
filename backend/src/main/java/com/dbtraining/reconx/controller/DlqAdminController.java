package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/dlq")
@PreAuthorize("hasRole('ADMIN')")
public class DlqAdminController {

    private final DlqMessageRepository repo;
    private final TradeEventProducer producer;

    public DlqAdminController(DlqMessageRepository repo, TradeEventProducer producer) {
        this.repo = repo;
        this.producer = producer;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAllByOrderByFirstSeenAsc().stream()
                .map(message -> Map.<String, Object>of(
                        "eventId", message.getEventId(),
                        "tradeRef", message.getTradeRef(),
                        "originalTopic", message.getOriginalTopic(),
                        "partition", message.getPartition(),
                        "offset", message.getOffset(),
                        "reason", message.getReason(),
                        "firstSeen", message.getFirstSeen(),
                        "payload", message.getPayload()
                ))
                .toList();
    }

    @PostMapping("/replay")
    public ResponseEntity<Map<String, Object>> replay(
            @RequestParam UUID eventId,
            @RequestParam(defaultValue = "false") boolean dryRun
    ) {
        DlqMessage message = repo.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No DLQ message: " + eventId));

        if (dryRun) {
            return ResponseEntity.ok(Map.of(
                    "dryRun", true,
                    "eventId", eventId,
                    "wouldReplayTo", message.getOriginalTopic(),
                    "tradeRef", message.getTradeRef(),
                    "payload", message.getPayload()
            ));
        }

        producer.publish(message.getPayload());
        repo.delete(message);

        return ResponseEntity.ok(Map.of(
                "replayed", true,
                "eventId", eventId,
                "topic", message.getOriginalTopic(),
                "tradeRef", message.getTradeRef()
        ));
    }
}
