package com.agent.agent.memory;

import com.agent.service.EmbeddingService;
import org.springframework.stereotype.Service;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class MemoryService {
    private final PineconeService pineconeService;
    private final EmbeddingService embeddingService;

    public MemoryService(PineconeService pineconeService, EmbeddingService embeddingService) {
        this.pineconeService = pineconeService;
        this.embeddingService = embeddingService;
    }

    public void storeMemory(String information, Map<String, String> metadata) {
        List<Float> embeddings = embeddingService.getEmbeddings(information);
        Map<String, Object> fullMetadata = new HashMap<>();
        fullMetadata.put("text", information);
        fullMetadata.put("timestamp", new Date().toString());
        fullMetadata.put("source", "memory");
        fullMetadata.putAll(metadata);

        // Generate a unique ID based on the document's content
        String id = generateDocumentId(information);

        pineconeService.upsertVector(id, embeddings, fullMetadata);
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

    public List<String> retrieveSimilarMemories(String query, int limit) {
        List<Float> queryEmbeddings = embeddingService.getEmbeddings(query);
        
        List<Map<String, Object>> matches = pineconeService.queryVectors(queryEmbeddings, limit);
        return matches.stream()
                .map(match -> (String) match.get("metadata"))
                .toList();
    }

    public void deleteMemory(String id) {
        pineconeService.deleteVectors(Collections.singletonList(id));
    }
} 