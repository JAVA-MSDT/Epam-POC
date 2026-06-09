# Jira Analysis Agent

## About

A production-ready AI agent that automates deep analysis of Jira tickets using Spring AI and a local Ollama LLM. The
agent retrieves ticket data, applies pluggable prompt templates, and returns structured analysis covering requirements,
technical complexity, risk, effort estimation, and a phased implementation strategy — all via a REST API.

## Features

- **AI-powered ticket analysis** — structured `TicketAnalysis` output including requirements, risks, effort, and
  implementation strategy
- **Pluggable prompt system** — swap or override prompt templates at runtime without redeployment using the external
  config directory
- **High-performance caching** — Caffeine `LoadingCache` with automatic refresh; prompts are loaded once and served from
  memory
- **Hot reload** — file watcher detects changes to external prompt files and invalidates the cache automatically
- **Multiple loading strategies** — `EXTERNAL_FIRST`, `INTERNAL_FIRST`, or `EXTERNAL_ONLY` prompt resolution
- **Cache pre-warming** — default prompts are loaded asynchronously at startup
- **Observability** — Micrometer tracing, `@Timed` metrics, and a `/monitoring` REST endpoint exposing health and cache
  statistics
- **Distributed tracing** — Brave/Zipkin integration for end-to-end request tracing

## Tech Stack

| Layer         | Technology                                |
|---------------|-------------------------------------------|
| Language      | Java 21                                   |
| Framework     | Spring Boot 3.3                           |
| AI            | Spring AI 1.0.0 + Ollama (`llama3.1:8b`)  |
| Vector Store  | pgvector (PostgreSQL)                     |
| Caching       | Caffeine 3.1.8                            |
| File Watching | directory-watcher 0.18.0                  |
| Observability | Micrometer, Brave, Zipkin                 |
| Testing       | JUnit 5, Spring Boot Test, Testcontainers |
| Build         | Maven                                     |
| Runtime       | Docker (Buildpacks, JVM 21)               |

## Project Structure

```
jira-analysis-agent/
├── external-config/
│   └── prompts/                        # Runtime prompt overrides (hot-reloaded)
├── src/
│   ├── main/
│   │   ├── java/com/javamsdt/agent/
│   │   │   ├── JiraAnalysisAgentApplication.java
│   │   │   ├── config/plugin/
│   │   │   │   ├── PluginManager.java              # Generic plugin interface
│   │   │   │   ├── PromptPlugin.java               # Prompt record (name, content, metadata)
│   │   │   │   └── OptimizedPromptPluginManager.java  # Caffeine-cached implementation
│   │   │   ├── controller/
│   │   │   │   └── MonitoringController.java       # /api/v1/monitoring endpoints
│   │   │   ├── model/
│   │   │   │   └── TicketAnalysis.java             # Full analysis output record
│   │   │   ├── service/
│   │   │   │   └── OptimizedJiraAnalysisService.java  # Core analysis orchestration
│   │   │   └── tools/
│   │   │       ├── JiraRetrievalTool.java          # @Tool — fetches Jira tickets
│   │   │       └── FileSystemTool.java             # @Tool — reads local files
│   │   └── resources/
│   │       ├── application.yml
│   │       └── prompts/default/                    # Built-in prompt templates
│   │           ├── analysis-prompt.md
│   │           ├── code-review-prompt.md
│   │           ├── risk-assessment-prompt.md
│   │           └── effort-estimation-prompt.md
│   └── test/
│       ├── java/com/javamsdt/agent/
│       │   └── JiraAnalysisAgentApplicationTests.java
│       └── resources/
│           └── application-test.yml
└── pom.xml
```

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- [Ollama](https://ollama.com) running locally with the `llama3.1:8b` model pulled
- PostgreSQL with the `pgvector` extension (optional — only required for vector store features)

### 1. Pull the model

```bash
ollama pull llama3.1:8b
```

### 2. Clone and build

```bash
git clone <repository-url>
cd jira-analysis-agent
mvn clean package -DskipTests
```

### 3. Run

```bash
mvn spring-boot:run
```

Or with the packaged jar:

```bash
java -jar target/jira-analysis-agent-1.0.0.jar
```

### 4. Verify

```bash
curl http://localhost:8080/api/v1/monitoring/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "jira-analysis-agent",
  "timestamp": 1234567890
}
```

### Docker

Build and run a container image using Spring Boot Buildpacks:

```bash
mvn spring-boot:build-image
docker run -p 8080:8080 jira-analysis-agent:1.0.0
```

## Configuration

All settings live in `src/main/resources/application.yml`.

| Property                             | Default                  | Description                                                                        |
|--------------------------------------|--------------------------|------------------------------------------------------------------------------------|
| `spring.ai.ollama.base-url`          | `http://localhost:11434` | Ollama server URL                                                                  |
| `spring.ai.ollama.chat.model`        | `llama3.1:8b`            | Model to use for analysis                                                          |
| `agent.plugins.external-config-path` | `./external-config`      | Directory scanned for prompt overrides                                             |
| `agent.plugins.strategy`             | `EXTERNAL_FIRST`         | Prompt resolution strategy (`EXTERNAL_FIRST` / `INTERNAL_FIRST` / `EXTERNAL_ONLY`) |
| `agent.plugins.hot-reload.enabled`   | `true`                   | Watch external directory for file changes                                          |

### Overriding a prompt at runtime

Drop a `.md` file into `external-config/prompts/` using the same name as the built-in template, for example:

```
external-config/
└── prompts/
    └── analysis-prompt.md   ← overrides the classpath default
```

The file watcher picks up the change immediately and invalidates the cache — no restart required.

### Monitoring endpoints

| Endpoint                              | Description                                         |
|---------------------------------------|-----------------------------------------------------|
| `GET /api/v1/monitoring/health`       | Service health and plugin availability              |
| `GET /api/v1/monitoring/plugin-stats` | Caffeine cache hit rate, load count, eviction count |
| `GET /actuator/metrics`               | Full Micrometer metrics                             |

## How to Contribute

1. Fork the repository and create a feature branch from `main`.
2. Make your changes, keeping commits focused and descriptive.
3. Add or update tests for any new behaviour.
4. Ensure the build passes: `mvn verify`
5. Open a pull request with a clear description of what was changed and why.

Please follow the existing code style — no Lombok, no unnecessary comments, Java records for immutable data, constructor
injection throughout.

## License

This project is licensed under the [MIT License](LICENSE).

## Author

Ahmed Samy — [@javamsdt](https://github.com/javamsdt)
