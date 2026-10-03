# Betriebsrente FAQ Bot
docker compose up -d postgres ollama
docker exec -it $(docker ps -qf name=ollama) ollama pull llama3.1:8b
docker exec -it $(docker ps -qf name=ollama) ollama pull nomic-embed-text
docker compose up --build app
docker compose exec app java -jar app.jar --import=testdaten/fachwissen_seed.json
http://localhost:8080
