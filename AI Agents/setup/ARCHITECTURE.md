# aidevworkflow — Architecture Diagrams

---

## 1. Project Structure

```
aidevworkflow/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/javamsdt/aidevworkflow/
    │   │   ├── Main.java                          ← entry point
    │   │   ├── agents/                            ← 8 stateless agent classes
    │   │   │   ├── TicketAnalysisAgent.java       ← Step 1: fetch from Jira or text
    │   │   │   ├── ProjectSetupAgent.java         ← Step 2: plan + create report folder
    │   │   │   ├── DeepDiveAgent.java             ← Step 3: analyse + scan codebase
    │   │   │   ├── VisualReportAgent.java         ← Step 4: write HTML report to disk
    │   │   │   ├── ReviewAgent.java               ← Step 5: open browser, iterate report
    │   │   │   ├── ImplementationAgent.java       ← Step 6: write code files to disk
    │   │   │   ├── QualityAssuranceAgent.java     ← Step 7: review written code files
    │   │   │   └── DeploymentAgent.java           ← Step 8: commit, push, create PR
    │   │   ├── context/
    │   │   │   └── WorkflowContext.java           ← shared pipeline state (POJO)
    │   │   ├── github/
    │   │   │   ├── GitClient.java                 ← branch, commit, push via git CLI
    │   │   │   └── GitHubClient.java              ← create PR, fetch PR comments
    │   │   ├── jira/
    │   │   │   ├── JiraClient.java                ← fetch ticket via Jira REST API
    │   │   │   └── JiraTicket.java                ← structured ticket record
    │   │   ├── llm/
    │   │   │   ├── LlmClient.java                 ← pluggable interface
    │   │   │   ├── ClaudeApiClient.java           ← Anthropic (ANTHROPIC_API_KEY)
    │   │   │   ├── OpenAiApiClient.java           ← OpenAI   (OPENAI_API_KEY)
    │   │   │   └── OllamaApiClient.java           ← Local    (OLLAMA_BASE_URL)
    │   │   ├── orchestrator/
    │   │   │   └── WorkflowOrchestrator.java      ← coordinates all 8 agents
    │   │   └── util/
    │   │       ├── FileSystemUtil.java            ← folder creation, file read/write, codebase scan
    │   │       ├── HtmlReportWriter.java          ← wraps HTML body in page shell, writes file
    │   │       └── MarkdownLoader.java            ← loads .md prompt templates from classpath
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
            ├── agents/TicketAnalysisAgentTest.java
            ├── llm/ClaudeApiClientTest.java
            ├── orchestrator/WorkflowOrchestratorTest.java
            └── util/MarkdownLoaderTest.java
```

---

## 2. Component Diagram

```mermaid
graph TB
    subgraph Entry["Entry Point"]
        Main["Main.java\n─────────────\nsets ctx inputs\nchooses mode"]
    end

    subgraph LLM["LLM Layer  (llm/)"]
        Interface["«interface»\nLlmClient\n─────────────\ncompletePrompt(prompt)"]
        Claude["ClaudeApiClient\nAnthropic API"]
        OpenAI["OpenAiApiClient\nOpenAI API"]
        Ollama["OllamaApiClient\nOllama local"]
        Claude --> Interface
        OpenAI --> Interface
        Ollama --> Interface
    end

    subgraph Integrations["External Integrations"]
        JiraC["JiraClient\n─────────────\nfetchTicket(id)"]
        GitC["GitClient\n─────────────\ncreateBranch()\ncommitAll()\npush()"]
        GitHubC["GitHubClient\n─────────────\ncreatePullRequest()\nfetchPrComments()"]
        FSU["FileSystemUtil\n─────────────\ncreateReportFolder()\nwriteFile()\nreadSourceFiles()"]
        HTML["HtmlReportWriter\n─────────────\nwrite(folder, file, title, body)"]
    end

    subgraph Orchestration["Orchestration  (orchestrator/)"]
        Orch["WorkflowOrchestrator\n─────────────\nrunWorkflow()\nrunWorkflowOptimized()\nhumanConfirm()"]
    end

    subgraph Agents["Agents  (agents/)"]
        A1["TicketAnalysisAgent\nStep 1"]
        A2["ProjectSetupAgent\nStep 2"]
        A3["DeepDiveAgent\nStep 3"]
        A4["VisualReportAgent\nStep 4"]
        A5["ReviewAgent\nStep 5"]
        A6["ImplementationAgent\nStep 6"]
        A7["QualityAssuranceAgent\nStep 7"]
        A8["DeploymentAgent\nStep 8"]
    end

    subgraph Context["Shared State  (context/)"]
        Ctx["WorkflowContext\n─────────────\njiraTicketId\nprojectRootPath\nticketText\nticketSummary\nprojectSetup\nreportFolderPath\ndeepDive\nvisualReport\nhtmlReportPath\nreviewNotes\nimplementation\nqaReport\ndeploymentStatus\nprUrl\nprComments"]
    end

    subgraph Prompts["Prompt Templates  (resources/agents/)"]
        MD["*.md files\n(one per agent)"]
    end

    Main --> Interface
    Main --> Orch
    Orch --> A1 & A2 & A3 & A4 & A5 & A6 & A7 & A8
    A1 & A2 & A3 & A4 & A5 & A6 & A7 & A8 --> Interface
    A1 & A2 & A3 & A4 & A5 & A6 & A7 & A8 --> Ctx
    A1 --> JiraC
    A2 --> FSU
    A3 --> FSU
    A4 --> HTML
    A5 --> HTML
    A6 --> FSU
    A7 --> FSU
    A8 --> GitC
    A8 --> GitHubC
    A8 --> FSU
    A1 & A2 & A3 & A4 & A5 & A6 & A7 & A8 --> MD
```

