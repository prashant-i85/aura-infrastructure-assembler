package com.aura.infrastructure.controller;

import com.aura.infrastructure.model.InfrastructureResponse;
import com.aura.infrastructure.model.ProvisioningStatus;
import com.aura.infrastructure.service.InfrastructureOrchestrator;
import com.aura.infrastructure.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InfrastructureController.class)
class InfrastructureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InfrastructureOrchestrator orchestrator;

    // ─── POST /provision ──────────────────────────────────────────────────────────

    @Test
    void provision_ValidRequest_Returns202() throws Exception {
        InfrastructureResponse mockResponse = InfrastructureResponse.builder()
                .requestId("test-id-123")
                .status(ProvisioningStatus.PENDING)
                .region("us-east-1")
                .instanceType("t2.micro")
                .message("Request accepted")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orchestrator.initiateProvisioning(any())).thenReturn(mockResponse);

        String requestBody = """
                {
                    "instanceType": "t2.micro",
                    "region": "us-east-1",
                    "amiId": "ami-0c02fb55956c7d316",
                    "instanceName": "test-server"
                }
                """;

        mockMvc.perform(post("/api/infrastructure/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value("test-id-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void provision_InvalidInstanceType_Returns400() throws Exception {
        String requestBody = """
                {
                    "instanceType": "m5.xlarge",
                    "region": "us-east-1",
                    "amiId": "ami-0c02fb55956c7d316",
                    "instanceName": "test-server"
                }
                """;

        mockMvc.perform(post("/api/infrastructure/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.instanceType").exists());
    }

    @Test
    void provision_InvalidRegion_Returns400() throws Exception {
        String requestBody = """
                {
                    "instanceType": "t2.micro",
                    "region": "US-EAST-1",
                    "amiId": "ami-0c02fb55956c7d316",
                    "instanceName": "test-server"
                }
                """;

        mockMvc.perform(post("/api/infrastructure/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.region").exists());
    }

    @Test
    void provision_InvalidAmiId_Returns400() throws Exception {
        String requestBody = """
                {
                    "instanceType": "t2.micro",
                    "region": "us-east-1",
                    "amiId": "invalid-ami",
                    "instanceName": "test-server"
                }
                """;

        mockMvc.perform(post("/api/infrastructure/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amiId").exists());
    }

    @Test
    void provision_BlankInstanceName_Returns400() throws Exception {
        String requestBody = """
                {
                    "instanceType": "t2.micro",
                    "region": "us-east-1",
                    "amiId": "ami-0c02fb55956c7d316",
                    "instanceName": "ab"
                }
                """;

        mockMvc.perform(post("/api/infrastructure/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.instanceName").exists());
    }

    // ─── GET /status/{requestId} ─────────────────────────────────────────────────

    @Test
    void getStatus_ExistingId_Returns200() throws Exception {
        InfrastructureResponse mockResponse = InfrastructureResponse.builder()
                .requestId("existing-id")
                .status(ProvisioningStatus.SUCCESS)
                .instanceId("i-0abcdef1234567890")
                .publicIp("54.1.2.3")
                .build();

        when(orchestrator.getStatus("existing-id")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/infrastructure/status/existing-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.instanceId").value("i-0abcdef1234567890"));
    }

    @Test
    void getStatus_UnknownId_Returns404() throws Exception {
        when(orchestrator.getStatus("unknown-id"))
                .thenThrow(new ResourceNotFoundException("InfrastructureRequest", "unknown-id"));

        mockMvc.perform(get("/api/infrastructure/status/unknown-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ─── GET /history ─────────────────────────────────────────────────────────────

    @Test
    void getHistory_Returns200WithList() throws Exception {
        when(orchestrator.getAllRequests()).thenReturn(List.of());

        mockMvc.perform(get("/api/infrastructure/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
