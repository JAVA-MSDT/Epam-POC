# Jira Analysis Agent

A production-ready AI agent that automatically analyzes Jira tickets using Spring AI and a local Ollama LLM. Submit a
ticket ID and the agent retrieves the ticket from Jira, applies a pluggable prompt template, and returns a fully
structured JSON analysis covering requirements, technical complexity, risk assessment, effort estimation, and a phased
implementation strategy.

## Features

- **Full REST API** — analysis, security, custom-prompt, and plugin-management endpoints out of the box
- **Real Jira integration** — `WebClient`-based Jira REST API calls with graceful stub fallback when credentials are not
  configured
- **Pluggable prompt system** — swap or override prompt templates at runtime without redeployment
- **Hot reload** — file watcher detects changes to external prompt files and invalidates the cache within seconds
- **Multiple loading strategies** — `EXTERNAL_FIRST`, `INTERNAL_FIRST`, or `EXTERNAL_ONLY` prompt resolution
- **Caffeine caching** — two-level cache (prompts + analysis results) with automatic refresh and stats
- **Structured AI output** — Spring AI maps LLM response directly to the `TicketAnalysis` Java record
- **Observability** — Micrometer tracing, `@Timed` metrics, `@Observed` spans, and Zipkin-compatible export
- **Global error handling** — clean JSON error responses for 400 and 500 scenarios

## Tech Stack

| Layer         | Technology                                       |
|---------------|--------------------------------------------------|
| Language      | Java 21                                          |
| Framework     | Spring Boot 3.5.0                                |
| AI            | Spring AI 1.0.0 + Ollama (`llama3.1:8b`)         |
| HTTP Client   | Spring WebFlux WebClient (Jira integration)      |
| Caching       | Caffeine 3.2.0                                   |
| File Watching | directory-watcher 0.18.0                         |
| Observability | Micrometer Tracing, Brave, Zipkin                |
| Testing       | JUnit 5, Spring Boot Test, Testcontainers 1.20.4 |
| Build         | Maven 3.9+, Spring Boot Buildpacks (JVM 21)      |

## Project Structure

```
jira-analysis-agent/
├── external-config/
│   └── prompts/                        # Runtime prompt overrides (hot-reloaded)
├── docs/
│   └── plugin-stats/README.md          # Plugin system deep-dive documentation
├── src/
│   ├── main/
│   │   ├── java/com/javamsdt/agent/
│   │   │   ├── JiraAnalysisAgentApplication.java
│   │   │   ├── config/
│   │   │   │   ├── JiraConfig.java                    # WebClient bean with Basic Auth
│   │   │   │   ├── JiraProperties.java                # @ConfigurationProperties jira.*
│   │   │   │   └── plugin/
│   │   │   │       ├── PluginManager.java             # Generic plugin interface
│   │   │   │       ├── PromptPlugin.java              # Immutable prompt record
│   │   │   │       └── OptimizedPromptPluginManager.java  # Caffeine-cached implementation
│   │   │   ├── controller/
│   │   │   │   ├── AnalysisController.java            # /api/v1/analysis endpoints
│   │   │   │   ├── PluginController.java              # /api/v1/plugins endpoints
│   │   │   │   └── MonitoringController.java          # /api/v1/monitoring endpoints
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java        # 400 / 500 error responses
│   │   │   ├── model/
│   │   │   │   ├── TicketAnalysis.java                # Full analysis output record
│   │   │   │   ├── AnalysisRequest.java               # Custom analysis request body
│   │   │   │   └── ErrorResponse.java                 # Structured error JSON
│   │   │   ├── service/
│   │   │   │   └── OptimizedJiraAnalysisService.java  # Core AI orchestration
│   │   │   └── tools/
│   │   │       ├── JiraRetrievalTool.java             # retrieve_jira_ticket / search_jira_tickets
│   │   │       └── FileSystemTool.java                # readFile / listDirectory / create_ticket_folder
│   │   └── resources/
│   │       ├── application.yml
│   │       └── prompts/default/
│   │           ├── analysis-prompt.md
│   │           ├── code-review-prompt.md
│   │           ├── risk-assessment-prompt.md
│   │           ├── effort-estimation-prompt.md
│   │           └── security-analysis-prompt.md
│   └── test/
│       ├── java/com/javamsdt/agent/
│       │   └── JiraAnalysisAgentApplicationTests.java
│       └── resources/
│           └── application-test.yml
└── pom.xml
```

---

## Getting Started

### Prerequisites

| Requirement  | Details                                                                                                                  |
|--------------|--------------------------------------------------------------------------------------------------------------------------|
| Java         | 21+                                                                                                                      |
| Maven        | 3.9+                                                                                                                     |
| Ollama       | Running at `http://localhost:11434`                                                                                      |
| llama3.1:8b  | `ollama pull llama3.1:8b`                                                                                                |
| Jira account | API token from `id.atlassian.com/manage-profile/security/api-tokens` (optional — agent works without it using stub data) |

