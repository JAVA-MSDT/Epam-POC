# aidevworkflow

## About

**Aidevworkflow** is a modular, AI-powered development workflow engine built in Java.
It orchestrates a sequence of eight specialized agents — each backed by a markdown-defined prompt — to
take a raw development ticket all the way from analysis through to a deployment plan, with human
review gates placed at the three most critical transitions.

The project demonstrates a clean separation of concerns: **Java handles orchestration logic**,
**markdown files define agent behavior**, and a **pluggable `LlmClient` interface** lets you switch
between Claude, OpenAI, or any future LLM provider without touching business code.

---

## Features

- **8-step AI workflow** — Ticket Analysis → Project Setup → Deep Dive → Visual Report →
  Review → Implementation → QA → Deployment, each step handled by a dedicated stateless agent.
- **Two execution modes**
    - `runWorkflow()` — full modular mode, one LLM call per agent (8 calls total); ideal for
      debugging and per-step visibility.
    - `runWorkflowOptimized()` — optimized mode, pairs of steps are batched into combined prompts
      (5 calls total); best for production to reduce latency and cost.
- **3 Human-in-the-Loop gates** — mandatory confirmation prompts after Steps 2 (project structure),
  5 (analysis), and 8 (deployment). The workflow halts if the user rejects any gate.
- **Pluggable LLM providers** — swap Claude and OpenAI without changing a single agent class.
  Add Gemini or any other provider by implementing one interface.
- **Markdown-driven agent prompts** — non-engineers can refine agent behavior by editing `.md` files,
  no Java required.
- **Spring Boot ready** — the modular package layout and interface-driven design convert to a full
  Spring Boot application by adding annotations — no structural refactoring needed.
- **Full test coverage** — unit tests with mocked LLM, integration test for the real API
  (skipped unless `ANTHROPIC_API_KEY` is present).

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
    │   │   │   ├── TicketAnalysisAgent.java       ← Step 1
    │   │   │   ├── ProjectSetupAgent.java         ← Step 2
    │   │   │   ├── DeepDiveAgent.java             ← Step 3
    │   │   │   ├── VisualReportAgent.java         ← Step 4
    │   │   │   ├── ReviewAgent.java               ← Step 5
    │   │   │   ├── ImplementationAgent.java       ← Step 6
    │   │   │   ├── QualityAssuranceAgent.java     ← Step 7
    │   │   │   └── DeploymentAgent.java           ← Step 8
    │   │   ├── context/
    │   │   │   └── WorkflowContext.java           ← shared state (POJO)
    │   │   ├── llm/
    │   │   │   ├── LlmClient.java                 ← pluggable interface
    │   │   │   ├── ClaudeApiClient.java           ← Anthropic implementation
    │   │   │   └── OpenAiApiClient.java           ← OpenAI implementation
    │   │   ├── orchestrator/
    │   │   │   └── WorkflowOrchestrator.java      ← coordinates all 8 agents
    │   │   └── util/
    │   │       └── MarkdownLoader.java            ← loads prompt templates
    │   └── resources/
    │       └── agents/                            ← one .md prompt per agent
    │           ├── ticket_analysis.md
    │           ├── project_setup.md
    │           ├── deep_dive.md
    │           ├── visual_report.md
    │           ├── review.md
    │           ├── implementation.md
    │           ├── quality_assurance.md
    │           └── deployment.md
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

| Decision              | Choice                                    | Reason                                            |
|-----------------------|-------------------------------------------|---------------------------------------------------|
| Steps 1+2 combined    | Single LLM call in optimized mode         | Same input, output of 1 feeds 2 immediately       |
| Steps 3+4 pipelined   | Single LLM call in optimized mode         | Deep Dive output feeds Visual Report directly     |
| Steps 6+7 pipelined   | Single LLM call in optimized mode         | QA validates the code generated by Implementation |
| 3 human gates         | After steps 2, 5, 8                       | Cover the three irreversible transitions          |
| `LlmClient` interface | All agents use interface only             | Swap providers by changing one constructor line   |
| Markdown prompts      | Agent behavior in `.md`, not Java strings | Non-engineers can refine prompts without a build  |

