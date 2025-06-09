package com.agent.agent.core;

import com.agent.agent.memory.MemoryService;
import com.agent.service.OllamaService;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class BaseAgent implements Agent {
    private final MemoryService memoryService;
    private final OllamaService ollamaService;
    private AgentState state;

    public BaseAgent(MemoryService memoryService, OllamaService ollamaService) {
        this.memoryService = memoryService;
        this.ollamaService = ollamaService;
        this.state = AgentState.IDLE;
    }

    @Override
    public CompletableFuture<String> processTask(String task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                state = AgentState.PROCESSING;
                
                // Retrieve relevant memories for context
                var relevantMemories = memoryService.retrieveSimilarMemories(task, 5);
                
                // Build prompt with context
                StringBuilder prompt = new StringBuilder();
                prompt.append("Task: ").append(task).append("\n\n");
                if (!relevantMemories.isEmpty()) {
                    prompt.append("Relevant context:\n");
                    relevantMemories.forEach(memory -> prompt.append("- ").append(memory).append("\n"));
                }
                prompt.append("\nPlease process this task considering the above context.");
                
                // Get response from Ollama
                String result = ollamaService.getChatCompletion(prompt.toString());
                
                // Store the result in memory
                Map<String, String> metadata = Map.of(
                    "type", "result",
                    "task", task
                );
                memoryService.storeMemory(result, metadata);
                
                state = AgentState.IDLE;
                return result;
            } catch (Exception e) {
                state = AgentState.ERROR;
                throw new RuntimeException("Error processing task: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public AgentState getState() {
        return state;
    }

    @Override
    public void updateMemory(String information) {
        memoryService.storeMemory(information, Map.of("type", "manual_update"));
    }
} 