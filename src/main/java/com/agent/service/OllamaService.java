package com.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OllamaService implements EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    private static final String OLLAMA_API_URL = "http://localhost:11434/api";
    private static final String EMBEDDINGS_ENDPOINT = "/api/embeddings";
    private static final String CHAT_ENDPOINT = "/chat";
    private static final int DIMENSION = 4096; // Default dimension for most Ollama models
    private static final int TARGET_DIMENSION = 1024; // Pinecone index dimension
    
    @Value("${ollama.model:llama2}")
    private String model;
    
    @Value("${ollama.api.url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        logger.info("OllamaService initialized with model: {}", model);
    }

    @Override
    public List<Float> getEmbeddings(String text) {
        logger.debug("Getting embeddings for text of length: {}", text.length());
        Map<String, String> request = new HashMap<>();
        request.put("model", model);
        request.put("prompt", text);

        String url = baseUrl + EMBEDDINGS_ENDPOINT;
        logger.debug("Ollama embeddings URL: {}", url);
        logger.debug("Ollama embeddings request: {}", request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            requestEntity,
            Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object embeddingObj = response.getBody().get("embedding");
            if (embeddingObj == null) {
                logger.error("No 'embedding' key in Ollama response: {}", response.getBody());
                throw new RuntimeException("No 'embedding' key in Ollama response");
            }
            List<Double> rawEmbeddings = (List<Double>) embeddingObj;
            List<Float> embeddings = rawEmbeddings.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());

            if (embeddings.size() > TARGET_DIMENSION) {
                embeddings = reduceDimensions(embeddings);
            } else if (embeddings.size() < TARGET_DIMENSION) {
                embeddings = padDimensions(embeddings);
            }

            logger.debug("Generated embeddings with dimension: {}", embeddings.size());
            return embeddings;
        } else {
            logger.error("Error getting embeddings from Ollama: {} Body: {}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Error getting embeddings from Ollama");
        }
    }

    private List<Float> reduceDimensions(List<Float> embeddings) {
        // Simple averaging approach to reduce dimensions
        int originalSize = embeddings.size();
        int targetSize = TARGET_DIMENSION;
        List<Float> reduced = new ArrayList<>(targetSize);
        
        for (int i = 0; i < targetSize; i++) {
            float sum = 0;
            int count = 0;
            // Calculate the range of original dimensions to average
            int start = (i * originalSize) / targetSize;
            int end = ((i + 1) * originalSize) / targetSize;
            
            for (int j = start; j < end; j++) {
                sum += embeddings.get(j);
                count++;
            }
            reduced.add(sum / count);
        }
        
        return reduced;
    }

    private List<Float> padDimensions(List<Float> embeddings) {
        // Pad with zeros to reach target dimension
        List<Float> padded = new ArrayList<>(embeddings);
        while (padded.size() < TARGET_DIMENSION) {
            padded.add(0.0f);
        }
        return padded;
    }

    public String getChatCompletion(String prompt) {
        try {
            logger.debug("Getting chat completion for prompt of length: {}", prompt.length());
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Collections.singletonList(Map.of(
                "role", "user",
                "content", prompt
            )));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + CHAT_ENDPOINT,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("message")) {
                    Map<String, String> message = (Map<String, String>) responseBody.get("message");
                    return message.get("content");
                }
            }
            
            logger.error("Failed to get chat completion. Status: {}", response.getStatusCode());
            throw new RuntimeException("Failed to get completion from Ollama: " + response.getStatusCode());
        } catch (Exception e) {
            logger.error("Error getting chat completion", e);
            throw new RuntimeException("Error getting chat completion: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }
} 