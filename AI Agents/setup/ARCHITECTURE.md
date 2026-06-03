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
    │   │   ├── agents/                            ← stateless agent classes
    │   │   │   ├── TicketAnalysisAgent.java       ← Step 1: fetch from Jira or text
    │   │   │   ├── ProjectSetupAgent.java         ← Step 2: plan + create report folder
    │   │   │   ├── DeepDiveAgent.java             ← Step 3: analyse + scan codebase (cached)
    │   │   │   ├── VisualReportAgent.java         ← Step 4: write HTML report to disk
    │   │   │   ├── ReviewAgent.java               ← Step 5: open browser, iterate report
    │   │   │   ├── ImplementationAgent.java       ← Step 6: write code files, track progress
    │   │   │   ├── QualityAssuranceAgent.java     ← Step 7: review code, update fileQaStatus
    │   │   │   ├── DeploymentAgent.java           ← Step 8: local commits → (Gate 3) → push → PR
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
    │   │   │   ├── ClaudeApiClient.java           ← Anthropic (prompt caching enabled)
    │   │   │   ├── OpenAiApiClient.java           ← OpenAI
    │   │   │   └── OllamaApiClient.java           ← Local (OLLAMA_BASE_URL)
    │   │   ├── orchestrator/
    │   │   │   └── WorkflowOrchestrator.java      ← coordinates all agents; rerunAnalysis, runRefactoring, saveContext, loadContext
    │   │   └── util/
    │   │       ├── FileSystemUtil.java            ← folder creation, file read/write, codebase scan
    │   │       ├── HtmlReportWriter.java          ← wraps HTML body in page shell, writes file
    │   │       ├── MarkdownLoader.java            ← loads .md prompt templates from classpath
    │   │       └── WorkflowContextSerializer.java ← Jackson JSON save/load for session persistence
    │   └── resources/
    │       └── agents/                            ← one .md prompt per agent
    │           ├── ticket_analysis.md
    │           ├── project_setup.md
    │           ├── deep_dive.md
    │           ├── visual_report.md               ← incremental HTML building
    │           ├── review.md
    │           ├── implementation.md              ← Javadoc generation
    │           ├── quality_assurance.md           ← JUnit 5 test generation
    │           ├── deployment.md                  ← conventional commit groups
    │           └── refactoring.md                 ← optional refactoring step
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
        Interface["«interface»\nLlmClient\n─────────────\ncompletePrompt(prompt)\ncompletePromptCached(context, task)"]
        Claude["ClaudeApiClient\nAnthropic API\nprompt caching enabled"]
        OpenAI["OpenAiApiClient\nOpenAI API"]
        Ollama["OllamaApiClient\nOllama local"]
        Claude --> Interface
        OpenAI --> Interface
        Ollama --> Interface
    end

    subgraph Integrations["External Integrations & Utils"]
        JiraC["JiraClient\n─────────────\nfetchTicket(id)"]
        GitC["GitClient\n─────────────\ncreateBranch()\ncommitAll()\ncommitFiles(paths, msg)\npush()"]
        GitHubC["GitHubClient\n─────────────\ncreatePullRequest()\nfetchPrComments()"]
        FSU["FileSystemUtil\n─────────────\ncreateReportFolder()\nwriteFile()\nreadSourceFiles()"]
        HTML["HtmlReportWriter\n─────────────\nwrite(folder, file, title, body)"]
        Ser["WorkflowContextSerializer\n─────────────\nsave(ctx, path)\nload(path)\nsaveToReportFolder(ctx)"]
    end

    subgraph Orchestration["Orchestration  (orchestrator/)"]
        Orch["WorkflowOrchestrator\n─────────────\nrunWorkflow()\nrunWorkflowOptimized()\nrerunAnalysis()\nrunRefactoring()\nsaveContext()\nloadContext()\nhumanConfirm() — auto-saves"]
    end

    subgraph Agents["Agents  (agents/)"]
        A1["TicketAnalysisAgent\nStep 1"]
        A2["ProjectSetupAgent\nStep 2"]
        A3["DeepDiveAgent\nStep 3\ncaches snapshot"]
        A4["VisualReportAgent\nStep 4"]
        A5["ReviewAgent\nStep 5"]
        A6["ImplementationAgent\nStep 6\ntracks writtenFiles"]
        A7["QualityAssuranceAgent\nStep 7\nupdates fileQaStatus"]
        A8["DeploymentAgent\nStep 8\nPhase 1: local commits\nPhase 2: push + PR"]
        AR["RefactoringAgent\n(optional)"]
    end

    subgraph Context["Shared State  (context/)"]
        Ctx["WorkflowContext\n─────────────\njiraTicketId / projectRootPath\nticketText / ticketSummary\nprojectSetup / reportFolderPath\ndeepDive / codebaseSnapshot\nvisualReport / htmlReportPath\nreviewNotes\nimplementation\nwrittenFiles / pendingFiles\nimplementationStep\nfileQaStatus\nqaReport / refactoringPlan\nfeatureBranchName\ndeploymentStatus / prUrl / prComments\ncommittedFiles"]
    end

    subgraph Prompts["Prompt Templates  (resources/agents/)"]
        MD["*.md files\n(one per agent)"]
    end

    Main --> Interface
    Main --> Orch
    Orch --> A1 & A2 & A3 & A4 & A5 & A6 & A7 & A8 & AR
    A1 & A2 & A3 & A4 & A5 & A6 & A7 & A8 & AR --> Interface
    A1 & A2 & A3 & A4 & A5 & A6 & A7 & A8 & AR --> Ctx
    A1 --> JiraC
    A2 --> FSU
    A3 --> FSU
    A4 --> HTML
    A5 --> HTML
    A6 --> FSU
    A7 --> FSU
    AR --> FSU
    A8 --> GitC
    A8 --> GitHubC
    A8 --> FSU
    Orch --> Ser
    A1 & A2 & A3 & A4 & A5 & A6 & A7 & A8 & AR --> MD
