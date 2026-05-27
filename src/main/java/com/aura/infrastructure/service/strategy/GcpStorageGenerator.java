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
public class GcpStorageGenerator implements TerraformGeneratorStrategy {

    @Override
    public boolean supports(String provider, String resourceType) {
        return "GCP".equalsIgnoreCase(provider) && "STORAGE".equalsIgnoreCase(resourceType);
    }

    @Override
    public void generate(InfrastructureRequest req, String workspacePathStr) {
        Path workspacePath = Paths.get(workspacePathStr);

        try {
            Files.createDirectories(workspacePath);
            String requestId = workspacePath.getFileName().toString();
            // GCP naming rules require globally unique bucket names
            String bucketName = req.getInstanceName().replace("_", "-").toLowerCase() + "-" + requestId.substring(0, 8);


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
resource "google_storage_bucket" "aura_storage" {
  name          = "%s"
  location      = "%s"
  force_destroy = true

  labels = {
    managed_by = "aura"
    request_id = "%s"
  }
}
""".formatted(bucketName, req.getRegion(), requestId);
            writeFile(workspacePath, "main.tf", mainContent);

            String outputsContent = """
output "instance_id" {
  value = google_storage_bucket.aura_storage.name
}
output "public_ip" {
  value = "N/A"
}
output "availability_zone" {
  value = google_storage_bucket.aura_storage.location
}
""";
            writeFile(workspacePath, "outputs.tf", outputsContent);

            log.info("Generated GCP Storage Terraform configurations");
        } catch (IOException e) {
            throw new TerraformExecutionException("Failed to generate GCP Storage Terraform files", e);
        }
    }

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Path filePath = dir.resolve(filename);
        Files.writeString(filePath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
