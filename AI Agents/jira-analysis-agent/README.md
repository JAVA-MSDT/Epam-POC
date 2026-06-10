# Jira Analysis Agent

A production-ready AI agent that automatically analyzes Jira tickets using Spring AI and a local Ollama LLM. Submit a
ticket ID and the agent fetches the ticket directly from Jira, extracts the essential content, and sends it to the LLM
for a fully structured JSON analysis covering requirements, technical complexity, risk assessment, effort estimation,
and a phased implementation strategy.

## How It Works

The agent uses a **two-phase approach** designed for reliability and speed:

**Phase 1 — Data collection (Java, no LLM)**

1. Calls Jira REST API directly to retrieve the ticket
2. Extracts only the fields the LLM needs: `key`, `summary`, `description` (capped at 4,000 chars), `issuetype`,
   `status`, `priority`, `assignee`, `labels`
3. Parses linked issue keys from `fields.issuelinks` and searches Jira for them
4. Creates a dedicated output folder for the ticket

**Phase 2 — Analysis (LLM only)**

1. Injects the compact ticket data into the prompt via `{{ticketData}}` and `{{linkedIssuesData}}` variables
2. Calls the LLM with a focused analysis prompt — **no tool calls during the LLM step**
3. Parses the structured JSON response into a `TicketAnalysis` record

This separation removes the multi-minute delay that occurs when a local 8B model must reason about when to call tools,
and eliminates JQL generation errors.

## Features

- **Two-phase analysis** — Java collects data, LLM only analyzes; no tool-call orchestration latency
- **Real Jira integration** — `WebClient`-based Jira REST API with graceful stub fallback when credentials are absent
- **Pluggable prompt system** — swap or override prompt templates at runtime without redeployment
- **Hot reload** — file watcher detects changes to external prompt files and invalidates the cache
- **Multiple loading strategies** — `EXTERNAL_FIRST`, `INTERNAL_FIRST`, or `EXTERNAL_ONLY` prompt resolution
- **Caffeine caching** — prompt cache + analysis result cache with stats
- **Structured JSON output** — `ObjectMapper` extracts and deserializes LLM response to `TicketAnalysis`
- **Observability** — Micrometer `@Timed` metrics, `@Observed` spans; Zipkin export opt-in
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
| Observability | Micrometer Tracing, Brave, Zipkin (opt-in)       |
| Testing       | JUnit 5, Spring Boot Test, Testcontainers 1.20.4 |
| Build         | Maven 3.9+, Spring Boot Buildpacks (JVM 21)      |

## Project Structure

```
jira-analysis-agent/
├── external-config/
│   └── prompts/                        # Runtime prompt overrides (hot-reloaded)
├── docs/
│   ├── plugin-stats/README.md          # Architecture deep-dive
│   └── jira-analysis-agent-guide.html # Full HTML guide
├── analysis-output/                    # Generated per-ticket analysis folders
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
│   │   │   │   └── OptimizedJiraAnalysisService.java  # Core orchestration
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

Without credentials the agent starts and analyzes using stub ticket data. To connect to a real Jira instance:

```bash
# Linux / macOS
export JIRA_BASE_URL=https://your-company.atlassian.net
export JIRA_USERNAME=your@email.com
export JIRA_API_TOKEN=your-api-token

# Windows
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

All properties support environment variable overrides.

| Property                                    | Env var                 | Default                   | Description                                           |
|---------------------------------------------|-------------------------|---------------------------|-------------------------------------------------------|
| `spring.ai.ollama.base-url`                 | `OLLAMA_BASE_URL`       | `http://localhost:11434`  | Ollama server URL                                     |
| `spring.ai.ollama.chat.model`               | —                       | `llama3.1:8b`             | Model used for analysis                               |
| `spring.ai.ollama.chat.options.temperature` | —                       | `0.2`                     | Lower = more deterministic output                     |
| `spring.ai.ollama.chat.options.num-ctx`     | —                       | `32768`                   | Context window tokens (input + output)                |
| `jira.base-url`                             | `JIRA_BASE_URL`         | _(empty)_                 | Jira instance URL                                     |
| `jira.username`                             | `JIRA_USERNAME`         | _(empty)_                 | Jira username or email                                |
| `jira.api-token`                            | `JIRA_API_TOKEN`        | _(empty)_                 | Jira API token                                        |
| `agent.output-directory`                    | `AGENT_OUTPUT_DIR`      | `./analysis-output`       | Directory for ticket analysis artifacts               |
| `agent.plugins.external-config-path`        | `AGENT_EXTERNAL_CONFIG` | `./external-config`       | Directory for prompt overrides                        |
| `agent.plugins.strategy`                    | —                       | `EXTERNAL_FIRST`          | `EXTERNAL_FIRST` / `INTERNAL_FIRST` / `EXTERNAL_ONLY` |
| `agent.plugins.hot-reload.enabled`          | `AGENT_HOT_RELOAD`      | `true`                    | Live prompt reload on file change                     |
| `management.tracing.sampling.probability`   | `TRACING_SAMPLE_RATE`   | `0.0`                     | Set to `1.0` to enable full tracing                   |
| `management.zipkin.tracing.export.enabled`  | `ZIPKIN_ENABLED`        | `false`                   | Enable Zipkin trace export                            |
| `management.zipkin.tracing.endpoint`        | `ZIPKIN_URL`            | `http://localhost:9411/…` | Zipkin collector endpoint                             |

