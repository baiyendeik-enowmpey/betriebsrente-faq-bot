# Betriebsrente FAQ Bot

A locally hosted, German-language question-answering application for internal teams working with occupational pensions (*Betriebsrente*). The bot retrieves passages from approved knowledge documents and uses a local large language model to draft an answer with source references.

> Answers are generated from the knowledge currently stored in the application. Review important legal, tax, and payroll decisions with the responsible specialist.

## What the application does

- Answers questions using retrieval-augmented generation (RAG) over stored occupational-pension knowledge.
- Displays the source documents used to answer a question.
- Lets administrators upload PDF, DOC/DOCX, TXT, and Markdown knowledge documents.
- Splits and indexes document text in PostgreSQL with pgvector for semantic retrieval.
- Imports the bundled `testdaten/fachwissen_seed.json` knowledge set into an empty database on first startup.
- Provides separate team and administrator roles, with an administrator page for uploading and deleting knowledge documents.
- Includes a logout button in the chat interface.

The application uses Spring Boot, Spring AI, Ollama, PostgreSQL/pgvector, and Thymeleaf. Ollama runs the chat model `llama3.1:8b` and embedding model `nomic-embed-text`.

## Run with Docker Compose

### Requirements

- Docker Desktop (or Docker Engine) with Docker Compose
- Internet access to download the container images and Ollama models on first setup
- Sufficient memory and disk space for the models; the `llama3.1:8b` model is several gigabytes

Start the database and Ollama first:

```sh
docker compose up -d postgres ollama
```

Download both required models into the persistent Ollama volume:

```sh
docker compose exec -T ollama ollama pull llama3.1:8b
docker compose exec -T ollama ollama pull nomic-embed-text
```

Build and start the application:

```sh
docker compose up --build -d app
```

Wait for the application to finish starting, then open <http://localhost:8080>. The login page is at <http://localhost:8080/login>. On its first run with an empty database, the application imports the bundled seed knowledge automatically; this can take a few minutes while embeddings are generated.

### Default local accounts

| Role | Username | Password | Access |
| --- | --- | --- | --- |
| Team | `team` | `Willkommen2024!` | Chat |
| Administrator | `fachbereich` | `Betriebsrente2024!` | Chat and knowledge administration |

These are development defaults, not production credentials. Change them before exposing the service to other users or networks. The current security configuration uses in-memory users and `{noop}` password encoding; it is intended for local development and must be replaced with an appropriate production identity and password setup.

### Useful Compose commands

```sh
docker compose ps
docker compose logs -f app
docker compose logs -f ollama
docker compose stop
docker compose down
```

The PostgreSQL and Ollama data are stored in named Docker volumes. `docker compose down` stops and removes the containers and network but preserves those volumes. To remove the persisted database and downloaded models as well:

```sh
docker compose down -v
```

This permanently deletes local database contents and downloaded Ollama models.

## Use the application

1. Sign in with the team account to ask a question.
2. Select a category or leave the category set to **Alle**.
3. Enter a question in German and submit it.
4. Review the answer and the source passages shown beside the chat.
5. Sign in with the administrator account and visit <http://localhost:8080/admin> to upload or remove knowledge documents.

Text is extracted from PDFs and Word documents before indexing. A scanned PDF without an embedded text layer may not contain enough extractable text for upload; the admin page reports this case.

## Run locally without the app container

Install JDK 25, Maven, Docker, and Ollama. Start PostgreSQL and Ollama with Compose, pull the two models as described above, then run Spring Boot from the project root:

```sh
docker compose up -d postgres ollama
docker compose exec -T ollama ollama pull llama3.1:8b
docker compose exec -T ollama ollama pull nomic-embed-text
mvn spring-boot:run
```

The default local configuration connects to PostgreSQL at `localhost:5432` and Ollama at `localhost:11434`. The seed importer checks the project-relative `testdaten/fachwissen_seed.json` file.

## Configuration

Default settings are in `src/main/resources/application.yml`; the Compose service connection settings are in `docker-compose.yml`.

| Setting | Default |
| --- | --- |
| Application URL | `http://localhost:8080` |
| PostgreSQL database | `betriebsrente` |
| PostgreSQL user | `botuser` |
| Chat model | `llama3.1:8b` |
| Embedding model | `nomic-embed-text` |
| Ollama model load timeout | 10 minutes |
| Chunk size / overlap | 650 tokens / 20% |

Spring configuration can be overridden using its environment-variable equivalents, such as `SPRING_DATASOURCE_URL`, `SPRING_AI_OLLAMA_BASE_URL`, `SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL`, `APP_SECURITY_TEAMUSER`, and `APP_SECURITY_TEAMPASS`. For a Compose deployment, set the corresponding values on the `app` service; keep the database credentials and datasource settings consistent.

For local CPU-only deployments, generating answers with the 8B model can be slow, especially on first load. GPU acceleration or a smaller compatible chat model can improve response time; if changing models, update the Ollama model configuration and ensure the selected models are pulled before asking questions.

## Build and tests

Use JDK 25:

```sh
mvn verify
```

This compiles the project, runs the JUnit tests, packages the application, and generates JaCoCo coverage reports under `target/site/jacoco/` (HTML and XML). Run one test class with:

```sh
mvn -Dtest=RagServiceTest test
```

## Main project paths

```text
src/main/java/                 Application, controllers, RAG, ingestion, and services
src/main/resources/templates/  Thymeleaf login, chat, and admin pages
src/main/resources/db/         Flyway database migrations
testdaten/                     Bundled seed knowledge JSON
docker-compose.yml             Local PostgreSQL, Ollama, and app services
Dockerfile                     Multi-stage Java 25 container build
```
