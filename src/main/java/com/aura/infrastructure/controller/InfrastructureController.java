package com.aura.infrastructure.controller;

import com.aura.infrastructure.model.InfrastructureRequest;
import com.aura.infrastructure.model.InfrastructureResponse;
import com.aura.infrastructure.service.InfrastructureOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // allows the React dev server to call this API
public class InfrastructureController {

    private final InfrastructureOrchestrator orchestrator;

    /**
     * POST /api/infrastructure/provision
     *
     * Accepts an infrastructure request, validates inputs, and starts asynchronous
     * provisioning. Returns HTTP 202 Accepted immediately with a requestId.
     */
    @PostMapping("/provision")
    public ResponseEntity<InfrastructureResponse> provisionInfrastructure(
            @Valid @RequestBody InfrastructureRequest request) {

        log.info("Received provision request: instanceType={}, region={}, instanceName={}",
                request.getInstanceType(), request.getRegion(), request.getInstanceName());

        InfrastructureResponse response = orchestrator.initiateProvisioning(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * GET /api/infrastructure/status/{requestId}
     *
     * Returns the current provisioning status for the given requestId.
     * Returns HTTP 404 if not found.
     */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<InfrastructureResponse> getStatus(@PathVariable String requestId) {
        log.debug("Status query for requestId={}", requestId);
        return ResponseEntity.ok(orchestrator.getStatus(requestId));
    }

    /**
     * DELETE /api/infrastructure/destroy/{requestId}
     *
     * Initiates asynchronous destruction of a provisioned resource.
     * Returns HTTP 202 Accepted immediately.
     */
    @DeleteMapping("/destroy/{requestId}")
    public ResponseEntity<InfrastructureResponse> destroyInfrastructure(
            @PathVariable String requestId) {

        log.info("Received destroy request for requestId={}", requestId);
        InfrastructureResponse response = orchestrator.initiateDestruction(requestId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * GET /api/infrastructure/history
     *
     * Returns all provisioning requests sorted by creation time (newest first).
     */
    @GetMapping("/history")
    public ResponseEntity<List<InfrastructureResponse>> getHistory() {
        return ResponseEntity.ok(orchestrator.getAllRequests());
    }
}