### application-local.yml (recommended for development)

Create `src/main/resources/application-local.yml` and activate with `-Dspring-boot.run.profiles=local`:

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

Uses `security-analysis-prompt` — focused on OWASP vulnerabilities, data privacy, compliance, and auth risks.

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/security
```

#### Custom analysis

```http
POST /api/v1/analysis/tickets/{ticketId}/custom
Content-Type: application/json
```

Uses any registered prompt with optional extra template variables.

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

Returns a cached result if available; triggers a fresh analysis otherwise. Results cached for 10 minutes.

```bash
curl "http://localhost:8080/api/v1/analysis/tickets/PROJ-123/cached?promptName=analysis-prompt"
```

---

### Plugin Management Endpoints

#### List available plugins

```http
GET /api/v1/plugins/available
```

```bash
curl http://localhost:8080/api/v1/plugins/available
# ["analysis-prompt","code-review-prompt","effort-estimation-prompt","risk-assessment-prompt","security-analysis-prompt"]
```

#### Reload a single plugin

```http
POST /api/v1/plugins/reload/{pluginName}
```

Invalidates and reloads a specific plugin — useful after editing an external prompt file when hot-reload is disabled.

```bash
curl -X POST http://localhost:8080/api/v1/plugins/reload/analysis-prompt
# {"status":"reloaded","plugin":"analysis-prompt"}
```

#### Reload all plugins

```http
POST /api/v1/plugins/reload-all
```

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
| `GET`  | `/actuator/health`                | Spring Boot full health (disk, ping)                |
| `GET`  | `/actuator/metrics`               | All Micrometer metrics                              |
| `GET`  | `/actuator/prometheus`            | Prometheus scrape endpoint                          |

---

## Usage Examples

### Example 1 — Analyze a ticket

```bash
curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123 | jq '.'

# Extract only requirements
curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123 \
  | jq '.requirements_analysis.functional_requirements'
```

### Example 2 — Security-focused analysis

```bash
curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/security \
  | jq '.risk_assessment.identified_risks[]'
```

### Example 3 — Effort estimation

```bash
curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/custom \
  -H "Content-Type: application/json" \
  -d '{"promptName": "effort-estimation-prompt", "context": {"teamSize": "3"}}' \
  | jq '.effort_estimation'
```

### Example 4 — Override a prompt without restarting

```bash
# 1. Write an override — uses the same {{ticketData}} / {{linkedIssuesData}} variables
cat > external-config/prompts/analysis-prompt.md << 'EOF'
Analyze the Jira ticket below focusing only on security and performance.

TICKET:
{{ticketData}}

Output ONLY valid JSON matching the TicketAnalysis schema.
EOF

# 2. The file watcher picks it up — test immediately (no restart needed)
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123

