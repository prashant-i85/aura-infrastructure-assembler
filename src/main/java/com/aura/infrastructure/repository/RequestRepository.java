package com.aura.infrastructure.repository;

import com.aura.infrastructure.model.InfrastructureResponse;
import com.aura.infrastructure.model.ProvisioningStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RequestRepository {

    private final ConcurrentHashMap<String, InfrastructureResponse> store = new ConcurrentHashMap<>();

    public void save(InfrastructureResponse response) {
        store.put(response.getRequestId(), response);
    }

    public Optional<InfrastructureResponse> findById(String requestId) {
        return Optional.ofNullable(store.get(requestId));
    }

    public List<InfrastructureResponse> findAll() {
        List<InfrastructureResponse> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(InfrastructureResponse::getCreatedAt).reversed());
        return list;
    }

    public void updateStatus(String requestId, ProvisioningStatus status, String message) {
        InfrastructureResponse response = store.get(requestId);
        if (response != null) {
            response.setStatus(status);
            response.setMessage(message);
            response.setUpdatedAt(LocalDateTime.now());
        }
    }

    public boolean exists(String requestId) {
        return store.containsKey(requestId);
    }
}
