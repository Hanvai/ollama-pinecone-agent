package com.agent.config;

import com.agent.agent.core.Agent;
import com.agent.agent.core.BaseAgent;
import com.agent.agent.memory.MemoryService;
import com.agent.agent.memory.PineconeService;
import com.agent.service.EmbeddingService;
import com.agent.service.OllamaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AgentConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    public PineconeService pineconeService(ObjectMapper objectMapper) {
        return new PineconeService(objectMapper);
    }

    @Bean
    @Primary
    public EmbeddingService embeddingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new OllamaService(restTemplate, objectMapper);
    }

    @Bean
    public OllamaService ollamaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new OllamaService(restTemplate, objectMapper);
    }

    @Bean
    public MemoryService memoryService(PineconeService pineconeService, EmbeddingService embeddingService) {
        return new MemoryService(pineconeService, embeddingService);
    }

    @Bean
    public Agent agent(MemoryService memoryService, OllamaService ollamaService) {
        return new BaseAgent(memoryService, ollamaService);
    }
} 