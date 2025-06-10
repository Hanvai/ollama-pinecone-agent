#!/bin/bash

# Pinecone Configuration

# Print configuration
echo "Environment variables set:"
echo "PINECONE_API_KEY: ${PINECONE_API_KEY:0:8}..."
echo "PINECONE_ENVIRONMENT: ${PINECONE_ENVIRONMENT}"
echo "PINECONE_INDEX_NAME: ${PINECONE_INDEX_NAME}"

# Make the script executable
chmod +x setup-env.sh
