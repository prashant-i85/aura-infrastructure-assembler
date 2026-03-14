package com.aura.infrastructure.service;

import com.aura.infrastructure.config.TerraformConfig;
import com.aura.infrastructure.model.InfrastructureRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TerraformGeneratorServiceTest {

    private TerraformGeneratorService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TerraformConfig config = new TerraformConfig();
        config.setWorkspacesDir(tempDir.toString());
        service = new TerraformGeneratorService(config);
    }

    private InfrastructureRequest buildRequest() {
        InfrastructureRequest req = new InfrastructureRequest();
        req.setInstanceType("t2.micro");
        req.setRegion("us-east-1");
        req.setAmiId("ami-0c02fb55956c7d316");
        req.setInstanceName("test-server");
        return req;
    }

    @Test
    void generateConfiguration_CreatesWorkspaceDirectory() {
        InfrastructureRequest request = buildRequest();
        Path workspacePath = service.generateConfiguration(request, "test-req-001");
        assertThat(workspacePath).isDirectory();
        assertThat(workspacePath.getFileName().toString()).isEqualTo("test-req-001");
    }

    @Test
    void generateConfiguration_CreatesAllFiveTfFiles() {
        Path workspacePath = service.generateConfiguration(buildRequest(), "test-req-002");

        assertThat(workspacePath.resolve("provider.tf")).exists();
        assertThat(workspacePath.resolve("variables.tf")).exists();
        assertThat(workspacePath.resolve("terraform.tfvars")).exists();
        assertThat(workspacePath.resolve("main.tf")).exists();
        assertThat(workspacePath.resolve("outputs.tf")).exists();
    }

    @Test
    void generateConfiguration_MainTfContainsAmiAndType() throws IOException {
        Path workspacePath = service.generateConfiguration(buildRequest(), "test-req-003");
        String mainContent = Files.readString(workspacePath.resolve("main.tf"));

        assertThat(mainContent).contains("aws_instance");
        assertThat(mainContent).contains("var.ami_id");
        assertThat(mainContent).contains("var.instance_type");
        assertThat(mainContent).contains("test-req-003");
    }

    @Test
    void generateConfiguration_TfvarsContainsCorrectValues() throws IOException {
        Path workspacePath = service.generateConfiguration(buildRequest(), "test-req-004");
        String tfvarsContent = Files.readString(workspacePath.resolve("terraform.tfvars"));

        assertThat(tfvarsContent).contains("us-east-1");
        assertThat(tfvarsContent).contains("t2.micro");
        assertThat(tfvarsContent).contains("ami-0c02fb55956c7d316");
        assertThat(tfvarsContent).contains("test-server");
    }

    @Test
    void generateConfiguration_OutputsTfHasAllOutputs() throws IOException {
        Path workspacePath = service.generateConfiguration(buildRequest(), "test-req-005");
        String outputContent = Files.readString(workspacePath.resolve("outputs.tf"));

        assertThat(outputContent).contains("instance_id");
        assertThat(outputContent).contains("public_ip");
        assertThat(outputContent).contains("availability_zone");
    }

    @Test
    void generateConfiguration_WithKeyPair_IncludesKeyName() throws IOException {
        InfrastructureRequest request = buildRequest();
        request.setKeyPairName("my-key-pair");

        Path workspacePath = service.generateConfiguration(request, "test-req-006");
        String mainContent = Files.readString(workspacePath.resolve("main.tf"));
        String tfvarsContent = Files.readString(workspacePath.resolve("terraform.tfvars"));

        assertThat(mainContent).contains("key_name");
        assertThat(tfvarsContent).contains("my-key-pair");
    }

    @Test
    void generateConfiguration_SanitizesSpecialCharactersInName() throws IOException {
        InfrastructureRequest request = buildRequest();
        request.setInstanceName("test; rm -rf /");

        Path workspacePath = service.generateConfiguration(request, "test-req-007");
        String tfvarsContent = Files.readString(workspacePath.resolve("terraform.tfvars"));

        // Dangerous characters must be stripped
        assertThat(tfvarsContent).doesNotContain(";");
        assertThat(tfvarsContent).doesNotContain(" ");
    }
}
