# Plugin-Stats Module — Architecture & Usage Guide

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Component Reference](#component-reference)
4. [Workflow](#workflow)
5. [Setup](#setup)
6. [How to Interact with the Agent](#how-to-interact-with-the-agent)
7. [Input Reference](#input-reference)
8. [Output Reference](#output-reference)
9. [Prompt Templates](#prompt-templates)
10. [Hot Reload](#hot-reload)
11. [Observability & Metrics](#observability--metrics)
12. [Known Limitations & TODOs](#known-limitations--todos)

---

## Overview

The **Plugin-Stats** subsystem is the prompt-management and observability layer of the Jira Analysis Agent. It is responsible for:

- Loading, caching, and serving Markdown prompt templates to the AI engine
- Choosing between external (file-system) and internal (classpath) prompt sources at runtime
- Hot-reloading prompts without restarting the application
- Exposing cache-health statistics via a REST monitoring endpoint

The subsystem sits between the REST/service layer and the LLM (Ollama/llama3.1:8b), ensuring the correct, up-to-date prompt is always delivered to the model.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     HTTP Clients / Tests                     │
└────────────────────────────┬────────────────────────────────┘
                             │ REST calls
┌────────────────────────────▼────────────────────────────────┐
│              MonitoringController  /api/v1/monitoring        │
│   GET /health          GET /plugin-stats                     │
└────────────────────────────┬────────────────────────────────┘
                             │ delegates
┌────────────────────────────▼────────────────────────────────┐
│           OptimizedJiraAnalysisService  (@Service)           │
│  analyzeTicket(ticketId)                                     │
│  analyzeTicket(ticketId, promptName, context)                │
│  getCachedAnalysis(ticketId, promptName)  (@Cacheable)       │
│  isHealthy()  /  getPluginStatistics()                       │
└────────┬───────────────────────────────────────────────────┘
         │ loadPlugin(name, context)
┌────────▼───────────────────────────────────────────────────┐
│       OptimizedPromptPluginManager  (@Component)            │
│                                                             │
│  ┌─────────────────┐    ┌──────────────────────────────┐   │
│  │  pluginCache     │    │  lastModifiedCache            │   │
│  │  (Caffeine)      │    │  (Caffeine)                   │   │
│  │  max 1000        │    │  max 1000                     │   │
│  │  expire 1h       │    │  expire 2h                    │   │
│  │  refresh 5min    │    │                               │   │
│  └────────┬────────┘    └──────────────────────────────┘   │
│           │ cache miss: loadPluginFromSource(name)           │
│  ┌────────▼─────────────────────────────────────────────┐   │
│  │              Loading Strategy (enum)                  │   │
│  │  EXTERNAL_FIRST │ INTERNAL_FIRST │ EXTERNAL_ONLY      │   │
│  └────────┬────────────────────┬────────────────────────┘   │
│           │                    │                             │
│  ┌────────▼──────┐   ┌────────▼──────────────────────────┐ │
│  │ External Disk │   │ Classpath (JAR/resources)          │ │
│  │ external-config│   │ prompts/default/*.md               │ │
│  │ /prompts/*.md │   │                                    │ │
│  └───────────────┘   └────────────────────────────────────┘ │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  DirectoryWatcher (methvin)  — hot reload on .md     │   │
│  │  change → invalidate cache → async re-load           │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
         │ prompt content
┌────────▼────────────────────────────────────────────────────┐
│          ChatClient (Spring AI / Ollama)                     │
│  .user(promptContent).tools(jiraRetrievalTool, fsTool)       │
│  .call().entity(TicketAnalysis.class)                        │
└────────┬────────────────────────────────────────────────────┘
         │ tool calls (LLM-initiated)
┌────────▼────────────────────────────────────────────────────┐
│  JiraRetrievalTool        │  FileSystemTool                  │
│  getTicket(ticketId)      │  readFile(path)                  │
│  searchTickets(jql)       │  listDirectory(path)             │
└─────────────────────────────────────────────────────────────┘
```

---

## Component Reference

### `PluginManager<T>` (interface)

Generic plugin contract. Any future plugin type (not only prompts) can implement this interface.

| Method | Description |
|---|---|
| `loadPlugin(name)` | Load plugin by name, no variable substitution |
| `loadPlugin(name, context)` | Load plugin with `{{key}}` template variables replaced from `context` |
| `reloadPlugin(name)` | Invalidate and reload a single plugin |
| `reloadAllPlugins()` | Invalidate all cached plugins and re-warm |
| `getAvailablePlugins()` | Returns list of all known plugin names |
| `isPluginAvailable(name)` | Returns `true` if the plugin can be resolved |

---

### `PromptPlugin` (record)

Immutable value object representing a loaded prompt.

| Field | Type | Description |
|---|---|---|
| `name` | `String` | Plugin identifier, matches the `.md` filename without extension |
| `content` | `String` | Full Markdown prompt text (after template substitution) |
| `metadata` | `Map<String, Object>` | Provenance info: file path, last modified, size, source |
| `lastModified` | `LocalDateTime` | When the source file was last modified |
| `source` | `PluginSource` | `EXTERNAL_FILE` or `INTERNAL_RESOURCE` |

---

### `OptimizedPromptPluginManager` (main implementation)

Spring `@Component`. Manages the full plugin lifecycle.

**Configuration properties** (all under `agent.plugins`):

| Property | Default | Description |
|---|---|---|
| `external-config-path` | `./external-config` | Root directory for external prompt overrides |
| `strategy` | `EXTERNAL_FIRST` | Plugin loading strategy (see below) |
| `hot-reload.enabled` | `true` | Enable filesystem watch for live prompt updates |

**Loading strategies:**

| Strategy | Behaviour |
|---|---|
| `EXTERNAL_FIRST` | Tries `external-config/prompts/<name>.md`; falls back to classpath |
| `INTERNAL_FIRST` | Tries classpath first; falls back to external directory |
| `EXTERNAL_ONLY` | Only loads from external directory; logs a warning and returns `null` if absent |

---

### `MonitoringController`

Base path: `/api/v1/monitoring`

| Endpoint | Response |
|---|---|
| `GET /health` | `{"status":"UP"/"DOWN","timestamp":<epoch ms>,"service":"jira-analysis-agent"}` |
| `GET /plugin-stats` | Caffeine cache stats — see [Observability](#observability--metrics) |

---

### `OptimizedJiraAnalysisService`

The service that ties the plugin manager, LLM, and tools together.

| Method | Micrometer Tag | Description |
|---|---|---|
| `analyzeTicket(ticketId)` | `jira.analysis.duration` | Quick call using default `analysis-prompt` |
| `analyzeTicket(ticketId, promptName, context)` | `jira.analysis.custom.duration` | Full call with custom prompt and template variables |
| `getCachedAnalysis(ticketId, promptName)` | — | Spring-cached wrapper (key: `{ticketId}_{promptName}`) |
| `isHealthy()` | — | Returns `true` if the plugin manager can load `analysis-prompt` |
| `getPluginStatistics()` | — | Delegates to `OptimizedPromptPluginManager.getCacheStatistics()` |

---

## Workflow

```
1. Request arrives
   └─▶ OptimizedJiraAnalysisService.analyzeTicket(ticketId, promptName, context)

2. Load prompt plugin
   └─▶ OptimizedPromptPluginManager.loadPlugin(promptName, {ticketId, ...context})
       ├─ Cache HIT  →  return cached PromptPlugin, apply {{}} substitution
       └─ Cache MISS →  resolve via loading strategy
              ├─ EXTERNAL_FIRST: read external-config/prompts/<name>.md
              │        (fallback to classpath on missing/error)
              └─ INTERNAL_FIRST: read classpath prompts/default/<name>.md
                       (fallback to external on missing/error)

3. Build prompt content
   └─▶ Replace {{ticketId}} and any other {{key}} placeholders

4. Call LLM
   └─▶ ChatClient.prompt()
         .user(promptContent)
         .tools(jiraRetrievalTool, fileSystemTool)
         .call()
         .entity(TicketAnalysis.class)

5. LLM tool calls (during reasoning)
   ├─▶ JiraRetrievalTool.getTicket(ticketId)     → ticket JSON
   ├─▶ JiraRetrievalTool.searchTickets(jql)      → related tickets JSON
   └─▶ FileSystemTool.readFile(path)             → file content string

6. Structured output deserialization
   └─▶ Spring AI maps model JSON response → TicketAnalysis record

7. Metadata enrichment
   └─▶ enhanceWithMetadata() fills AnalysisMetadata:
       timestamp, model, version, processingTimeMs, promptName, promptSource

8. Return TicketAnalysis
```

---

## Setup

### Prerequisites

| Requirement | Details |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Ollama | Running locally at `http://localhost:11434` |
| llama3.1:8b model | `ollama pull llama3.1:8b` |

### Install Ollama and pull the model

```bash
# macOS / Linux
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3.1:8b

# Windows
# Download installer from https://ollama.com/download
ollama pull llama3.1:8b
```

### Build and run

```bash
cd jira-analysis-agent
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

### Optional: external prompt directory

Create the external config directory (auto-created at startup if it does not exist):

```bash
mkdir -p external-config/prompts
```

Place any `.md` files here to override the built-in prompts without rebuilding. With `hot-reload.enabled: true` (default), changes are picked up automatically within seconds.

### Configuration overrides

All settings live in `src/main/resources/application.yml`. Key values to customise:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434   # Ollama server URL
      chat:
        model: llama3.1:8b              # Model to use
        options:
          temperature: 0.2              # Lower = more deterministic
          num-ctx: 8192                 # Context window tokens

agent:
  plugins:
    external-config-path: ./external-config   # Path to override prompts
    strategy: EXTERNAL_FIRST                  # EXTERNAL_FIRST | INTERNAL_FIRST | EXTERNAL_ONLY
    hot-reload:
      enabled: true                           # Live prompt reload
```

---

## How to Interact with the Agent

> **Important:** There is currently no analysis REST endpoint wired up. `OptimizedJiraAnalysisService.analyzeTicket()` is fully implemented but only reachable from integration tests or by calling the service bean directly (e.g., via a Spring test or a custom controller you add). The only exposed endpoints are the monitoring ones.

### Currently available HTTP endpoints

#### Health check

```http
GET http://localhost:8080/api/v1/monitoring/health
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": 1717891200000,
  "service": "jira-analysis-agent"
}
```

#### Plugin cache statistics

```http
GET http://localhost:8080/api/v1/monitoring/plugin-stats
```

**Response:**
```json
{
  "hitRate": 0.85,
  "missRate": 0.15,
  "loadCount": 12,
  "evictionCount": 0,
  "estimatedSize": 4
}
```

#### Spring Boot Actuator (built-in)

```http
GET http://localhost:8080/actuator/health
GET http://localhost:8080/actuator/metrics
GET http://localhost:8080/actuator/prometheus
GET http://localhost:8080/actuator/info
```

### Calling the analysis service directly (from Java)

```java
@Autowired
OptimizedJiraAnalysisService analysisService;

// Simple analysis — uses the built-in "analysis-prompt"
TicketAnalysis result = analysisService.analyzeTicket("PROJ-123");

// Custom prompt
TicketAnalysis result = analysisService.analyzeTicket(
    "PROJ-123",
    "risk-assessment-prompt",
    Map.of("extraContext", "This is a P0 incident")
);

// Cached — repeated calls with the same ticketId + promptName hit the cache
TicketAnalysis cached = analysisService.getCachedAnalysis("PROJ-123", "analysis-prompt");
```

### Adding your own analysis REST endpoint

The service is ready — you just need to expose it. Create a controller:

```java
@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final OptimizedJiraAnalysisService analysisService;

    public AnalysisController(OptimizedJiraAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/{ticketId}")
    public TicketAnalysis analyze(@PathVariable String ticketId) {
        return analysisService.analyzeTicket(ticketId);
    }

    @PostMapping("/{ticketId}")
    public TicketAnalysis analyzeWithPrompt(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "analysis-prompt") String promptName,
            @RequestBody(required = false) Map<String, Object> context) {
        return analysisService.analyzeTicket(ticketId, promptName, context != null ? context : Map.of());
    }
}
```

---

## Input Reference

### Ticket ID

A Jira-style ticket identifier string, e.g. `"PROJ-123"`, `"BACKEND-42"`. Passed to:
- The `getTicket(ticketId)` tool call the LLM makes during reasoning
- The `{{ticketId}}` placeholder in every prompt template

### Prompt name

| Name | File | Purpose |
|---|---|---|
| `analysis-prompt` | `prompts/default/analysis-prompt.md` | Comprehensive full analysis (default) |
| `code-review-prompt` | `prompts/default/code-review-prompt.md` | Code quality, security, performance review |
| `risk-assessment-prompt` | `prompts/default/risk-assessment-prompt.md` | Risk identification and scoring |
| `effort-estimation-prompt` | `prompts/default/effort-estimation-prompt.md` | Story point / day estimation |

### Context map

Optional `Map<String, Object>` of additional template variables. Any key `foo` in the map replaces `{{foo}}` in the prompt. The `ticketId` key is always injected automatically.

Example:
```json
{
  "teamSize": "3",
  "sprintLength": "2 weeks",
  "techStack": "Spring Boot, React"
}
```

---

## Output Reference

All analysis methods return a `TicketAnalysis` record serialised as JSON. Top-level structure:

```json
{
  "ticket_id": "PROJ-123",
  "summary": "Brief summary of the ticket",
  "requirements_analysis": { ... },
  "technical_analysis": { ... },
  "risk_assessment": { ... },
  "effort_estimation": { ... },
  "implementation_strategy": { ... },
  "metadata": { ... }
}
```

### `requirements_analysis`

```json
{
  "functional_requirements": ["list of functional requirements"],
  "non_functional_requirements": ["performance, scalability, ..."],
  "acceptance_criteria": ["Given / When / Then statements"],
  "dependencies": ["external systems or teams"],
  "assumptions": ["things assumed true during analysis"]
}
```

### `technical_analysis`

```json
{
  "complexity_score": 7,
  "technical_challenges": ["list of challenges"],
  "recommended_approach": "description of approach",
  "architecture_considerations": ["list of considerations"],
  "technology_stack": ["Spring Boot", "PostgreSQL", "..."],
  "performance_considerations": ["list of perf concerns"]
}
```

### `risk_assessment`

```json
{
  "identified_risks": [
    {
      "description": "Risk description",
      "category": "TECHNICAL | BUSINESS | TIMELINE | RESOURCE",
      "impact": "HIGH | MEDIUM | LOW",
      "probability": "HIGH | MEDIUM | LOW",
      "mitigation_strategy": "How to prevent",
      "contingency_plan": "What to do if it happens"
    }
  ],
  "overall_risk_level": "HIGH | MEDIUM | LOW",
  "risk_score": 6
}
```

### `effort_estimation`

```json
{
  "development_days": 5,
  "testing_days": 2,
  "documentation_days": 1,
  "review_days": 1,
  "total_days": 9,
  "confidence_level": "HIGH | MEDIUM | LOW",
  "estimation_method": "Story Points | T-shirt sizing | ...",
  "team_size_assumption": 2
}
```

### `implementation_strategy`

```json
{
  "phases": [
    {
      "name": "Phase 1",
      "description": "...",
      "estimated_days": 3,
      "deliverables": ["..."],
      "dependencies": ["..."],
      "risks": ["..."]
    }
  ],
  "key_milestones": [
    {
      "name": "Milestone name",
      "description": "...",
      "target_date": "...",
      "success_criteria": ["..."]
    }
  ],
  "success_criteria": ["overall success criteria"],
  "rollback_strategy": "How to roll back if things go wrong"
}
```

### `metadata`

```json
{
  "analysis_timestamp": "2024-06-09T14:30:00",
  "model_used": "llama3.1:8b",
  "analysis_version": "1.0.0",
  "processing_time_ms": 4823,
  "prompt_name": "analysis-prompt",
  "prompt_source": "EXTERNAL_FILE | INTERNAL_RESOURCE",
  "prompt_last_modified": "2024-06-09T10:00:00"
}
```

---

## Prompt Templates

Built-in templates live at `src/main/resources/prompts/default/`. All support the `{{ticketId}}` placeholder plus any additional keys from the context map.

### Overriding a built-in prompt

1. Create `external-config/prompts/<name>.md` (same filename as the built-in).
2. Write your custom prompt. Use `{{ticketId}}` where the ticket ID should appear.
3. If `hot-reload.enabled: true` (default), the new prompt is picked up within seconds — no restart needed.
4. If you want to revert, delete the external file. The built-in classpath version takes over on the next cache miss.

### Writing a custom prompt

The model will always try to return a JSON object matching `TicketAnalysis`. Your prompt must instruct it to:

- Call `getTicket(ticketId)` to retrieve ticket data.
- Return a valid JSON object that matches the `TicketAnalysis` schema.

Minimal custom prompt skeleton:

```markdown
You are an expert Jira analyst.

Analyse ticket {{ticketId}}:
1. Call getTicket("{{ticketId}}") to fetch the ticket details.
2. Perform your analysis.
3. Return a JSON object matching the TicketAnalysis schema.

Focus on: [your specific instructions here]
```

---

## Hot Reload

The `DirectoryWatcher` monitors `external-config/` (configurable via `agent.plugins.external-config-path`).

**Trigger condition:** Any `.md` file under a `prompts/` path inside the watched directory is created, modified, or deleted.

**Effect:**
1. Both `pluginCache` and `lastModifiedCache` entries for that plugin name are invalidated.
2. The plugin is asynchronously reloaded from the new source.
3. The next call to `loadPlugin(name, ...)` gets the updated content.

**Disable hot reload** (e.g., in production to avoid filesystem permissions issues):

```yaml
agent:
  plugins:
    hot-reload:
      enabled: false
```

---

## Observability & Metrics

### Plugin cache statistics (`GET /api/v1/monitoring/plugin-stats`)

| Field | Description |
|---|---|
| `hitRate` | Fraction of cache lookups served from cache (0.0–1.0) |
| `missRate` | Fraction that required a load from disk/classpath |
| `loadCount` | Total number of cache loads performed |
| `evictionCount` | Number of entries evicted from the cache |
| `estimatedSize` | Current approximate number of cached plugins |

### Micrometer timers

| Metric name | Description |
|---|---|
| `jira.analysis.duration` | Time for default `analyzeTicket(ticketId)` |
| `jira.analysis.custom.duration` | Time for `analyzeTicket(ticketId, promptName, context)` |

Both are tagged with `@Observed` for distributed tracing (Brave/Zipkin). Traces are exported at 100% sampling by default (`management.tracing.sampling.probability: 1.0`). Set to `0.1` in production to reduce volume.

### Prometheus / Grafana

Metrics are available at `GET /actuator/prometheus`. Point Prometheus at this endpoint and import the Spring Boot + Micrometer dashboard in Grafana.

---

## Known Limitations & TODOs

| Area | Status | Notes |
|---|---|---|
| Jira REST API | **Stub only** | `JiraRetrievalTool.getTicket()` and `searchTickets()` return hard-coded placeholder data. Replace with actual Jira REST API calls using your Jira base URL and API token. |
| Analysis REST endpoint | **Not wired** | `analyzeTicket()` is implemented but no HTTP controller exposes it yet. See [How to Interact](#how-to-interact-with-the-agent) for the snippet to add. |
| Template engine | **Partially used** | StringTemplate 4 (ST4) is on the classpath but `{{}}` substitution is done via a simple `String.replace` loop. Full ST4 support (conditionals, loops) is planned. |
| File access security | **Open** | `FileSystemTool.readFile()` accepts arbitrary paths. Add a path whitelist before exposing the analysis endpoint publicly. |
| pgvector / embeddings | **Removed** | The vector store dependency was removed. Re-add `spring-ai-starter-vector-store-pgvector` + a datasource config if semantic search over historical tickets is required. |
