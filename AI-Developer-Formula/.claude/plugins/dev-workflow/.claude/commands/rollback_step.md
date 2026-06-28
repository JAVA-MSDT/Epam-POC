You are executing the `/rollback-step` workflow. Follow every step in order.

## Step 1 — Read State

Read the active state file. First read `<codebase_path>/.dev-workflow/active_state.json` to find the current state file
path. If `active_state.json` does not exist, look for any `*_state.json` file in `.dev-workflow/` and use the most
recently modified one.

If no state file is found, stop and tell the user: "No active workflow found. Nothing to rollback."

Extract: `completed_steps`, `current_step`, `state_path`.

**Validate required fields.** If any of these are missing or null, stop and tell the user:
> "The state file is incomplete (missing: `<field list>`). This usually means `/start-ticket-analysis` did not finish
> successfully. Re-run it to create a fresh analysis."

Required: `state_path`, `current_step`, `completed_steps`.

If `completed_steps` is empty, stop and tell the user: "No completed steps to rollback."

## Step 2 — Determine Which Step to Rollback

- If a step number was passed as an argument, find that step in `completed_steps`.
- Otherwise, use the last entry in `completed_steps`.

If the requested step is not found in `completed_steps`, tell the user which steps are available to rollback.

## Step 3 — Find the Commit Hash

First check `completed_steps[N].commit`:

- If it is `"later"` — stop and tell the user:
  > "Step N (`<title>`) was marked **commit-later** — it has no standalone commit to revert.
  > To undo these changes, run:
  > ```bash
  > git checkout -- <files from this step>
  > ```
  > Then update the state manually by removing this step from `completed_steps` and setting `current_step` to N−1."

- If it is `"committed"`, `"pending"`, or missing — find the commit using git log:

```bash
git log --oneline -10
```

Match the commit message from the step's `commit_message` field to find the right hash. Show the matches to the user and
ask them to confirm which commit to revert if there is ambiguity.

## Step 4 — Confirm

Display:

```
Rollback Step N: <title>
Commit to revert: <commit hash> — "<commit message>"
Warning: This creates a revert commit. The original commit stays in git history.
```

Ask: "Confirm rollback of step N? (yes / no)"

Wait for explicit confirmation. Do not proceed without it.

## Step 5 — Provide the Revert Command

Show the user the exact command to run:

```bash
git revert <commit_hash> --no-edit
```

Tell the user:
> "Run the command above to revert step N: **<title>**.
>
> **If the revert succeeds:** come back here and confirm so I can update the workflow state.
>
> **If there are merge conflicts:**
> 1. Resolve the conflicts in your editor
> 2. Run `git add <conflicted files>`
> 3. Run `git revert --continue`
> 4. Then come back here and confirm.
>
> **Where you are:** You were on step N of <total> — steps <list of still-completed steps> remain committed. After this
> revert, step N will be undone and you can re-implement it or revise the plan."

Do not run the revert automatically — let the developer execute it.

## Step 6 — Update State After User Confirms Revert is Done

Ask: "Has the revert been completed?"

Once the user confirms:

## Step 7 — Update State

Update the state file at `state_path`:

- Remove the rolled-back step from `completed_steps`
- Set `current_step` to the step number before the rolled-back step (or 0 if it was step 1)
- Add a `rollbacks` array (or append to it) with:
  ```json
  { "step": N, "original_commit": "<hash>", "revert_commit": "<revert_hash>" }
  ```

## Step 8 — Present to User

Show a full summary:

```
✓ Step N reverted: <title>
  Revert commit: <revert_hash>

  Workflow position:
    Completed steps: <list remaining completed steps, or "none">
    Current step:    <new current_step>
    Remaining steps: <count> steps left in the plan

  What to do next:
  • Run /approve-step          — re-implement step N (same plan, fresh attempt)
  • Run /submit-review-feedback — revise the plan before re-implementing
  • Run /rollback-step          — roll back another step if needed
```
