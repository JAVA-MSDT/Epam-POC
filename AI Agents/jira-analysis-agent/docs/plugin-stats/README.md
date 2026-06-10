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

Analysis follows a **two-phase design**: Java collects all Jira data directly (Phase 1), then injects that data into the
prompt so the LLM focuses entirely on analysis (Phase 2). The LLM makes no tool calls.

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
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PHASE 1 — Java data collection (no LLM)                 │  │
│  │                                                          │  │
│  │  collectJiraData(ticketId)                               │  │
│  │    ├─ JiraRetrievalTool.retrieveJiraTicket(ticketId)     │  │
│  │    ├─ extractLinkedIssueKeys(ticketJson)                 │  │
│  │    ├─ JiraRetrievalTool.searchJiraTickets(jql)           │  │
│  │    └─ FileSystemTool.createTicketFolder(ticketId)        │  │
│  │                                                          │  │
│  │  extractEssentialTicketData(rawJson)  — compact fields   │  │
│  │  extractEssentialLinkedData(rawJson)  — key+summary only │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PHASE 2 — LLM analysis (no tool calls)                  │  │
│  │                                                          │  │
│  │  prompt = template.replace("{{ticketData}}", ...)        │  │
│  │                   .replace("{{linkedIssuesData}}", ...)  │  │
│  │  callLlmForAnalysis(prompt, ticketId)                    │  │
│  │  parseJsonResponse(raw, ticketId)                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  analyzeTicket(ticketId)                    @Timed @Observed    │
│  analyzeTicket(ticketId, promptName, ctx)   @Timed @Observed    │
│  getCachedAnalysis(ticketId, promptName)    @Cacheable          │
│  isHealthy()  │  getPluginStatistics()                          │
└────────────┬────────────────┬──────────────────────────────────┘
             │ loadPlugin     │ direct Java calls
┌────────────▼──────────┐  ┌─▼──────────────────────────────────┐
│ OptimizedPromptPlugin  │  │  JiraRetrievalTool / FileSystemTool │
│ Manager  (@Component)  │  │                                    │
│                        │  │  retrieveJiraTicket(ticketId)      │
│ pluginCache            │  │    GET /rest/api/2/issue/{id}      │
│ LoadingCache<String,   │  │    exchangeToMono — logs HTTP code │
│   PromptPlugin>        │  │                                    │
│ max 1000 / expire 1h   │  │  searchJiraTickets(jql)            │
│ refresh 5min / stats   │  │    GET /rest/api/2/search?jql=...  │
│                        │  │                                    │
│ lastModifiedCache      │  │  createTicketFolder(ticketId)      │
│ max 1000 / expire 2h   │  │    creates output/{ticketId}/      │
│                        │  └────────────────────────────────────┘
│ EXTERNAL_FIRST         │
│ INTERNAL_FIRST         │
│ EXTERNAL_ONLY          │
│                        │
│ DirectoryWatcher       │
│ hot-reload on .md      │
└────────────────────────┘
             │ prompt content
┌────────────▼───────────────────────────────────────────────────┐
│          ChatClient (Spring AI / Ollama llama3.1:8b)            │
│  .prompt().user(promptWithInjectedData).call().content()        │
│  (NO .tools() — LLM receives pre-fetched data, analysis only)   │
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

Spring `@Component` called directly from Java in Phase 1 (not by the LLM). Also annotated with `@Tool` for optional
direct use.

| Method                         | Description                                                                                                                                                |
|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `retrieveJiraTicket(ticketId)` | `GET /rest/api/2/issue/{id}` via `WebClient.exchangeToMono()`. Logs HTTP status code and response size. Returns stub JSON if Jira credentials are not set. |
| `searchJiraTickets(jql)`       | `GET /rest/api/2/search?jql=...` with up to 20 results. JQL is built from real linked-issue keys extracted by Java. Returns empty array if not configured. |

