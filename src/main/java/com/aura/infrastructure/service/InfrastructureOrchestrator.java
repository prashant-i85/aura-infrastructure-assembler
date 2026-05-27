package com.aura.infrastructure.service;

import com.aura.infrastructure.config.TerraformConfig;
import com.aura.infrastructure.exception.InvalidRequestException;
import com.aura.infrastructure.exception.ResourceNotFoundException;
import com.aura.infrastructure.exception.TerraformExecutionException;
import com.aura.infrastructure.model.InfrastructureRequest;
import com.aura.infrastructure.model.InfrastructureResponse;
import com.aura.infrastructure.model.ProvisioningStatus;
import com.aura.infrastructure.model.TerraformOutput;
import com.aura.infrastructure.repository.RequestRepository;
import com.aura.infrastructure.service.strategy.TerraformGeneratorFactory;
import com.aura.infrastructure.service.strategy.TerraformGeneratorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfrastructureOrchestrator {

        @org.springframework.beans.factory.annotation.Autowired
        @org.springframework.context.annotation.Lazy
        private InfrastructureOrchestrator self;

        private final TerraformGeneratorFactory generatorFactory;
        private final TerraformConfig config;
        private final TerraformExecutorService executorService;
        private final RequestRepository repository;

        // ─── Provision ───────────────────────────────────────────────────────────────

        /**
         * Initiates a new provisioning request. Persists the request immediately with
         * status PENDING and triggers asynchronous Terraform execution.
         *
         * @return The requestId to be returned to the caller in the HTTP 202 response
         */
        public InfrastructureResponse initiateProvisioning(InfrastructureRequest request) {
                String requestId = UUID.randomUUID().toString();
                LocalDateTime now = LocalDateTime.now();

                InfrastructureResponse response = InfrastructureResponse.builder()
                                .requestId(requestId)
                                .status(ProvisioningStatus.PENDING)
                                .region(request.getRegion())
                                .instanceType(request.getInstanceType())
                                .instanceName(request.getInstanceName())
                                .message("Infrastructure provisioning request accepted. " +
                                                "Poll GET /api/infrastructure/status/" + requestId
                                                + " to track progress.")
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                repository.save(response);
                log.info("Provisioning request accepted: requestId={}", requestId);

                // Kick off background execution through the proxy
                self.provisionAsync(request, requestId);

                return response;
        }

        /**
         * Asynchronous provisioning pipeline. Runs in the auraTaskExecutor thread pool.
         * Progresses through: IN_PROGRESS → generate → init → plan → apply → output →
         * SUCCESS
         * On any failure: FAILED.
         */
        @Async("auraTaskExecutor")
        public void provisionAsync(InfrastructureRequest request, String requestId) {
                log.info("Starting async provisioning for requestId={}", requestId);
                Path workspacePath = null;

                try {
                        // ── Phase 1: Generate Terraform files ──────────────────────────────
                        repository.updateStatus(requestId, ProvisioningStatus.IN_PROGRESS,
                                        "Generating Terraform configuration files...");
                                        
                        workspacePath = Paths.get(config.getWorkspacesDir(), requestId);
                        TerraformGeneratorStrategy strategy = generatorFactory.getStrategy(request.getProvider(), request.getResourceType());
                        strategy.generate(request, workspacePath.toString());
                        
                        updateWorkspacePath(requestId, workspacePath);

                        // ── Phase 2: terraform init ────────────────────────────────────────
                        repository.updateStatus(requestId, ProvisioningStatus.IN_PROGRESS,
                                        "Initializing Terraform (downloading AWS provider)...");
                        log.info("[{}] Running terraform init", requestId);
                        executorService.init(workspacePath);

                        // ── Phase 3: terraform plan ────────────────────────────────────────
                        repository.updateStatus(requestId, ProvisioningStatus.IN_PROGRESS,
                                        "Running terraform plan...");
                        log.info("[{}] Running terraform plan", requestId);
                        executorService.plan(workspacePath);

                        // ── Phase 4: terraform apply ───────────────────────────────────────
                        repository.updateStatus(requestId, ProvisioningStatus.IN_PROGRESS,
                                        "Applying Terraform plan (provisioning AWS resources)...");
                        log.info("[{}] Running terraform apply", requestId);
                        executorService.apply(workspacePath);

                        // ── Phase 5: terraform output ──────────────────────────────────────
                        repository.updateStatus(requestId, ProvisioningStatus.IN_PROGRESS,
                                        "Extracting resource metadata from Terraform state...");
                        log.info("[{}] Running terraform output", requestId);
                        TerraformOutput tfOutput = executorService.output(workspacePath);

                        // ── Phase 6: Persist success ───────────────────────────────────────
                        applySuccessfulOutput(request, requestId, tfOutput);
                        log.info("[{}] Provisioning SUCCEEDED. instanceId={}, publicIp={}",
                                        requestId, tfOutput.getInstanceId(), tfOutput.getPublicIp());

                } catch (TerraformExecutionException e) {
                        log.error("[{}] Provisioning FAILED: {}", requestId, e.getMessage());
                        repository.updateStatus(requestId, ProvisioningStatus.FAILED,
                                        "Provisioning failed: " + e.getMessage());

                } catch (Exception e) {
                        log.error("[{}] Unexpected error during provisioning: {}", requestId, e.getMessage(), e);
                        repository.updateStatus(requestId, ProvisioningStatus.FAILED,
                                        "Unexpected error: " + e.getMessage());
                }
        }

        // ─── Destroy ─────────────────────────────────────────────────────────────────

        /**
         * Initiates async destruction of a previously provisioned resource.
         */
        public InfrastructureResponse initiateDestruction(String requestId) {
                InfrastructureResponse existing = repository.findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("InfrastructureRequest", requestId));

                if (existing.getStatus() == ProvisioningStatus.DESTROYING ||
                                existing.getStatus() == ProvisioningStatus.DESTROYED) {
                        throw new InvalidRequestException(
                                        "Resource is already being destroyed or has been destroyed.");
                }

                if (existing.getStatus() == ProvisioningStatus.FAILED ||
                                existing.getStatus() == ProvisioningStatus.PENDING) {
                        throw new InvalidRequestException(
                                        "Cannot destroy a resource in state: " + existing.getStatus());
                }

                repository.updateStatus(requestId, ProvisioningStatus.DESTROYING,
                                "Destruction initiated. Running terraform destroy...");
                log.info("Destruction request accepted for requestId={}", requestId);

                destroyAsync(requestId, existing.getWorkspacePath());

                return repository.findById(requestId).orElseThrow();
        }

        @Async("auraTaskExecutor")
        public void destroyAsync(String requestId, String workspacePathStr) {
                log.info("Starting async destruction for requestId={}", requestId);
                try {
                        if (workspacePathStr == null) {
                                throw new TerraformExecutionException(
                                                "No workspace path recorded for requestId: " + requestId);
                        }
                        Path workspacePath = Path.of(workspacePathStr);
                        executorService.destroy(workspacePath);

                        repository.updateStatus(requestId, ProvisioningStatus.DESTROYED,
                                        "Infrastructure successfully destroyed.");
                        log.info("[{}] Destruction SUCCEEDED", requestId);

                } catch (TerraformExecutionException e) {
                        log.error("[{}] Destruction FAILED: {}", requestId, e.getMessage());
                        repository.updateStatus(requestId, ProvisioningStatus.FAILED,
                                        "Destruction failed: " + e.getMessage());
                }
        }

        // ─── Query ───────────────────────────────────────────────────────────────────

        public InfrastructureResponse getStatus(String requestId) {
                return repository.findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("InfrastructureRequest", requestId));
        }

        public List<InfrastructureResponse> getAllRequests() {
                return repository.findAll();
        }

        // ─── Helpers ─────────────────────────────────────────────────────────────────

        private void updateWorkspacePath(String requestId, Path workspacePath) {
                repository.findById(requestId).ifPresent(r -> {
                        r.setWorkspacePath(workspacePath.toAbsolutePath().toString());
                        r.setUpdatedAt(LocalDateTime.now());
                });
        }

        private void applySuccessfulOutput(InfrastructureRequest request, String requestId, TerraformOutput tfOutput) {
                repository.findById(requestId).ifPresent(r -> {
                        r.setStatus(ProvisioningStatus.SUCCESS);
                        r.setInstanceId(tfOutput.getInstanceId());
                        r.setPublicIp(tfOutput.getPublicIp());
                        r.setAvailabilityZone(tfOutput.getAvailabilityZone());
                        r.setMessage(String.format("%s %s provisioned successfully.", request.getProvider(), request.getResourceType()));
                        r.setUpdatedAt(LocalDateTime.now());
                });
        }
}
