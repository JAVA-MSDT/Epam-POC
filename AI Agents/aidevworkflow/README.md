# aidevworkflow

## About

**Aidevworkflow** is a modular, AI-powered development workflow engine built in Java.
It orchestrates a sequence of eight specialised agents — each backed by a markdown-defined prompt — to
take a Jira ticket all the way from analysis through to a merged GitHub pull request, with human
review gates at the three most critical transitions.

The project demonstrates a clean separation of concerns: **Java handles orchestration and external integrations**,
**markdown files define agent behaviour**, and a **pluggable `LlmClient` interface** lets you switch
between Claude, OpenAI, Ollama, or any future LLM provider without touching business code.

---

## Features

- **8-step AI workflow** — Ticket Analysis → Project Setup → Deep Dive → Visual Report →
  Review → Implementation → QA → Deployment, each step handled by a dedicated stateless agent.
- **Real external integrations** — agents connect to Jira, the local filesystem, and GitHub rather
  than operating purely on in-memory text.
- **Two execution modes**
    - `runWorkflow()` — full modular mode, one LLM call per agent (8 calls total); ideal for
      debugging and per-step visibility.
    - `runWorkflowOptimized()` — optimized mode, pairs of steps are batched into combined prompts
      (5 calls total); best for production to reduce latency and cost.
- **3 Human-in-the-Loop gates** — mandatory confirmation prompts after Steps 2 (project structure),
  5 (HTML report review with iteration), and 8 (deployment). Gate 3 specifically gates the *push*:
  local commits are made first, and only pushed to origin after the user approves.
- **HTML report with iteration** — Step 4 writes a styled HTML report to disk and Step 5 opens it in
  the browser; the developer can request improvements (up to 5 iterations) before approving.
- **Live codebase scanning with snapshot cache** — Step 3 scans the project source files once and
  caches the result in `WorkflowContext.codebaseSnapshot`; Steps 7 and the optional Refactoring step
  reuse the cache, eliminating redundant filesystem reads.
- **Code written to disk** — Step 6 parses `// FILE:` annotated code blocks from the LLM response
  and writes each file to the project directory.
- **Progress tracking** — `WorkflowContext` tracks `writtenFiles`, `pendingFiles`, `fileQaStatus`
  (PENDING / REVIEWED / PASS per file), `committedFiles`, and `implementationStep` so every agent
  knows exactly where the pipeline stands.
- **Two-phase deployment with conventional commits** — the deployment plan groups files into logical
  commits (`feat`, `fix`, `test`, `docs`, etc.); each group is committed locally first.
  `pushAndCreatePr()` is called only after Gate 3 human approval, so commits are reviewable before
  they become public.
- **Automated GitHub PR** — after push, Step 8 creates a GitHub PR. PR review comments are fetched
  and appended to the HTML report.
- **Optional Refactoring step** — `RefactoringAgent` (between Steps 7 and 8) reviews QA findings
  and applies targeted, behavior-preserving refactorings. Triggered via `orchestrator.runRefactoring()`.
- **Workflow context persistence** — the full `WorkflowContext` is auto-saved to
  `<reportFolderPath>/workflow_context.json` at every human gate. Use
  `WorkflowOrchestrator.loadContext(path, llmClient)` to resume an interrupted session without
  re-running completed steps.
- **Anthropic Prompt Caching** — when using `ClaudeApiClient`, the codebase snapshot is sent as a
  cached content block (`cache_control: ephemeral`). The Anthropic server caches it for up to 5 minutes,
  cutting input-token cost and latency on repeated calls with the same codebase. Other providers
  fall back to simple concatenation transparently.
- **Pluggable LLM providers** — swap Claude, OpenAI, or Ollama without changing a single agent class.
- **Markdown-driven agent prompts** — non-engineers can refine agent behaviour by editing `.md` files,
  no Java required. Prompts now include Javadoc instructions (implementation), conventional commit
  templates (deployment), JUnit 5 test generation (QA), and incremental HTML building (visual report).
- **Spring Boot ready** — the modular package layout and interface-driven design convert to a full
  Spring Boot application by adding annotations — no structural refactoring needed.

---

## Tech Stack

