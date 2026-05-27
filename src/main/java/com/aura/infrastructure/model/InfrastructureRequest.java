package com.aura.infrastructure.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class InfrastructureRequest {

    @NotBlank(message = "provider is required")
    @Pattern(regexp = "^(AWS|GCP)$", message = "provider must be AWS or GCP")
    private String provider;

    @NotBlank(message = "resourceType is required")
    @Pattern(regexp = "^(COMPUTE|STORAGE|AI_WORKSPACE)$", message = "resourceType must be COMPUTE, STORAGE, or AI_WORKSPACE")
    private String resourceType;

    @Pattern(regexp = "^(t[2-3]\\.(nano|micro|small|medium|large)|e2-micro)$", message = "instanceType must be a valid type (e.g., t2.micro, e2-micro)")
    private String instanceType;

    @NotBlank(message = "region is required")
    @Pattern(regexp = "^[a-z]{2}-[a-z]+-\\d+$", message = "region must be a valid region format (e.g., us-east-1, us-central1)")
    private String region;

    @Pattern(regexp = "^(ami-[a-f0-9]{8,17}|debian-11-bullseye-v\\d+|ubuntu-os-cloud/ubuntu-2004-lts)$", message = "amiId/imageId must be valid")
    private String amiId;

    @NotBlank(message = "instanceName is required")
    @Size(min = 3, max = 64, message = "instanceName must be between 3 and 64 characters")
    private String instanceName;

    // Optional — if null, no key pair is associated with the instance
    private String keyPairName;

    // Optional for STORAGE
    @Min(value = 1, message = "storageSizeGb must be at least 1 GB")
    private Integer storageSizeGb;

    // Optional for AI_WORKSPACE
    private String aiFramework;
}
