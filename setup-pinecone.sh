#!/bin/bash

# Prompt for Pinecone API key
echo "Please enter your Pinecone API key:"
read -s PINECONE_API_KEY

# Export the API key
export PINECONE_API_KEY

# Print confirmation (only first 8 characters for security)
echo "Pinecone API key set: ${PINECONE_API_KEY:0:8}..."

# Test the configuration
echo "Testing Pinecone connection..."
curl http://localhost:8080/api/agent/test-pinecone 