### 1. Install Ollama and pull the model

```bash
# macOS / Linux
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3.1:8b

# Windows — download from https://ollama.com/download, then:
ollama pull llama3.1:8b
```

### 2. Configure Jira credentials (optional)

Without credentials the agent starts and analyzes using stub ticket data. To connect to a real Jira instance set the
following environment variables before starting:

```bash
export JIRA_BASE_URL=https://your-company.atlassian.net
export JIRA_USERNAME=your@email.com
export JIRA_API_TOKEN=your-api-token
```

On Windows:

```cmd
set JIRA_BASE_URL=https://your-company.atlassian.net
set JIRA_USERNAME=your@email.com
set JIRA_API_TOKEN=your-api-token
```

### 3. Build and run

```bash
git clone <repository-url>
cd jira-analysis-agent
mvn clean package -DskipTests
mvn spring-boot:run
```

Or run the packaged jar:

```bash
java -jar target/jira-analysis-agent-1.0.0.jar
```

### 4. Verify the agent is up

```bash
curl http://localhost:8080/api/v1/monitoring/health
```

Expected:

```json
{
  "status": "UP",
  "service": "jira-analysis-agent",
  "timestamp": 1717891200000
}
```

### Docker

```bash
mvn spring-boot:build-image
docker run -p 8080:8080 \
  -e JIRA_BASE_URL=https://your-company.atlassian.net \
  -e JIRA_USERNAME=your@email.com \
  -e JIRA_API_TOKEN=your-token \
  jira-analysis-agent:1.0.0
```

---

## Configuration

All properties support environment variable overrides (shown in brackets).

| Property                                    | Env var                 | Default                  | Description                                           |
|---------------------------------------------|-------------------------|--------------------------|-------------------------------------------------------|
| `spring.ai.ollama.base-url`                 | `OLLAMA_BASE_URL`       | `http://localhost:11434` | Ollama server URL                                     |
| `spring.ai.ollama.chat.model`               | —                       | `llama3.1:8b`            | Model used for analysis                               |
| `spring.ai.ollama.chat.options.temperature` | —                       | `0.2`                    | Lower = more deterministic output                     |
| `spring.ai.ollama.chat.options.num-ctx`     | —                       | `8192`                   | Context window in tokens                              |
| `jira.base-url`                             | `JIRA_BASE_URL`         | _(empty)_                | Jira instance URL                                     |
| `jira.username`                             | `JIRA_USERNAME`         | _(empty)_                | Jira username or email                                |
| `jira.api-token`                            | `JIRA_API_TOKEN`        | _(empty)_                | Jira API token                                        |
| `agent.output-directory`                    | `AGENT_OUTPUT_DIR`      | `./analysis-output`      | Directory for ticket analysis artifacts               |
| `agent.plugins.external-config-path`        | `AGENT_EXTERNAL_CONFIG` | `./external-config`      | Directory for prompt overrides                        |
| `agent.plugins.strategy`                    | —                       | `EXTERNAL_FIRST`         | `EXTERNAL_FIRST` / `INTERNAL_FIRST` / `EXTERNAL_ONLY` |
| `agent.plugins.hot-reload.enabled`          | `AGENT_HOT_RELOAD`      | `true`                   | Live prompt reload on file change                     |

### application-local.yml (recommended for development)

Create `src/main/resources/application-local.yml` and activate it with `-Dspring-boot.run.profiles=local`:

```yaml
jira:
  base-url: https://your-company.atlassian.net
  username: your@email.com
  api-token: your-api-token

agent:
  output-directory: ./analysis-output
  plugins:
    external-config-path: ./external-config
    hot-reload:
      enabled: true
```

---

## API Reference

### Analysis Endpoints

#### Default analysis

```http
POST /api/v1/analysis/tickets/{ticketId}
```

Analyzes the ticket using the built-in `analysis-prompt`. No request body needed.

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123
```

#### Security analysis

```http
POST /api/v1/analysis/tickets/{ticketId}/security
```

Analyzes the ticket using the `security-analysis-prompt`, focused on OWASP vulnerabilities, data privacy, compliance,
and authentication risks.

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/security
```

#### Custom analysis

```http
POST /api/v1/analysis/tickets/{ticketId}/custom
Content-Type: application/json
```

Analyzes with any registered prompt and optional template variables.

**Request body:**

```json
{
  "promptName": "risk-assessment-prompt",
  "context": {
    "teamSize": "3",
    "sprintLength": "2 weeks"
  }
}
```

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/custom \
  -H "Content-Type: application/json" \
  -d '{"promptName": "effort-estimation-prompt", "context": {"teamSize": "2"}}'
