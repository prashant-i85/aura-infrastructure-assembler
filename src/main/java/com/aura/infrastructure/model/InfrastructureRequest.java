package com.aura.infrastructure.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InfrastructureRequest {

    @NotBlank(message = "instanceType is required")
    @Pattern(
        regexp = "^t[2-3]\\.(nano|micro|small|medium|large)$",
        message = "instanceType must be a valid T2/T3 type (e.g., t2.micro, t3.small)"
    )
    private String instanceType;

    @NotBlank(message = "region is required")
    @Pattern(
        regexp = "^[a-z]{2}-[a-z]+-\\d$",
        message = "region must be a valid AWS region format (e.g., us-east-1, ap-south-1)"
    )
    private String region;

    @NotBlank(message = "amiId is required")
    @Pattern(
        regexp = "^ami-[a-f0-9]{8,17}$",
        message = "amiId must be a valid AMI ID (e.g., ami-0c02fb55956c7d316)"
    )
    private String amiId;

    @NotBlank(message = "instanceName is required")
    @Size(min = 3, max = 64, message = "instanceName must be between 3 and 64 characters")
    private String instanceName;

    // Optional — if null, no key pair is associated with the instance
    private String keyPairName;
}