```

---

## 3. Agent Workflow — Full Modular Mode (8 LLM calls)

```mermaid
flowchart TD
    START([Ticket: Jira ID or text])

    S1["Step 1 — Ticket Analysis\nTicketAnalysisAgent\n• fetch ticket from Jira (if jiraTicketId set)\n• writes: ticketSummary"]
    S2["Step 2 — Project Setup\nProjectSetupAgent\n• create report folder on disk\n• writes: projectSetup, reportFolderPath"]
    G1{{"HUMAN GATE 1\nApprove project\nstructure?\n(auto-saves context)"}}
    S3["Step 3 — Deep Dive\nDeepDiveAgent\n• scan codebase → cache in codebaseSnapshot\n• writes: deepDive (with gap analysis)"]
    S4["Step 4 — Visual Report\nVisualReportAgent\n• LLM generates HTML body (incremental)\n• write analysis_report.html to disk\n• writes: visualReport, htmlReportPath"]
    S5["Step 5 — Review\nReviewAgent\n• open HTML report in browser\n• iterate (up to 5×) on developer feedback\n• writes: reviewNotes"]
    G2{{"HUMAN GATE 2\nApprove analysis\nbefore coding?\n(auto-saves context)"}}
    S6["Step 6 — Implementation\nImplementationAgent\n• LLM generates annotated code blocks\n• parse // FILE: blocks, write files to disk\n• writes: implementation, writtenFiles, pendingFiles"]
    S7["Step 7 — Quality Assurance\nQualityAssuranceAgent\n• reuse cached codebaseSnapshot\n• writes: qaReport, fileQaStatus (REVIEWED)"]
    SREF["Optional — Refactoring\nRefactoringAgent\n• review QA findings + codebase\n• write refactored files to disk\n• writes: refactoringPlan; clears snapshot"]
    S8["Step 8 — Deployment\nDeploymentAgent Phase 1\n• LLM generates plan with commit groups\n• git branch → per-group local commits\n• writes: deploymentStatus, featureBranchName, committedFiles"]
    G3{{"HUMAN GATE 3\nPush branch and\nopen PR?\n(auto-saves context)"}}
    S8B["DeploymentAgent Phase 2\n• push featureBranch to origin\n• create GitHub PR → ctx.prUrl\n• fetch PR comments → update HTML report"]
    END([Workflow Complete\nPR open on GitHub])
    HALT([Workflow Halted])
    NOPR([Branch kept local\nNo PR created])

    START --> S1 --> S2 --> G1
    G1 -- "y (approved)" --> S3
    G1 -- "n (rejected)" --> HALT
    S3 --> S4 --> S5 --> G2
    G2 -- "y (approved)" --> S6
    G2 -- "n (rejected)" --> HALT
    S6 --> S7
    S7 -.->|optional| SREF
    SREF --> S8
    S7 --> S8
    S8 --> G3
    G3 -- "y (approved)" --> S8B --> END
    G3 -- "n (rejected)" --> NOPR

    style G1 fill:#f5a623,color:#000
    style G2 fill:#f5a623,color:#000
    style G3 fill:#f5a623,color:#000
    style HALT fill:#d9534f,color:#fff
    style NOPR fill:#d9534f,color:#fff
    style END fill:#5cb85c,color:#fff
    style START fill:#5bc0de,color:#000
    style SREF fill:#9b59b6,color:#fff
