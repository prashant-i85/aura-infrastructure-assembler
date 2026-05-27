package com.aura.infrastructure.service;

import com.aura.infrastructure.model.PricingComparisonResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PricingService {

    private static final Map<String, PricingComparisonResponse> PRICING_MOCK_DB = new HashMap<>();

    static {
        PRICING_MOCK_DB.put("COMPUTE", PricingComparisonResponse.builder()
                .awsDetails("AWS EC2 t2.micro")
                .awsPricePerHour(0.0116)
                .gcpDetails("GCP Compute Engine e2-micro")
                .gcpPricePerHour(0.0085)
                .recommendation("GCP is cheaper for COMPUTE.")
                .build());

        PRICING_MOCK_DB.put("STORAGE", PricingComparisonResponse.builder()
                .awsDetails("AWS S3 Standard (per GB)")
                .awsPricePerHour(0.023)
                .gcpDetails("GCP Cloud Storage Standard (per GB)")
                .gcpPricePerHour(0.020)
                .recommendation("GCP is slightly cheaper for STORAGE.")
                .build());

        PRICING_MOCK_DB.put("AI_WORKSPACE", PricingComparisonResponse.builder()
                .awsDetails("AWS SageMaker Notebook (ml.t2.medium)")
                .awsPricePerHour(0.0464)
                .gcpDetails("GCP Vertex AI Workbench (n1-standard-1)")
                .gcpPricePerHour(0.0475)
                .recommendation("AWS is slightly cheaper for AI_WORKSPACE.")
                .build());
    }

    public PricingComparisonResponse comparePricing(String resourceType) {
        if (resourceType == null) {
            throw new IllegalArgumentException("Resource type must be provided");
        }
        PricingComparisonResponse response = PRICING_MOCK_DB.get(resourceType.toUpperCase());
        if (response == null) {
            throw new IllegalArgumentException("No pricing data available for resource type: " + resourceType);
        }
        return response;
    }
}