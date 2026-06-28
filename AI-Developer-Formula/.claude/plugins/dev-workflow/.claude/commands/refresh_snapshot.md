You are executing the `/refresh-snapshot` workflow. Follow every step in order.

## Step 1 — Read State

Read the active state file. First read `<codebase_path>/.dev-workflow/active_state.json` to find the current state file
path. If `active_state.json` does not exist, look for any `*_state.json` file in `.dev-workflow/` and use the most
recently modified one.

If no state file is found, stop and tell the user: "No active workflow found. Nothing to refresh — run
`/start-ticket-analysis` to begin."

Read the state file and extract: `codebase_path`, `scope`, `file_prefix`, `state_path`.

**Validate required fields.** If any of these are missing or null, stop and tell the user:
> "The state file is incomplete (missing: `<field list>`). This usually means `/start-ticket-analysis` did not finish
> successfully. Re-run it to create a fresh analysis."

Required: `codebase_path`, `state_path`, `file_prefix`.

## Step 2 — Confirm Refresh

Tell the user:

```
Snapshot refresh requested.

  Active ticket:  <file_prefix>
  Codebase:       <codebase_path>
  Scope:          <scope if set, or "full codebase">

This will re-explore the codebase and rewrite codebase_context.md.
The state file, report, and implementation plan will NOT be changed.
```

Ask: "Proceed with snapshot refresh? (yes / no)"

Wait for explicit confirmation. Do not proceed without it.

## Step 3 — Re-explore the Codebase

Determine the exploration root:

- If `scope` is set, explore only `<codebase_path>/<scope_dir>` for each directory listed.
- If `scope` is null, explore from `codebase_path` (full codebase).

Tell the user: **"Re-exploring codebase — reading directly from codebase (not snapshot)."**

Using Read, Glob, and Grep on the exploration root:

1. Map the top-level project structure — language, framework, key entry points.
2. Re-read the files listed in the existing `codebase_context.md` relevant file map to check for changes — use Glob and
   Grep to find any new files related to the active ticket scope.
3. Note any files that have been added, removed, or significantly changed since the previous snapshot (you can compare
   against what the old snapshot listed).

Hold all findings in memory.

## Step 4 — Rewrite the Snapshot

Delete the existing `<codebase_path>/.dev-workflow/codebase_context.md`.

Write a fresh snapshot using findings from Step 3:

```markdown
# Codebase Context
Generated: <date> | Ticket: <file_prefix> | Refreshed: yes

## Tech Stack
<detected language, framework, test runner, key dependencies — one line each>

## Relevant File Map
| File | Purpose | Key symbols |
|------|---------|-------------|
| path/to/file | what it does | exported functions / classes |

## Key Patterns
<architecture patterns, naming conventions, auth approach, DB layer, error handling — bullet points>

## Entry Points
<files that bootstrap the app, register routes, or are the main execution entry>
```

## Step 5 — Present to User

Report what changed between the old and new snapshot:

```
Snapshot refreshed.

  Files added since last snapshot:    <list, or "none detected">
  Files removed since last snapshot:  <list, or "none detected">
  Notable changes:                    <any key structural changes, or "none detected">

The implementation plan has NOT been updated. If new files affect your
remaining steps, run /submit-review-feedback to update the plan before
continuing with /approve-step.
```

Then say: "Snapshot is up to date. Continue with `/approve-step` to implement the next step, or run
`/submit-review-feedback` if the pulled changes affect your implementation plan."
