package com.aura.infrastructure.controller;

import com.aura.infrastructure.model.PricingComparisonResponse;
import com.aura.infrastructure.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/compare")
    public ResponseEntity<PricingComparisonResponse> comparePricing(@RequestParam String resourceType) {
        try {
            PricingComparisonResponse response = pricingService.comparePricing(resourceType);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}