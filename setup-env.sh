#!/bin/bash

# Pinecone Configuration
export PINECONE_API_KEY="pcsk_Jdu7h_uvTbCjyovgBvVmw3ySfjvpDcHnmxKbJ1CwctdVZx589HfKRMx8tmKminJJmRaw"
export PINECONE_ENVIRONMENT="aped-4627-b74a"  # From your Pinecone host URL
export PINECONE_INDEX_NAME="agent-memory-v1"  # Your actual index name

# Print configuration
echo "Environment variables set:"
echo "PINECONE_API_KEY: ${PINECONE_API_KEY:0:8}..."
echo "PINECONE_ENVIRONMENT: ${PINECONE_ENVIRONMENT}"
echo "PINECONE_INDEX_NAME: ${PINECONE_INDEX_NAME}"

# Make the script executable
chmod +x setup-env.sh