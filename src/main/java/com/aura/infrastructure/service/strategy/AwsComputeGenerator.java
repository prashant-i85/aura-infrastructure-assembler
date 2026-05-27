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
public class AwsComputeGenerator implements TerraformGeneratorStrategy {

    @Override
    public boolean supports(String provider, String resourceType) {
        return "AWS".equalsIgnoreCase(provider) && "COMPUTE".equalsIgnoreCase(resourceType);
    }

    @Override
    public void generate(InfrastructureRequest req, String workspacePathStr) {
        Path workspacePath = Paths.get(workspacePathStr);

        try {
            Files.createDirectories(workspacePath);
            log.info("Created workspace directory: {}", workspacePath.toAbsolutePath());
            String requestId = workspacePath.getFileName().toString();

            // provider.tf
            String providerContent = """
provider "aws" {
  region = "%s"
}
""".formatted(req.getRegion());
            writeFile(workspacePath, "provider.tf", providerContent);

            // main.tf
            String keyNameLine = (req.getKeyPairName() != null && !req.getKeyPairName().isBlank())
                    ? "  key_name      = \"%s\"".formatted(req.getKeyPairName())
                    : "  # key_name not specified";

            String mainContent = """
resource "aws_instance" "aura_instance" {
  ami           = "%s"
  instance_type = "%s"
%s

  tags = {
    Name      = "%s"
    ManagedBy = "AURA"
    RequestId = "%s"
  }
}
""".formatted(
                    req.getAmiId(),
                    req.getInstanceType() != null ? req.getInstanceType() : "t2.micro",
                    keyNameLine,
                    req.getInstanceName(),
                    requestId);
            writeFile(workspacePath, "main.tf", mainContent);

            // outputs.tf
            String outputsContent = """
output "instance_id" {
  value = aws_instance.aura_instance.id
}
output "public_ip" {
  value = aws_instance.aura_instance.public_ip
}
output "availability_zone" {
  value = aws_instance.aura_instance.availability_zone
}
""";
            writeFile(workspacePath, "outputs.tf", outputsContent);

            log.info("All Terraform configuration files generated for request: {}", requestId);

        } catch (IOException e) {
            throw new TerraformExecutionException(
                    "Failed to generate Terraform configuration files: " + e.getMessage(), e);
        }
    }

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Path filePath = dir.resolve(filename);
        Files.writeString(filePath, content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        log.debug("Wrote file: {}", filePath);
    }
}