| Layer             | Technology                             |
|-------------------|----------------------------------------|
| Language          | Java 17                                |
| Build             | Apache Maven 3.x                       |
| HTTP Client       | OkHttp 4.12                            |
| JSON              | Jackson Databind 2.16                  |
| LLM — Primary     | Anthropic Claude (`claude-sonnet-4-6`) |
| LLM — Alternative | OpenAI (`gpt-4o`)                      |
| LLM — Local/Free  | Ollama (any local model)               |
| Ticket Source     | Jira REST API v3                       |
| Version Control   | Git CLI (`ProcessBuilder`)             |
| PR / Code Review  | GitHub REST API v3                     |
| Report Output     | HTML file (written to disk)            |
| Testing           | JUnit 5.10 + Mockito 5.8               |
| Agent Prompts     | Markdown (`.md` template files)        |

---

## Project Structure

```
aidevworkflow/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/javamsdt/aidevworkflow/
    │   │   ├── Main.java                          ← entry point / demo
    │   │   ├── agents/
    │   │   │   ├── TicketAnalysisAgent.java       ← Step 1: fetch from Jira or text
    │   │   │   ├── ProjectSetupAgent.java         ← Step 2: plan + create report folder
    │   │   │   ├── DeepDiveAgent.java             ← Step 3: analyse + scan codebase (cached)
    │   │   │   ├── VisualReportAgent.java         ← Step 4: write HTML report to disk
    │   │   │   ├── ReviewAgent.java               ← Step 5: open browser, iterate report
    │   │   │   ├── ImplementationAgent.java       ← Step 6: write code files, track progress
    │   │   │   ├── QualityAssuranceAgent.java     ← Step 7: review written code, update fileQaStatus
    │   │   │   ├── DeploymentAgent.java           ← Step 8: local commits → push → PR (two-phase)
    │   │   │   └── RefactoringAgent.java          ← Optional: refactor between QA and Deployment
    │   │   ├── context/
    │   │   │   └── WorkflowContext.java           ← shared pipeline state (POJO, JSON-serializable)
    │   │   ├── github/
    │   │   │   ├── GitClient.java                 ← branch, commitAll, commitFiles, push via git CLI
    │   │   │   └── GitHubClient.java              ← create PR, fetch PR comments
    │   │   ├── jira/
    │   │   │   ├── JiraClient.java                ← fetch ticket via Jira REST API
    │   │   │   └── JiraTicket.java                ← structured ticket record
    │   │   ├── llm/
    │   │   │   ├── LlmClient.java                 ← pluggable interface (completePrompt + completePromptCached)
    │   │   │   ├── ClaudeApiClient.java           ← Anthropic implementation with prompt caching
    │   │   │   ├── OpenAiApiClient.java           ← OpenAI implementation
    │   │   │   └── OllamaApiClient.java           ← Ollama local implementation
    │   │   ├── orchestrator/
    │   │   │   └── WorkflowOrchestrator.java      ← coordinates all agents; rerunAnalysis, runRefactoring, saveContext, loadContext
    │   │   └── util/
    │   │       ├── FileSystemUtil.java            ← folder creation, file read/write, codebase scan
    │   │       ├── HtmlReportWriter.java          ← wraps HTML body in page shell, writes to disk
    │   │       ├── MarkdownLoader.java            ← loads prompt templates from classpath
    │   │       └── WorkflowContextSerializer.java ← Jackson JSON save/load for session persistence
    │   └── resources/
    │       └── agents/                            ← one .md prompt per agent
    │           ├── ticket_analysis.md
    │           ├── project_setup.md
    │           ├── deep_dive.md
    │           ├── visual_report.md               ← incremental HTML building
    │           ├── review.md
    │           ├── implementation.md              ← Javadoc generation instructions
    │           ├── quality_assurance.md           ← JUnit 5 test generation
    │           ├── deployment.md                  ← conventional commit groups
    │           └── refactoring.md                 ← optional refactoring step
    └── test/
        └── java/com/javamsdt/aidevworkflow/
            ├── agents/
            │   └── TicketAnalysisAgentTest.java
            ├── llm/
            │   └── ClaudeApiClientTest.java       ← integration, skipped without API key
            ├── orchestrator/
            │   └── WorkflowOrchestratorTest.java
            └── util/
                └── MarkdownLoaderTest.java
```

### Key Design Decisions