```

---

## 4. Agent Workflow — Optimized Mode (5 LLM calls)

```mermaid
flowchart TD
    START([Ticket: Jira ID or text])

    B1["LLM Call 1 — Batch: Steps 1 + 2\nTicket Analysis + Project Setup\n• fetch from Jira, create report folder\n• writes: ticketSummary, projectSetup, reportFolderPath"]
    G1{{"HUMAN GATE 1\nApprove project\nstructure?\n(auto-saves context)"}}
    B2["LLM Call 2 — Batch: Steps 3 + 4\nDeep Dive + Visual Report\n• scan codebase (cached), write HTML to disk\n• writes: deepDive, codebaseSnapshot, visualReport, htmlReportPath"]
    B3["LLM Call 3 — Step 5\nReview (with browser iteration loop)\n• writes: reviewNotes"]
    G2{{"HUMAN GATE 2\nApprove analysis\nbefore coding?\n(auto-saves context)"}}
    B4["LLM Call 4 — Batch: Steps 6 + 7\nImplementation + QA\n• write code files, review from disk\n• writes: implementation, writtenFiles, qaReport, fileQaStatus"]
    B5["LLM Call 5 — Step 8\nDeployment Phase 1\n• generate plan with commit groups\n• local commits per group\n• writes: deploymentStatus, featureBranchName"]
    G3{{"HUMAN GATE 3\nPush branch and\nopen PR?\n(auto-saves context)"}}
    B5B["Deployment Phase 2\n• push branch → create PR\n• fetch PR comments → update HTML"]
    END([Workflow Complete\nPR open on GitHub])
    HALT([Workflow Halted])
    NOPR([Branch kept local\nNo PR created])

    START --> B1 --> G1
    G1 -- "y" --> B2 --> B3 --> G2
    G1 -- "n" --> HALT
    G2 -- "y" --> B4 --> B5 --> G3
    G2 -- "n" --> HALT
    G3 -- "y" --> B5B --> END
    G3 -- "n" --> NOPR

    style G1 fill:#f5a623,color:#000
    style G2 fill:#f5a623,color:#000
    style G3 fill:#f5a623,color:#000
    style HALT fill:#d9534f,color:#fff
    style NOPR fill:#d9534f,color:#fff
    style END fill:#5cb85c,color:#fff
    style START fill:#5bc0de,color:#000
