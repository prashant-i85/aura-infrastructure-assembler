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
public class AwsStorageGenerator implements TerraformGeneratorStrategy {

    @Override
    public boolean supports(String provider, String resourceType) {
        return "AWS".equalsIgnoreCase(provider) && "STORAGE".equalsIgnoreCase(resourceType);
    }

    @Override
    public void generate(InfrastructureRequest req, String workspacePathStr) {
        Path workspacePath = Paths.get(workspacePathStr);

        try {
            Files.createDirectories(workspacePath);
            String requestId = workspacePath.getFileName().toString();
            // S3 naming rules require globally unique bucket names
            String bucketName = req.getInstanceName().replace("_", "-").toLowerCase() + "-" + requestId.substring(0, 8);


            String providerContent = """
provider "aws" {
  region = "%s"
}
""".formatted(req.getRegion());
            writeFile(workspacePath, "provider.tf", providerContent);

            String mainContent = """
resource "aws_s3_bucket" "aura_storage" {
  bucket = "%s"
  tags = {
    ManagedBy = "AURA"
    RequestId = "%s"
  }
}
""".formatted(bucketName, requestId);
            writeFile(workspacePath, "main.tf", mainContent);

            String outputsContent = """
output "instance_id" {
  value = aws_s3_bucket.aura_storage.id
}
output "public_ip" {
  value = "N/A"
}
output "availability_zone" {
  value = "global"
}
""";
            writeFile(workspacePath, "outputs.tf", outputsContent);

            log.info("Generated AWS Storage Terraform configurations");
        } catch (IOException e) {
            throw new TerraformExecutionException("Failed to generate AWS Storage Terraform files", e);
        }
    }

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Path filePath = dir.resolve(filename);
        Files.writeString(filePath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
