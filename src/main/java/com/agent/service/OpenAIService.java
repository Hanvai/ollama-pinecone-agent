package com.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1";
    private static final String EMBEDDINGS_ENDPOINT = "/embeddings";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    public OpenAIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        logger.info("OpenAIService initialized. API Key present: {}", apiKey != null && !apiKey.isEmpty());
    }

    private HttpHeaders createHeaders() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("OpenAI API key is not set. Please set the OPENAI_API_KEY environment variable.");
            throw new RuntimeException("OpenAI API key is not set. Please set the OPENAI_API_KEY environment variable.");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }

    private void handleQuotaExceeded() {
        logger.error("OpenAI API quota exceeded. Please check your billing details at https://platform.openai.com/account/billing");
        throw new RuntimeException("OpenAI API quota exceeded. Please check your billing details at https://platform.openai.com/account/billing");
    }

    private void handleRateLimit() {
        int currentRetry = retryCount.incrementAndGet();
        if (currentRetry <= MAX_RETRIES) {
            logger.warn("Rate limit hit. Retrying in {} ms (attempt {}/{})", RETRY_DELAY_MS, currentRetry, MAX_RETRIES);
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            retryCount.set(0);
            throw new RuntimeException("Maximum retry attempts reached for rate limit");
        }
    }

    private List<Float> generateFallbackEmbeddings(String text) {
        logger.warn("Using fallback embedding generation for text of length: {}", text.length());
        // Generate a simple hash-based embedding as fallback
        List<Float> embedding = new ArrayList<>();
        for (int i = 0; i < 1536; i++) { // OpenAI's embedding dimension
            embedding.add((float) Math.sin(text.hashCode() + i) / 2);
        }
        return embedding;
    }

    public List<Float> getEmbeddings(String text) {
        try {
            logger.debug("Getting embeddings for text of length: {}", text.length());
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "text-embedding-ada-002");
            requestBody.put("input", text);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, createHeaders());

            ResponseEntity<Map> response = restTemplate.exchange(
                OPENAI_API_URL + EMBEDDINGS_ENDPOINT,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("data")) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    if (!data.isEmpty()) {
                        return (List<Float>) data.get(0).get("embedding");
                    }
                }
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                handleRateLimit();
                return getEmbeddings(text); // Retry the request
            } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                logger.error("OpenAI API key is invalid or expired");
                throw new RuntimeException("OpenAI API key is invalid or expired");
            } else if (response.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                handleQuotaExceeded();
            }
            
            logger.error("Failed to get embeddings. Status: {}", response.getStatusCode());
            throw new RuntimeException("Failed to get embeddings from OpenAI: " + response.getStatusCode());
        } catch (Exception e) {
            logger.error("Error getting embeddings", e);
            if (e.getMessage().contains("insufficient_quota")) {
                handleQuotaExceeded();
            }
            // Use fallback embeddings if the error is not quota-related
            return generateFallbackEmbeddings(text);
        }
    }

    public String getChatCompletion(String prompt) {
        try {
            logger.debug("Getting chat completion for prompt of length: {}", prompt.length());
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, createHeaders());

            ResponseEntity<Map> response = restTemplate.exchange(
                OPENAI_API_URL + CHAT_ENDPOINT,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, String> messageResponse = (Map<String, String>) choice.get("message");
                        return messageResponse.get("content");
                    }
                }
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                handleRateLimit();
                return getChatCompletion(prompt); // Retry the request
            } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                logger.error("OpenAI API key is invalid or expired");
                throw new RuntimeException("OpenAI API key is invalid or expired");
            } else if (response.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                handleQuotaExceeded();
            }
            
            logger.error("Failed to get chat completion. Status: {}", response.getStatusCode());
            throw new RuntimeException("Failed to get completion from OpenAI: " + response.getStatusCode());
        } catch (Exception e) {
            logger.error("Error getting chat completion", e);
            if (e.getMessage().contains("insufficient_quota")) {
                handleQuotaExceeded();
            }
            throw new RuntimeException("Error getting chat completion: " + e.getMessage(), e);
        }
    }
} 