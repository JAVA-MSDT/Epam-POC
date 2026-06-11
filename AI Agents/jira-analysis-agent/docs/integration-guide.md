# Jira Analysis Agent — Integration Guide

## Table of Contents

1. [Using the Agent Directly](#1-using-the-agent-directly)
2. [Integrating with Claude Projects](#2-integrating-with-claude-projects)
3. [Integrating via Claude API Tool Use](#3-integrating-via-claude-api-tool-use)
4. [Exposing as an MCP Server](#4-exposing-as-an-mcp-server)
5. [Chaining with Other AI Agents](#5-chaining-with-other-ai-agents)
6. [Spring AI Multi-Agent Chains](#6-spring-ai-multi-agent-chains)
7. [LangChain / LangGraph Integration](#7-langchain--langgraph-integration)
8. [Automation Platform Integration (n8n / Make / Zapier)](#8-automation-platform-integration-n8n--make--zapier)
9. [Event-Driven Pipeline (Jira Webhook)](#9-event-driven-pipeline-jira-webhook)
10. [Batch Analysis Patterns](#10-batch-analysis-patterns)
11. [Response Schema Reference](#11-response-schema-reference)

---

## 1. Using the Agent Directly

### 1.1 Prerequisites

| Requirement    | Version | Notes                                            |
|----------------|---------|--------------------------------------------------|
| Java           | 21+     | `java -version`                                  |
| Maven          | 3.9+    | `mvn -version`                                   |
| Ollama         | latest  | Running at `http://localhost:11434`              |
| llama3.1:8b    | —       | `ollama pull llama3.1:8b`                        |
| Jira API token | —       | Optional — agent works with stub data without it |

### 1.2 Start the Agent

```bash
# Minimal — stub Jira data, analysis still works
mvn spring-boot:run

# With real Jira
export JIRA_BASE_URL=https://your-company.atlassian.net
export JIRA_USERNAME=you@company.com
export JIRA_API_TOKEN=your-token
mvn spring-boot:run
```

Agent listens on `http://localhost:8080` by default.

### 1.3 Run Your First Analysis

```bash
# Default comprehensive analysis
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123

# Security-focused analysis
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/security

# Risk assessment
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/custom \
  -H "Content-Type: application/json" \
  -d '{"promptName": "risk-assessment-prompt"}'

# Effort estimation with team context
curl -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123/custom \
  -H "Content-Type: application/json" \
  -d '{
    "promptName": "effort-estimation-prompt",
    "context": {
      "teamSize": "3",
      "sprintLength": "2 weeks",
      "techStack": "Spring Boot, React, PostgreSQL"
    }
  }'
```

### 1.4 Use the Cached Endpoint for Repeated Reads

If you call the same ticket multiple times (for example from a dashboard or multiple agents),
use the cached endpoint to avoid redundant LLM calls:

```bash
# Returns cached result if available; triggers fresh analysis on cache miss
curl "http://localhost:8080/api/v1/analysis/tickets/PROJ-123/cached?promptName=analysis-prompt"
```

Cache TTL is 10 minutes (`expireAfterWrite=600s`). A cache hit returns in milliseconds.

### 1.5 Verify Agent Health Before Calling

```bash
curl http://localhost:8080/api/v1/monitoring/health
# {"status":"UP","timestamp":...,"service":"jira-analysis-agent"}
```

Use this as a liveness/readiness probe before dispatching analysis requests.

### 1.6 List Available Prompts

```bash
curl http://localhost:8080/api/v1/plugins/available
# ["analysis-prompt","code-review-prompt","effort-estimation-prompt",
#  "risk-assessment-prompt","security-analysis-prompt"]
```

---

## 2. Integrating with Claude Projects

A Claude Project can use this agent as its data source — Claude reasons over the structured
analysis output rather than raw Jira JSON.

### 2.1 Architecture

```
User ──▶ Claude Project (browser or API)
              │
              ▼
     Claude calls this agent's REST API
     (via tool use or manual paste)
              │
              ▼
     Jira Analysis Agent
     ├── Phase 1: fetches Jira data
     └── Phase 2: LLM-generates TicketAnalysis JSON
              │
              ▼
     Claude receives structured JSON
     and reasons over it for the user
```

### 2.2 System Prompt for a Claude Project

Create a Claude Project and set this system prompt:

```
You are a senior engineering assistant with access to a Jira Analysis Agent running at http://localhost:8080.

When a user provides a Jira ticket ID:
1. Call POST http://localhost:8080/api/v1/analysis/tickets/{ticketId} to get the full analysis.
2. Present the key findings: summary, top 3 risks, effort estimate, and recommended approach.
3. Answer follow-up questions by reasoning over the structured analysis JSON.

The agent returns a TicketAnalysis JSON with these top-level fields:
- ticket_id, summary
- requirements_analysis (functional_requirements, acceptance_criteria, dependencies, assumptions)
- technical_analysis (complexity_score 1-10, technical_challenges, recommended_approach, technology_stack)
- risk_assessment (identified_risks[], overall_risk_level, risk_score 1-10)
- effort_estimation (development_days, testing_days, total_days, confidence_level)
- implementation_strategy (phases[], key_milestones[], rollback_strategy)
- analysis_metadata (model_used, processing_time_ms, prompt_name)

For security tickets, use POST /api/v1/analysis/tickets/{ticketId}/security instead.
For risk focus, use POST /api/v1/analysis/tickets/{ticketId}/custom with body {"promptName":"risk-assessment-prompt"}.
```

### 2.3 Knowledge Files in the Project

Upload these files as project knowledge so Claude understands the agent without re-reading them each session:

- `docs/plugin-stats/README.md` — full API and architecture reference
- `docs/integration-guide.md` — this file

### 2.4 Typical Conversation Flow

```
User:    "Analyze PROJ-123 and tell me if we should prioritize it this sprint."

Claude:  [calls POST /api/v1/analysis/tickets/PROJ-123]
         [receives TicketAnalysis JSON]
         "PROJ-123 has an overall risk score of 7/10 with HIGH complexity (8/10).
          The top blocker is an external OAuth provider dependency that isn't yet
          provisioned. Estimated effort is 14 days for a team of 2.
          Recommendation: do not pull into this sprint — resolve the provider
          dependency first."

User:    "What would change if we had 3 developers?"

Claude:  [reasons over existing JSON — no new API call needed]
         "With 3 developers, the development phase (currently 8 days) could be
          parallelized. The OAuth integration and the data layer are independent,
          so the timeline could compress to ~10 days total..."
```

---

## 3. Integrating via Claude API Tool Use

If you are building a custom application using the Anthropic API, register the analysis
endpoints as tools so Claude can call them autonomously.

### 3.1 Tool Definitions (Anthropic Python SDK)

```python
import anthropic
import httpx

client = anthropic.Anthropic()

AGENT_BASE = "http://localhost:8080"

tools = [
    {
        "name": "analyze_jira_ticket",
        "description": (
            "Analyzes a Jira ticket using AI and returns a structured JSON report "
            "covering requirements, technical complexity, risks, effort estimate, "
            "and implementation strategy. Use this whenever the user mentions a ticket ID."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "ticket_id": {
                    "type": "string",
                    "description": "Jira ticket key, e.g. PROJ-123"
                },
                "analysis_type": {
                    "type": "string",
                    "enum": ["default", "security", "risk", "effort", "code-review"],
                    "description": "Type of analysis to perform",
                    "default": "default"
                }
            },
            "required": ["ticket_id"]
        }
    },
    {
        "name": "check_agent_health",
        "description": "Check if the Jira Analysis Agent is running and healthy.",
        "input_schema": {
            "type": "object",
            "properties": {}
        }
    }
]


def execute_tool(tool_name: str, tool_input: dict) -> str:
    if tool_name == "analyze_jira_ticket":
        ticket_id = tool_input["ticket_id"]
        analysis_type = tool_input.get("analysis_type", "default")

        prompt_map = {
            "default": ("POST", f"/api/v1/analysis/tickets/{ticket_id}", None),
            "security": ("POST", f"/api/v1/analysis/tickets/{ticket_id}/security", None),
            "risk": ("POST", f"/api/v1/analysis/tickets/{ticket_id}/custom",
                     {"promptName": "risk-assessment-prompt"}),
            "effort": ("POST", f"/api/v1/analysis/tickets/{ticket_id}/custom",
                       {"promptName": "effort-estimation-prompt"}),
            "code-review": ("POST", f"/api/v1/analysis/tickets/{ticket_id}/custom",
                            {"promptName": "code-review-prompt"}),
        }
        method, path, body = prompt_map[analysis_type]
        response = httpx.request(method, f"{AGENT_BASE}{path}", json=body, timeout=120)
        response.raise_for_status()
        return response.text

    if tool_name == "check_agent_health":
        response = httpx.get(f"{AGENT_BASE}/api/v1/monitoring/health", timeout=5)
        return response.text

    raise ValueError(f"Unknown tool: {tool_name}")


def run_agent_loop(user_message: str):
    messages = [{"role": "user", "content": user_message}]

    while True:
        response = client.messages.create(
            model="claude-opus-4-7",
            max_tokens=4096,
            tools=tools,
            messages=messages
        )

        if response.stop_reason == "end_turn":
            # Extract final text response
            for block in response.content:
                if hasattr(block, "text"):
                    print(block.text)
            break

        if response.stop_reason == "tool_use":
            messages.append({"role": "assistant", "content": response.content})
            tool_results = []
            for block in response.content:
                if block.type == "tool_use":
                    result = execute_tool(block.name, block.input)
                    tool_results.append({
                        "type": "tool_result",
                        "tool_use_id": block.id,
                        "content": result
                    })
            messages.append({"role": "user", "content": tool_results})


# Usage
run_agent_loop("Analyze ticket PROJ-123 and summarize the top three risks.")
```

### 3.2 Tool Definitions (Anthropic TypeScript SDK)

```typescript
import Anthropic from "@anthropic-ai/sdk";

const client = new Anthropic();
const AGENT_BASE = "http://localhost:8080";

const tools: Anthropic.Tool[] = [
    {
        name: "analyze_jira_ticket",
        description:
            "Analyzes a Jira ticket and returns a structured JSON report covering " +
            "requirements, risks, effort estimate, and implementation strategy.",
        input_schema: {
            type: "object",
            properties: {
                ticket_id: {type: "string", description: "Jira ticket key, e.g. PROJ-123"},
                prompt_name: {
                    type: "string",
                    description: "Optional prompt override. Built-in values: analysis-prompt, " +
                        "security-analysis-prompt, risk-assessment-prompt, effort-estimation-prompt, code-review-prompt",
                    default: "analysis-prompt"
                }
            },
            required: ["ticket_id"]
        }
    }
];

async function executeTool(name: string, input: Record<string, string>): Promise<string> {
    const {ticket_id, prompt_name = "analysis-prompt"} = input;
    const url = prompt_name === "analysis-prompt"
        ? `${AGENT_BASE}/api/v1/analysis/tickets/${ticket_id}`
        : `${AGENT_BASE}/api/v1/analysis/tickets/${ticket_id}/custom`;
    const body = prompt_name === "analysis-prompt" ? undefined : JSON.stringify({promptName: prompt_name});
    const res = await fetch(url, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body,
        signal: AbortSignal.timeout(120_000)
    });
    return res.text();
}
```

---

## 4. Exposing as an MCP Server

[Model Context Protocol (MCP)](https://modelcontextprotocol.io) lets Claude Desktop and other
MCP-capable clients discover and call tools at runtime. You can wrap this agent's REST API as
an MCP server so any MCP client can use it without custom code.

### 4.1 MCP Server Wrapper (Node.js)

Create `mcp-server/index.js`:

```javascript
import {Server} from "@modelcontextprotocol/sdk/server/index.js";
import {StdioServerTransport} from "@modelcontextprotocol/sdk/server/stdio.js";
import {
    CallToolRequestSchema,
    ListToolsRequestSchema
} from "@modelcontextprotocol/sdk/types.js";

const AGENT_BASE = process.env.JIRA_AGENT_BASE ?? "http://localhost:8080";

const server = new Server(
    {name: "jira-analysis-agent", version: "1.0.0"},
    {capabilities: {tools: {}}}
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: "analyze_ticket",
            description: "Analyze a Jira ticket. Returns requirements, risks, effort, and implementation strategy.",
            inputSchema: {
                type: "object",
                properties: {
                    ticket_id: {type: "string", description: "Jira ticket key e.g. PROJ-123"},
                    prompt_name: {
                        type: "string",
                        description: "analysis-prompt | security-analysis-prompt | risk-assessment-prompt | effort-estimation-prompt | code-review-prompt",
                        default: "analysis-prompt"
                    },
                    context: {
                        type: "object",
                        description: "Optional key-value context injected as {{key}} in the prompt",
                        additionalProperties: {type: "string"}
                    }
                },
                required: ["ticket_id"]
            }
        },
        {
            name: "list_prompts",
            description: "List all available analysis prompt names.",
            inputSchema: {type: "object", properties: {}}
        },
        {
            name: "agent_health",
            description: "Check if the Jira Analysis Agent is running.",
            inputSchema: {type: "object", properties: {}}
        }
    ]
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const {name, arguments: args} = request.params;

    if (name === "analyze_ticket") {
        const {ticket_id, prompt_name = "analysis-prompt", context = {}} = args;
        const isDefault = prompt_name === "analysis-prompt";
        const url = isDefault
            ? `${AGENT_BASE}/api/v1/analysis/tickets/${ticket_id}`
            : `${AGENT_BASE}/api/v1/analysis/tickets/${ticket_id}/custom`;
        const body = isDefault ? undefined : JSON.stringify({promptName: prompt_name, context});
        const res = await fetch(url, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body,
            signal: AbortSignal.timeout(120_000)
        });
        const text = await res.text();
        return {content: [{type: "text", text}]};
    }

    if (name === "list_prompts") {
        const res = await fetch(`${AGENT_BASE}/api/v1/plugins/available`);
        return {content: [{type: "text", text: await res.text()}]};
    }

    if (name === "agent_health") {
        const res = await fetch(`${AGENT_BASE}/api/v1/monitoring/health`);
        return {content: [{type: "text", text: await res.text()}]};
    }

    throw new Error(`Unknown tool: ${name}`);
});

const transport = new StdioServerTransport();
await server.connect(transport);
```

### 4.2 Register in Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS)
or `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "jira-analysis-agent": {
      "command": "node",
      "args": [
        "/absolute/path/to/mcp-server/index.js"
      ],
      "env": {
        "JIRA_AGENT_BASE": "http://localhost:8080"
      }
    }
  }
}
```

Restart Claude Desktop. Claude will now discover `analyze_ticket`, `list_prompts`, and
`agent_health` as built-in tools and call them automatically when you mention a ticket ID.

---

## 5. Chaining with Other AI Agents

The agent exposes a simple REST API, so any orchestration layer can include it as a step.

### 5.1 Recommended Chain Patterns

#### Pattern A — Sequential Enrichment

```
Jira Analysis Agent
    │  TicketAnalysis JSON
    ▼
Security Scanner Agent    (reads risk_assessment, focuses on SECURITY risks)
    │  SecurityReport JSON
    ▼
Notification Agent         (posts findings to Slack / comments on Jira)
```

```bash
# Step 1: get analysis
ANALYSIS=$(curl -s -X POST http://localhost:8080/api/v1/analysis/tickets/PROJ-123)

# Step 2: extract risk score and pass to next agent
RISK_SCORE=$(echo "$ANALYSIS" | jq '.risk_assessment.risk_score')
echo "$ANALYSIS" | curl -s -X POST http://your-security-agent/review \
  -H "Content-Type: application/json" \
  -d @-
```

#### Pattern B — Parallel Perspective Analysis

Run multiple prompts concurrently to get different viewpoints on the same ticket:

```bash
TICKET=PROJ-123

curl -s -X POST "http://localhost:8080/api/v1/analysis/tickets/$TICKET" &
curl -s -X POST "http://localhost:8080/api/v1/analysis/tickets/$TICKET/security" &
curl -s -X POST "http://localhost:8080/api/v1/analysis/tickets/$TICKET/custom" \
  -H "Content-Type: application/json" \
  -d '{"promptName":"risk-assessment-prompt"}' &

wait
```

An orchestrating LLM can then synthesize the three perspectives into a single recommendation.

#### Pattern C — Gating / Triage Pipeline

Use the analysis output to decide whether a ticket needs human review before it enters the sprint:

```python
import httpx


def triage(ticket_id: str) -> str:
    r = httpx.post(f"http://localhost:8080/api/v1/analysis/tickets/{ticket_id}", timeout=120)
    analysis = r.json()
    risk_score = analysis["risk_assessment"]["risk_score"]
    complexity = analysis["technical_analysis"]["complexity_score"]

    if risk_score >= 8 or complexity >= 8:
        return "BLOCK — requires architect review before sprint entry"
    elif risk_score >= 5 or complexity >= 6:
        return "WARN — flag for sprint planning discussion"
    else:
        return "OK — cleared for sprint"


print(triage("PROJ-123"))
```

### 5.2 Data the Downstream Agent Receives

All downstream agents receive a `TicketAnalysis` JSON. The most useful fields for chaining:

| Field path                                    | Type            | Typical use in downstream agent     |
|-----------------------------------------------|-----------------|-------------------------------------|
| `summary`                                     | string          | Context summary for the next prompt |
| `technical_analysis.complexity_score`         | int 1–10        | Routing / triage decisions          |
| `risk_assessment.overall_risk_level`          | LOW/MEDIUM/HIGH | Escalation logic                    |
| `risk_assessment.risk_score`                  | int 1–10        | Numeric threshold gating            |
| `risk_assessment.identified_risks[]`          | array           | Security scanner input              |
| `effort_estimation.total_days`                | int             | Sprint capacity planning            |
| `requirements_analysis.acceptance_criteria[]` | array           | Test case generation input          |
| `implementation_strategy.phases[]`            | array           | Project plan generator input        |
| `analysis_metadata.processing_time_ms`        | long            | Monitoring / SLO tracking           |

---

## 6. Spring AI Multi-Agent Chains

If you are building a Spring AI application, you can call this agent's REST API from another
Spring service and compose it into a larger pipeline.

### 6.1 Calling This Agent from Another Spring Service

```java

@Service
public class PipelineOrchestrator {

    private final WebClient analysisClient = WebClient.builder()
            .baseUrl("http://localhost:8080")
            .build();

    public TicketAnalysis fetchAnalysis(String ticketId) {
        return analysisClient.post()
                .uri("/api/v1/analysis/tickets/{id}", ticketId)
                .retrieve()
                .bodyToMono(TicketAnalysis.class)
                .block(Duration.ofMinutes(3));
    }

    public void runPipeline(String ticketId) {
        TicketAnalysis analysis = fetchAnalysis(ticketId);

        // Route to specialist agent based on risk level
        String riskLevel = analysis.riskAssessment().overallRiskLevel();
        if ("HIGH".equals(riskLevel)) {
            securityAgent.review(analysis);
        }

        // Pass effort estimate to sprint planner
        sprintPlanner.propose(ticketId, analysis.effortEstimation().totalDays());
    }
}
```

### 6.2 Sharing the `TicketAnalysis` Record

Copy `src/main/java/com/javamsdt/agent/model/TicketAnalysis.java` into your consuming service,
or publish it as a shared Maven module so both services use the same Jackson-annotated record.

The record uses snake_case JSON field names (`@JsonProperty("ticket_id")`), so any standard
`ObjectMapper` will deserialize it correctly without additional configuration.

### 6.3 Using This Agent as a Spring AI Tool

If the consuming service is also a Spring AI application, you can expose the HTTP call as a
`@Tool` so the orchestrating LLM can call it on demand:

```java

@Component
public class JiraAnalysisAgentTool {

    private final WebClient client = WebClient.builder()
            .baseUrl("http://localhost:8080")
            .build();

    @Tool(description = "Analyze a Jira ticket and return requirements, risks, and effort estimate as JSON")
    public String analyzeJiraTicket(String ticketId) {
        return client.post()
                .uri("/api/v1/analysis/tickets/{id}", ticketId)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMinutes(3));
    }
}

// In your orchestrating service:
chatClient.

prompt()
    .

user("Analyze PROJ-123 and decide if it should go into this sprint.")
    .

tools(jiraAnalysisAgentTool)
    .

call()
    .

content();
```

---

## 7. LangChain / LangGraph Integration

### 7.1 LangChain Tool (Python)

```python
from langchain.tools import tool
import httpx

AGENT_BASE = "http://localhost:8080"


@tool
def analyze_jira_ticket(ticket_id: str, prompt_name: str = "analysis-prompt") -> str:
    """
    Analyze a Jira ticket using the Jira Analysis Agent.
    Returns a JSON string with requirements, technical analysis, risks, and effort estimate.
    prompt_name options: analysis-prompt, security-analysis-prompt,
    risk-assessment-prompt, effort-estimation-prompt, code-review-prompt
    """
    if prompt_name == "analysis-prompt":
        url = f"{AGENT_BASE}/api/v1/analysis/tickets/{ticket_id}"
        r = httpx.post(url, timeout=120)
    else:
        url = f"{AGENT_BASE}/api/v1/analysis/tickets/{ticket_id}/custom"
        r = httpx.post(url, json={"promptName": prompt_name}, timeout=120)
    r.raise_for_status()
    return r.text


@tool
def check_jira_agent_health() -> str:
    """Check if the Jira Analysis Agent is running."""
    return httpx.get(f"{AGENT_BASE}/api/v1/monitoring/health", timeout=5).text
```

### 7.2 LangGraph Workflow Node

```python
from langgraph.graph import StateGraph, END
from typing import TypedDict


class SprintState(TypedDict):
    ticket_id: str
    analysis: dict | None
    decision: str | None


def analysis_node(state: SprintState) -> SprintState:
    result = analyze_jira_ticket.invoke({"ticket_id": state["ticket_id"]})
    return {**state, "analysis": json.loads(result)}


def triage_node(state: SprintState) -> SprintState:
    risk = state["analysis"]["risk_assessment"]["risk_score"]
    complexity = state["analysis"]["technical_analysis"]["complexity_score"]
    decision = "APPROVED" if risk < 6 and complexity < 7 else "REVIEW_REQUIRED"
    return {**state, "decision": decision}


def route_after_triage(state: SprintState) -> str:
    return "notify_team" if state["decision"] == "APPROVED" else "escalate"


workflow = StateGraph(SprintState)
workflow.add_node("analyze", analysis_node)
workflow.add_node("triage", triage_node)
workflow.add_node("notify_team", lambda s: s)
workflow.add_node("escalate", lambda s: s)

workflow.set_entry_point("analyze")
workflow.add_edge("analyze", "triage")
workflow.add_conditional_edges("triage", route_after_triage)
workflow.add_edge("notify_team", END)
workflow.add_edge("escalate", END)

app = workflow.compile()
result = app.invoke({"ticket_id": "PROJ-123", "analysis": None, "decision": None})
```

---

## 8. Automation Platform Integration (n8n / Make / Zapier)

The agent's REST API works out-of-the-box with any platform that supports HTTP requests.

### 8.1 n8n Workflow

1. **Trigger node** — Jira trigger (new issue created), schedule, or Webhook.
2. **HTTP Request node** — `POST http://localhost:8080/api/v1/analysis/tickets/{{ $json.key }}`
    - Method: `POST`
    - Body: (none for default analysis, or JSON for custom)
    - Timeout: 120000ms
3. **JSON Parse node** — parse the response body.
4. **IF node** — branch on `{{ $json.risk_assessment.risk_score >= 7 }}`.
5. **Slack / Jira Comment node** — post the summary and risks.

For the agent to be reachable from n8n cloud, expose it via a reverse proxy or ngrok:

```bash
ngrok http 8080
# Use the generated https URL in your n8n HTTP Request node
```

### 8.2 Make (formerly Integromat)

1. **Watch Jira Issues** module → triggers when a new issue is created.
2. **HTTP Make a Request** module:
    - URL: `http://your-host:8080/api/v1/analysis/tickets/{{key}}`
    - Method: `POST`
    - Parse response: `Yes`
3. **Router** → branch on `risk_assessment.risk_score`.
4. **Slack Send Message** / **Jira Add Comment** module.

### 8.3 Zapier

Use the **Webhooks by Zapier** action step with:

- Method: `POST`
- URL: `http://your-host:8080/api/v1/analysis/tickets/{{ticket_key}}`
- Data pass-through: `yes` (to access the JSON response in subsequent steps)

---

## 9. Event-Driven Pipeline (Jira Webhook)

The agent does not include a built-in Jira webhook receiver, but you can front it with a thin
listener that triggers analysis when Jira events arrive.

### 9.1 Simple Webhook Receiver (Spring Boot)

Add this to a separate controller (or a new lightweight service):

```java

@RestController
@RequestMapping("/webhooks")
public class JiraWebhookController {

    private final WebClient analysisClient = WebClient.builder()
            .baseUrl("http://localhost:8080")
            .build();

    @PostMapping("/jira")
    public ResponseEntity<Void> onJiraEvent(@RequestBody Map<String, Object> payload) {
        String eventType = (String) payload.get("webhookEvent");
        if (!"jira:issue_created".equals(eventType) && !"jira:issue_updated".equals(eventType)) {
            return ResponseEntity.ok().build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
        String ticketId = (String) issue.get("key");

        // Trigger async analysis — don't block the Jira webhook response
        analysisClient.post()
                .uri("/api/v1/analysis/tickets/{id}", ticketId)
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(result -> log.info("Analysis complete for {}", ticketId));

        return ResponseEntity.accepted().build();
    }
}
```

### 9.2 Register the Webhook in Jira

1. Go to **Jira Settings → System → Webhooks**.
2. Create a new webhook pointing to `https://your-host/webhooks/jira`.
3. Select events: `Issue Created`, `Issue Updated`.
4. Optionally filter by JQL: `project = PROJ AND issuetype in (Story, Bug)`.

---

## 10. Batch Analysis Patterns

### 10.1 Bash Script

```bash
#!/usr/bin/env bash
TICKETS=("PROJ-101" "PROJ-102" "PROJ-103" "PROJ-104")
OUTPUT_DIR="./batch-results"
mkdir -p "$OUTPUT_DIR"

for TICKET in "${TICKETS[@]}"; do
  echo "Analyzing $TICKET..."
  curl -s -X POST "http://localhost:8080/api/v1/analysis/tickets/$TICKET" \
    > "$OUTPUT_DIR/$TICKET.json"
  echo "  Done: $OUTPUT_DIR/$TICKET.json"
  sleep 2  # respect Ollama concurrency limits
done

echo "Batch complete. Aggregating risk scores..."
jq -r '[.ticket_id, .risk_assessment.risk_score, .technical_analysis.complexity_score] | @csv' \
  "$OUTPUT_DIR"/*.json
```

### 10.2 Python Batch with Concurrency Control

```python
import httpx
import asyncio
import json
from pathlib import Path

AGENT_BASE = "http://localhost:8080"
TICKETS = ["PROJ-101", "PROJ-102", "PROJ-103", "PROJ-104", "PROJ-105"]
CONCURRENCY = 2  # llama3.1:8b handles 1-2 concurrent requests well on a single GPU

sem = asyncio.Semaphore(CONCURRENCY)


async def analyze(client: httpx.AsyncClient, ticket_id: str) -> dict:
    async with sem:
        r = await client.post(
            f"{AGENT_BASE}/api/v1/analysis/tickets/{ticket_id}",
            timeout=180
        )
        r.raise_for_status()
        return r.json()


async def batch():
    Path("batch-results").mkdir(exist_ok=True)
    async with httpx.AsyncClient() as client:
        tasks = [analyze(client, t) for t in TICKETS]
        results = await asyncio.gather(*tasks, return_exceptions=True)

    summary = []
    for ticket_id, result in zip(TICKETS, results):
        if isinstance(result, Exception):
            print(f"FAILED {ticket_id}: {result}")
            continue
        Path(f"batch-results/{ticket_id}.json").write_text(json.dumps(result, indent=2))
        summary.append({
            "ticket": ticket_id,
            "risk_score": result["risk_assessment"]["risk_score"],
            "total_days": result["effort_estimation"]["total_days"],
        })

    summary.sort(key=lambda x: x["risk_score"], reverse=True)
    print("\n=== Risk Ranking ===")
    for row in summary:
        print(f"  {row['ticket']:12s}  risk={row['risk_score']}  effort={row['total_days']}d")


asyncio.run(batch())
```

---

## 11. Response Schema Reference

All endpoints return a `TicketAnalysis` JSON. This is the complete schema:

```json
{
  "ticket_id": "PROJ-123",
  "summary": "<2-3 sentence analytical summary>",
  "requirements_analysis": {
    "functional_requirements": [
      "<string>"
    ],
    "non_functional_requirements": [
      "<string>"
    ],
    "acceptance_criteria": [
      "<string>"
    ],
    "dependencies": [
      "<string>"
    ],
    "assumptions": [
      "<string>"
    ]
  },
  "technical_analysis": {
    "complexity_score": 5,
    "technical_challenges": [
      "<string>"
    ],
    "recommended_approach": "<string>",
    "architecture_considerations": [
      "<string>"
    ],
    "technology_stack": [
      "<string>"
    ],
    "performance_considerations": [
      "<string>"
    ]
  },
  "risk_assessment": {
    "identified_risks": [
      {
        "description": "<string>",
        "category": "TECHNICAL | BUSINESS | TIMELINE | RESOURCE | SECURITY",
        "impact": "LOW | MEDIUM | HIGH",
        "probability": "LOW | MEDIUM | HIGH",
        "mitigation_strategy": "<string>",
        "contingency_plan": "<string>"
      }
    ],
    "overall_risk_level": "LOW | MEDIUM | HIGH",
    "risk_score": 5
  },
  "effort_estimation": {
    "development_days": 3,
    "testing_days": 1,
    "documentation_days": 1,
    "review_days": 1,
    "total_days": 6,
    "confidence_level": "LOW | MEDIUM | HIGH",
    "estimation_method": "<string>",
    "team_size_assumption": 2
  },
  "implementation_strategy": {
    "phases": [
      {
        "name": "<string>",
        "description": "<string>",
        "estimated_days": 3,
        "deliverables": [
          "<string>"
        ],
        "dependencies": [
          "<string>"
        ],
        "risks": [
          "<string>"
        ]
      }
    ],
    "key_milestones": [
      {
        "name": "<string>",
        "description": "<string>",
        "target_date": "<string>",
        "success_criteria": [
          "<string>"
        ]
      }
    ],
    "success_criteria": [
      "<string>"
    ],
    "rollback_strategy": "<string>"
  },
  "analysis_metadata": {
    "analysis_timestamp": "2024-01-01T00:00:00",
    "model_used": "llama3.1:8b",
    "analysis_version": "1.0.0",
    "processing_time_ms": 38400,
    "prompt_name": "analysis-prompt",
    "prompt_source": "INTERNAL_RESOURCE | EXTERNAL_FILE",
    "prompt_last_modified": "2024-01-01T00:00:00"
  }
}
```

### Error Response

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Prompt plugin not found: unknown-prompt",
  "timestamp": "2024-01-01T00:00:00"
}
```

HTTP 400 for invalid prompt name or missing parameters. HTTP 500 for LLM failures or Jira
connectivity errors.

---

## Quick Reference

| Goal                              | How                                                                              |
|-----------------------------------|----------------------------------------------------------------------------------|
| One-shot analysis                 | `POST /api/v1/analysis/tickets/{id}`                                             |
| Security review                   | `POST /api/v1/analysis/tickets/{id}/security`                                    |
| Risk / effort / code-review       | `POST /api/v1/analysis/tickets/{id}/custom` with `promptName`                    |
| Avoid redundant LLM calls         | `GET /api/v1/analysis/tickets/{id}/cached?promptName=…`                          |
| Add context to a prompt           | `POST /custom` with `"context": {"key": "value"}` → `{{key}}` in prompt template |
| Override a built-in prompt        | Drop `<name>.md` in `external-config/prompts/` — reloads automatically           |
| Claude Desktop integration        | Wrap as MCP server (see Section 4)                                               |
| Claude API integration            | Register as tool (see Section 3)                                                 |
| Spring AI service-to-service call | `WebClient` → `/api/v1/analysis/tickets/{id}` → deserialize `TicketAnalysis`     |
| LangChain / LangGraph             | `@tool` wrapper around the HTTP call (see Section 7)                             |
| n8n / Make / Zapier               | HTTP Request node → `POST /api/v1/analysis/tickets/{id}` (see Section 8)         |
| Jira webhook trigger              | Thin receiver controller → async `WebClient` call to agent (see Section 9)       |
| Batch multiple tickets            | Semaphore-gated async loop, concurrency 1–2 for local Ollama (see Section 10)    |