```

#### Cached analysis

```http
GET /api/v1/analysis/tickets/{ticketId}/cached?promptName=analysis-prompt
```

Returns cached result if available, otherwise triggers a new analysis. Results are cached for 10 minutes (Spring
`@Cacheable`).

```bash
curl "http://localhost:8080/api/v1/analysis/tickets/PROJ-123/cached?promptName=analysis-prompt"
```

---

### Plugin Management Endpoints

#### List available plugins

```http
GET /api/v1/plugins/available
```

Returns all plugin names resolvable from classpath and external directory.

```bash
curl http://localhost:8080/api/v1/plugins/available
# ["analysis-prompt","code-review-prompt","effort-estimation-prompt","risk-assessment-prompt","security-analysis-prompt"]
```

#### Reload a single plugin

```http
POST /api/v1/plugins/reload/{pluginName}
```

Invalidates and reloads a specific plugin. Useful after manually editing an external prompt file when hot-reload is
disabled.

```bash
curl -X POST http://localhost:8080/api/v1/plugins/reload/analysis-prompt
# {"status":"reloaded","plugin":"analysis-prompt"}
```

#### Reload all plugins

```http
POST /api/v1/plugins/reload-all
```

Invalidates all cached plugins and re-warms the cache from scratch.

```bash
curl -X POST http://localhost:8080/api/v1/plugins/reload-all
# {"status":"reloaded","count":"all"}
```

---

### Monitoring Endpoints

| Method | Path                              | Description                                         |
|--------|-----------------------------------|-----------------------------------------------------|
| `GET`  | `/api/v1/monitoring/health`       | Agent health — checks plugin manager is responsive  |
| `GET`  | `/api/v1/monitoring/plugin-stats` | Caffeine cache hit rate, load count, eviction count |
| `GET`  | `/actuator/health`                | Spring Boot full health including disk, ping        |
| `GET`  | `/actuator/metrics`               | All Micrometer metrics                              |
| `GET`  | `/actuator/prometheus`            | Prometheus scrape endpoint                          |

```bash
curl http://localhost:8080/api/v1/monitoring/plugin-stats
```

```json
{
  "hitRate": 0.95,
  "missRate": 0.05,
  "loadCount": 24,
  "evictionCount": 0,
  "estimatedSize": 5
}
```

---

## Usage Examples

### Example 1 — Analyze a feature request

```bash
curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/FEAT-456 | jq '.'
```

Extract only the functional requirements:

```bash
curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/FEAT-456 \
  | jq '.requirements_analysis.functional_requirements'
```

```json
[
  "User registration with email verification",
  "Profile management interface",
  "Password reset functionality"
]
```

---

### Example 2 — Security-focused analysis

```bash
curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/SEC-789/security \
  | jq '.risk_assessment.identified_risks[]'
```

```json
{
  "description": "Potential SQL injection in search endpoint",
  "category": "TECHNICAL",
  "impact": "HIGH",
  "probability": "MEDIUM",
  "mitigation_strategy": "Use parameterized queries and input validation",
  "contingency_plan": "Immediate patch + security audit"
}
```

---

### Example 3 — Effort estimation with context

```bash
curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-101/custom \
  -H "Content-Type: application/json" \
  -d '{
    "promptName": "effort-estimation-prompt",
    "context": {
      "teamSize": "3",
      "sprintLength": "2 weeks"
    }
  }' | jq '.effort_estimation'
```

```json
{
  "development_days": 6,
  "testing_days": 2,
  "documentation_days": 1,
  "review_days": 1,
  "total_days": 10,
  "confidence_level": "HIGH",
  "estimation_method": "Story Points",
  "team_size_assumption": 3
}
```

---

### Example 4 — Hot-reload a prompt without restarting

```bash
# 1. Create a custom override
cat > external-config/prompts/analysis-prompt.md << 'EOF'
# Custom Analysis for {{ticketId}}

Retrieve ticket {{ticketId}} using retrieve_jira_ticket.
Focus only on security and performance risks.
Return a complete TicketAnalysis JSON.
EOF

# 2. The watcher picks it up automatically — test immediately
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-999

# 3. Revert — delete the file and the built-in classpath prompt takes over
rm external-config/prompts/analysis-prompt.md
```

---

### Example 5 — Monitor cache performance

```bash
# Watch hit rate every 5 seconds
watch -n 5 'curl -s http://localhost:8080/api/v1/monitoring/plugin-stats | jq ".hitRate"'

# Check analysis timing from Micrometer
curl -s http://localhost:8080/actuator/metrics/jira.analysis.duration | jq '.measurements'