| Decision                       | Choice                                                                      | Reason                                                                        |
|--------------------------------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| Steps 1+2 combined             | Single LLM call in optimized mode                                           | Same input, output of 1 feeds 2 immediately                                   |
| Steps 3+4 pipelined            | Single LLM call in optimized mode                                           | Deep Dive output feeds Visual Report directly                                 |
| Steps 6+7 pipelined            | Single LLM call in optimized mode                                           | QA validates the code generated by Implementation                             |
| 3 human gates                  | After steps 2, 5, 8                                                         | Cover the three irreversible transitions                                      |
| Gate 3 gates push, not confirm | Phase 1: local commits; Phase 2: push only after approval                   | Lets the developer review commits before they become visible to the team      |
| HTML report iteration loop     | Up to 5 rounds of developer feedback in Step 5                              | Let the developer shape the analysis before code is written                   |
| `// FILE:` code annotation     | LLM annotates each block with its target file path                          | Allows the agent to write files without hardcoding paths                      |
| Conventional commit groups     | LLM outputs file-to-group mapping; one `commitFiles()` call per group       | Clean semantic git history; each logical unit is a separate commit            |
| Codebase snapshot cache        | First scan stored in `codebaseSnapshot`; reused by QA + Refactoring         | Eliminates duplicate filesystem reads across three agents                     |
| Progress tracking fields       | `writtenFiles`, `pendingFiles`, `fileQaStatus`, `committedFiles` on context | Single source of truth for pipeline progress without shared mutable state     |
| Refactoring as optional step   | `RefactoringAgent` not in main flow; called via `runRefactoring()`          | Keeps the core 8-step pipeline clean; refactoring is triggered on demand      |
| Context persistence            | Jackson JSON auto-save at every gate; `loadContext()` factory               | Resume interrupted sessions without re-running completed steps                |
| Prompt caching                 | `completePromptCached()` on interface; override in `ClaudeApiClient`        | 5-min server-side cache cuts cost/latency; other providers fall back silently |
| Jira fallback                  | `jiraTicketId` optional; falls back to `ticketText`                         | Supports both local testing and production Jira workflows                     |
| Git via `ProcessBuilder`       | Shell out to local `git` CLI                                                | No extra dependency; works with any git configuration                         |
| `LlmClient` interface          | All agents use interface only                                               | Swap providers by changing one constructor line                               |
| Markdown prompts               | Agent behaviour in `.md`, not Java strings                                  | Non-engineers can refine prompts without a build                              |

---

## Getting Started

### Prerequisites

