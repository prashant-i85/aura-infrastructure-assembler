package com.aura.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingComparisonResponse {
    private String awsDetails;
    private double awsPricePerHour;
    private String gcpDetails;
    private double gcpPricePerHour;
    private String recommendation;
}