```

---

## 5. WorkflowContext Data Flow

Each agent reads specific fields and writes exactly one or two new fields. The diagram shows the full blackboard data
flow including new fields added in the implementation.

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
    A3 -->|write| CS["codebaseSnapshot\n(cached; reused by A7, AR)"]

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
    A6 -->|write| IM["implementation"]
    A6 -->|write| WF["writtenFiles\n(absolute paths)"]
    A6 -->|write| PF["pendingFiles\n(decrements per write)"]
    A6 -->|write| IS["implementationStep\n(counter)"]

    IM -->|read| A7
    CS -->|read| A7["QualityAssuranceAgent\nStep 7"]
    WF -->|read| A7
    A7 -->|write| QA["qaReport"]
    A7 -->|write| FQ["fileQaStatus\nPENDING → REVIEWED"]

    IM -->|read| AR
    QA -->|read| AR
    CS -->|read| AR["RefactoringAgent\n(optional)"]
    AR -->|write| RP["refactoringPlan"]
    AR -->|update| WF
    AR -->|clear| CS

    IM -->|read| A8
    QA -->|read| A8
    JID -->|read| A8
    HR -->|read| A8["DeploymentAgent\nStep 8"]
    WF -->|read| A8
    A8 -->|write| FB["featureBranchName"]
    A8 -->|write| CF["committedFiles"]
    A8 -->|write| DS["deploymentStatus"]
    A8 -->|write| PU["prUrl\n(GitHub PR)"]
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
        AR["RefactoringAgent\n(optional)"]
        A8["DeploymentAgent\nStep 8"]
        Orch["WorkflowOrchestrator\n(gates)"]
    end

    Jira["Jira REST API\nhttps://org.atlassian.net\nAuth: Basic (email + API token)\nEnv: JIRA_BASE_URL\n     JIRA_USER_EMAIL\n     JIRA_API_TOKEN"]

    FS["Local Filesystem\nreports/<ticketId>/\n  analysis_report.html\n  workflow_context.json\nprojectRootPath/\n  src/.../NewClass.java"]

    Browser["Default Browser\nopens analysis_report.html\nfor developer review"]

    GitHub["GitHub REST API\nhttps://api.github.com\nAuth: Bearer token\nEnv: GITHUB_TOKEN\n     GITHUB_REPO"]

    Git["Local git CLI\nbranch, commitFiles (per group)\ncommitAll (fallback), push\nRequires: git on PATH"]

    A1 -->|"fetchTicket(id)"| Jira
    A2 -->|"createReportFolder()"| FS
    A3 -->|"readSourceFiles() → cache"| FS
    A4 -->|"write analysis_report.html"| FS
    A5 -->|"open in browser / iterate HTML"| Browser
    A5 -->|"rewrite HTML on feedback"| FS
    A6 -->|"write code files (// FILE: blocks)"| FS
    A7 -->|"read codebaseSnapshot (cached)"| FS
    AR -->|"write refactored files"| FS
    A8 -->|"createBranch / commitFiles / push"| Git
    A8 -->|"createPullRequest / fetchPrComments"| GitHub
    A8 -->|"append PR comments to HTML"| FS
    Orch -->|"saveToReportFolder(ctx)\nat every gate"| FS
```

---

## 7. LLM Provider Swap

```mermaid
flowchart LR
    subgraph App["Application Code (unchanged)"]
        Orch["WorkflowOrchestrator"]
        Agents["Agents 1–8 + RefactoringAgent"]
    end

    subgraph Interface["«interface» LlmClient"]
        CP["completePrompt(prompt) : String"]
        CPC["completePromptCached(context, task) : String\ndefault: concatenate → completePrompt"]
    end

    subgraph Providers["Swap in Main.java — one line change"]
        Claude["ClaudeApiClient\nAnthropic API\ncache_control: ephemeral\nanthropic-beta: prompt-caching\nRequires: ANTHROPIC_API_KEY"]
        OpenAI["OpenAiApiClient\nOpenAI API\nfallback: concatenates\nRequires: OPENAI_API_KEY"]
        Ollama["OllamaApiClient\nLocal Ollama\nfallback: concatenates\nDefault model: phi3"]
    end

    Orch --> CP
    Orch --> CPC
    Agents --> CP
    Agents --> CPC
    CP --> Claude
    CP --> OpenAI
    CP --> Ollama
    CPC --> Claude
    CPC --> OpenAI
    CPC --> Ollama
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
    participant Orch as WorkflowOrchestrator
    participant A8 as DeploymentAgent (Step 8)

    A2->>FS: createReportFolder(baseDir, ticketId)
    FS-->>A2: reportFolderPath stored in ctx

    A4->>LLM: completePrompt(visual_report.md) — incremental HTML
    LLM-->>A4: HTML body (skeleton → sections filled)
    A4->>FS: HtmlReportWriter.write(folder, "analysis_report.html", body)
    FS-->>A4: htmlReportPath stored in ctx

    Orch->>FS: saveToReportFolder(ctx) — Gate 1 auto-save
    FS-->>Orch: workflow_context.json written

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

    Orch->>FS: saveToReportFolder(ctx) — Gate 2 auto-save
    FS-->>Orch: workflow_context.json written

    Orch->>FS: saveToReportFolder(ctx) — Gate 3 auto-save
    FS-->>Orch: workflow_context.json written

    A8->>FS: read analysis_report.html
    A8->>FS: append PR comments section
    FS-->>A8: report updated with PR comments
```

