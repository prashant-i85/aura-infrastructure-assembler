package com.aura.infrastructure.service.strategy;

import com.aura.infrastructure.exception.TerraformExecutionException;
import com.aura.infrastructure.model.InfrastructureRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
@Slf4j
public class GcpAiWorkspaceGenerator implements TerraformGeneratorStrategy {

    @Override
    public boolean supports(String provider, String resourceType) {
        return "GCP".equalsIgnoreCase(provider) && "AI_WORKSPACE".equalsIgnoreCase(resourceType);
    }

    @Override
    public void generate(InfrastructureRequest req, String workspacePathStr) {
        Path workspacePath = Paths.get(workspacePathStr);

        try {
            Files.createDirectories(workspacePath);
            String requestId = workspacePath.getFileName().toString();
            String machineType = req.getInstanceType() != null ? req.getInstanceType() : "e2-standard-4";

            String providerContent = """
terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
  }
}

provider "google" {
  project = "aura-gcp-project"
  region  = "%s"
}
""".formatted(req.getRegion());
            writeFile(workspacePath, "provider.tf", providerContent);

            String mainContent = """
resource "google_notebooks_instance" "aura_ai_workspace" {
  name         = "%s"
  location     = "%s-a"
  machine_type = "%s"
  
  vm_image {
    project      = "deeplearning-platform-release"
    image_family = "common-cpu"
  }
  
  labels = {
    managed_by = "aura"
    request_id = "%s"
    framework  = "%s"
  }
}
""".formatted(req.getInstanceName().replace("_", "-").toLowerCase(), 
              req.getRegion(), 
              machineType, 
              requestId, 
              req.getAiFramework() != null ? req.getAiFramework().toLowerCase() : "default");
            writeFile(workspacePath, "main.tf", mainContent);

            String outputsContent = """
output "instance_id" {
  value = google_notebooks_instance.aura_ai_workspace.id
}
output "public_ip" {
  value = google_notebooks_instance.aura_ai_workspace.proxy_uri
}
output "availability_zone" {
  value = google_notebooks_instance.aura_ai_workspace.location
}
""";
            writeFile(workspacePath, "outputs.tf", outputsContent);

            log.info("Generated GCP AI Workspace Terraform configurations");
        } catch (IOException e) {
            throw new TerraformExecutionException("Failed to generate GCP AI Workspace Terraform files", e);
        }
    }

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Path filePath = dir.resolve(filename);
        Files.writeString(filePath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
