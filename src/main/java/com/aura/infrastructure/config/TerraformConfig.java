package com.aura.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "terraform")
@Data
public class TerraformConfig {

    /**
     * Base directory where per-request Terraform workspaces are created.
     * Default: terraform-workspaces (relative to CWD)
     */
    private String workspacesDir = "terraform-workspaces";

    /**
     * Path (or command name) of the Terraform binary.
     * Default: "terraform" (assumes it's on the system PATH)
     */
    private String terraformPath = "terraform";

    /**
     * Maximum time in seconds to wait for a Terraform command to complete.
     * Default: 300 seconds (5 minutes)
     */
    private int commandTimeoutSeconds = 300;
}
