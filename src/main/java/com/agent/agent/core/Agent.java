package com.agent.agent.core;

import java.util.concurrent.CompletableFuture;

public interface Agent {
    /**
     * Process a task and return a result
     * @param task The task to process
     * @return A CompletableFuture containing the result
     */
    CompletableFuture<String> processTask(String task);

    /**
     * Get the current state of the agent
     * @return The current state
     */
    AgentState getState();

    /**
     * Update the agent's memory with new information
     * @param information The information to store
     */
    void updateMemory(String information);
} 