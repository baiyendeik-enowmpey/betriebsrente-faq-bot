# Copilot instructions for betriebsrente-faq-bot

## Build, test, and verification

This repository is a Spring Boot application with a Maven build. There is no dedicated lint task configured in `pom.xml`; the working verification commands are Maven-based.

- Local runtime stack:
  - `docker compose up -d postgres ollama`
  - `docker exec -it $(docker ps -qf name=ollama) ollama pull llama3.1:8b`
  - `docker exec -it $(docker ps -qf name=ollama) ollama pull nomic-embed-text`
  - `docker compose up --build app`
  - `docker compose exec app java -jar app.jar --import=testdaten/fachwissen_seed.json`
- App URL: `http://localhost:8090`
- Build/test commands:
  - `mvn test`
  - `mvn -Dtest=de.team.betriebsrente.rag.RagServiceTest test` for a single test class
  - `mvn -Dtest=RagServiceTest#ask test` for a single test method when the method name is known
  - `mvn package -DskipTests` for a packaging build without running tests
  - UI E2E (requires the app running at `http://localhost:8090` and Playwright Chromium installed):
    - `mvn -Pe2e -Dexec.mainClass=com.microsoft.playwright.CLI "-Dexec.args=install chromium" -Dexec.classpathScope=test exec:java` (one-time browser setup)
    - PowerShell: `$env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD="1"; mvn -Pe2e -Dexec.mainClass=org.testng.TestNG "-Dexec.args=-testclass de.team.betriebsrente.e2e.ChatUiE2ETest" -Dexec.classpathScope=test test-compile exec:java`
    - Set `E2E_BASE_URL` to target a different app URL; login can be overridden with `E2E_USERNAME` and `E2E_PASSWORD`.

## High-level architecture

This project is a Spring Boot FAQ assistant for Betriebsrente questions. The app combines a web UI with a retrieval-augmented generation flow backed by PostgreSQL + pgvector and a local Ollama model.

- `src/main/java/de/team/betriebsrente/config/`: Spring Security and authentication setup (PostgreSQL-backed users, bootstrap accounts, roles, and form-based login).
- `src/main/java/de/team/betriebsrente/controller/`: HTTP layer for the chat UI and admin endpoints.
- `src/main/java/de/team/betriebsrente/rag/`: retrieval logic and ingestion pipeline (`RagService`, `IngestionService`, `ChunkingService`, OCR/PDF parsing, audit export, seed-data initializer).
- `src/main/java/de/team/betriebsrente/model/`: JPA entities such as `Fachwissen` that store the source knowledge in the database.
- `src/main/resources/application.yml`: main runtime configuration for PostgreSQL, Flyway, pgvector, Ollama, and app-level security defaults.
- `docker-compose.yml`: composes the PostgreSQL + pgvector database and the Ollama runtime; the app service depends on them and exposes port 8090.

The main data flow is: JSON knowledge seed -> `IngestionService` -> persisted `Fachwissen` rows plus vector chunks -> `RagService` similarity search -> prompt assembly with source metadata -> final answer from Ollama.

## Key conventions

- The repository uses Spring Boot defaults throughout: constructor injection, `@Service` for logic classes, and `@Controller`/`@ResponseBody` for request handling.
- Authentication is configured centrally in `SecurityConfig`; `/admin/**` and `/api/admin/**` are admin-only, while the rest of the app is available to user/admin roles.
- Knowledge ingestion is driven by JSON files and the `--import` boot argument handled by `AdminImportController`. The expected seed fields are `titel`, `kategorie`, `original_text`, `quelle`, and optional `training_phrases`.
- Chunking is configured with `app.chunk.size` and `app.chunk.overlap` in `application.yml`; source metadata (`titel`, `kategorie`, `quelle`, chunk index) is kept alongside each chunk because the prompt builds citation context from it.
- Flyway is enabled and schema validation is in place (`hibernate.ddl-auto: validate`), so DB schema changes should be handled as migrations rather than ad hoc changes to the database.
- The app uses pgvector-backed similarity search, so any retrieval-related change should preserve metadata and keep the searchable text aligned with the underlying FAQ content.
