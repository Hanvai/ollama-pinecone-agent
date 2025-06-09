package com.agent.api.controllers;

import com.agent.service.OllamaService;
import com.agent.agent.memory.PineconeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final OllamaService ollamaService;
    private final PineconeService pineconeService;

    public HealthController(OllamaService ollamaService, PineconeService pineconeService) {
        this.ollamaService = ollamaService;
        this.pineconeService = pineconeService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> services = new HashMap<>();
        
        // Check Ollama
        try {
            String testPrompt = "Hello";
            ollamaService.getEmbeddings(testPrompt);
            services.put("ollama", Map.of(
                "status", "UP",
                "model", ollamaService.getModelName()
            ));
        } catch (Exception e) {
            services.put("ollama", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }

        // Check Pinecone
        try {
            String testUrl = pineconeService.getBaseUrl();
            services.put("pinecone", Map.of(
                "status", "UP",
                "index", pineconeService.getIndexName()
            ));
        } catch (Exception e) {
            services.put("pinecone", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }

        health.put("status", services.values().stream().allMatch(s -> "UP".equals(((Map)s).get("status"))) ? "UP" : "DOWN");
        health.put("services", services);
        
        return ResponseEntity.ok(health);
    }
} 