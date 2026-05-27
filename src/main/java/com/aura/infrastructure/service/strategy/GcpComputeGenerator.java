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
public class GcpComputeGenerator implements TerraformGeneratorStrategy {

    @Override
    public boolean supports(String provider, String resourceType) {
        return "GCP".equalsIgnoreCase(provider) && "COMPUTE".equalsIgnoreCase(resourceType);
    }

    @Override
    public void generate(InfrastructureRequest req, String workspacePathStr) {
        Path workspacePath = Paths.get(workspacePathStr);

        try {
            Files.createDirectories(workspacePath);
            String requestId = workspacePath.getFileName().toString();

            // provider.tf
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

            // main.tf
            String mainContent = """
resource "google_compute_instance" "aura_instance" {
  name         = "%s"
  machine_type = "%s"
  zone         = "%s-a" # Defaulting to zone a in region

  boot_disk {
    initialize_params {
      image = "%s"
    }
  }

  network_interface {
    network = "default"
    access_config {
      // Ephemeral public IP
    }
  }

  labels = {
    managed_by = "aura"
    request_id = "%s"
  }
}
""".formatted(req.getInstanceName(), req.getInstanceType() != null ? req.getInstanceType() : "e2-micro", req.getRegion(), req.getAmiId() != null ? req.getAmiId() : "debian-cloud/debian-11", requestId);
            writeFile(workspacePath, "main.tf", mainContent);

            // outputs.tf
            String outputsContent = """
output "instance_id" {
  value = google_compute_instance.aura_instance.name
}

output "public_ip" {
  value = google_compute_instance.aura_instance.network_interface.0.access_config.0.nat_ip
}

output "availability_zone" {
  value = google_compute_instance.aura_instance.zone
}
""";
            writeFile(workspacePath, "outputs.tf", outputsContent);

            log.info("Generated GCP Compute Terraform configurations");
        } catch (IOException e) {
            throw new TerraformExecutionException("Failed to generate GCP Terraform files", e);
        }
    }

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Path filePath = dir.resolve(filename);
        Files.writeString(filePath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