---

## 9. Prompt Template Resolution

Each agent resolves its prompt at runtime by loading a Markdown template and substituting placeholders with live context
values. Agents that use the codebase snapshot send it as a separate cached block.

```mermaid
sequenceDiagram
    participant Agent as DeepDiveAgent (example)
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
    Agent->>Agent: replace {{codebase_context}} → "(see codebase context above)"

    alt ClaudeApiClient (caching enabled)
        Agent->>LlmClient: completePromptCached(codebaseSnapshot, taskPrompt)
        LlmClient->>LLM: HTTP POST — content: [{text: snapshot, cache_control: ephemeral}, {text: taskPrompt}]\nHeader: anthropic-beta: prompt-caching-2024-07-31
        LLM-->>LlmClient: JSON response (cache HIT on 2nd+ call)
    else Other providers (fallback)
        Agent->>LlmClient: completePromptCached(codebaseSnapshot, taskPrompt)
        LlmClient->>LlmClient: default: codebaseSnapshot + "\n\n" + taskPrompt
        LlmClient->>LLM: HTTP POST — single content block
        LLM-->>LlmClient: JSON response
    end

    LlmClient-->>Agent: extracted text string
    Agent->>Agent: ctx.setDeepDive(responseText)
    Agent->>Agent: ctx.setCodebaseSnapshot(scannedFiles)
```

---

## 10. Context Persistence & Session Resume

The `WorkflowContextSerializer` saves the full pipeline state to JSON at every human gate.
An interrupted session can be resumed without re-running completed steps.

```mermaid
sequenceDiagram
    participant Orch as WorkflowOrchestrator
    participant FS as FileSystem
    participant Ser as WorkflowContextSerializer

    Note over Orch: Gate 1 — after Steps 1+2
    Orch->>Ser: saveToReportFolder(ctx)
    Ser->>FS: write reportFolderPath/workflow_context.json
    FS-->>Ser: OK
    Orch->>Orch: humanConfirm() — user approves or halts

    Note over Orch: Gate 2 — after Step 5
    Orch->>Ser: saveToReportFolder(ctx)
    Ser->>FS: overwrite workflow_context.json
    FS-->>Ser: OK

    Note over Orch: Gate 3 — after Step 8 Phase 1
    Orch->>Ser: saveToReportFolder(ctx)
    Ser->>FS: overwrite workflow_context.json
    FS-->>Ser: OK
    Orch->>Orch: humanConfirm() — user approves push or keeps branch local

    Note over FS,Ser: --- Next session (resume after halt) ---
    participant NewOrch as WorkflowOrchestrator (new session)
    NewOrch->>Ser: load("path/to/workflow_context.json")
    Ser->>FS: read JSON file
    FS-->>Ser: raw JSON bytes
    Ser-->>NewOrch: WorkflowContext (fully restored)
    Note over NewOrch: All agent outputs preserved\nResume from halted step
```

---

## 11. Optional Steps: rerunAnalysis & runRefactoring

Two optional operations can be triggered at any point after their prerequisite steps complete.

```mermaid
flowchart TD
    subgraph Normal["Normal Pipeline"]
        S3["Step 3\nDeepDiveAgent\n(caches snapshot)"]
        S4["Step 4\nVisualReportAgent"]
        S7["Step 7\nQualityAssuranceAgent"]
        S8["Step 8\nDeploymentAgent"]
    end

    subgraph Optional["Optional Operations (orchestrator helpers)"]
        RA["rerunAnalysis()\n1. ctx.codebaseSnapshot = null\n2. deepDiveAgent.execute(ctx)\n3. visualReportAgent.execute(ctx)\nUse: codebase changed after implementation"]

        RF["runRefactoring()\n1. refactoringAgent.execute(ctx)\n   - reuse cached snapshot\n   - write refactored files to disk\n   - clear snapshot after writes\nUse: QA found code smells to fix\nbefore final commit"]
    end

    S3 -.->|"codebase changed?\ncall to refresh"| RA
    S4 -.-> RA
    RA -.->|"re-runs 3+4\nwith fresh scan"| S3

    S7 -.->|"QA found smells?"| RF
    RF -.->|"modified files written\nsnapshot cleared"| S8

    style RA fill:#3498db,color:#fff
    style RF fill:#9b59b6,color:#fff
```