# Health check for uptime monitors
curl -f http://localhost:8080/actuator/health || echo "Service is DOWN"
```

---

## Built-in Prompt Templates

Five prompt templates ship with the agent. All accept the `{{ticketId}}` placeholder and any additional `{{key}}`
variables from the context map.

| Prompt name                | Endpoint shortcut             | Focus                                                             |
|----------------------------|-------------------------------|-------------------------------------------------------------------|
| `analysis-prompt`          | `POST /tickets/{id}`          | Full comprehensive analysis (default)                             |
| `security-analysis-prompt` | `POST /tickets/{id}/security` | OWASP, data privacy, compliance, auth                             |
| `code-review-prompt`       | custom                        | Correctness, security, performance, maintainability, architecture |
| `risk-assessment-prompt`   | custom                        | TECHNICAL / BUSINESS / TIMELINE / RESOURCE risks with score 1–10  |
| `effort-estimation-prompt` | custom                        | Dev / test / doc / review day breakdown with confidence level     |

### Writing a custom prompt

Place a `.md` file in `external-config/prompts/` and reference it by name (without the extension) in the `/custom`
endpoint or plugin reload API.

Rules for custom prompts:

1. Use `retrieve_jira_ticket` to fetch ticket data — this is the tool name registered with the LLM.
2. Use `search_jira_tickets` to find related or blocking tickets.
3. Use `create_ticket_folder` to persist artifacts for the ticket.
4. Always instruct the model to return a JSON object matching the `TicketAnalysis` schema.

Minimal skeleton:

```markdown
# My Custom Analysis for {{ticketId}}

1. Call retrieve_jira_ticket("{{ticketId}}") to fetch ticket details.
2. Analyze the ticket.
3. Return a valid TicketAnalysis JSON object.

Additional context: {{extraContext}}
```

---

## Output Reference

All analysis endpoints return `application/json` matching the `TicketAnalysis` schema:

```json
{
  "ticket_id": "PROJ-123",
  "summary": "Implement user authentication",
  "requirements_analysis": {
    "functional_requirements": [
      "..."
    ],
    "non_functional_requirements": [
      "..."
    ],
    "acceptance_criteria": [
      "..."
    ],
    "dependencies": [
      "..."
    ],
    "assumptions": [
      "..."
    ]
  },
  "technical_analysis": {
    "complexity_score": 7,
    "technical_challenges": [
      "..."
    ],
    "recommended_approach": "...",
    "architecture_considerations": [
      "..."
    ],
    "technology_stack": [
      "..."
    ],
    "performance_considerations": [
      "..."
    ]
  },
  "risk_assessment": {
    "identified_risks": [
      {
        "description": "...",
        "category": "TECHNICAL | BUSINESS | TIMELINE | RESOURCE",
        "impact": "HIGH | MEDIUM | LOW",
        "probability": "HIGH | MEDIUM | LOW",
        "mitigation_strategy": "...",
        "contingency_plan": "..."
      }
    ],
    "overall_risk_level": "MEDIUM",
    "risk_score": 5
  },
  "effort_estimation": {
    "development_days": 5,
    "testing_days": 2,
    "documentation_days": 1,
    "review_days": 1,
    "total_days": 9,
    "confidence_level": "HIGH",
    "estimation_method": "Story Points",
    "team_size_assumption": 2
  },
  "implementation_strategy": {
    "phases": [
      {
        "name": "Phase 1",
        "description": "...",
        "estimated_days": 3,
        "deliverables": [
          "..."
        ],
        "dependencies": [
          "..."
        ],
        "risks": [
          "..."
        ]
      }
    ],
    "key_milestones": [
      {
        "name": "...",
        "description": "...",
        "target_date": "...",
        "success_criteria": [
          "..."
        ]
      }
    ],
    "success_criteria": [
      "..."
    ],
    "rollback_strategy": "..."
  },
  "analysis_metadata": {
    "analysis_timestamp": "2024-06-09T10:30:00",
    "model_used": "llama3.1:8b",
    "analysis_version": "1.0.0",
    "processing_time_ms": 2500,
    "prompt_name": "analysis-prompt",
    "prompt_source": "INTERNAL_RESOURCE",
    "prompt_last_modified": "2024-06-09T09:00:00"
  }
}
```

---

## How to Contribute

1. Fork the repository and create a feature branch from `main`.
2. Make your changes, keeping commits focused and descriptive.
3. Add or update tests for any new behaviour.
4. Ensure the build passes: `mvn verify`
5. Open a pull request with a clear description of what changed and why.

Code conventions: no Lombok, no unnecessary comments, Java records for immutable data, constructor injection throughout.

---

## Author

Ahmed Samy — [@javamsdt](https://github.com/javamsdt)
