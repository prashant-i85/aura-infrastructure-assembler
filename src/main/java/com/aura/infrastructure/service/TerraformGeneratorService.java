package com.aura.infrastructure.service;

import com.aura.infrastructure.config.TerraformConfig;
import com.aura.infrastructure.exception.TerraformExecutionException;
import com.aura.infrastructure.model.InfrastructureRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerraformGeneratorService {

    private final TerraformConfig config;

    /**
     * Generates all Terraform configuration files for the given request
     * inside an isolated workspace directory.
     *
     * @param request   The validated infrastructure request
     * @param requestId The unique identifier for this request
     * @return Path to the created workspace directory
     */
    public Path generateConfiguration(InfrastructureRequest request, String requestId) {
        Path workspacePath = Paths.get(config.getWorkspacesDir(), requestId);

        try {
            Files.createDirectories(workspacePath);
            log.info("Created workspace directory: {}", workspacePath.toAbsolutePath());

            writeFile(workspacePath, "provider.tf",
                    renderTemplate("provider.tf.template", request, requestId));

            writeFile(workspacePath, "variables.tf",
                    renderTemplate("variables.tf.template", request, requestId));

            writeFile(workspacePath, "terraform.tfvars",
                    renderTemplate("terraform.tfvars.template", request, requestId));

            writeFile(workspacePath, "main.tf",
                    renderTemplate("main.tf.template", request, requestId));

            writeFile(workspacePath, "outputs.tf",
                    renderTemplate("outputs.tf.template", request, requestId));

            log.info("All Terraform configuration files generated for request: {}", requestId);
            return workspacePath;

        } catch (IOException e) {
            throw new TerraformExecutionException(
                    "Failed to generate Terraform configuration files: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a template file from the classpath, replaces all ${placeholder} tokens
     * with the actual values from the request.
     */
    private String renderTemplate(String templateName, InfrastructureRequest request, String requestId) {
        String templatePath = "/templates/" + templateName;
        try (InputStream is = getClass().getResourceAsStream(templatePath)) {
            if (is == null) {
                throw new TerraformExecutionException(
                        "Template not found on classpath: " + templatePath);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return applySubstitutions(content, request, requestId);
        } catch (IOException e) {
            throw new TerraformExecutionException(
                    "Failed to read template " + templateName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Replaces all placeholder tokens in the template string with actual values.
     * Optionally injects key_name block only if keyPairName is provided.
     */
    private String applySubstitutions(String template, InfrastructureRequest request, String requestId) {
        String keyPairBlock = (request.getKeyPairName() != null && !request.getKeyPairName().isBlank())
                ? "  key_name      = var.key_pair_name"
                : "  # key_name not specified";

        String keyPairVarValue = (request.getKeyPairName() != null && !request.getKeyPairName().isBlank())
                ? "\"" + sanitize(request.getKeyPairName()) + "\""
                : "\"\"";

        return template
                .replace("${requestId}", sanitize(requestId))
                .replace("${instanceType}", sanitize(request.getInstanceType()))
                .replace("${region}", sanitize(request.getRegion()))
                .replace("${amiId}", sanitize(request.getAmiId()))
                .replace("${instanceName}", sanitize(request.getInstanceName()))
                .replace("${keyPairName}", keyPairVarValue)
                .replace("${keyPairBlock}", keyPairBlock);
    }

    /**
     * Basic sanitization: strips characters that could break HCL syntax or enable injection.
     * AWS resource names/IDs only ever use alphanumeric, hyphens, underscores, dots.
     */
    private String sanitize(String value) {
        if (value == null) return "";
        return value.replaceAll("[^a-zA-Z0-9\\-_./]", "");
    }

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Path filePath = dir.resolve(filename);
        Files.writeString(filePath, content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        log.debug("Wrote file: {}", filePath);
    }

    /**
     * Recursively deletes the workspace directory for cleanup purposes.
     */
    public void cleanupWorkspace(Path workspacePath) {
        try {
            if (Files.exists(workspacePath)) {
                deleteRecursive(workspacePath);
                log.info("Cleaned up workspace: {}", workspacePath);
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup workspace {}: {}", workspacePath, e.getMessage());
        }
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursive(entry);
                }
            }
        }
        Files.delete(path);
    }
}
