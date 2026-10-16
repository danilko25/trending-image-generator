# Trending Image Generator

Asynchronous merch-design generation pipeline built on Kafka. It orchestrates LLM-based trend filtering, image prompt generation and calls to external AI APIs (Groq, fal.ai).

Input is a trend string and a set of image styles. Output is one design (image plus metadata) per style, with optional post-processing (background removal, upscaling).

## Pipeline

```
POST /api/v1/generation/start
        |
        v
  GenerationTask (PENDING)
        |  after commit
        v
  Kafka: task topic
        |
        v
  Trend processing: LLM filter -> Niche
        |  one subtask per style
        v
  Kafka: image-prompt topic
        |
        v
  Prompt generation (LLM)
        |  after commit
        v
  Kafka: image topic
        |
        v
  Image generation (fal.ai) -> Design
        |
        v
  Task COMPLETED / FAILED
```

Task: `PENDING` -> `FILTERING` -> `GENERATING` -> `COMPLETED` / `FAILED`

Subtask: `PENDING` -> `PROMPT_GENERATING` -> `PROMPT_GENERATED` -> `IMAGE_GENERATING` -> `COMPLETED` / `FAILED`

Trend filtering is optional (`filter` flag). When enabled, the LLM classifies a trend as `KEPT`, `GENERALIZED` (brand-free niche, e.g. "Starbucks pumpkin spice" -> "cozy autumn drinks aesthetic") or `FILTERED` (rejected: tragedies, real people, copyrighted works). A rejected trend fails the task.

Post-processing (background removal, upscaling) is not routed through Kafka. It is triggered by a `PUT` request and runs in-process on virtual threads after the transaction commits. Status is tracked per operation in `processed_image`.

## Engineering notes

- **Events after commit.** Kafka messages are sent from `@TransactionalEventListener(AFTER_COMMIT)`, so a consumer never receives an ID that is not yet visible in the database.
- **Idempotent consumers.** Every listener moves the entity forward with a conditional `UPDATE ... WHERE status = :old`. If it affects 0 rows, the message is a duplicate or arrived in a wrong state and is skipped. If processing throws, the status is rolled back to the previous value so the retry can run.
- **Retries and DLT.** `@RetryableTopic` with a fixed backoff and 2 attempts. `EntityNotFoundException` is excluded from retries. The DLT handler marks the task or subtask as `FAILED` and stores the error message.
- **Task completion under concurrency.** When a subtask finishes (or fails), the parent task row is locked with `SELECT ... FOR UPDATE` before comparing finished and total subtask counts. Without the lock, two subtasks finishing at the same time can both see an incomplete count and the task never completes.
- **Limiting external calls.** Post-processing uses `Executors.newVirtualThreadPerTaskExecutor()` and a `Semaphore` (`parallel.connections.count`) around each API call.
- **Provider boundaries.** Image generation, background removal and upscaling sit behind interfaces (`DesignGeneratorClient`, `RemoveBackgroundClient`, `ImageUpscaleClient`). Processors are selected by `ImageProcessorFactory`. fal.ai is the only implementation.
- **Schema.** Flyway migrations, `ddl-auto: validate`.

## Stack

- Java 21, Spring Boot, Spring Data JPA
- Spring AI (OpenAI-compatible client, Groq `openai/gpt-oss-120b`), fal.ai Java client
- Apache Kafka (Spring Kafka)
- PostgreSQL 16, Flyway
- JUnit 5, Mockito, AssertJ, Awaitility, Testcontainers

## Running locally

Requirements: JDK 21+, Docker, Groq API key, fal.ai API key.

```bash
# PostgreSQL :5433, Kafka :19092, Kafka UI :8081
docker compose up -d

export GROQ_API_KEY=...
export FAL_KEY=...

./mvnw spring-boot:run
```

PowerShell: `$env:GROQ_API_KEY="..."`, `$env:FAL_KEY="..."`.

The app listens on `8086`. Migrations run on startup and insert a set of default image styles.

Main settings (`src/main/resources/application.yaml`):

| Property | Description |
|---|---|
| `spring.datasource.*` | PostgreSQL connection |
| `spring.kafka.bootstrap-servers` | Kafka address |
| `spring.ai.openai.*` | LLM base URL, key, model |
| `fal.*-model-endpoint` | fal.ai models: generation, background removal, upscale |
| `parallel.connections.count` | Max parallel calls to image APIs |
| `*.generation.topic` | Kafka topic names |

## API

Generation:

```bash
curl -X POST http://localhost:8086/api/v1/generation/start \
  -H "Content-Type: application/json" \
  -d '{"rawTrend": "cozy autumn coffee", "styleIds": ["<style-uuid>"], "filter": true}'
```

`rawTrend`: 5-100 characters. `styleIds`: non-empty. Style IDs come from `GET /api/v1/image-style/dropdown`. Returns `201`.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/generation/start` | Create a generation task |
| GET | `/api/v1/design/card` | Paginated design cards |
| GET | `/api/v1/design/{id}/short` | Design summary and processing statuses |
| GET | `/api/v1/design/{id}/full` | Design with prompt details |
| PUT | `/api/v1/design/{id}/remove-background` | Remove background |
| PUT | `/api/v1/design/{id}/upscale` | Upscale original |
| PUT | `/api/v1/design/{id}/upscale-removed-background` | Upscale image with removed background |
| GET | `/api/v1/niche/search` | Search niches |
| GET | `/api/v1/niche/{id}/details` | Niche details |
| GET | `/api/v1/image-style/dropdown` | Styles for selection |
| GET | `/api/v1/image-style/table` | Paginated styles |
| GET | `/api/v1/image-style/{id}/details` | Style details |
| POST | `/api/v1/image-style` | Create style |
| POST | `/api/v1/image-style/{id}` | Update style |
| PUT | `/api/v1/image-style/{id}/enable`, `/disable` | Toggle style |

## Tests

```bash
./mvnw test
```

Requires Docker. Tests are integration tests against real PostgreSQL and Kafka (Testcontainers); only the LLM and image APIs are mocked. They cover the full Kafka pipeline (task -> subtasks -> prompt -> image -> `COMPLETED`), duplicate delivery, retry and DLT handling, trend filtering outcomes, post-processing flows, and the service layer for styles, niches, prompts and designs.

## Layout

```
src/main/java/com/ai/generation/
  common/            shared request objects
  config/            beans: AI clients, executors, Jackson, CORS, events
  design/            design entity, API, generation client
  exception/         custom exceptions, global handler
  generation/        entry point: start generation
  generationtask/    task/subtask entities, repositories, status transitions
  image/             background removal, upscaling, async processing
  imageprompt/       prompt generation
  imagestyle/        styles CRUD and search
  kafka/             producer, listeners, events, retry/DLT
  niche/             niches and search
  trendprocessing/   LLM trend filtering, Google Trends RSS client
```