---

## 3. Agent Workflow — Full Modular Mode (8 LLM calls)

```mermaid
flowchart TD
    START([Ticket: Jira ID or text])

    S1["Step 1 — Ticket Analysis\nTicketAnalysisAgent\n• fetch ticket from Jira (if jiraTicketId set)\n• writes: ticketSummary"]
    S2["Step 2 — Project Setup\nProjectSetupAgent\n• create report folder on disk\n• writes: projectSetup, reportFolderPath"]
    G1{{"HUMAN GATE 1\nApprove project\nstructure?"}}
    S3["Step 3 — Deep Dive\nDeepDiveAgent\n• scan codebase source files\n• writes: deepDive (with gap analysis)"]
    S4["Step 4 — Visual Report\nVisualReportAgent\n• LLM generates HTML body\n• write analysis_report.html to disk\n• writes: visualReport, htmlReportPath"]
    S5["Step 5 — Review\nReviewAgent\n• open HTML report in browser\n• iterate (up to 5×) on developer feedback\n• writes: reviewNotes"]
    G2{{"HUMAN GATE 2\nApprove analysis\nbefore coding?"}}
    S6["Step 6 — Implementation\nImplementationAgent\n• LLM generates annotated code blocks\n• parse // FILE: blocks, write files to disk\n• writes: implementation"]
    S7["Step 7 — Quality Assurance\nQualityAssuranceAgent\n• read written code files from disk\n• writes: qaReport"]
    S8["Step 8 — Deployment\nDeploymentAgent\n• LLM generates deployment plan\n• git branch → commit → push → GitHub PR\n• fetch PR comments → update HTML report\n• writes: deploymentStatus, prUrl, prComments"]
    G3{{"HUMAN GATE 3\nApprove deployment?"}}
    END([Workflow Complete\nPR open on GitHub])
    HALT([Workflow Halted])

    START --> S1 --> S2 --> G1
    G1 -- "y (approved)" --> S3
    G1 -- "n (rejected)" --> HALT
    S3 --> S4 --> S5 --> G2
    G2 -- "y (approved)" --> S6
    G2 -- "n (rejected)" --> HALT
    S6 --> S7 --> S8 --> G3
    G3 --> END

    style G1 fill:#f5a623,color:#000
    style G2 fill:#f5a623,color:#000
    style G3 fill:#f5a623,color:#000
    style HALT fill:#d9534f,color:#fff
    style END fill:#5cb85c,color:#fff
    style START fill:#5bc0de,color:#000
```

---

## 4. Agent Workflow — Optimized Mode (5 LLM calls)

