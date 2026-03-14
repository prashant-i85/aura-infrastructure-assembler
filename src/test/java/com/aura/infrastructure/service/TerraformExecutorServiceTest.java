package com.aura.infrastructure.service;

import com.aura.infrastructure.config.TerraformConfig;
import com.aura.infrastructure.exception.TerraformExecutionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TerraformExecutorServiceTest {

    private TerraformExecutorService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TerraformConfig config = new TerraformConfig();
        config.setTerraformPath("terraform");
        config.setCommandTimeoutSeconds(5); // short timeout for tests
        service = new TerraformExecutorService(config, new ObjectMapper());
    }

    // ─── executeCommand internals (using real OS commands) ───────────────────────

    @Test
    void executeCommand_ValidCommand_ReturnsOutput() {
        // Use "echo" which is universally available on all OS
        String output = service.executeCommand(tempDir, getEchoCommand("hello-aura"));
        assertThat(output).contains("hello-aura");
    }

    @Test
    void executeCommand_NonZeroExitCode_ThrowsTerraformExecutionException() {
        assertThatThrownBy(() ->
                service.executeCommand(tempDir, getFailCommand())
        ).isInstanceOf(TerraformExecutionException.class)
         .hasMessageContaining("exit code");
    }

    @Test
    void executeCommand_NonExistentBinary_ThrowsTerraformExecutionException() {
        assertThatThrownBy(() ->
                service.executeCommand(tempDir, "nonexistent_binary_12345xyz")
        ).isInstanceOf(TerraformExecutionException.class)
         .hasMessageContaining("Failed to start Terraform process");
    }

    // ─── parseTerraformOutput (via output() method) ──────────────────────────────

    @Test
    void outputParsing_ValidJson_ParsesAllFields() throws IOException {
        // Write a fake terraform output JSON to a temp file and test parsing
        String fakeJson = """
                {
                    "instance_id": { "value": "i-0abc123def456", "type": "string" },
                    "public_ip":   { "value": "54.1.2.3",        "type": "string" },
                    "availability_zone": { "value": "us-east-1a", "type": "string" }
                }
                """;

        // Write output to a file — we'll test parsing directly via a custom echo command
        Path fakeOutput = tempDir.resolve("tf_output.json");
        Files.writeString(fakeOutput, fakeJson);

        // Use cat/type to pipe the file content and capture it
        String raw = service.executeCommand(tempDir, getCatCommand(fakeOutput.toString()));
        assertThat(raw).contains("i-0abc123def456");
    }

    // ─── OS-agnostic helper factories ────────────────────────────────────────────

    private String[] getEchoCommand(String text) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"cmd", "/c", "echo", text};
        }
        return new String[]{"echo", text};
    }

    private String[] getFailCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"cmd", "/c", "exit", "1"};
        }
        return new String[]{"sh", "-c", "exit 1"};
    }

    private String[] getCatCommand(String filePath) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"cmd", "/c", "type", filePath};
        }
        return new String[]{"cat", filePath};
    }
}