- Java 17 or later
- Apache Maven 3.6+
- One of:
    - An Anthropic API key (`ANTHROPIC_API_KEY`)
    - An OpenAI API key (`OPENAI_API_KEY`)
    - [Ollama](https://ollama.com) running locally (free, no key required)
- *(Optional)* Jira credentials for live ticket fetching
- *(Optional)* GitHub token for automated PR creation

### Quick setup with Ollama (free, no API key)

Ollama lets you run LLMs locally at no cost — ideal for development and testing.

**1. Install Ollama**

Download and install from [https://ollama.com/download](https://ollama.com/download), then verify:

```bash
ollama --version
```

**2. Pull a model**

```bash
# Default used by OllamaApiClient
ollama pull phi3

# More capable alternatives
ollama pull llama3.2
ollama pull mistral
```

**3. Start the Ollama server**

```bash
ollama serve
# Server listens on http://localhost:11434 by default
```

The server starts automatically on most platforms after install.
Run `ollama serve` only if it is not already running.

**4. (Optional) Override the base URL**

```bash
export OLLAMA_BASE_URL=http://localhost:11434   # default — can be omitted
```

**5. Wire Ollama into the workflow**

In `Main.java`, replace the client line:

```java
LlmClient llmClient = OllamaApiClient.fromEnv();
```

Then build and run as normal:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.javamsdt.aidevworkflow.Main"
```

---

### 1. Clone the repository

```bash
git clone https://github.com/your-org/aidevworkflow.git
cd aidevworkflow
```

### 2. Set environment variables

```bash
# LLM — choose one
export ANTHROPIC_API_KEY=your_anthropic_key_here
export OPENAI_API_KEY=your_openai_key_here
# (or use Ollama — no key needed)

# Jira — required only if fetching tickets from Jira
export JIRA_BASE_URL=https://your-org.atlassian.net
export JIRA_USER_EMAIL=you@example.com
export JIRA_API_TOKEN=your_jira_api_token

# GitHub — required only for automated PR creation
export GITHUB_TOKEN=your_github_personal_access_token
export GITHUB_REPO=your-org/your-repo
```

### 3. Configure the workflow inputs

In `Main.java`, set the inputs that drive the workflow:

```java
WorkflowContext ctx = new WorkflowContext();

// Option A — pull ticket from Jira
ctx.

setJiraTicketId("PROJ-123");

// Option B — use inline text (local testing)
ctx.

setTicketText("As a user I want to log in...");

// Path to the project being implemented (used by DeepDive and Implementation agents)
ctx.

setProjectRootPath("/path/to/your/project");
```

### 4. Build the project

```bash
mvn clean compile
```

### 5. Run the tests

```bash
mvn test
```

Unit tests run without an API key. The real-API integration test (`ClaudeApiClientTest`)
is automatically skipped unless `ANTHROPIC_API_KEY` is set.

### 6. Run the workflow

```bash
# Full modular mode (8 LLM calls, human gates at the console)
mvn exec:java -Dexec.mainClass="com.javamsdt.aidevworkflow.Main"

# Optimized mode (5 LLM calls)
mvn exec:java -Dexec.mainClass="com.javamsdt.aidevworkflow.Main" -Dexec.args="optimized"
```

---

## Configuration

### Switching LLM providers

Open `Main.java` and replace the client construction:

```java
// Anthropic Claude (requires ANTHROPIC_API_KEY)
LlmClient llmClient = ClaudeApiClient.fromEnv();

// OpenAI (requires OPENAI_API_KEY)
LlmClient llmClient = OpenAiApiClient.fromEnv();

// Ollama — free local inference, no API key needed
LlmClient llmClient = OllamaApiClient.fromEnv();

// Specific model
LlmClient llmClient = new ClaudeApiClient(System.getenv("ANTHROPIC_API_KEY"), "claude-opus-4-7");
LlmClient llmClient = new OllamaApiClient("http://localhost:11434", "mistral");
```

### Enabling Jira integration

```java
WorkflowOrchestrator orchestrator = new WorkflowOrchestrator(
        llmClient, ctx,
        false,                          // autoApprove
        JiraClient.fromEnv(),           // reads JIRA_BASE_URL, JIRA_USER_EMAIL, JIRA_API_TOKEN
        null,                           // GitClient (optional)
        null                            // GitHubClient (optional)
);
```

When `jiraTicketId` is set on the context, `TicketAnalysisAgent` fetches the ticket from Jira
and populates `ticketText` with structured fields (summary, description, type, priority, comments).
If `jiraTicketId` is not set, `ticketText` is used directly (local/test mode).

### Enabling GitHub PR creation

```java
WorkflowOrchestrator orchestrator = new WorkflowOrchestrator(
        llmClient, ctx,
        false,
        JiraClient.fromEnv(),
        new GitClient(ctx.getProjectRootPath()),   // runs git CLI in projectRootPath
        GitHubClient.fromEnv()                     // reads GITHUB_TOKEN, GITHUB_REPO
);
```

`DeploymentAgent` will:

1. Parse the LLM deployment plan for logical commit groups (conventional commit messages + file lists)
2. Create a branch named `feature/<ticketId>`
3. Stage and commit each group separately using `GitClient.commitFiles()` and the LLM-generated commit message
4. Store the branch name in `ctx.featureBranchName` and return — branch stays local until Gate 3
5. After Gate 3 approval, push to `origin` and open a GitHub PR
6. Fetch any PR review comments and append them to the HTML report

### Bypassing human gates (automated / CI mode)

Pass `autoApprove = true` to the orchestrator constructor:

```java
WorkflowOrchestrator orchestrator =
        new WorkflowOrchestrator(llmClient, ctx, true);
```

This skips all three confirmation prompts and the HTML report iteration loop, letting the
workflow run end-to-end without user interaction — useful for CI pipelines or automated testing.

### Customising agent prompts

All agent prompts live in `src/main/resources/agents/`. Each file is a markdown template
with `{{variable}}` placeholders filled at runtime.

| Prompt file            | Key placeholders                                                  |
|------------------------|-------------------------------------------------------------------|
| `ticket_analysis.md`   | `{{ticket}}`                                                      |
| `project_setup.md`     | `{{ticket_summary}}`, `{{report_folder}}`                         |
| `deep_dive.md`         | `{{ticket_summary}}`, `{{project_setup}}`, `{{codebase_context}}` |
| `visual_report.md`     | `{{deep_dive}}`                                                   |
| `review.md`            | `{{deep_dive}}`, `{{visual_report}}`                              |
| `implementation.md`    | `{{review_notes}}`, `{{project_root}}`                            |
| `quality_assurance.md` | `{{implementation}}`, `{{written_code}}`                          |
| `deployment.md`        | `{{implementation}}`, `{{qa_report}}`                             |
| `refactoring.md`       | `{{implementation}}`, `{{qa_report}}`, `{{codebase_snapshot}}`    |

### Prompt Caching (Anthropic Claude only)

Prompt caching is enabled automatically when `ClaudeApiClient` is used. The codebase snapshot
is sent as a separate content block marked `cache_control: {"type": "ephemeral"}`.
The Anthropic server caches it for up to 5 minutes — subsequent calls that reuse the same
codebase snapshot (Steps 7 and Refactoring) skip re-tokenising it, reducing input-token cost
and latency on large codebases.

No configuration is needed. `LlmClient.completePromptCached()` falls back to simple
concatenation for OpenAI and Ollama, so behaviour is identical regardless of provider.
To disable caching even on Claude, call `completePrompt()` directly in the agent.

### Session Resume

The context is auto-saved to `<reportFolderPath>/workflow_context.json` at every human gate.
To resume a workflow that was halted or crashed:

```java
// Resume from last saved checkpoint
WorkflowOrchestrator orchestrator =
        WorkflowOrchestrator.loadContext("/path/to/workflow_context.json", llmClient);

// Re-attach git and GitHub clients if needed (they are not serialized to JSON)
// Then continue from wherever the workflow left off
orchestrator.

runWorkflow();
```

Save manually at any point:

```java
orchestrator.saveContext();                          // saves to reportFolderPath
orchestrator.

saveContext("/custom/path/ctx.json");   // saves to an explicit path
```

### Optional Refactoring Step

Between QA (Step 7) and Deployment (Step 8), call `runRefactoring()` to apply targeted
behaviour-preserving code improvements identified in the QA report:

```java
qualityAssuranceAgent.execute(ctx);   // Step 7

orchestrator.

runRefactoring();        // optional: generates refactoring plan, writes files to disk

deploymentAgent.

execute(ctx);         // Step 8
```

`RefactoringAgent` reuses the cached codebase snapshot, writes modified files to disk, and clears
the snapshot so any subsequent re-scan reflects the updated code. `WorkflowContext.refactoringPlan`
holds the full LLM response including the refactoring summary and unchanged-files list.

---

## How to Contribute

Contributions are welcome. Please follow these steps:

1. **Fork** the repository and create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Follow the existing patterns** when adding new agents:
    - Create `src/main/java/.../agents/YourAgent.java` (stateless, constructor-injected `LlmClient`).
    - Create `src/main/resources/agents/your_agent.md` (purpose, instructions, input variables, expected output format).
    - Add `execute(WorkflowContext ctx)` that reads from and writes to the context.
    - Register the agent in `WorkflowOrchestrator`.
    - Write a unit test in `src/test/` using a mocked `LlmClient`.

3. **Run the full test suite** before submitting:
   ```bash
   mvn clean test
   ```

4. **Open a Pull Request** with a clear description of what the change does and why.

### Code Style Guidelines

- Java 17 idiomatic code; no external style framework required.
- Agents must be stateless — no instance state beyond the injected `LlmClient` and optional integration clients.
- No direct LLM provider imports inside agent classes — use `LlmClient` only.
- Keep `WorkflowContext` as the single source of truth; agents must not call each other directly.
- One test class per production class, minimum two test cases per public method.

---

## License

This project is licensed under the **MIT License**.

```
MIT License

Copyright (c) 2026 javamsdt

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Author

**Ahmed Samy Bakry Mahmoud** (`javamsdt`)

Built as part of the *AI Agents for Developer Assistance* presentation series — a progressive
guide to designing, orchestrating, and optimising multi-agent AI workflows in Java.

| File | Topic                                                              |
|------|--------------------------------------------------------------------|
| `01` | AI Agents fundamentals & 8-step workflow overview                  |
| `02` | System architecture diagrams & advanced agent concepts             |
| `03` | Workflow optimisation — 5-call strategy & human-in-the-loop design |
| `04` | Maven project setup guide                                          |
| `05` | Full project generation prompt                                     |
| `06` | Claude's examination & implementation plan (this project)          |
