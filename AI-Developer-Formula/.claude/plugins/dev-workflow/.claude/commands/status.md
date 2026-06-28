You are executing the `/status` command. Display the current workflow state. This command is strictly read-only — do not
write or edit any files.

## Step 1 — Find the Active State File

Read `active_state.json` using the same discovery logic as all other commands:

1. Ask the user: "What is your `codebase_path`?" if it is not already known from context.
2. Read `<codebase_path>/.dev-workflow/active_state.json` to get `state_path`.
3. If `active_state.json` does not exist, look for any `*_state.json` file in `<codebase_path>/.dev-workflow/` and use
   the most recently modified one.

**Case A — No state file found, or `active_state.json` has `"state_path": null`:**
Stop and display:

```
No active workflow found.
Run /start-ticket-analysis to begin.
```

**Case B — State file exists but is not valid JSON:**
Stop and display:

```
Warning: <state_path> is not valid JSON — the file may be corrupted.
To reset: delete the file and run /start-ticket-analysis.
```

Do not attempt to display partial content.

**Case C — Valid JSON but missing expected fields:**
Continue to Step 2, but for each missing field output `[field_name]: not found` in place of its value.

**Case D — Fully valid state:**
Continue to Step 2.

## Step 2 — Display Status

Print a formatted status summary using the fields below.

```
─────────────────────────────────────────
  Dev Workflow — Current Status
─────────────────────────────────────────
  Phase:            <phase>
  Ticket source:    <ticket_source>
  Ticket ID:        <ticket_id or "n/a">
  Codebase:         <codebase_path>
  Scope:            <scope or "full codebase">
  Report:           <report_path>
  Review cycles:    <review_iterations>

  Implementation Progress:
    <current_step> of <total steps> steps complete

  Completed Steps:
    <for each entry in completed_steps:>
    ✓ Step <step>: <title>  [commit: <commit>]
    <if completed_steps is empty:>
    (none yet)

  Remaining Steps:
    <for each step in implementation_plan where step > current_step:>
    • Step <step>: <title>
    <if all steps complete:>
    (all steps complete)
─────────────────────────────────────────
```

Where `<total steps>` is the length of the `implementation_plan` array.

## Step 3 — Done

Do not modify any files. Do not update state. The command is complete.
