package com.agent.api.controllers;

import com.agent.agent.core.Agent;
import com.agent.agent.core.AgentState;
import com.agent.agent.memory.PineconeService;
import com.agent.service.OllamaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Collections;
import java.util.Date;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final Agent agent;
    private final PineconeService pineconeService;
    private final OllamaService ollamaService;
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    public AgentController(Agent agent, PineconeService pineconeService, OllamaService ollamaService) {
        this.agent = agent;
        this.pineconeService = pineconeService;
        this.ollamaService = ollamaService;
    }

    @PostMapping("/task")
    public CompletableFuture<ResponseEntity<String>> processTask(@RequestBody String task) {
        return agent.processTask(task)
            .thenApply(ResponseEntity::ok)
            .exceptionally(e -> ResponseEntity.internalServerError().body("Error processing task: " + e.getMessage()));
    }

    @GetMapping("/state")
    public ResponseEntity<AgentState> getState() {
        return ResponseEntity.ok(agent.getState());
    }

    @PostMapping("/memory")
    public ResponseEntity<Void> updateMemory(@RequestBody String information) {
        agent.updateMemory(information);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test-pinecone")
    public ResponseEntity<?> testPineconeConnection() {
        try {
            // Test vector (1024 dimensions of zeros)
            List<Float> vectorList = new ArrayList<>();
            for (int i = 0; i < 1024; i++) {
                vectorList.add(0.0f);
            }
            
            // Try to query with the test vector
            List<Map<String, Object>> results = pineconeService.queryVectors(vectorList, 1);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Successfully connected to Pinecone");
            response.put("index", pineconeService.getIndexName());
            response.put("environment", pineconeService.getEnvironment());
            response.put("results", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to connect to Pinecone: " + e.getMessage());
            error.put("index", pineconeService.getIndexName());
            error.put("environment", pineconeService.getEnvironment());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/test-embeddings")
    public ResponseEntity<?> testEmbeddings(@RequestBody String text) {
        try {
            // 1. Generate embeddings using Ollama
            List<Float> embeddings = ollamaService.getEmbeddings(text);
            
            // 2. Create a vector with metadata
            Map<String, Object> vector = new HashMap<>();
            vector.put("id", UUID.randomUUID().toString());
            vector.put("values", embeddings);
            vector.put("metadata", Map.of(
                "text", text,
                "timestamp", new Date().toString(),
                "source", "test-embeddings"
            ));

            // 3. Store in Pinecone
            pineconeService.upsertVectors(Collections.singletonList(vector));

            // 4. Query back using the same embeddings
            List<Map<String, Object>> results = pineconeService.queryVectors(embeddings, 5);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Successfully stored and queried embeddings");
            response.put("input_text", text);
            response.put("embedding_dimensions", embeddings.size());
            response.put("stored_vector_id", vector.get("id"));
            response.put("query_results", results);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error testing embeddings: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/test-semantic-search")
    public ResponseEntity<?> testSemanticSearch(@RequestParam String query) {
        try {
            // 1. Generate embeddings for the query
            List<Float> queryEmbeddings = ollamaService.getEmbeddings(query);
            
            // 2. Search in Pinecone
            List<Map<String, Object>> results = pineconeService.queryVectors(queryEmbeddings, 5);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Semantic search completed");
            response.put("query", query);
            response.put("results", results);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error performing semantic search: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/test-batch")
    public ResponseEntity<?> testBatchStorage() {
        List<Map<String, Object>> documents = new ArrayList<>();
        List<String> testTexts = Arrays.asList(
            "Artificial Intelligence (AI) is transforming healthcare by enabling faster diagnosis and personalized treatment plans. Machine learning algorithms can analyze medical images and patient data to identify patterns and predict outcomes.",
            "Climate change is causing rising global temperatures and extreme weather events. Scientists are using AI to model climate patterns and predict future changes, helping governments make informed decisions about environmental policies.",
            "The stock market is influenced by various factors including company performance, economic indicators, and investor sentiment. AI systems can analyze market data to identify trends and make trading recommendations.",
            "Natural Language Processing (NLP) is a branch of AI that focuses on understanding and generating human language. Modern NLP models can translate between languages, summarize text, and answer questions about content.",
            "Robotics combines AI with mechanical engineering to create machines that can perform tasks autonomously. These robots are being used in manufacturing, healthcare, and even space exploration."
        );

        for (String text : testTexts) {
            List<Float> embeddings = ollamaService.getEmbeddings(text);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("text", text);
            metadata.put("timestamp", new Date().toString());
            metadata.put("source", "batch-test");

            // Generate a unique ID based on the document's content
            String id = generateDocumentId(text);

            Map<String, Object> document = new HashMap<>();
            document.put("id", id);
            document.put("text", text);
            document.put("embedding_dimensions", embeddings.size());
            documents.add(document);

            pineconeService.upsertVector(id, embeddings, metadata);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("documents", documents);
        response.put("message", "Successfully stored batch documents");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    private String generateDocumentId(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating document ID", e);
        }
    }

    @GetMapping("/test-batch-search")
    public ResponseEntity<Map<String, Object>> testBatchSearch(@RequestParam String query) {
        try {
            List<Float> queryEmbeddings = ollamaService.getEmbeddings(query);
            List<Map<String, Object>> results = pineconeService.queryVectors(queryEmbeddings, 5);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("query", query);
            response.put("message", "Batch search completed");
            response.put("results", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in batch search: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error in batch search: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 