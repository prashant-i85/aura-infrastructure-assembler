package com.aura.infrastructure.service.strategy;

import com.aura.infrastructure.model.InfrastructureRequest;

public interface TerraformGeneratorStrategy {
    boolean supports(String provider, String resourceType);
    void generate(InfrastructureRequest req, String workspacePath);
}
