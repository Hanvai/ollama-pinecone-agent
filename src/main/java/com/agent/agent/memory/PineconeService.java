package com.agent.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import org.springframework.web.client.RestTemplate;

@Service
public class PineconeService {
    private static final Logger logger = LoggerFactory.getLogger(PineconeService.class);

    @Value("${pinecone.environment}")
    private String environment;
    
    @Value("${pinecone.index.name}")
    private String indexName;
    
    @Value("${pinecone.api.key}")
    private String apiKey;
    
    @Value("${pinecone.api.url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private static final int VECTOR_DIMENSION = 1024; // Updated to match Pinecone index configuration
    private static final String INDEX_IDENTIFIER = "anki83u"; // Specific identifier for your index

    public PineconeService(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.httpClient = HttpClients.createDefault();
        logger.info("PineconeService initialized with environment: {}, index: {}", environment, indexName);
        logger.debug("API Key (first 8 chars): {}", apiKey != null ? apiKey.substring(0, 8) + "..." : "null");
    }

    public String getIndexName() {
        return indexName;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getBaseUrl() {
        if (environment == null || indexName == null) {
            String error = "Pinecone environment and index name must be configured";
            logger.error(error);
            throw new IllegalStateException(error);
        }
        // Use the exact host URL from your index
        String baseUrl = String.format("https://%s-%s.svc.%s.pinecone.io", 
            indexName, 
            INDEX_IDENTIFIER,  // Use the specific identifier instead of random UUID
            environment);
        logger.debug("Generated Pinecone base URL: {}", baseUrl);
        return baseUrl;
    }

    public void upsertVectors(List<Map<String, Object>> vectors) {
        try {
            if (apiKey == null) {
                String error = "Pinecone API key must be configured";
                logger.error(error);
                throw new IllegalStateException(error);
            }

            String url = getBaseUrl() + "/vectors/upsert";
            logger.info("Upserting vectors to URL: {}", url);
            
            HttpPost request = new HttpPost(url);
            request.setHeader("Api-Key", apiKey);
            request.setHeader("Content-Type", "application/json");

            Map<String, Object> payload = new HashMap<>();
            payload.put("vectors", vectors);

            String payloadJson = objectMapper.writeValueAsString(payload);
            logger.debug("Request payload: {}", payloadJson);
            logger.debug("Request headers: Api-Key={}, Content-Type={}", 
                apiKey.substring(0, 8) + "...", 
                request.getFirstHeader("Content-Type").getValue());

            StringEntity entity = new StringEntity(payloadJson, ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                logger.info("Response status: {}", statusCode);
                logger.debug("Response body: {}", responseBody);

                if (statusCode != 200) {
                    String error = String.format("Pinecone API returned status code %d: %s", statusCode, responseBody);
                    logger.error(error);
                    throw new RuntimeException(error);
                }
                return null;
            });
            logger.info("Successfully upserted {} vectors", vectors.size());
        } catch (Exception e) {
            String error = "Error upserting vectors to Pinecone: " + e.getMessage();
            logger.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    public List<Map<String, Object>> queryVectors(List<Float> vector, int topK) {
        try {
            if (apiKey == null) {
                String error = "Pinecone API key must be configured";
                logger.error(error);
                throw new IllegalStateException(error);
            }

            String url = getBaseUrl() + "/query";
            logger.info("Querying vectors from URL: {}", url);
            
            HttpPost request = new HttpPost(url);
            request.setHeader("Api-Key", apiKey);
            request.setHeader("Content-Type", "application/json");

            Map<String, Object> payload = new HashMap<>();
            payload.put("vector", vector);
            payload.put("topK", topK);
            payload.put("includeMetadata", true);

            String payloadJson = objectMapper.writeValueAsString(payload);
            logger.debug("Request payload: {}", payloadJson);
            logger.debug("Request headers: Api-Key={}, Content-Type={}", 
                apiKey.substring(0, 8) + "...", 
                request.getFirstHeader("Content-Type").getValue());

            StringEntity entity = new StringEntity(payloadJson, ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                logger.info("Response status: {}", statusCode);
                logger.debug("Response body: {}", responseBody);

                if (statusCode != 200) {
                    String error = String.format("Pinecone API returned status code %d: %s", statusCode, responseBody);
                    logger.error(error);
                    throw new RuntimeException(error);
                }

                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                Object matches = responseMap.get("matches");
                if (matches instanceof List<?> list) {
                    List<Map<String, Object>> results = list.stream()
                            .filter(item -> item instanceof Map)
                            .map(item -> (Map<String, Object>) item)
                            .toList();
                    logger.info("Successfully retrieved {} matches from Pinecone", results.size());
                    return results;
                }
                logger.warn("No matches found in Pinecone response");
                return Collections.emptyList();
            });
        } catch (Exception e) {
            String error = "Error querying vectors from Pinecone: " + e.getMessage();
            logger.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    public void deleteVectors(List<String> ids) {
        try {
            if (apiKey == null) {
                String error = "Pinecone API key must be configured";
                logger.error(error);
                throw new IllegalStateException(error);
            }

            String url = getBaseUrl() + "/vectors/delete";
            logger.info("Deleting vectors from URL: {}", url);
            
            HttpPost request = new HttpPost(url);
            request.setHeader("Api-Key", apiKey);
            request.setHeader("Content-Type", "application/json");

            Map<String, Object> payload = new HashMap<>();
            payload.put("ids", ids);

            String payloadJson = objectMapper.writeValueAsString(payload);
            logger.debug("Request payload: {}", payloadJson);
            logger.debug("Request headers: Api-Key={}, Content-Type={}", 
                apiKey.substring(0, 8) + "...", 
                request.getFirstHeader("Content-Type").getValue());

            StringEntity entity = new StringEntity(payloadJson, ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                logger.info("Response status: {}", statusCode);
                logger.debug("Response body: {}", responseBody);

                if (statusCode != 200) {
                    String error = String.format("Pinecone API returned status code %d: %s", statusCode, responseBody);
                    logger.error(error);
                    throw new RuntimeException(error);
                }
                return null;
            });
            logger.info("Successfully deleted {} vectors", ids.size());
        } catch (Exception e) {
            String error = "Error deleting vectors from Pinecone: " + e.getMessage();
            logger.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    public void upsertVector(String id, List<Float> values, Map<String, Object> metadata) {
        Map<String, Object> vector = new HashMap<>();
        vector.put("id", id);
        vector.put("values", values);
        vector.put("metadata", metadata);

        // Check if the document already exists
        List<Map<String, Object>> existingVectors = queryVectors(values, 1);
        if (!existingVectors.isEmpty()) {
            // Update the existing document
            updateVector(id, values, metadata);
        } else {
            // Insert the new document
            insertVector(vector);
        }
    }

    private void updateVector(String id, List<Float> values, Map<String, Object> metadata) {
        Map<String, Object> vector = new HashMap<>();
        vector.put("id", id);
        vector.put("values", values);
        vector.put("metadata", metadata);

        // Call Pinecone's update API
        restTemplate.postForObject(baseUrl + "/vectors/update", vector, Map.class);
    }

    private void insertVector(Map<String, Object> vector) {
        restTemplate.postForObject(baseUrl + "/vectors/upsert", vector, Map.class);
    }
} 