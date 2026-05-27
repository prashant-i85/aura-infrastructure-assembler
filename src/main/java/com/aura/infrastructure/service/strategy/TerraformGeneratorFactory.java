package com.aura.infrastructure.service.strategy;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class TerraformGeneratorFactory {

    private final List<TerraformGeneratorStrategy> strategies;

    public TerraformGeneratorFactory(List<TerraformGeneratorStrategy> strategies) {
        this.strategies = strategies;
    }

    public TerraformGeneratorStrategy getStrategy(String provider, String resourceType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(provider, resourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider/resourceType: " + provider + "/" + resourceType));
    }
}
