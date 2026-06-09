# Plugin & Stats System — Architecture & Usage Guide

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Component Reference](#component-reference)
4. [End-to-End Workflow](#end-to-end-workflow)
5. [Setup & Configuration](#setup--configuration)
6. [REST API — Analysis Endpoints](#rest-api--analysis-endpoints)
7. [REST API — Plugin Management](#rest-api--plugin-management)
8. [REST API — Monitoring & Stats](#rest-api--monitoring--stats)
9. [Input Reference](#input-reference)
10. [Output Reference](#output-reference)
11. [Prompt Templates](#prompt-templates)
12. [Hot Reload](#hot-reload)
13. [Jira Integration](#jira-integration)
14. [Observability & Metrics](#observability--metrics)
15. [Known Gaps & Future Work](#known-gaps--future-work)

---

## Overview

The Plugin & Stats system is the prompt-management, caching, and observability backbone of the Jira Analysis Agent. It
is responsible for:

- Resolving and caching Markdown prompt templates from classpath or the external filesystem
- Hot-reloading prompts without an application restart
- Exposing cache health statistics and plugin management controls via REST
- Providing Micrometer timer metrics and distributed traces for every analysis request

The subsystem sits between the REST API layer and the LLM (Ollama / `llama3.1:8b`), ensuring the correct, up-to-date
prompt is always delivered to the model.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      HTTP Clients / curl / Tests                 │
└────────────┬──────────────────────┬────────────────────────────┘
             │                      │
┌────────────▼──────┐  ┌────────────▼──────────────────────────┐
│ AnalysisController │  │  PluginController / MonitoringController│
│                    │  │                                        │
│ POST /tickets/{id} │  │ GET  /plugins/available                │
│ POST /tickets/{id} │  │ POST /plugins/reload/{name}            │
│       /security    │  │ POST /plugins/reload-all               │
│ POST /tickets/{id} │  │ GET  /monitoring/health                │
│       /custom      │  │ GET  /monitoring/plugin-stats          │
│ GET  /tickets/{id} │  └────────────────────────────────────────┘
│       /cached      │
└────────────┬───────┘
             │ calls
┌────────────▼───────────────────────────────────────────────────┐
│               OptimizedJiraAnalysisService  (@Service)          │
│                                                                 │
│  analyzeTicket(ticketId)                    @Timed @Observed    │
│  analyzeTicket(ticketId, promptName, ctx)   @Timed @Observed    │
│  getCachedAnalysis(ticketId, promptName)    @Cacheable          │
│  isHealthy()  │  getPluginStatistics()                          │
└────────────┬───────────────────────────────────────────────────┘
             │ loadPlugin(name, context)
┌────────────▼───────────────────────────────────────────────────┐
│           OptimizedPromptPluginManager  (@Component)            │
│                                                                 │
│  ┌───────────────────────┐    ┌─────────────────────────────┐  │
│  │  pluginCache           │    │  lastModifiedCache           │  │
│  │  LoadingCache<String,  │    │  Cache<String, LocalDateTime>│  │
│  │    PromptPlugin>       │    │  max 1000 / expire 2h        │  │
│  │  max 1000 / expire 1h  │    └─────────────────────────────┘  │
│  │  refresh 5min / stats  │                                     │
│  └───────────┬───────────┘                                     │
│              │ cache miss → loadPluginFromSource(name)          │
│  ┌───────────▼─────────────────────────────────────────────┐   │
│  │            PluginLoadingStrategy (enum)                   │   │
│  │   EXTERNAL_FIRST │ INTERNAL_FIRST │ EXTERNAL_ONLY         │   │
│  └───────────┬─────────────────┬───────────────────────────┘   │
│              │                 │                                 │
│  ┌───────────▼──────┐  ┌───────▼──────────────────────────┐    │
│  │  External Disk    │  │  Classpath (JAR resources)        │    │
│  │  external-config/ │  │  prompts/default/*.md             │    │
│  │  prompts/*.md     │  └──────────────────────────────────┘    │
│  └──────────────────┘                                           │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  DirectoryWatcher — file change → invalidate → reload     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
             │ prompt content
┌────────────▼───────────────────────────────────────────────────┐
│          ChatClient (Spring AI / Ollama llama3.1:8b)            │
│  .user(promptContent).tools(jiraRetrievalTool, fileSystemTool)  │
│  .call().entity(TicketAnalysis.class)                           │
└────────────┬───────────────────────────────────────────────────┘
             │ LLM-initiated tool calls
┌────────────▼───────────────────────────────────────────────────┐
│  JiraRetrievalTool                  FileSystemTool              │
│  @Tool retrieve_jira_ticket         @Tool readFile              │
│  @Tool search_jira_tickets          @Tool listDirectory         │
│  (WebClient → Jira REST API)        @Tool create_ticket_folder  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Reference

### `PluginManager<T>` (interface)

Generic plugin contract. `OptimizedPromptPluginManager` is the only current implementation.

| Method                      | Description                                                     |
|-----------------------------|-----------------------------------------------------------------|
| `loadPlugin(name)`          | Load plugin by name with no variable substitution               |
| `loadPlugin(name, context)` | Load plugin with `{{key}}` placeholders replaced from `context` |
| `reloadPlugin(name)`        | Invalidate and asynchronously reload a single plugin            |
| `reloadAllPlugins()`        | Invalidate all cached plugins and re-warm from source           |
| `getAvailablePlugins()`     | List all resolvable plugin names (classpath + external)         |
| `isPluginAvailable(name)`   | Returns `true` if the named plugin can be resolved              |

---

### `PromptPlugin` (record)

Immutable value object representing a loaded and optionally template-substituted prompt.

| Field          | Type                  | Description                                                      |
|----------------|-----------------------|------------------------------------------------------------------|
| `name`         | `String`              | Plugin identifier — matches the `.md` filename without extension |
| `content`      | `String`              | Full Markdown prompt text after `{{}}` substitution              |
| `metadata`     | `Map<String, Object>` | Provenance: `file-path`, `last-modified`, `size`, `source`       |
| `lastModified` | `LocalDateTime`       | Timestamp of the source file when last loaded                    |
| `source`       | `PluginSource`        | `EXTERNAL_FILE` or `INTERNAL_RESOURCE`                           |

---

### `OptimizedPromptPluginManager`

Spring `@Component`. Owns the full plugin lifecycle from resolution to eviction.

**Config properties** (all under `agent.plugins`):

| Property               | Default             | Env var                 | Description                        |
|------------------------|---------------------|-------------------------|------------------------------------|
| `external-config-path` | `./external-config` | `AGENT_EXTERNAL_CONFIG` | Root dir for external prompt files |
| `strategy`             | `EXTERNAL_FIRST`    | —                       | Loading strategy enum              |
| `hot-reload.enabled`   | `true`              | `AGENT_HOT_RELOAD`      | Enable filesystem change detection |

**Loading strategies:**

| Strategy         | Behaviour                                                                       |
|------------------|---------------------------------------------------------------------------------|
| `EXTERNAL_FIRST` | Tries `external-config/prompts/<name>.md`; falls back to classpath              |
| `INTERNAL_FIRST` | Tries classpath first; falls back to external directory                         |
| `EXTERNAL_ONLY`  | Only loads from external directory; returns `null` and logs a warning if absent |

**Internal caches:**

| Cache               | Type           | Max  | TTL     | Refresh     |
|---------------------|----------------|------|---------|-------------|
| `pluginCache`       | `LoadingCache` | 1000 | 1 hour  | every 5 min |
| `lastModifiedCache` | `Cache`        | 1000 | 2 hours | —           |

`pluginCache.stats()` feeds the `/monitoring/plugin-stats` endpoint.

---

### `JiraRetrievalTool`

Spring `@Component` exposing two `@Tool`-annotated methods to the LLM.

| Tool name              | Method                         | Description                                                                                                          |
|------------------------|--------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `retrieve_jira_ticket` | `retrieveJiraTicket(ticketId)` | Calls `GET /rest/api/2/issue/{id}` on the configured Jira server. Returns stub JSON if Jira credentials are not set. |
| `search_jira_tickets`  | `searchJiraTickets(jql)`       | Calls `GET /rest/api/2/search?jql=...` with up to 20 results. Returns empty array if not configured.                 |

Configuration: `jira.base-url`, `jira.username`, `jira.api-token` (see [Jira Integration](#jira-integration)).

---

### `FileSystemTool`

Spring `@Component` exposing three `@Tool`-annotated methods to the LLM.

| Tool name              | Method                         | Description                                                                  |
|------------------------|--------------------------------|------------------------------------------------------------------------------|
| `readFile`             | `readFile(filePath)`           | Reads a file at the given path and returns its content                       |
| `listDirectory`        | `listDirectory(directoryPath)` | Lists files in a directory, newline-separated                                |
| `create_ticket_folder` | `createTicketFolder(ticketId)` | Creates `{agent.output-directory}/{ticketId}/` and returns the absolute path |

---

### `OptimizedJiraAnalysisService`

Service that wires prompt loading, LLM call, tool execution, and metadata enrichment.

| Method                                     | Annotation                                            | Description                                            |
|--------------------------------------------|-------------------------------------------------------|--------------------------------------------------------|
| `analyzeTicket(ticketId)`                  | `@Timed("jira.analysis.duration")` `@Observed`        | Default analysis using `analysis-prompt`               |
| `analyzeTicket(ticketId, promptName, ctx)` | `@Timed("jira.analysis.custom.duration")` `@Observed` | Analysis with any named prompt and context variables   |
| `getCachedAnalysis(ticketId, promptName)`  | `@Cacheable("analysisResults")`                       | Spring-cached wrapper; key = `{ticketId}_{promptName}` |
| `isHealthy()`                              | —                                                     | Checks plugin manager can load `analysis-prompt`       |
| `getPluginStatistics()`                    | —                                                     | Delegates to `pluginManager.getCacheStatistics()`      |

---

### Controllers

| Controller               | Base path            | Responsibilities                                                |
|--------------------------|----------------------|-----------------------------------------------------------------|
| `AnalysisController`     | `/api/v1/analysis`   | Trigger analysis, custom prompt, cached result                  |
| `PluginController`       | `/api/v1/plugins`    | List, reload single, reload all plugins                         |
| `MonitoringController`   | `/api/v1/monitoring` | Health check, plugin cache stats                                |
| `GlobalExceptionHandler` | —                    | Maps `IllegalArgumentException` → 400, `RuntimeException` → 500 |

---

## End-to-End Workflow

```
1. HTTP request arrives at AnalysisController
   └─▶ POST /api/v1/analysis/tickets/{ticketId}
       POST /api/v1/analysis/tickets/{ticketId}/security
       POST /api/v1/analysis/tickets/{ticketId}/custom  (body: {promptName, context})

2. Controller delegates to OptimizedJiraAnalysisService.analyzeTicket(...)

3. Plugin loading
   └─▶ OptimizedPromptPluginManager.loadPlugin(promptName, {ticketId, ...context})
       ├─ Cache HIT  →  return cached PromptPlugin + apply {{}} substitution
       └─ Cache MISS →  resolve via strategy
              EXTERNAL_FIRST: external-config/prompts/<name>.md → classpath fallback
              INTERNAL_FIRST: classpath prompts/default/<name>.md → external fallback
              EXTERNAL_ONLY : external only, null if missing

4. Template processing
   └─▶ Replace {{ticketId}} and any {{key}} from context map in prompt content

5. LLM call
   └─▶ ChatClient.prompt()
         .user(processedPromptContent)
         .tools(jiraRetrievalTool, fileSystemTool)
         .call()
         .entity(TicketAnalysis.class)

6. LLM tool calls (during model reasoning)
   ├─▶ retrieve_jira_ticket(ticketId)  →  Jira REST API or stub JSON
   ├─▶ search_jira_tickets(jql)        →  Jira search results or empty
   ├─▶ create_ticket_folder(ticketId)  →  creates ./analysis-output/{ticketId}/
   └─▶ readFile(path)                  →  file content (code-review prompt)

7. Spring AI structured output
   └─▶ LLM JSON response deserialized → TicketAnalysis record

8. Metadata enrichment
   └─▶ enhanceWithMetadata() fills AnalysisMetadata:
       analysisTimestamp, modelUsed, analysisVersion,
       processingTimeMs, promptName, promptSource, promptLastModified

9. Return TicketAnalysis as JSON to caller
```

---

## Setup & Configuration

### Prerequisites

| Requirement | Version | Notes                               |
|-------------|---------|-------------------------------------|
| Java        | 21+     |                                     |
| Maven       | 3.9+    |                                     |
| Ollama      | latest  | Running at `http://localhost:11434` |
| llama3.1:8b | —       | `ollama pull llama3.1:8b`           |

### Minimum startup (no Jira)

```bash
mvn spring-boot:run
```

The agent starts with stub Jira data. All analysis endpoints work; results are based on the placeholder ticket returned
by `JiraRetrievalTool`.

### With real Jira

```bash
export JIRA_BASE_URL=https://your-company.atlassian.net
export JIRA_USERNAME=your@email.com
export JIRA_API_TOKEN=your-api-token
mvn spring-boot:run
```

### Key YAML configuration

```yaml
jira:
  base-url: ${JIRA_BASE_URL:}        # leave empty → stub mode
  username: ${JIRA_USERNAME:}
  api-token: ${JIRA_API_TOKEN:}

agent:
  output-directory: ${AGENT_OUTPUT_DIR:./analysis-output}
  plugins:
    external-config-path: ${AGENT_EXTERNAL_CONFIG:./external-config}
    strategy: EXTERNAL_FIRST          # EXTERNAL_FIRST | INTERNAL_FIRST | EXTERNAL_ONLY
    hot-reload:
      enabled: ${AGENT_HOT_RELOAD:true}

spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        model: llama3.1:8b
        options:
          temperature: 0.2
          num-ctx: 8192
```

---

## REST API — Analysis Endpoints

Base path: `/api/v1/analysis`

### Default analysis

```http
POST /api/v1/analysis/tickets/{ticketId}
```

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123
```

Uses `analysis-prompt`. No request body required.

---

### Security analysis

```http
POST /api/v1/analysis/tickets/{ticketId}/security
```

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/security
```

Uses `security-analysis-prompt` (OWASP, data privacy, compliance, auth/authz).

---

### Custom analysis

```http
POST /api/v1/analysis/tickets/{ticketId}/custom
Content-Type: application/json
```

**Request body** (all fields optional):

| Field        | Type     | Default             | Description                             |
|--------------|----------|---------------------|-----------------------------------------|
| `promptName` | `String` | `"analysis-prompt"` | Name of the plugin to use               |
| `context`    | `Object` | `{}`                | Additional `{{key}}` template variables |

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/custom \
  -H "Content-Type: application/json" \
  -d '{
    "promptName": "risk-assessment-prompt",
    "context": {
      "teamSize": "3",
      "deadline": "2024-08-01"
    }
  }'
```

---

### Cached analysis

```http
GET /api/v1/analysis/tickets/{ticketId}/cached?promptName=analysis-prompt
```

Returns a cached `TicketAnalysis` if one exists for the `ticketId + promptName` combination, otherwise triggers a fresh
analysis and caches the result for 10 minutes.

```bash
curl "http://localhost:8080/api/v1/analysis/tickets/PROJ-123/cached"
curl "http://localhost:8080/api/v1/analysis/tickets/PROJ-123/cached?promptName=risk-assessment-prompt"
```

---

## REST API — Plugin Management

Base path: `/api/v1/plugins`

### List available plugins

```http
GET /api/v1/plugins/available
```

```bash
curl http://localhost:8080/api/v1/plugins/available
```

```json
[
  "analysis-prompt",
  "code-review-prompt",
  "effort-estimation-prompt",
  "risk-assessment-prompt",
  "security-analysis-prompt"
]
```

Also includes any `.md` files present in `external-config/prompts/`.

---

### Reload a single plugin

```http
POST /api/v1/plugins/reload/{pluginName}
```

```bash
curl -X POST http://localhost:8080/api/v1/plugins/reload/analysis-prompt
```

```json
{
  "status": "reloaded",
  "plugin": "analysis-prompt"
}
```

Useful when `hot-reload.enabled` is `false` and you need to push an updated external prompt.

---

### Reload all plugins

```http
POST /api/v1/plugins/reload-all
```

```bash
curl -X POST http://localhost:8080/api/v1/plugins/reload-all
```

```json
{
  "status": "reloaded",
  "count": "all"
}
```

Invalidates all cached plugins and synchronously re-warms the default five.

---

## REST API — Monitoring & Stats

### Agent health

```http
GET /api/v1/monitoring/health
```

```json
{
  "status": "UP",
  "timestamp": 1717920600000,
  "service": "jira-analysis-agent"
}
```

Returns `"DOWN"` if the plugin manager fails to load `analysis-prompt`.

---

### Plugin cache statistics

```http
GET /api/v1/monitoring/plugin-stats
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

| Field           | Healthy threshold                                  |
|-----------------|----------------------------------------------------|
| `hitRate`       | > 0.90                                             |
| `evictionCount` | Low (cache pressure indicator)                     |
| `estimatedSize` | Should equal the number of distinct prompts loaded |

---

### Spring Boot Actuator

```bash
GET /actuator/health          # full health with disk / ping
GET /actuator/metrics         # all Micrometer metrics
GET /actuator/prometheus      # Prometheus scrape endpoint
GET /actuator/info            # application info
```

---

## Input Reference

### Ticket ID

A Jira-style key string such as `"PROJ-123"` or `"BACKEND-42"`. It is:

- Passed to `retrieve_jira_ticket(ticketId)` so the LLM can fetch ticket data
- Injected as `{{ticketId}}` into every prompt template
- Used as the folder name in `create_ticket_folder(ticketId)`

### Prompt name

| Name                       | Default for                   | Focus                                               |
|----------------------------|-------------------------------|-----------------------------------------------------|
| `analysis-prompt`          | `POST /tickets/{id}`          | Full comprehensive analysis                         |
| `security-analysis-prompt` | `POST /tickets/{id}/security` | OWASP, privacy, compliance, auth                    |
| `code-review-prompt`       | custom                        | Correctness, security, performance, maintainability |
| `risk-assessment-prompt`   | custom                        | TECHNICAL / BUSINESS / TIMELINE / RESOURCE risks    |
| `effort-estimation-prompt` | custom                        | Dev / test / doc / review day breakdown             |

### Context map

Optional `Map<String, Object>` passed as the `"context"` field in the request body. Any key `foo` replaces `{{foo}}` in
the prompt. `ticketId` is always injected automatically.

```json
{
  "teamSize": "3",
  "sprintLength": "2 weeks",
  "techStack": "Spring Boot, React",
  "deadline": "2024-08-01"
}
```

---

## Output Reference

All analysis endpoints return `Content-Type: application/json` with a `TicketAnalysis` payload.

```json
{
  "ticket_id": "PROJ-123",
  "summary": "Implement user authentication system",
  "requirements_analysis": {
    "functional_requirements": [
      "User login/logout",
      "Password reset",
      "Session management"
    ],
    "non_functional_requirements": [
      "Security: OAuth 2.0",
      "Performance: < 2s login"
    ],
    "acceptance_criteria": [
      "Users can login with email/password",
      "Failed attempts are logged"
    ],
    "dependencies": [
      "Database schema updates",
      "Identity provider integration"
    ],
    "assumptions": [
      "OAuth provider is available",
      "Email service is configured"
    ]
  },
  "technical_analysis": {
    "complexity_score": 7,
    "technical_challenges": [
      "Security complexity",
      "Integration with legacy systems"
    ],
    "recommended_approach": "Implement OAuth 2.0 with Spring Security and JWT",
    "architecture_considerations": [
      "Stateless JWT tokens",
      "Redis session storage"
    ],
    "technology_stack": [
      "Spring Security",
      "JWT",
      "Redis"
    ],
    "performance_considerations": [
      "Token expiry strategy",
      "Cache warm-up on startup"
    ]
  },
  "risk_assessment": {
    "identified_risks": [
      {
        "description": "Security vulnerability in token handling",
        "category": "TECHNICAL",
        "impact": "HIGH",
        "probability": "MEDIUM",
        "mitigation_strategy": "Penetration testing + security code review",
        "contingency_plan": "Immediate rollback and patch cycle"
      }
    ],
    "overall_risk_level": "MEDIUM",
    "risk_score": 5
  },
  "effort_estimation": {
    "development_days": 8,
    "testing_days": 3,
    "documentation_days": 2,
    "review_days": 1,
    "total_days": 14,
    "confidence_level": "HIGH",
    "estimation_method": "Story Points",
    "team_size_assumption": 2
  },
  "implementation_strategy": {
    "phases": [
      {
        "name": "Phase 1: Core Authentication",
        "description": "Login, logout, JWT issuance",
        "estimated_days": 5,
        "deliverables": [
          "Login API",
          "JWT implementation"
        ],
        "dependencies": [
          "DB schema migration"
        ],
        "risks": [
          "OAuth provider availability"
        ]
      }
    ],
    "key_milestones": [
      {
        "name": "Auth API Complete",
        "description": "All auth endpoints functional",
        "target_date": "Sprint 3",
        "success_criteria": [
          "All acceptance criteria met",
          "Security review passed"
        ]
      }
    ],
    "success_criteria": [
      "All acceptance criteria met",
      "Security audit passed"
    ],
    "rollback_strategy": "Feature flag to disable new auth, revert to legacy"
  },
  "analysis_metadata": {
    "analysis_timestamp": "2024-06-09T10:30:00",
    "model_used": "llama3.1:8b",
    "analysis_version": "1.0.0",
    "processing_time_ms": 3200,
    "prompt_name": "analysis-prompt",
    "prompt_source": "INTERNAL_RESOURCE",
    "prompt_last_modified": "2024-06-09T09:00:00"
  }
}
```

### Error responses

All errors return an `ErrorResponse` JSON with HTTP status 400 or 500:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Prompt plugin not found: unknown-prompt",
  "timestamp": "2024-06-09T10:30:00"
}
```

---

## Prompt Templates

### Built-in templates

Located at `src/main/resources/prompts/default/`. All five templates:

- Accept `{{ticketId}}` (always injected)
- Accept any additional `{{key}}` from the context map
- Instruct the LLM to call the registered tools by name

### Tool names used in prompts

| Tool name              | Registered on       | Purpose                                |
|------------------------|---------------------|----------------------------------------|
| `retrieve_jira_ticket` | `JiraRetrievalTool` | Fetch full ticket JSON from Jira       |
| `search_jira_tickets`  | `JiraRetrievalTool` | JQL search for related tickets         |
| `create_ticket_folder` | `FileSystemTool`    | Create output directory for the ticket |
| `readFile`             | `FileSystemTool`    | Read source files (code review prompt) |

### Overriding a built-in prompt

1. Create `external-config/prompts/<name>.md` using the same filename as the built-in.
2. Write your prompt using the tool names above.
3. With `hot-reload.enabled: true` (default), the file is detected within seconds and the cache is invalidated — no
   restart needed.
4. To revert, delete the external file. The classpath version takes over on the next cache miss.

### Writing a custom prompt from scratch

```markdown
# My Custom Analysis for {{ticketId}}

You are an expert engineer.

1. Call retrieve_jira_ticket("{{ticketId}}") to fetch ticket data.
2. Call search_jira_tickets("project = MY_PROJECT AND issueType = Bug") for related bugs.
3. Call create_ticket_folder("{{ticketId}}") to set up output storage.
4. Perform your analysis.
5. Return a complete TicketAnalysis JSON object.

Additional context:

- Team size: {{teamSize}}
- Expected load: {{expectedLoad}}
```

Save it to `external-config/prompts/my-custom-prompt.md` and call it with:

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/custom \
  -H "Content-Type: application/json" \
  -d '{"promptName": "my-custom-prompt", "context": {"teamSize": "3", "expectedLoad": "1000 rps"}}'
```

---

## Hot Reload

The `DirectoryWatcher` (methvin) monitors the `external-config/` directory.

**Trigger:** any `.md` file under a `prompts/` sub-path is created, modified, or deleted.

**Effect:**

1. `pluginCache.invalidate(pluginName)` — removes the stale entry
2. `lastModifiedCache.invalidate(pluginName)` — clears the timestamp record
3. `CompletableFuture.runAsync(() -> pluginCache.get(pluginName))` — asynchronously reloads

The next HTTP request to use that plugin name gets the updated content. The turnaround is typically under one second.

**Disable hot reload:**

```yaml
agent:
  plugins:
    hot-reload:
      enabled: false
```

When disabled, use `POST /api/v1/plugins/reload/{name}` to manually push updates.

---

## Jira Integration

`JiraRetrievalTool` makes real Jira REST API calls when `jira.base-url`, `jira.username`, and `jira.api-token` are all
set. Otherwise it falls back to stub data, which keeps the agent fully functional for development and demo purposes.

### Configure credentials

```bash
export JIRA_BASE_URL=https://your-company.atlassian.net
export JIRA_USERNAME=your@email.com
export JIRA_API_TOKEN=your-api-token
```

API tokens are created at: `https://id.atlassian.com/manage-profile/security/api-tokens`

### Jira API calls made

| Tool                   | Endpoint                                                                 | Fields fetched           |
|------------------------|--------------------------------------------------------------------------|--------------------------|
| `retrieve_jira_ticket` | `GET /rest/api/2/issue/{id}?expand=description,comment,labels,priority`  | Full ticket JSON         |
| `search_jira_tickets`  | `GET /rest/api/2/search?jql=...&maxResults=20&fields=summary,status,...` | Up to 20 matching issues |

### Stub mode

When credentials are absent, `retrieve_jira_ticket` returns:

```json
{
  "id": "PROJ-123",
  "key": "PROJ-123",
  "fields": {
    "summary": "Stub ticket — configure jira.base-url, jira.username, jira.api-token to fetch real data",
    "status": {
      "name": "Open"
    },
    "priority": {
      "name": "Medium"
    }
  }
}
```

Analysis still completes; the LLM reasons over the stub. This is useful for testing prompts before connecting to a real
Jira.

---

## Observability & Metrics

### Micrometer timers

| Metric                          | Tag                                    | Threshold |
|---------------------------------|----------------------------------------|-----------|
| `jira.analysis.duration`        | `jira-analysis-complete` (observation) | < 30s     |
| `jira.analysis.custom.duration` | `jira-analysis-custom` (observation)   | < 30s     |

```bash
curl -s http://localhost:8080/actuator/metrics/jira.analysis.duration | jq '.measurements'
```

### Distributed tracing

Both `@Observed` methods export spans to the configured Zipkin-compatible backend. Default sampling is 100% (
`management.tracing.sampling.probability: 1.0`). Reduce to `0.1` in production.

### Plugin cache stats

```bash
curl http://localhost:8080/api/v1/monitoring/plugin-stats
```

| Field           | Description                   | Alert when     |
|-----------------|-------------------------------|----------------|
| `hitRate`       | Cache hit fraction (0–1)      | < 0.85         |
| `missRate`      | Cache miss fraction (0–1)     | > 0.15         |
| `loadCount`     | Total loads from source       | —              |
| `evictionCount` | Evictions due to capacity/TTL | Rising rapidly |
| `estimatedSize` | Number of cached plugins      | —              |

### Prometheus / Grafana

```bash
GET /actuator/prometheus
```

Add this endpoint as a Prometheus scrape target and import the Spring Boot + Micrometer dashboard in Grafana. The
`jira.analysis.duration` and `jira.analysis.custom.duration` timers will appear as histograms.

---

## Known Gaps & Future Work

| Area                        | Status                  | Notes                                                                                                                                                                                        |
|-----------------------------|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Template engine             | Simple `String.replace` | StringTemplate 4 is on the classpath but only basic `{{key}}` substitution is implemented. ST4 conditionals and loops are planned.                                                           |
| File access security        | No path whitelist       | `FileSystemTool.readFile()` accepts arbitrary paths. Add a configurable whitelist before exposing the agent publicly.                                                                        |
| Analysis result persistence | Folder only             | `create_ticket_folder` creates the directory but analysis JSON is not yet auto-saved to disk. The LLM could write it, or a post-analysis hook could be added.                                |
| pgvector / embeddings       | Not included            | Removed due to DataSource startup failure when unconfigured. Re-add `spring-ai-starter-vector-store-pgvector` and a datasource config if semantic search over historical analyses is needed. |
| Jira write-back             | Not implemented         | The agent reads Jira but does not post comments or update fields with analysis results.                                                                                                      |
