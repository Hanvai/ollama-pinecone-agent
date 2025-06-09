package com.agent.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class LocalEmbeddingService implements EmbeddingService {
    private static final int DIMENSION = 1536; // Same as OpenAI for compatibility

    @Override
    public List<Float> getEmbeddings(String text) {
        try {
            // Generate a hash of the text
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            
            // Convert hash to embedding
            List<Float> embedding = new ArrayList<>(DIMENSION);
            for (int i = 0; i < DIMENSION; i++) {
                // Use different parts of the hash for each dimension
                int hashIndex = i % hash.length;
                float value = (float) (hash[hashIndex] & 0xFF) / 255.0f; // Normalize to 0-1
                embedding.add(value);
            }
            return embedding;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating embeddings", e);
        }
    }

    @Override
    public String getModelName() {
        return "local-hash";
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }
} 