# 3. Revert: delete the file and the classpath version takes over
rm external-config/prompts/analysis-prompt.md
```

### Example 5 — Monitor cache performance

```bash
watch -n 5 'curl -s http://localhost:8080/api/v1/monitoring/plugin-stats | jq ".hitRate"'
curl -s http://localhost:8080/actuator/metrics/jira.analysis.duration | jq '.measurements'
```

---

## Built-in Prompt Templates

Five templates ship with the agent. All receive `{{ticketData}}` (compact ticket JSON) and `{{linkedIssuesData}}` (
compact linked issues JSON) automatically — no tool-call instructions needed.

| Prompt name                | Endpoint shortcut             | Focus                                                             |
|----------------------------|-------------------------------|-------------------------------------------------------------------|
| `analysis-prompt`          | `POST /tickets/{id}`          | Full comprehensive analysis (default)                             |
| `security-analysis-prompt` | `POST /tickets/{id}/security` | OWASP, data privacy, compliance, auth                             |
| `code-review-prompt`       | custom                        | Correctness, security, performance, maintainability, architecture |
| `risk-assessment-prompt`   | custom                        | TECHNICAL / BUSINESS / TIMELINE / RESOURCE risks with score 1–10  |
| `effort-estimation-prompt` | custom                        | Dev / test / doc / review day breakdown with confidence level     |

### Writing a custom prompt

Place a `.md` file in `external-config/prompts/` and use the built-in template variables:

| Variable               | Content                                                             |
|------------------------|---------------------------------------------------------------------|
| `{{ticketData}}`       | Compact JSON with key, summary, description, status, priority, etc. |
| `{{linkedIssuesData}}` | Compact JSON array of linked issue key + summary + status           |

The description field is capped at **4,000 characters** to stay within the model's context window. All other Jira
metadata (avatars, rendered fields, watchers, etc.) is stripped before the prompt is built.

Minimal custom prompt skeleton:

```markdown
Analyze the following Jira ticket for {{teamSize}} developers and output the JSON response.

TICKET:
{{ticketData}}

LINKED ISSUES:
{{linkedIssuesData}}

Output ONLY valid JSON matching the TicketAnalysis schema. No prose. No markdown fences.
```

Save to `external-config/prompts/my-prompt.md` and call with:

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/custom \
  -H "Content-Type: application/json" \
  -d '{"promptName": "my-prompt", "context": {"teamSize": "3"}}'
```

---

## Output Reference

All analysis endpoints return `application/json` matching the `TicketAnalysis` schema:

```json
{
  "ticket_id": "PROJ-123",
  "summary": "2-3 sentence analytical summary of the ticket",
  "requirements_analysis": {
    "functional_requirements": ["..."],
    "non_functional_requirements": ["..."],
    "acceptance_criteria": ["..."],
    "dependencies": ["..."],
    "assumptions": ["..."]
  },
  "technical_analysis": {
    "complexity_score": 7,
    "technical_challenges": ["..."],
    "recommended_approach": "...",
    "architecture_considerations": ["..."],
    "technology_stack": ["..."],
    "performance_considerations": ["..."]
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
    "estimation_method": "Expert Judgement",
    "team_size_assumption": 2
  },
  "implementation_strategy": {
    "phases": [
      {
        "name": "Phase 1 — ...",
        "description": "...",
        "estimated_days": 3,
        "deliverables": ["..."],
        "dependencies": ["..."],
        "risks": ["..."]
      }
    ],
    "key_milestones": [
      {
        "name": "...",
        "description": "...",
        "target_date": "End of Day 3",
        "success_criteria": ["..."]
      }
    ],
    "success_criteria": ["..."],
    "rollback_strategy": "..."
  },
  "analysis_metadata": {
    "analysis_timestamp": "2026-06-10T13:00:00",
    "model_used": "llama3.1:8b",
    "analysis_version": "1.0.0",
    "processing_time_ms": 45000,
    "prompt_name": "analysis-prompt",
    "prompt_source": "INTERNAL_RESOURCE",
    "prompt_last_modified": "2026-06-10T09:00:00"
  }
}
```

### Error responses

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Prompt plugin not found: unknown-prompt",
  "timestamp": "2026-06-10T10:30:00"
}
```

---

## Performance Notes

Analysis time is dominated by the LLM generating the JSON output (~800–1,000 tokens). With `llama3.1:8b` on a mid-range
GPU, expect **30–120 seconds** per ticket. The Jira fetch itself takes ~2–6 seconds.

To improve throughput:

- Switch to a faster local model: `ollama pull llama3.2:3b` and update `spring.ai.ollama.chat.model`
- Use a cloud model (OpenAI / Anthropic) by swapping `spring-ai-starter-model-ollama` for the corresponding Spring AI
  starter

The description field is capped at 4,000 characters. For tickets with very long descriptions, the most important
content (scope, acceptance criteria, requirements) should appear early in the description.

---

## How to Contribute

1. Fork the repository and create a feature branch from `main`.
2. Keep commits focused and descriptive.
3. Add or update tests for any new behaviour.
4. Ensure the build passes: `mvn verify`
5. Open a pull request with a clear description of what changed and why.

Code conventions: no Lombok, no unnecessary comments, Java records for immutable data, constructor injection throughout.

---

## Author

Ahmed Samy — [@javamsdt](https://github.com/javamsdt)
