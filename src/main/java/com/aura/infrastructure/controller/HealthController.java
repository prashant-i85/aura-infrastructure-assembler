package com.aura.infrastructure.controller;

import com.aura.infrastructure.config.TerraformConfig;
import com.aura.infrastructure.service.TerraformExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HealthController {

    private final TerraformExecutorService executorService;
    private final TerraformConfig config;

    /**
     * GET /api/health
     *
     * Reports system status including Terraform CLI availability.
     * Useful for verifying the environment is correctly configured before submitting requests.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("application", "AURA Infrastructure Assembler");
        result.put("timestamp", LocalDateTime.now().toString());

        // Check Terraform CLI availability
        boolean terraformAvailable = false;
        String terraformVersion = null;
        try {
            String versionOutput = executorService.version();
            terraformAvailable = true;
            // Extract first line: "Terraform v1.x.y"
            terraformVersion = versionOutput.lines().findFirst().orElse("Unknown").trim();
        } catch (Exception e) {
            log.warn("Terraform CLI not available: {}", e.getMessage());
        }

        result.put("terraformAvailable", terraformAvailable);
        result.put("terraformVersion", terraformVersion);
        result.put("terraformPath", config.getTerraformPath());
        result.put("workspacesDir", config.getWorkspacesDir());

        return ResponseEntity.ok(result);
    }
}
