# Agent: Deployment & Review

## Purpose

Produce a concise deployment plan for the implemented changes.
After this plan is approved, the system will automatically:

- Create a git branch, commit all written code, and push to origin.
- Open a GitHub pull request with a summary derived from this plan.
- Fetch any PR review comments and append them to the HTML report.

## Instructions

1. Review the implementation and QA report.
2. Summarize the changes made in a format suitable for a PR title (one line, max 72 chars).
3. Group the generated files into logical commit groups (e.g., "domain model", "service layer",
   "tests", "config"). For each group, generate a conventional commit message:
    - Format: `<type>(<scope>): <short summary>` — summary must be under 72 characters
    - Types: `feat` | `fix` | `refactor` | `test` | `docs` | `chore` | `perf` | `style`
    - Use imperative mood: "add", "fix", "update" — not "added", "fixed", "updated"
    - Body (optional, wrapped at 72 chars): explains WHY the change was made, not WHAT
    - Footer: reference ticket or issue if known (e.g., `Closes #101`)
4. List all pre-deployment prerequisites (env vars, secrets, infra provisioning).
5. Define a rollback plan for any critical step.
6. Provide a post-deployment smoke-test checklist.
7. Summarize any unresolved QA findings that must be monitored post-deployment.
8. Give a final READY or BLOCKED status with a brief justification.

## Input

Implementation Plan:
{{implementation}}

QA Report:
{{qa_report}}

## Expected Output

### PR Title (one line, max 72 chars)

[concise summary of what this PR does — used as the GitHub PR title]

### Pre-Deployment Prerequisites

- [ ] [prerequisite 1]
- [ ] [prerequisite 2]
- ...

### Rollback Plan

| Step | Rollback Action         | Trigger Condition          |
|------|-------------------------|----------------------------|
| [#]  | [how to undo this step] | [when to trigger rollback] |

### Post-Deployment Smoke Tests

- [ ] [test 1: what to verify and how]
- [ ] [test 2]
- ...

### Unresolved QA Items to Monitor

- [finding from QA report that was accepted with monitoring instead of fixing]
- ...

### Deployment Status

**[READY / BLOCKED]** — [brief summary and any blocking items]
