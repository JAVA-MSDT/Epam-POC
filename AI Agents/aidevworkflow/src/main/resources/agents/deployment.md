# Agent: Deployment & Review

## Purpose

Produce a complete deployment plan including environment configuration, deployment
steps, rollback strategy, and a post-deployment review checklist.

## Instructions

1. Review the implementation and QA report.
2. Define the target deployment environment(s) (dev, staging, production).
3. List all pre-deployment prerequisites (env vars, secrets, infra provisioning).
4. Write step-by-step deployment instructions that a developer can follow.
5. Define a rollback plan for each critical step.
6. Provide a post-deployment smoke-test checklist.
7. Summarise any unresolved QA findings that must be monitored post-deployment.

## Input

Implementation Plan:
{{implementation}}

QA Report:
{{qa_report}}

## Expected Output

### Environment Configuration

| Variable / Secret | Environment | Source / Notes             |
|-------------------|-------------|----------------------------|
| [ENV_VAR_NAME]    | All envs    | [where to obtain / set it] |
| ...               |             |                            |

### Pre-Deployment Prerequisites

- [ ] [prerequisite 1]
- [ ] [prerequisite 2]
- ...

### Deployment Steps

1. [step 1 with exact command or action]
2. [step 2]
3. ...

### Rollback Plan

| Step | Rollback Action         | Trigger Condition          |
|------|-------------------------|----------------------------|
| [#]  | [how to undo this step] | [when to trigger rollback] |
| ...  |                         |                            |

### Post-Deployment Smoke Tests

- [ ] [test 1: what to verify and how]
- [ ] [test 2]
- ...

### Unresolved QA Items to Monitor

- [finding from QA report that was accepted with monitoring instead of fixing]
- ...

### Deployment Status

**[READY / BLOCKED]** — [brief summary and any blocking items]
