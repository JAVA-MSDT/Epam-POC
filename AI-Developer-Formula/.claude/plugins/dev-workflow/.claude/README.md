# Dev Workflow Plugin

---

## About

Automates ticket analysis, iterative review, and step-by-step implementation with Human-In-The-Loop (HITL) approval
using Claude agents, commands, and skills.

This plugin mirrors exactly how a senior developer handles a ticket: understand it deeply, validate the analysis, then
implement in small verified steps. Every decision that matters requires your explicit approval.

```
PHASE 1 — ANALYSIS
─────────────────────────────────────────────────────────────────────
You           → Run /start-ticket-analysis
              → Provide: ticket (JIRA ID / URL / pasted text),
                         codebase path, output format (html/md)

Claude        → Spawns ticket_analysis_agent (Haiku):
                  fetches or reads the requirements, returns structured JSON
              → Spawns report_generator_agent (Sonnet):
                  explores the codebase (Glob, Grep, Read)
                  maps affected files, risks, edge cases
                  builds a numbered implementation plan
                  writes codebase snapshot to .dev-workflow/codebase_context.md
                  writes report to .dev-workflow/<prefix>.html
              → Saves state to .dev-workflow/<prefix>_state.json

You           → Open and read the report
              → Identify gaps, unclear areas, missing scenarios

PHASE 2 — REVIEW LOOP  (repeat until satisfied)
─────────────────────────────────────────────────────────────────────
You           → Run /submit-review-feedback
              → Paste your findings:
                "The auth edge case for expired tokens is missing.
                 Section 3 doesn't cover the background job impact."

Claude        → Spawns reanalysis_agent (Sonnet):
                  patches the snapshot for completed steps
                  re-analyzes the specific areas you flagged
                  returns revised findings
              → Spawns report_generator_agent (Sonnet):
                  updates the report in place (adds "Review Iteration N")
                  revises the implementation plan if needed

You           → Review the updated report
              → Repeat as many times as needed
              → Stop when you are satisfied with the analysis

PHASE 3 — IMPLEMENTATION  (one step at a time)
─────────────────────────────────────────────────────────────────────
You           → Run /approve-step

Claude        → Shows you exactly what step N will do:
                title, files to touch, test command, commit message
              → Asks for confirmation AND test mode before touching any code

You           → Confirm (or ask to show files first)
              → Choose test mode:
                - auto   — agent runs tests, full output enters context (costs tokens)
                - manual — you run the command yourself and paste results (saves tokens)

Claude        → Spawns implementation_agent (Sonnet):
                  reads every target file (creates new ones if required)
                  implements the step
                  runs tests if auto; skips if manual
                  verifies all step.files were actually created/modified
                  returns result JSON (never commits)
              → If any expected file is missing: re-spawns agent with correction
              → Shows git diff of actual changes
              → Asks: "Does the implementation look correct?"

You           → Review the code
              → Reply "looks good" OR provide corrections

Claude        → If corrections: fixes the code, re-runs tests, asks again
              → If looks good: shows the exact git commit command to run

You           → Run the commit command yourself, then answer:
                - "yes"    → committed now, workflow advances
                - "later"  → skip commit, batch with future steps
                - "not yet"→ repeat the command, workflow holds
              → Run /approve-step for the next step
              → OR run /rollback-step if something needs to be undone
              → On the last step: receive a ready-to-paste PR description

ROLLBACK  (any time during implementation)
─────────────────────────────────────────────────────────────────────
You           → Run /rollback-step (or /rollback-step 2 for a specific step)

Claude        → Shows which commit will be reverted
              → Asks for confirmation

You           → Confirm

Claude        → Provides `git revert <hash> --no-edit` for you to run
              → Waits for your confirmation that the revert is done
              → Updates state so /approve-step resumes from the right place

SNAPSHOT REFRESH  (if teammates merge significant changes mid-ticket)
─────────────────────────────────────────────────────────────────────
You           → git pull (teammates' changes land)
              → Run /refresh-snapshot

Claude        → Re-explores the codebase (respecting scope if set)
              → Rewrites codebase_context.md with current state
              → Reports what files were added, removed, or changed
              → State file, report, and implementation plan are untouched

You           → If pulled changes affect remaining steps:
                run /submit-review-feedback to update the plan
              → Otherwise: continue with /approve-step
```