```mermaid
flowchart TD
    START([Ticket: Jira ID or text])

    B1["LLM Call 1 — Batch: Steps 1 + 2\nTicket Analysis + Project Setup\n• fetch from Jira, create report folder\n• writes: ticketSummary, projectSetup, reportFolderPath"]
    G1{{"HUMAN GATE 1\nApprove project\nstructure?"}}
    B2["LLM Call 2 — Batch: Steps 3 + 4\nDeep Dive + Visual Report\n• scan codebase, write HTML to disk\n• writes: deepDive, visualReport, htmlReportPath"]
    B3["LLM Call 3 — Step 5\nReview (with browser iteration loop)\n• writes: reviewNotes"]
    G2{{"HUMAN GATE 2\nApprove analysis\nbefore coding?"}}
    B4["LLM Call 4 — Batch: Steps 6 + 7\nImplementation + QA\n• write code files, review from disk\n• writes: implementation, qaReport"]
    B5["LLM Call 5 — Step 8\nDeployment\n• git branch → commit → push → PR\n• writes: deploymentStatus, prUrl, prComments"]
    G3{{"HUMAN GATE 3\nApprove deployment?"}}
    END([Workflow Complete\nPR open on GitHub])
    HALT([Workflow Halted])

    START --> B1 --> G1
    G1 -- "y" --> B2 --> B3 --> G2
    G1 -- "n" --> HALT
    G2 -- "y" --> B4 --> B5 --> G3
    G2 -- "n" --> HALT
    G3 --> END

    style G1 fill:#f5a623,color:#000
    style G2 fill:#f5a623,color:#000
    style G3 fill:#f5a623,color:#000
    style HALT fill:#d9534f,color:#fff
    style END fill:#5cb85c,color:#fff
    style START fill:#5bc0de,color:#000
```

---

## 5. WorkflowContext Data Flow

Each agent reads specific fields and writes exactly one or two new fields. The diagram shows the full blackboard data flow.

```mermaid
flowchart LR
    JID["jiraTicketId\n(set by Main)"]
    ROOT["projectRootPath\n(set by Main)"]
    T["ticketText\n(set by Main or\nfetched from Jira)"]

    JID -->|read| A1["TicketAnalysisAgent\nStep 1"]
    T -->|read| A1
    A1 -->|write| TS["ticketSummary"]

    TS -->|read| A2["ProjectSetupAgent\nStep 2"]
    JID -->|read| A2
    ROOT -->|read| A2
    A2 -->|write| PS["projectSetup"]
    A2 -->|write| RF["reportFolderPath\n(disk folder created)"]

    TS -->|read| A3
    PS -->|read| A3
    ROOT -->|read| A3["DeepDiveAgent\nStep 3"]
    A3 -->|write| DD["deepDive"]

    DD -->|read| A4["VisualReportAgent\nStep 4"]
    RF -->|read| A4
    A4 -->|write| VR["visualReport"]
    A4 -->|write| HR["htmlReportPath\n(HTML file on disk)"]

    DD -->|read| A5
    VR -->|read| A5
    HR -->|read| A5["ReviewAgent\nStep 5"]
    A5 -->|write| RN["reviewNotes"]

    RN -->|read| A6
    ROOT -->|read| A6["ImplementationAgent\nStep 6"]
    A6 -->|write| IM["implementation\n(code files written to disk)"]

    IM -->|read| A7
    ROOT -->|read| A7["QualityAssuranceAgent\nStep 7"]
    A7 -->|write| QA["qaReport"]

    IM -->|read| A8
    QA -->|read| A8
    JID -->|read| A8
    HR -->|read| A8["DeploymentAgent\nStep 8"]
    A8 -->|write| DS["deploymentStatus"]
    A8 -->|write| PU["prUrl\n(GitHub PR created)"]
    A8 -->|write| PC["prComments\n(appended to HTML)"]
```

---

## 6. External Integration Points

