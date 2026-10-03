#!/bin/bash
docker compose up -d postgres ollama
sleep 5
docker exec $(docker ps -qf "name=ollama") ollama pull llama3.1:8b || true
docker exec $(docker ps -qf "name=ollama") ollama pull nomic-embed-text || true
docker compose up --build -d app
