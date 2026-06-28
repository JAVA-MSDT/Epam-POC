You are executing the `/submit-review-feedback` workflow. Follow every step in order.

## Step 1 — Read State

Read `<codebase_path>/.dev-workflow/active_state.json` to find the current state file path. If it does not exist,
look for any `*_state.json` file in `.dev-workflow/` and use the most recently modified one.

If no state file is found, stop: "No active analysis found. Run `/start-ticket-analysis` first."

Extract: `report_path`, `codebase_path`, `implementation_plan`, `review_iterations`, `state_path`,
`completed_steps`, `output_format`, `file_prefix`.

**Validate required fields.** Stop if any are missing or null:
> "The state file is incomplete (missing: `<field list>`). Re-run `/start-ticket-analysis` to create a fresh analysis."

Required: `codebase_path`, `state_path`, `report_path`, `implementation_plan`.
`completed_steps` defaults to `[]` if absent — not an error.

## Step 2 — Collect Findings

If the user did not pass findings as an argument, ask:
> "What gaps, errors, or areas need more analysis in the current report?"

Wait for free-form text before proceeding.

## Step 3 — Spawn reanalysis_agent

Use the Task tool to spawn `reanalysis_agent` with this prompt:

```
Re-analyze the codebase based on reviewer findings.

findings: <findings from Step 2>
current_report_path: <report_path>
codebase_path: <codebase_path>
implementation_plan: <implementation_plan JSON>
completed_steps: <completed_steps JSON>
```

Wait for the agent to complete. It returns:

```json
{
  "addressed_findings": [{ "finding": "string", "resolution": "string", "plan_changes": "string | null" }],
  "new_open_questions": ["string"],
  "updated_implementation_plan": [<full plan array or null if unchanged>],
  "new_affected_files": ["string"]
}
```

If the agent returns an error, stop and tell the user what went wrong.

Store the result as `reanalysis_result`.

## Step 4 — Spawn report_generator_agent

Use the Task tool to spawn `report_generator_agent` with this prompt:

```
Update the existing report with re-analysis findings. This is review iteration <review_iterations + 1>.

requirements: <reconstructed from state: ticket_source, title from report, requirements from state>
codebase_path: <codebase_path>
output_format: <output_format>
file_prefix: <file_prefix>
report_path: <report_path>
scope: <scope from state or null>
findings: <reanalysis_result JSON>
review_iteration: <review_iterations + 1>
```

Wait for the agent to complete. It returns the same structure as in `/start-ticket-analysis`:

```json
{
  "report_path": "string",
  "implementation_plan": [...],
  "open_questions": ["string"],
  "affected_files_count": 0,
  "step_count": 0,
  "test_strategy": { ... }
}
```

If the agent returns an error, stop and tell the user what went wrong.

Store the result as `report_result`.

## Step 5 — Update State

Update the state file at `state_path`:

- Increment `review_iterations` by 1
- If `reanalysis_result.updated_implementation_plan` is not null, overwrite `implementation_plan` with it
- If `report_result.test_strategy` changed, overwrite `test_strategy`

## Step 6 — Present to User

Show:

1. Which findings were addressed (from `reanalysis_result.addressed_findings`)
2. Any new open questions discovered
3. Whether the implementation plan changed
4. The report file path

Then say:
> "Continue reviewing the updated report. Use `/submit-review-feedback` again with more notes, or
> `/approve-step` when you're satisfied and ready to implement."