---

## Features

- Accepts requirements from any source: JIRA ticket ID, URL, or pasted text
- Optional `scope` parameter constrains analysis to specific subdirectories — essential for large repos
- Analyzes the codebase against requirements and generates a full HTML or Markdown report
- Writes a `codebase_context.md` snapshot — reused by review iterations to avoid redundant file reads
- Builds a structured, self-contained implementation plan as part of the report
- Supports unlimited review/feedback iterations — report updates in place
- Shows `git diff` after each implementation step — review real changes, not Claude's description
- Implements code step-by-step with HITL confirmation before each step; developer runs git commands manually; after each step choose **yes** (committed), **later** (batch with future steps), or **not yet** (re-prompt)
- Generates a ready-to-paste PR description when all steps are complete
- Rollback any step with `git revert` — git history is always preserved
- Validates state file integrity on every command — clear error if a previous run failed mid-way
- Persists workflow state in `<your-project>/.dev-workflow/` — survives session restarts

---

## Tech Stack

| Component           | Technology                                                                                                             |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| AI runtime          | [Claude Code](https://claude.ai/code)                                                                                  |
| Agent model tiers   | **Haiku** (`ticket_analysis_agent` — parse/fetch only) · **Sonnet** (`report_generator`, `reanalysis`, `implementation`) — see `.claude/models.md` |
| Agent orchestration | Claude Code subagents (`.claude/agents/`) spawned via the `Task` tool                                                 |
| Commands            | Claude Code slash commands (`.claude/commands/`) — thin launchers, all user interactions stay inline                  |
| Ticket source       | JIRA REST API v3, any URL, or plain text                                                                               |
| Version control     | Git (`git add`, `git commit`, `git revert`)                                                                            |
| Report output       | Self-contained HTML (inline CSS) or Markdown                                                                           |
| State persistence   | JSON files in `<your-project>/.dev-workflow/`                                                                          |

No external packages or build tools required — this is a prompt-only plugin.

---

## Project Structure

```
dev-workflow/
├── CLAUDE.md                        ← Plugin entry point (read by Claude Code)
└── .claude/
    ├── README.md                    ← This file
    ├── settings.json                ← Permissions and lifecycle hooks
    ├── models.md                    ← Model tier reference (update here + agent frontmatter when a new model ships)
    ├── agents/
    │   ├── ticket_analysis_agent.md ← Haiku — fetches/parses requirements, returns structured JSON
    │   ├── report_generator_agent.md← Sonnet — analyzes codebase, builds report + implementation plan
    │   ├── reanalysis_agent.md      ← Sonnet — patches snapshot, re-analyzes flagged areas
    │   └── implementation_agent.md  ← Sonnet — edits files, runs tests, returns result JSON (never commits)
    ├── commands/
    │   ├── start_ticket_analysis.md ← /start-ticket-analysis — thin launcher: spawns ticket_analysis_agent then report_generator_agent
    │   ├── submit_review_feedback.md← /submit-review-feedback — spawns reanalysis_agent then report_generator_agent
    │   ├── approve_step.md          ← /approve-step — HITL confirm → spawns implementation_agent → git diff → commit HITL
    │   ├── rollback_step.md         ← /rollback-step
    │   ├── refresh_snapshot.md      ← /refresh-snapshot
    │   └── status.md                ← /status
    └── skills/
        ├── fetch_jira_ticket.md     ← JIRA API / URL fetch / paste fallback
        ├── analyze_codebase.md      ← Glob + Grep + Read patterns for analysis
        ├── generate_html_report.md  ← HTML report structure and inline CSS spec
        ├── generate_md_report.md    ← Markdown report structure spec
        └── implement_code_change.md ← Edit + test logic and rollback patterns
```

---

## Getting Started

### 1. Open the plugin directory in Claude Code

Claude Code must be opened with `dev-workflow/` as the working directory so it loads the `.claude/` config.

```bash
cd path/to/dev-workflow
claude
```

Or open it as the root in VS Code / JetBrains with the Claude Code extension.

### 2. Verify the plugin loads

Type `/` in the Claude Code prompt. You should see all six commands:

- `/start-ticket-analysis`
- `/submit-review-feedback`
- `/approve-step`
- `/rollback-step`
- `/refresh-snapshot`
- `/status`

If any are missing, check that `.claude/commands/` contains the matching `.md` files.

### 3. Run your first analysis

```
/start-ticket-analysis
```

Claude will ask for:

- **ticket_source** — a JIRA ID (e.g., `PROJ-123`), a URL, or paste your requirements directly
- **codebase_path** — path to the repo you want analyzed
- **output_format** — `html` (default) or `md`
- **test_command** *(optional)* — e.g., `npm test`, `pytest`, `go test ./...`
- **scope** *(optional)* — comma-separated subdirectories to limit analysis (e.g., `src/auth,src/api`). Omit for full
  codebase analysis. Use for large repos where the ticket affects a known subsystem.

### Using This Plugin in Another Project

The plugin is a self-contained Claude Code project. You open it in its own directory and point it at whichever project
you want to analyze. **It never touches the target project's `.claude/` configuration** — this means it can sit
alongside any other Claude Code plugins or project settings without conflict.

```
dev-workflow/          ← open Claude Code here
  .claude/             ← plugin's own commands, agents, settings
  CLAUDE.md

your-project/          ← passed as codebase_path
  src/
  .claude/             ← your project's own config — untouched
  .dev-workflow/       ← state and reports written here by the plugin
```

Open Claude Code in the `dev-workflow/` directory, then provide your project path when prompted:

```
/start-ticket-analysis
  codebase_path = C:\Projects\your-project
  test_command  = npm test
```

The plugin writes all state and reports into `your-project/.dev-workflow/` — not inside itself. Each analysis gets its
own named files (`PROJ-123_add-token-refresh.html`, `PROJ-123_add-token-refresh_state.json`), so you can run multiple
analyses without overwriting anything.

> **Do not copy `.claude/` into your project.** That would overwrite your project's own Claude Code settings. The plugin
> is designed to be used as a separate directory.

### What you need to provide

| Input           | Description                                | Required                       |
| --------------- | ------------------------------------------ | ------------------------------ |
| `ticket_source` | JIRA ID, URL, or pasted text               | Yes                            |
| `codebase_path` | Path to the repo root you want analyzed    | Yes                            |
| `output_format` | `html` (default) or `md`                   | No                             |
| `test_command`  | e.g. `npm test`, `pytest`, `go test ./...` | No (asked per step if missing) |

### Testing the Plugin

All tests below use the plugin against its own directory — no external project needed.

---

#### Test 1 — Initial analysis (smoke test)

```
/start-ticket-analysis
  ticket_source = "Add a /status command that shows the current workflow state"
  codebase_path = .
  output_format = html
  test_command  = echo "no tests"
```

**What to verify:**

- Claude asks for any missing inputs before proceeding
- Claude does NOT accept "yes" or "ok" as a `codebase_path` — rejects and asks again
- Terminal shows hook output proving agents were spawned:
  ```
  [timestamp] >>> Subagent STARTED: ticket_analysis_agent
  [timestamp] <<< Subagent FINISHED: ticket_analysis_agent
  [timestamp] >>> Subagent STARTED: report_generator_agent
  [timestamp] <<< Subagent FINISHED: report_generator_agent
  ```
- `.dev-workflow/` folder is created with a `.gitignore` prompt
- Report created at `.dev-workflow/pasted_add-status-command.html`
- Report contains: ticket summary, affected files table, risk section, numbered implementation plan, open questions
- `.dev-workflow/pasted_add-status-command_state.json` exists and contains `"phase": "review"`, `"current_step": 0`,
  non-empty `implementation_plan`
- `.dev-workflow/active_state.json` contains `"state_path"` pointing to the state file above
- `.dev-workflow/codebase_context.md` exists and contains tech stack, file map, and key patterns

---

#### Test 2 — Scoped analysis (large repo simulation)

```
/start-ticket-analysis
  ticket_source = "Improve error handling in commands"
  codebase_path = .
  scope         = .claude/commands
  output_format = html
  test_command  = echo "no tests"
```

**What to verify:**

- Claude only explores `.claude/commands/` — does not read agent or skill files
- The snapshot `codebase_context.md` header shows the scope constraint
- Report's affected files list only contains files under `.claude/commands/`

---

#### Test 3 — Review loop

After Test 1, run:

```
/submit-review-feedback
  findings = "The report doesn't mention what happens if workflow_state.json is corrupted or partially written"
```

**What to verify:**

- Claude reads `codebase_context.md` first (not re-exploring from scratch)
- Claude does targeted reads of only the relevant command files
- Report gains a `## Review Iteration 1` section near the top
- Open questions or risk sections are updated
- State file shows `"review_iterations": 1`
- `codebase_context.md` is NOT rewritten (it should only be regenerated by `/start-ticket-analysis`)

Run a second iteration:

```
/submit-review-feedback
  findings = "Also check how rollback handles the case where git revert has merge conflicts"
```

**What to verify:**

- Report gains `## Review Iteration 2`
- State file shows `"review_iterations": 2`

---

#### Test 4 — State validation

Manually corrupt the state file — open `pasted_add-status-command_state.json` and delete the `codebase_path` field. Then
run:

```
/approve-step
```

**What to verify:**

- Claude stops immediately with a clear error: "The state file is incomplete (missing: `codebase_path`)"
- Claude does NOT attempt to implement anything
- Claude tells you to re-run `/start-ticket-analysis`

Restore the field before continuing.

---

#### Test 5 — Implementation with HITL and git diff

```
/approve-step
```

**What to verify:**

- Claude shows step details (title, description, files, test command, commit message) and asks two questions together:
  "Proceed? And how should tests run? (yes auto / yes manual / no / show me the files first)"
- Claude does NOT implement anything until you explicitly say yes
- Terminal shows hook output confirming implementation_agent was spawned:
  ```
  [timestamp] >>> Subagent STARTED: implementation_agent
  [timestamp] <<< Subagent FINISHED: implementation_agent
  ```
- If you chose `auto`: agent ran the test command and output appears in the response
- If you chose `manual`: Claude asks you to run the command and paste the result before proceeding
- Claude runs `git diff` and displays the actual diff — not just a description
- Claude asks "Does the implementation look correct?" before giving the commit command
- If you say "no" or provide corrections: Claude fixes the code and asks again before giving commit command
- If you say "looks good": Claude shows explicit `git add <files>` and `git commit -m "..."` commands for you to run —
  it does NOT commit automatically
- Claude then asks "Have you committed step N? (yes / later / not yet)"
- Reply **"yes"**: state records `"commit": "committed"`, workflow advances
- Reply **"later"**: state records `"commit": "later"`, workflow advances — Claude reminds you to commit before pushing; rollback requires manual `git checkout` since no standalone commit exists
- Reply **"not yet"**: Claude repeats the git commands and waits again — workflow does not advance
- If any earlier steps are marked `"later"`, each new `/approve-step` shows a `⚠ Uncommitted steps` banner listing them
- On the final step, if any steps are `"later"`, Claude shows the full list with exact commit commands before generating the PR description

---

#### Test 6 — Rollback

After committing step 1 manually (run the git commands Claude provided), run:

```
/rollback-step
```

**What to verify:**

- Claude reads state and identifies the last completed step
- Claude shows: step title, commit to revert, and a confirmation prompt
- Claude does NOT revert automatically — waits for "yes"
- After confirmation: Claude provides `git revert <hash> --no-edit` for you to run
- After you confirm the revert is done: state file removes step 1 from `completed_steps`, sets `current_step` to 0

---

#### Test 7 — Completion and PR description

Implement all steps (run `/approve-step` repeatedly, committing each step manually). On the final step:

**What to verify:**

- After you say "looks good" on the last step: Claude generates a PR description with ticket title, summary, per-step
  bullets with commit messages, full files-changed list, and test commands
- `active_state.json` is updated to `{"state_path": null}`
- Running `/approve-step` again shows "All N steps are complete" — not an error

---

#### Test 8 — Session persistence

After any command mid-workflow, close Claude Code completely. Reopen it in the `dev-workflow/` directory. Run:

```
/approve-step
```

**What to verify:**

- Claude reads `active_state.json`, finds the state file, and resumes exactly where you left off
- No need to re-run `/start-ticket-analysis`
- `/status` shows the current workflow state correctly

---

#### Test 9 — Snapshot staleness protection

Run `/start-ticket-analysis` twice on the same `codebase_path` with different `ticket_source` values.

**What to verify:**

- Each run deletes and regenerates `codebase_context.md` — never reuses the previous ticket's snapshot
- The snapshot header shows the new ticket's `file_prefix`
- `active_state.json` points to the new ticket's state file

---

#### Test 10 — Snapshot refresh mid-ticket

After Test 1 (with an active workflow in place), simulate a teammate's change by adding a new file to
`.claude/commands/`:

```bash
# Simulate a teammate commit landing
echo "# placeholder" > .claude/commands/test_placeholder.md
```

Then run:

```
/refresh-snapshot
```

**What to verify:**

- Claude shows the confirmation prompt with active ticket, codebase path, and scope before proceeding
- Claude does NOT proceed without explicit "yes"
- After confirmation: Claude announces "Re-exploring codebase — reading directly from codebase (not snapshot)"
- `codebase_context.md` is rewritten — snapshot header shows `Refreshed: yes` and the new timestamp
- The summary output lists `test_placeholder.md` as a newly added file
- The state file, report file, and `implementation_plan` are completely unchanged
- Claude ends with a recommendation to run `/submit-review-feedback` if the changes affect remaining steps

Clean up afterwards:

```bash
rm .claude/commands/test_placeholder.md
```

---

## Configuration

### JIRA credentials

Only needed if you use JIRA ticket IDs. Set these environment variables before starting:

```bash
export JIRA_URL=https://yourcompany.atlassian.net
export JIRA_USERNAME=your.email@company.com
export JIRA_API_KEY=your_api_token    # from id.atlassian.com → Security → API tokens
```

If you don't have JIRA, just paste the requirements text when prompted — the plugin works the same way.

### Test runner

You can tell the plugin your project's test command upfront:

```
/start-ticket-analysis test_command="npm test"
```

Or leave it out — Claude will ask per step during implementation.

### Permissions and hooks (`settings.json`)

The plugin's `settings.json` grants these permissions automatically:

| Permission                                            | Purpose                                  |
| ----------------------------------------------------- | ---------------------------------------- |
| `Read(*)`, `Write(*)`, `Edit(*)`                      | Codebase analysis and report generation  |
| `Bash(curl:*)`                                        | Fetching JIRA tickets                    |
| `Bash(git add/commit/revert/log/status/diff/stash:*)` | Committing and rolling back steps        |
| `Bash(mkdir/cat/echo:*)`                              | Creating the state directory and logging |

Three lifecycle hooks log to the terminal with timestamps:

| Hook             | When it fires                        | Output                                          |
| ---------------- | ------------------------------------ | ----------------------------------------------- |
| `Stop`           | Main agent finishes its turn         | `[2026-06-22 14:03:01] Agent finished`          |
| `SubagentStart`  | A subagent is spawned via Task       | `[2026-06-22 14:03:02] >>> Subagent STARTED: ticket_analysis_agent` |
| `SubagentStop`   | A subagent finishes and returns      | `[2026-06-22 14:03:08] <<< Subagent FINISHED: ticket_analysis_agent` |

The agent name is extracted from the JSON payload Claude Code passes to the hook — it matches the filename in `.claude/agents/` (without `.md`). These hooks let you confirm that agents are actually spawned, not just simulated inline.

### State management

Workflow state is written to `<codebase_path>/.dev-workflow/`. Each analysis gets its own named files derived from the
ticket ID and title:

```
your-project/.dev-workflow/
  PROJ-123_add-token-refresh.html          ← analysis report
  PROJ-123_add-token-refresh_state.json    ← workflow state
  active_state.json                        ← pointer to current active analysis
  codebase_context.md                      ← codebase snapshot (reused by review iterations to avoid re-reading files)
```

Key fields in the state file:

| Field                 | Description                                                        |
| --------------------- | ------------------------------------------------------------------ |
| `phase`               | `review` or `implementation`                                       |
| `ticket_source`       | The original ticket input                                          |
| `file_prefix`         | The slug used for this analysis's filenames                        |
| `codebase_path`       | The analyzed repo path                                             |
| `report_path`         | Full path to the HTML/MD report                                    |
| `implementation_plan` | Array of step objects (title, files, test_command, commit_message) |
| `current_step`        | Last completed step number                                         |
| `completed_steps`     | Array of completed steps with commit status                        |
| `review_iterations`   | Number of feedback cycles completed                                |

**To start a new analysis:** just run `/start-ticket-analysis` — it creates new files with the new ticket's name.
Previous analyses remain in `.dev-workflow/` untouched.

**To reset everything:** delete the `.dev-workflow/` folder from your project root.

**Add to `.gitignore`:** the plugin offers to do this automatically on first run. If you skipped it, add manually:

```
.dev-workflow/
```

### User advice

**Before you start**

- **Commit a clean state first.** The plugin creates commits during implementation. Starting from a dirty working tree
  makes rollback much harder. Run `git status` and commit or stash anything in progress.
- **Be specific with `codebase_path`.** Point it at the actual repository root — not a parent directory with many
  unrelated projects.

**During analysis**

- **The more specific your findings, the better the re-analysis.** Instead of "the auth section needs more detail",
  say "the report doesn't cover what happens when the refresh token is expired and the user has an active request in
  flight."
- **You can iterate as many times as you need.** There is no limit on `/submit-review-feedback` cycles. Stop only when
  you trust the implementation plan.
- **Trust the open questions section.** If the report flags something as an open question, answer it before approving
  implementation. Open questions during analysis become bugs during implementation.

**During implementation**

- **Read each step before confirming.** Claude will show you the step details and ask for confirmation. If something
  looks wrong, say no and use `/submit-review-feedback` to revise the plan.
- **Each step is one commit.** This is intentional. Small commits make it easy to `git bisect`, rollback individual
  steps, and understand the change history.
- **If tests fail, don't skip them lightly.** Claude will ask whether to fix, skip, or rollback. Skipping tests defeats
  the purpose of the gating.
- **`/rollback-step` is safe.** It uses `git revert` — it never rewrites history. You can rollback, revise the plan, and
  re-implement.

**State and sessions**

- **State survives session restarts.** If you close Claude Code mid-workflow, re-open in the same directory and
  continue.
- **Don't commit `.dev-workflow/` to your repo.** State files, reports, and the codebase snapshot are session-specific
  and contain local paths. The plugin offers to add it to `.gitignore` automatically on first run.

---

## Roadmap

### Done — Implemented

| #   | What                                | Status                                                                                                                                                                                   |
| --- | ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **`git diff` after implementation** | ✅ `approve_step.md` Step 6 now runs `git diff` and displays the full output before asking for review.                                                                                    |
| 2   | **Snapshot regeneration guarantee** | ✅ Snapshot write moved to Step 6 (after `mkdir`); old `codebase_context.md` deleted before each new run.                                                                                 |
| 3   | **PR description on completion**    | ✅ `approve_step.md` Step 9 generates a copy-ready PR description when all steps complete.                                                                                                |
| 4   | **State validity check**            | ✅ All three commands (`approve_step`, `submit_review_feedback`, `rollback_step`) validate required fields after reading state.                                                           |
| 5   | **Analysis scope parameter**        | ✅ Optional `scope` input added to `/start-ticket-analysis`; constrains exploration and snapshot to specified subdirectories.                                                             |
| 6   | **`/refresh-snapshot` command**     | ✅ Re-explores the codebase and rewrites `codebase_context.md` without touching the state file, report, or implementation plan. Run after any significant pull during an active workflow. |
| 7   | **Multi-agent architecture**        | ✅ Commands are now thin launchers — each spawns specialized subagents via the `Task` tool. `ticket_analysis_agent` uses Haiku (parse-only); `report_generator_agent`, `reanalysis_agent`, and `implementation_agent` use Sonnet. Model assignments live in `.claude/models.md` + each agent's frontmatter. The `orchestrator_agent.md` (previously unused) was removed. |
| 8   | **Lifecycle hooks fixed**           | ✅ Previous `PostToolUse { matcher: "Task" }` hook never fired because commands were monolithic. Replaced with `SubagentStart` and `SubagentStop` hooks that extract and print the agent name from the stdin JSON payload — visible proof that each agent is truly spawned. |
| 9   | **Commit verification**             | ✅ After the developer says "yes" to a commit, `approve_step.md` runs `git log --oneline -1` and checks the output matches the expected commit message before advancing state. |
| 10  | **Per-step test mode**              | ✅ Before each implementation step, developer chooses `auto` (agent runs tests, full output enters context — costs tokens) or `manual` (developer runs tests, pastes result — saves tokens). Choice is per-step, not session-wide. |
| 11  | **Missing file guard**              | ✅ Two-layer check: `implementation_agent` self-verifies all `step.files` were touched before returning; `approve_step.md` cross-checks `files_modified` against `step.files` after the agent returns and re-spawns with a correction if any file is missing. Test files are treated as required deliverables, not optional. |

### Future — Larger Features

These require more work but would meaningfully extend the plugin's value.

| #   | What                        | Why                                                                                                                                                                                                            |
| --- | --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 7   | **`/edit-plan` command**    | Common scenario: the report is good but you want to reorder or split steps before implementing. Currently forces a full `/submit-review-feedback` cycle. A direct plan editor removes that friction.           |
| 8   | **JIRA write-back**         | After implementation, post a comment to the JIRA ticket with commit hashes and report path. Closes the loop on the ticket lifecycle without leaving the workflow.                                              |
| 9   | **Step `depends_on` field** | The plan is strictly sequential but some steps are genuinely parallel (update tests + update docs). An optional `depends_on` field on each step would surface which steps the developer could run in parallel. |

### Known Limitations

- **Prompt-only execution** — there is no code enforcing that Claude followed every step. Report quality depends on the
  model's context at the time. Always read the report critically.
- **Large codebases** — for repos over ~500k LOC, the snapshot will inevitably be incomplete. Treat the analysis as
  directional, not exhaustive.
- **Trivial tickets** — the analysis overhead is not worth it for one-liner changes, config updates, or renames. Use the
  plugin for medium-to-large features and non-trivial bug fixes.

---

## License

MIT

---

## Author

- **Ahmed Samy** — collected from real-world AI experience and shared for the community.
- **LinkedIn** [Catch Me There](https://www.linkedin.com/in/java-msdt/)

## Design Notes

- **No external dependencies (by design):** JIRA fetching uses `curl` via Claude's `Bash` tool; all file I/O uses
  Claude's native `Read`/`Write`/`Glob`/`Grep` tools. No npm install, no bundled scripts, no files added to your project
  beyond the Markdown commands themselves.

## To be fixed


## To be tested

- I can see that the hook is not firing for the agent or the subagent finishes the work.
  - TO BE TESTED

- I would like also to add 
  - When all the steps are implemented, nothing else to be done and the story finished.
  - I would like to give the developer the last message which is a Merge Request details as bellow
    - Title: in that pattern: [Story Number] Story title
    - Description:
      - what was the task about
      - What have been done
      - What test approaches were used
- TO BE TESTED

- I would like to add this feature
  - Context
    - in the implementation steps, if we have file A in step one , then we need to add something to it in another step, we will need to read it again
    - that means we will use more tokens due to reading the same file twice even tho we need to update something minor or depends.
  - My idea
    - during the analysis phase and generating the implementation plan
    - Generating the plan that takes into consideration the following
      - is it possible to implement all the change in the targeted file at once or not
      - If yes we can combine this step to be one step
      - If not, we can see if the changes will be a lot for the one steps
    - My point is that we need to be economic when it comes to Tokens, we can do that by making sure that we have a concise implementation steps that will not update the same multiple times, also step that will update only one small change we can combine with other step, there is no need that each step.
- TO BE TESTED

- I noticed that if i start running the `/start-ticket-analysis` in the middle of the implementation the plugin will really started again and will start reading everything, should it validate first if there is already analyze exists for that ticket, description or the url, by validating the .dev-workflow exists and the required task for analyze already analyzed before and the developer should use  `/refresh-snapshot` instead and giving a warning about the existence of the file that what is in the .dev-workflow already matching the provided task, also if the dev insist to start the fresh analyze we can inform him to validate the .dev-workflow folder, then deleting it to start a brand new analyze.
- TO BE TESTED