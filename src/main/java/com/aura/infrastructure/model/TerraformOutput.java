package com.aura.infrastructure.model;

import lombok.Data;

@Data
public class TerraformOutput {

    private String instanceId;
    private String publicIp;
    private String availabilityZone;
}