---

## Getting Started

### Prerequisites

- Java 17 or later
- Apache Maven 3.6+
- An Anthropic API key **or** an OpenAI API key

### 1. Clone the repository

```bash
git clone https://github.com/your-org/aidevworkflow.git
cd aidevworkflow
```

### 2. Set your API key

```bash
# For Claude (default)
export ANTHROPIC_API_KEY=your_anthropic_key_here

# For OpenAI (optional — see Configuration)
export OPENAI_API_KEY=your_openai_key_here
```

### 3. Build the project

```bash
mvn clean compile
```

### 4. Run the tests

```bash
mvn test
```

Unit tests run without an API key. The real-API integration test (`ClaudeApiClientTest`)
is automatically skipped unless `ANTHROPIC_API_KEY` is set.

### 5. Run the workflow

```bash
# Full modular mode (8 LLM calls, human gates at the console)
mvn exec:java -Dexec.mainClass="com.javamsdt.aidevworkflow.Main"

# Optimized mode (5 LLM calls)
mvn exec:java -Dexec.mainClass="com.javamsdt.aidevworkflow.Main" -Dexec.args="optimized"
```

`Main.java` uses a built-in sample ticket (a login-feature story). Replace the
`SAMPLE_TICKET` constant or wire in your own ticket source to use real input.

---

## Configuration

### Switching LLM providers

Open `Main.java` and replace the client construction:

```java
// Default — Claude
LlmClient llmClient = ClaudeApiClient.fromEnv();

// Switch to OpenAI
LlmClient llmClient = OpenAiApiClient.fromEnv();
```

Both `fromEnv()` factories read their key from the corresponding environment variable.
To use a specific model version, use the two-argument constructor:

```java
// Claude — specific model
LlmClient llmClient = new ClaudeApiClient(System.getenv("ANTHROPIC_API_KEY"), "claude-opus-4-7");

// OpenAI — specific model
LlmClient llmClient = new OpenAiApiClient(System.getenv("OPENAI_API_KEY"), "gpt-4-turbo");
```

### Adding a new LLM provider (e.g., Gemini)

Implement the `LlmClient` interface:

```java
public class GeminiApiClient implements LlmClient {
    @Override
    public String completePrompt(String prompt) {
        // call Gemini API and return the text response
    }
}
```

Pass it to the orchestrator — no other code changes required.

### Customising agent prompts

All agent prompts live in `src/main/resources/agents/`. Each file is a markdown template
with `{{variable}}` placeholders that are filled at runtime.

To change how an agent behaves, edit its `.md` file:

```
src/main/resources/agents/ticket_analysis.md   ← Step 1 prompt
src/main/resources/agents/project_setup.md     ← Step 2 prompt
...
```

Variable names must match exactly what the corresponding agent class passes to
`String.replace()`. See each agent's `execute()` method for the list of variables it uses.

### Bypassing human gates (automated / CI mode)

Pass `autoApprove = true` to the orchestrator constructor:

```java
WorkflowOrchestrator orchestrator =
        new WorkflowOrchestrator(llmClient, ctx, true);
```

This skips all three confirmation prompts and lets the workflow run end-to-end without
user interaction — useful for CI pipelines or automated testing.

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
- Agents must be stateless — no instance state beyond the injected `LlmClient`.
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

The series HTML guides that informed this project's architecture:

| File | Topic                                                              |
|------|--------------------------------------------------------------------|
| `01` | AI Agents fundamentals & 8-step workflow overview                  |
| `02` | System architecture diagrams & advanced agent concepts             |
| `03` | Workflow optimisation — 5-call strategy & human-in-the-loop design |
| `04` | Maven project setup guide                                          |
| `05` | Full project generation prompt                                     |
| `06` | Claude's examination & implementation plan (this project)          |
