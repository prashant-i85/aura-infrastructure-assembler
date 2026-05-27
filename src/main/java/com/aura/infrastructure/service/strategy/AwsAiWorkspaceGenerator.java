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
public class AwsAiWorkspaceGenerator implements TerraformGeneratorStrategy {

    @Override
    public boolean supports(String provider, String resourceType) {
        return "AWS".equalsIgnoreCase(provider) && "AI_WORKSPACE".equalsIgnoreCase(resourceType);
    }

    @Override
    public void generate(InfrastructureRequest req, String workspacePathStr) {
        Path workspacePath = Paths.get(workspacePathStr);

        try {
            Files.createDirectories(workspacePath);
            String requestId = workspacePath.getFileName().toString();
            String instanceType = req.getInstanceType() != null ? req.getInstanceType() : "ml.t2.medium";

            String providerContent = """
provider "aws" {
  region = "%s"
}
""".formatted(req.getRegion());
            writeFile(workspacePath, "provider.tf", providerContent);

            String mainContent = """
resource "aws_iam_role" "sagemaker_role" {
  name = "sagemaker_role_${requestId}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "sagemaker.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_sagemaker_notebook_instance" "ni" {
  name          = "%s"
  role_arn      = aws_iam_role.sagemaker_role.arn
  instance_type = "%s"
  
  tags = {
    ManagedBy = "AURA"
    RequestId = "%s"
    Framework = "%s"
  }
}
""".formatted(req.getInstanceName().replace("_", "-"), 
              instanceType, 
              requestId, 
              req.getAiFramework() != null ? req.getAiFramework() : "DEFAULT");
            writeFile(workspacePath, "main.tf", mainContent);

            String outputsContent = """
output "instance_id" {
  value = aws_sagemaker_notebook_instance.ni.id
}
output "public_ip" {
  value = aws_sagemaker_notebook_instance.ni.url
}
output "availability_zone" {
  value = "N/A"
}
""";
            writeFile(workspacePath, "outputs.tf", outputsContent);

            log.info("Generated AWS AI Workspace Terraform configurations");
        } catch (IOException e) {
            throw new TerraformExecutionException("Failed to generate AWS AI Workspace Terraform files", e);
        }
    }

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Path filePath = dir.resolve(filename);
        Files.writeString(filePath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