```mermaid
flowchart LR
    subgraph Workflow["Workflow Agents"]
        A1["TicketAnalysisAgent\nStep 1"]
        A2["ProjectSetupAgent\nStep 2"]
        A3["DeepDiveAgent\nStep 3"]
        A4["VisualReportAgent\nStep 4"]
        A5["ReviewAgent\nStep 5"]
        A6["ImplementationAgent\nStep 6"]
        A7["QualityAssuranceAgent\nStep 7"]
        A8["DeploymentAgent\nStep 8"]
    end

    Jira["Jira REST API\nhttps://org.atlassian.net\nAuth: Basic (email + API token)\nEnv: JIRA_BASE_URL\n     JIRA_USER_EMAIL\n     JIRA_API_TOKEN"]

    FS["Local Filesystem\nreports/<ticketId>/\n  analysis_report.html\nprojectRootPath/\n  src/.../NewClass.java"]

    Browser["Default Browser\nopens analysis_report.html\nfor developer review"]

    GitHub["GitHub REST API\nhttps://api.github.com\nAuth: Bearer token\nEnv: GITHUB_TOKEN\n     GITHUB_REPO"]

    Git["Local git CLI\nbranch, commit, push\nRequires: git on PATH"]

    A1 -->|"fetchTicket(id)"| Jira
    A2 -->|"createReportFolder()"| FS
    A3 -->|"readSourceFiles()"| FS
    A4 -->|"write analysis_report.html"| FS
    A5 -->|"open in browser\niterate HTML"| Browser
    A5 -->|"rewrite HTML on feedback"| FS
    A6 -->|"write code files\n(// FILE: blocks)"| FS
    A7 -->|"read written code files"| FS
    A8 -->|"createBranch\ncommitAll\npush"| Git
    A8 -->|"createPullRequest\nfetchPrComments"| GitHub
    A8 -->|"append PR comments\nto HTML report"| FS
```

---

## 7. LLM Provider Swap

```mermaid
flowchart LR
    subgraph App["Application Code (unchanged)"]
        Orch["WorkflowOrchestrator"]
        Agents["Agents 1–8"]
    end

    subgraph Interface["«interface» LlmClient"]
        CP["completePrompt(prompt) : String"]
    end

    subgraph Providers["Swap in Main.java — one line change"]
        Claude["ClaudeApiClient\nAnthropic API\nRequires: ANTHROPIC_API_KEY"]
        OpenAI["OpenAiApiClient\nOpenAI API\nRequires: OPENAI_API_KEY"]
        Ollama["OllamaApiClient\nLocal Ollama\nDefault model: phi3\nRequires: Ollama running locally"]
    end

    Orch --> CP
    Agents --> CP
    CP --> Claude
    CP --> OpenAI
    CP --> Ollama
```

---

## 8. HTML Report Lifecycle

The HTML report is the primary artefact that travels through Steps 2–8, evolving as the workflow progresses.

```mermaid
sequenceDiagram
    participant A2 as ProjectSetupAgent (Step 2)
    participant FS as FileSystem
    participant A4 as VisualReportAgent (Step 4)
    participant LLM as LLM Provider
    participant A5 as ReviewAgent (Step 5)
    participant Dev as Developer
    participant A8 as DeploymentAgent (Step 8)

    A2->>FS: createReportFolder(baseDir, ticketId)
    FS-->>A2: reportFolderPath stored in ctx

    A4->>LLM: completePrompt(visual_report.md)
    LLM-->>A4: HTML body content
    A4->>FS: HtmlReportWriter.write(folder, "analysis_report.html", body)
    FS-->>A4: htmlReportPath stored in ctx

    A5->>Dev: open analysis_report.html in browser
    loop up to 5 iterations
        Dev->>A5: provide feedback
        A5->>LLM: regenerate HTML with feedback
        LLM-->>A5: updated HTML body
        A5->>FS: rewrite analysis_report.html
        A5->>Dev: open updated report
    end
    Dev->>A5: approve report
    A5->>A5: generate reviewNotes via LLM

    A8->>FS: read analysis_report.html
    A8->>FS: append PR comments section
    FS-->>A8: report updated with PR comments
```

---

## 9. Prompt Template Resolution

Each agent resolves its prompt at runtime by loading a Markdown template and substituting placeholders with live context values.

```mermaid
sequenceDiagram
    participant Agent
    participant MarkdownLoader
    participant Classpath as resources/agents/*.md
    participant LlmClient
    participant LLM as LLM Provider

    Agent->>MarkdownLoader: load("agents/deep_dive.md")
    MarkdownLoader->>Classpath: getResourceAsStream(path)
    Classpath-->>MarkdownLoader: raw markdown bytes
    MarkdownLoader-->>Agent: template string

    Agent->>Agent: replace {{ticket_summary}} → ctx.ticketSummary
    Agent->>Agent: replace {{project_setup}}  → ctx.projectSetup
    Agent->>Agent: replace {{codebase_context}} → scanned source files

    Agent->>LlmClient: completePrompt(filledPrompt)
    LlmClient->>LLM: HTTP POST (Anthropic / OpenAI / Ollama)
    LLM-->>LlmClient: JSON response
    LlmClient-->>Agent: extracted text string

    Agent->>Agent: ctx.setDeepDive(responseText)
```