Configuration: `jira.base-url`, `jira.username`, `jira.api-token` (see [Jira Integration](#jira-integration)).

---

### `FileSystemTool`

Spring `@Component` called directly from Java in Phase 1 (not by the LLM).

| Method                         | Description                                                                  |
|--------------------------------|------------------------------------------------------------------------------|
| `createTicketFolder(ticketId)` | Creates `{agent.output-directory}/{ticketId}/` and returns the absolute path |

---

### `OptimizedJiraAnalysisService`

Service that orchestrates the two-phase analysis pipeline.

| Method                                     | Annotation                                            | Description                                            |
|--------------------------------------------|-------------------------------------------------------|--------------------------------------------------------|
| `analyzeTicket(ticketId)`                  | `@Timed("jira.analysis.duration")` `@Observed`        | Default analysis using `analysis-prompt`               |
| `analyzeTicket(ticketId, promptName, ctx)` | `@Timed("jira.analysis.custom.duration")` `@Observed` | Analysis with any named prompt and context variables   |
| `getCachedAnalysis(ticketId, promptName)`  | `@Cacheable("analysisResults")`                       | Spring-cached wrapper; key = `{ticketId}_{promptName}` |
| `isHealthy()`                              | —                                                     | Checks plugin manager can load `analysis-prompt`       |
| `getPluginStatistics()`                    | —                                                     | Delegates to `pluginManager.getCacheStatistics()`      |

**Internal Phase 1 methods:**

| Method                               | Description                                                                                     |
|--------------------------------------|-------------------------------------------------------------------------------------------------|
| `collectJiraData(ticketId)`          | Fetches ticket JSON, extracts linked keys, searches linked issues, creates output folder        |
| `extractLinkedIssueKeys(ticketJson)` | Parses `fields.issuelinks[].inwardIssue.key` and `outwardIssue.key` from ticket JSON            |
| `extractEssentialTicketData(json)`   | Extracts key, summary, description (≤4000 chars), issuetype, status, priority, assignee, labels |
| `extractEssentialLinkedData(json)`   | Extracts only `key`, `summary`, `status` for each linked issue — keeps prompt compact           |

**Internal Phase 2 methods:**

| Method                             | Description                                                                         |
|------------------------------------|-------------------------------------------------------------------------------------|
| `callLlmForAnalysis(prompt, id)`   | Calls `chatClient.prompt().user(prompt).call().content()` — no tool registration    |
| `parseJsonResponse(raw, ticketId)` | Extracts `{…}` substring, then `ObjectMapper.readValue(…, TicketAnalysis.class)`    |
| `enhanceWithMetadata(analysis, …)` | Overwrites `AnalysisMetadata` with actual timestamp, processing time, prompt source |

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
       ├─ Cache HIT  →  return cached PromptPlugin
       └─ Cache MISS →  resolve via strategy
              EXTERNAL_FIRST: external-config/prompts/<name>.md → classpath fallback
              INTERNAL_FIRST: classpath prompts/default/<name>.md → external fallback
              EXTERNAL_ONLY : external only, null if missing

── PHASE 1: Java data collection (no LLM) ──────────────────────────────────────

4. Fetch ticket
   └─▶ JiraRetrievalTool.retrieveJiraTicket(ticketId)
       └─ WebClient.exchangeToMono() → GET /rest/api/2/issue/{id}
          Logs HTTP status and response size. Returns stub JSON if unconfigured.

5. Extract linked issue keys
   └─▶ extractLinkedIssueKeys(ticketJson)
       └─ parses fields.issuelinks[].inwardIssue.key / outwardIssue.key

6. Fetch linked issues (if any exist)
   └─▶ JiraRetrievalTool.searchJiraTickets("key in (KEY-1, KEY-2, ...)")
       └─ real JQL built from step 5 — no LLM-generated placeholders

7. Create output folder
   └─▶ FileSystemTool.createTicketFolder(ticketId)
       └─ creates ./analysis-output/{ticketId}/

8. Compact data for LLM context
   ├─▶ extractEssentialTicketData()  →  key, summary, description (≤4000 chars), status, priority, ...
   └─▶ extractEssentialLinkedData()  →  key + summary + status per linked issue

── PHASE 2: LLM analysis (no tool calls) ────────────────────────────────────────

9. Inject data into prompt
   └─▶ promptContent = template
         .replace("{{ticketData}}", compactTicketJson)
         .replace("{{linkedIssuesData}}", compactLinkedJson)
         + JSON_ONLY_REMINDER

10. LLM call — analysis only
    └─▶ ChatClient.prompt().user(promptContent).call().content()
        No .tools() registered — LLM receives pre-fetched data and returns JSON analysis

11. Parse LLM response
    └─▶ Extract {…} substring → ObjectMapper.readValue(…, TicketAnalysis.class)

12. Metadata enrichment
    └─▶ enhanceWithMetadata() fills AnalysisMetadata:
        analysisTimestamp, modelUsed, analysisVersion,
        processingTimeMs, promptName, promptSource, promptLastModified

13. Return TicketAnalysis as JSON to caller
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
          num-ctx: 32768              # large context window for data-injection prompts

management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLE_RATE:0.0}   # 0 by default; set to 1.0 to enable
  zipkin:
    tracing:
      export:
        enabled: ${ZIPKIN_ENABLED:false}          # opt-in; requires a running Zipkin server
      endpoint: ${ZIPKIN_URL:http://localhost:9411/api/v2/spans}
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

- Passed to `JiraRetrievalTool.retrieveJiraTicket(ticketId)` in Java during Phase 1
- Injected as `{{ticketId}}` into every prompt template for reference
- Used as the folder name in `FileSystemTool.createTicketFolder(ticketId)`

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
  "summary": "This task requires implementing user authentication to allow secure login and session management for end users; the security team needs this to meet compliance requirements; the key technical challenge is integrating JWT issuance with the existing legacy identity system.",
  "requirements_analysis": {
    "functional_requirements": [
      "User login and logout via email/password",
      "Password reset flow with email verification",
      "Session management with configurable expiry",
      "Audit log of failed authentication attempts"
    ],
    "non_functional_requirements": [
      "Login response time under 2 seconds",
      "OAuth 2.0 / JWT token security",
      "GDPR-compliant handling of user credentials"
    ],
    "acceptance_criteria": [
      "Users can login with email/password and receive a JWT",
      "Failed attempts after 5 tries trigger a lockout"
    ],
    "dependencies": [
      "Database schema migration for users table",
      "Identity provider or OAuth server",
      "Email service for password reset"
    ],
    "assumptions": [
      "OAuth provider is available and accessible",
      "Email service is configured",
      "Database is provisioned with sample user data"
    ]
  },
  "technical_analysis": {
    "complexity_score": 7,
    "technical_challenges": [
      "JWT token lifecycle management (expiry, refresh, revocation)",
      "Integration with legacy identity system",
      "Secure storage and transmission of credentials"
    ],
    "recommended_approach": "Implement Spring Security with JWT issuance. Start with core login/logout, then layer in refresh tokens and rate limiting. Validate against acceptance criteria before adding OAuth provider integration.",
    "architecture_considerations": [
      "Stateless JWT tokens — no server-side session state",
      "Redis or in-memory blacklist for revoked tokens",
      "Separate auth service vs embedded in monolith"
    ],
    "technology_stack": [
      "Java 21, Spring Boot 3.5",
      "Spring Security + JWT (jjwt)",
      "PostgreSQL for user storage",
      "Redis for token blacklist"
    ],
    "performance_considerations": [
      "Token validation adds ~1ms overhead per request — acceptable",
      "Redis lookup latency must stay under 5ms to meet 2s SLA"
    ]
  },
  "risk_assessment": {
    "identified_risks": [
      {
        "description": "JWT secret key exposure in configuration files",
        "category": "SECURITY",
        "impact": "HIGH",
        "probability": "MEDIUM",
        "mitigation_strategy": "Store JWT secret in Vault or environment variable, never in source code",
        "contingency_plan": "Rotate secret immediately, invalidate all existing tokens"
      },
      {
        "description": "Legacy identity system API instability causing login failures",
        "category": "TECHNICAL",
        "impact": "HIGH",
        "probability": "MEDIUM",
        "mitigation_strategy": "Implement circuit breaker pattern with fallback to local credential store",
        "contingency_plan": "Switch to local-only auth mode until legacy system is restored"
      },
      {
        "description": "Sprint timeline at risk if OAuth integration is more complex than estimated",
        "category": "TIMELINE",
        "impact": "MEDIUM",
        "probability": "MEDIUM",
        "mitigation_strategy": "Timebox OAuth integration to 2 days; ship basic JWT auth first",
        "contingency_plan": "Defer OAuth to next sprint, release with email/password only"
      }
    ],
    "overall_risk_level": "MEDIUM",
    "risk_score": 6
  },
  "effort_estimation": {
    "development_days": 8,
    "testing_days": 3,
    "documentation_days": 2,
    "review_days": 1,
    "total_days": 14,
    "confidence_level": "HIGH",
    "estimation_method": "Expert Judgement",
    "team_size_assumption": 2
  },
  "implementation_strategy": {
    "phases": [
      {
        "name": "Phase 1 — Core Auth & Data Layer",
        "description": "Implement login/logout endpoints, JWT issuance, user schema migration, and unit tests",
        "estimated_days": 5,
        "deliverables": [
          "Login/logout REST endpoints",
          "JWT token service",
          "DB migration scripts"
        ],
        "dependencies": [
          "Database schema agreed"
        ],
        "risks": [
          "Schema changes may require stakeholder approval"
        ]
      },
      {
        "name": "Phase 2 — Security Hardening & Integration",
        "description": "Add rate limiting, token refresh, password reset flow, OAuth integration, and integration tests",
        "estimated_days": 9,
        "deliverables": [
          "Password reset flow",
          "Rate limiting middleware",
          "Security review sign-off"
        ],
        "dependencies": [
          "Phase 1 complete",
          "Email service configured"
        ],
        "risks": [
          "OAuth provider setup may require external coordination"
        ]
      }
    ],
    "key_milestones": [
      {
        "name": "Core Auth API Complete",
        "description": "Login and logout endpoints functional with JWT issuance",
        "target_date": "End of Day 5",
        "success_criteria": [
          "Login returns 200 + JWT",
          "Invalid credentials return 401"
        ]
      },
      {
        "name": "Security Review Passed",
        "description": "All acceptance criteria met, penetration test completed",
        "target_date": "End of Day 14",
        "success_criteria": [
          "No high-severity findings",
          "All acceptance criteria green"
        ]
      }
    ],
    "success_criteria": [
      "All acceptance criteria from the ticket are met",
      "Security review completed with no high-severity findings",
      "Login response time under 2 seconds under normal load"
    ],
    "rollback_strategy": "Feature flag to disable new auth module; revert to legacy auth path if critical issues found post-deploy"
  },
  "analysis_metadata": {
    "analysis_timestamp": "2024-06-09T10:30:00",
    "model_used": "llama3.1:8b",
    "analysis_version": "1.0.0",
    "processing_time_ms": 38400,
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

- Use `{{ticketData}}` — pre-fetched, compacted ticket JSON injected by Java before the LLM call
- Use `{{linkedIssuesData}}` — pre-fetched linked issue summaries injected by Java before the LLM call
- Accept `{{ticketId}}` and any additional `{{key}}` from the context map
- Do **not** instruct the LLM to call any tools — all data is already present in the prompt

### Template variables

| Variable               | Injected by                    | Content                                                                                |
|------------------------|--------------------------------|----------------------------------------------------------------------------------------|
| `{{ticketData}}`       | `extractEssentialTicketData()` | key, summary, description (≤4000 chars), issuetype, status, priority, assignee, labels |
| `{{linkedIssuesData}}` | `extractEssentialLinkedData()` | Array of `{key, summary, status}` for each linked issue                                |
| `{{ticketId}}`         | Plugin context map             | Raw ticket key string, e.g. `PROJ-123`                                                 |
| Any `{{key}}`          | `context` request body field   | Custom variables from the caller                                                       |

### Overriding a built-in prompt

1. Create `external-config/prompts/<name>.md` using the same filename as the built-in.
2. Write your prompt using `{{ticketData}}` and `{{linkedIssuesData}}` — the data is injected automatically.
3. With `hot-reload.enabled: true` (default), the file is detected within seconds and the cache is invalidated — no
   restart needed.
4. To revert, delete the external file. The classpath version takes over on the next cache miss.

### Writing a custom prompt from scratch

```markdown
# My Custom Analysis for {{ticketId}}

You are an expert engineer. Analyze the ticket data below.

TICKET:
{{ticketData}}

LINKED ISSUES:
{{linkedIssuesData}}

Additional context:

- Team size: {{teamSize}}
- Expected load: {{expectedLoad}}

Perform your analysis and output ONLY a valid JSON object starting with { and ending with }.
```

Save it to `external-config/prompts/my-custom-prompt.md` and call it with:

```bash
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/custom \
  -H "Content-Type: application/json" \
  -d '{"promptName": "my-custom-prompt", "context": {"teamSize": "3", "expectedLoad": "1000 rps"}}'
```

The service automatically fetches ticket and linked issue data from Jira, compacts it, and injects it into your template
before sending to the LLM. No tool call instructions are needed in the prompt.

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

Both methods are called directly from Java in Phase 1 — **not** triggered by the LLM.

### Configure credentials

```bash
export JIRA_BASE_URL=https://your-company.atlassian.net
export JIRA_USERNAME=your@email.com
export JIRA_API_TOKEN=your-api-token
```

API tokens are created at: `https://id.atlassian.com/manage-profile/security/api-tokens`

### Jira API calls made

| Method               | Endpoint                                       | Notes                                                         |
|----------------------|------------------------------------------------|---------------------------------------------------------------|
| `retrieveJiraTicket` | `GET /rest/api/2/issue/{id}`                   | No `?expand=` parameter — base fields are sufficient          |
| `searchJiraTickets`  | `GET /rest/api/2/search?jql=...&maxResults=20` | JQL uses real keys extracted from `fields.issuelinks` in Java |

Both use `WebClient.exchangeToMono()` which logs the HTTP status code on every response. Non-2xx responses are thrown as
`WebClientResponseException` with the response body included for diagnosis.

### Stub mode

When credentials are absent, `retrieveJiraTicket` returns:

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

| Metric                          | Tag                                    | Notes                                  |
|---------------------------------|----------------------------------------|----------------------------------------|
| `jira.analysis.duration`        | `jira-analysis-complete` (observation) | End-to-end including Phase 1 + Phase 2 |
| `jira.analysis.custom.duration` | `jira-analysis-custom` (observation)   | Same but for custom-prompt calls       |

```bash
curl -s http://localhost:8080/actuator/metrics/jira.analysis.duration | jq '.measurements'
```

Typical values with `llama3.1:8b` on a local GPU: 30–90 seconds (dominated by LLM token generation).

### Distributed tracing (Zipkin)

Tracing is **disabled by default**. Enable it by setting environment variables before startup:

```bash
export ZIPKIN_ENABLED=true
export ZIPKIN_URL=http://your-zipkin-host:9411/api/v2/spans
export TRACING_SAMPLE_RATE=1.0    # 1.0 = 100% sampling; use 0.1 in production
mvn spring-boot:run
```

Without a running Zipkin server, leave `ZIPKIN_ENABLED` unset (defaults to `false`) to avoid
`java.net.ConnectException` errors on startup.

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

| Area                        | Status                  | Notes                                                                                                                                                                                                    |
|-----------------------------|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Description truncation      | 4000-char hard limit    | `extractEssentialTicketData()` silently truncates descriptions longer than 4000 characters. Tickets with long acceptance-criteria sections may lose content. Increase `MAX_DESCRIPTION_CHARS` if needed. |
| Template engine             | Simple `String.replace` | StringTemplate 4 is on the classpath but only basic `{{key}}` substitution is implemented. ST4 conditionals and loops are planned.                                                                       |
| File access security        | No path whitelist       | `FileSystemTool.readFile()` accepts arbitrary paths. Add a configurable whitelist before exposing the agent publicly.                                                                                    |
| Analysis result persistence | Folder only             | `createTicketFolder` creates the directory but analysis JSON is not yet auto-saved to disk. A post-analysis hook could write the result file.                                                            |
| pgvector / embeddings       | Not included            | Removed due to DataSource startup failure when unconfigured. Re-add `spring-ai-starter-vector-store-pgvector` and a datasource config if semantic search over historical analyses is needed.             |
| Jira write-back             | Not implemented         | The agent reads Jira but does not post comments or update fields with analysis results.                                                                                                                  |
| LLM model                   | llama3.1:8b local only  | Generation speed is constrained by local hardware. Cloud providers (Anthropic Claude, OpenAI GPT-4o) via Spring AI would reduce analysis time from ~60s to ~5s.                                          |
