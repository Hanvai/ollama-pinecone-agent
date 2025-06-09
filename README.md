# ollama-pinecone-agent

An intelligent agent system using Ollama for local LLM capabilities and Pinecone for vector-based semantic search and memory management.

## Overview

This project implements an AI agent that leverages:
- **Ollama** (running Llama2 locally) for language understanding and generation.
- **Pinecone** for vector-based semantic search and memory management.

The agent can process tasks, retrieve relevant context from memory, and generate responses using a local LLM.

## Features

- **Task Processing**: Send tasks to the agent, which retrieves relevant memories and generates a response.
- **Embeddings Generation**: Uses Ollama to generate embeddings for text, which are stored in Pinecone.
- **Semantic Search**: Performs vector-based semantic search to find similar memories or documents.
- **Memory Management**: Store, retrieve, and delete memories using Pinecone.
- **REST API**: Exposes endpoints for interaction, documented with Swagger/OpenAPI.

## API Endpoints

- **`/api/agent/task`**: Process a new task with context.
- **`/api/agent/test-embeddings`**: Test embedding generation and storage.
- **`/api/agent/test-semantic-search`**: Test semantic search.
- **`/api/agent/memory`**: Update memory.
- **`/api/health`**: Check health of Ollama and Pinecone services.

## Swagger Documentation

The API is documented using Swagger/OpenAPI. You can access the Swagger UI at:

```
http://localhost:8080/swagger-ui/index.html
```

## Setup

1. **Prerequisites**:
   - Java 17 or higher
   - Maven
   - Ollama running locally (default: http://localhost:11434)
   - Pinecone account and API key

2. **Configuration**:
   - Update `application.properties` with your Pinecone API key, environment, and index name.
   - Ensure Ollama is running and accessible.

3. **Build and Run**:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## Usage

1. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

2. **Access the Swagger UI**:
   Open your browser and navigate to:
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

3. **Test the API**:
   Use the Swagger UI to explore and test the available endpoints.

## License

This project is licensed under the MIT License. See the LICENSE file for details.
