package com.aura.infrastructure.service;

import com.aura.infrastructure.config.TerraformConfig;
import com.aura.infrastructure.exception.TerraformExecutionException;
import com.aura.infrastructure.model.TerraformOutput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerraformExecutorService {

    private final TerraformConfig config;
    private final ObjectMapper objectMapper;

    // ─── Public API ──────────────────────────────────────────────────────────────

    public String init(Path workspacePath) {
        log.info("Running: terraform init in {}", workspacePath);
        return executeCommand(workspacePath, config.getTerraformPath(), "init", "-no-color");
    }

    public String plan(Path workspacePath) {
        log.info("Running: terraform plan in {}", workspacePath);
        return executeCommand(workspacePath, config.getTerraformPath(), "plan", "-no-color");
    }

    public String apply(Path workspacePath) {
        log.info("Running: terraform apply in {}", workspacePath);
        return executeCommand(workspacePath, config.getTerraformPath(),
                "apply", "-auto-approve", "-no-color");
    }

    public TerraformOutput output(Path workspacePath) {
        log.info("Running: terraform output -json in {}", workspacePath);
        String jsonOutput = executeCommand(workspacePath, config.getTerraformPath(),
                "output", "-json", "-no-color");
        return parseTerraformOutput(jsonOutput);
    }

    public String destroy(Path workspacePath) {
        log.info("Running: terraform destroy in {}", workspacePath);
        return executeCommand(workspacePath, config.getTerraformPath(),
                "destroy", "-auto-approve", "-no-color");
    }

    public String version() {
        log.debug("Checking Terraform version");
        return executeCommand(null, config.getTerraformPath(), "version");
    }

    // ─── Core Execution Engine ────────────────────────────────────────────────────

    /**
     * Executes a Terraform command as a child OS process.
     *
     * <p>Uses a StreamGobbler thread to consume stdout/stderr asynchronously,
     * preventing buffer deadlocks. Enforces a configurable timeout.
     *
     * @param workingDir Working directory (the request workspace); null for version checks
     * @param command    The executable and its arguments
     * @return Captured stdout+stderr output as a String
     * @throws TerraformExecutionException if the process exits with non-zero or times out
     */
    String executeCommand(Path workingDir, String... command) {
        List<String> cmdList = new ArrayList<>(List.of(command));
        log.debug("Executing: {}", String.join(" ", cmdList));

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        pb.redirectErrorStream(true); // merge stderr into stdout

        if (workingDir != null) {
            pb.directory(workingDir.toAbsolutePath().toFile());
        }

        // Pass through host environment (includes AWS_PROFILE, AWS_* env vars)
        pb.environment().putAll(System.getenv());

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new TerraformExecutionException(
                    "Failed to start Terraform process: " + e.getMessage(), e);
        }

        // ── Async stream reading via StreamGobbler ──────────────────────────────
        StringBuilder output = new StringBuilder();
        Thread gobbler = new Thread(new StreamGobbler(
                process.getInputStream(),
                line -> {
                    output.append(line).append("\n");
                    log.debug("[terraform] {}", line);
                }
        ));
        gobbler.start();

        // ── Wait with timeout ───────────────────────────────────────────────────
        boolean finished;
        try {
            finished = process.waitFor(config.getCommandTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new TerraformExecutionException("Terraform command interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new TerraformExecutionException(
                    "Terraform command timed out after " + config.getCommandTimeoutSeconds() + " seconds",
                    output.toString());
        }

        try {
            gobbler.join(5000); // give gobbler up to 5s to flush remaining output
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int exitCode = process.exitValue();
        String capturedOutput = output.toString();

        if (exitCode != 0) {
            log.error("Terraform command failed (exit {}): {}", exitCode, capturedOutput);
            throw new TerraformExecutionException(
                    "Terraform command failed with exit code " + exitCode,
                    capturedOutput);
        }

        log.debug("Terraform command succeeded (exit 0)");
        return capturedOutput;
    }

    // ─── Output Parser ────────────────────────────────────────────────────────────

    private TerraformOutput parseTerraformOutput(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            TerraformOutput output = new TerraformOutput();

            JsonNode instanceIdNode = root.path("instance_id").path("value");
            if (!instanceIdNode.isMissingNode()) {
                output.setInstanceId(instanceIdNode.asText());
            }

            JsonNode publicIpNode = root.path("public_ip").path("value");
            if (!publicIpNode.isMissingNode()) {
                output.setPublicIp(publicIpNode.asText());
            }

            JsonNode azNode = root.path("availability_zone").path("value");
            if (!azNode.isMissingNode()) {
                output.setAvailabilityZone(azNode.asText());
            }

            return output;
        } catch (Exception e) {
            log.error("Failed to parse terraform output JSON: {}", json);
            throw new TerraformExecutionException(
                    "Failed to parse terraform output: " + e.getMessage(), json);
        }
    }

    // ─── StreamGobbler (Inner Class) ──────────────────────────────────────────────

    /**
     * Reads an InputStream line by line in a dedicated thread.
     * Prevents stdout/stderr buffer deadlocks when child processes produce lots of output.
     */
    private record StreamGobbler(
            java.io.InputStream inputStream,
            java.util.function.Consumer<String> consumer
    ) implements Runnable {

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                reader.lines().forEach(consumer);
            } catch (IOException e) {
                // Stream closed — normal when process exits
            }
        }
    }
}
