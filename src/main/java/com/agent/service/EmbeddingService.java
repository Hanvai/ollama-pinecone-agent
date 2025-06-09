package com.agent.service;

import java.util.List;

public interface EmbeddingService {
    List<Float> getEmbeddings(String text);
    String getModelName();
    int getDimension();
} 