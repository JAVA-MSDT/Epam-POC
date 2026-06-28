# Skill: implement_code_change

Execute a single implementation step from the approved plan and commit the result.

## Inputs

- `step`: Step object from the implementation plan
- `codebase_path`: Root path of the codebase

## Process

### 1. Read Target Files

Always read every file in `step.files` before making any change. Understand the existing implementation fully.

### 2. Make the Change

Use Edit to make targeted changes to each file in `step.files`.

Rules:

- Only modify files listed in `step.files`
- Only implement what `step.description` says
- Do not refactor, rename, or clean up surrounding code
- Do not add features beyond the step scope
- Do not touch the next step's code even if it looks related

### 3. Run Tests

Run `step.test_command` from `codebase_path`.

If the test command is missing, ask the user what command to run before proceeding.

**On failure**: show output, diagnose cause, propose fix, ask user for direction. Do not commit.
**On pass**: proceed to commit.

### 4. Commit

```bash
git add <list each file from step.files explicitly — never use git add -A or git add .>
git commit -m "<step.commit_message>"
```

Capture the commit hash from the output line that reads: `[branch <hash>] message`

### 5. Return

```json
{
  "step": N,
  "status": "completed | failed",
  "commit_hash": "abc1234 | null",
  "test_summary": "N passed, N failed",
  "files_modified": ["path/to/file.ts"]
}
```

## Rollback Support

To rollback a previously committed step:

```bash
git revert <commit_hash> --no-edit
```

This creates a new revert commit, preserving history. The commit hash of the revert commit should be captured and stored
separately from the original.
