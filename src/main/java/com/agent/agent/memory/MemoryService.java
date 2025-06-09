package com.agent.agent.memory;

import com.agent.service.EmbeddingService;
import org.springframework.stereotype.Service;
import java.util.*;

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
        
        Map<String, Object> vector = new HashMap<>();
        vector.put("id", UUID.randomUUID().toString());
        vector.put("values", embeddings);
        vector.put("metadata", metadata);
        
        pineconeService.upsertVectors(Collections.singletonList(vector));
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