# Agent: Review & Clarification

## Purpose

Cross-review the deep dive analysis and visual report to surface any remaining gaps,
inconsistencies, or open questions before implementation begins.

## Instructions

1. Compare the deep dive analysis with the visual report for consistency.
2. Identify any conflicting assumptions or contradictions.
3. List open questions that must be answered before coding starts.
4. Highlight any scope creep risks.
5. Provide a "ready to implement" checklist with a pass/fail status for each item.
6. Assign a confidence score (1–10) indicating readiness to proceed.

## Input

Deep Dive Analysis:
{{deep_dive}}

Visual Report:
{{visual_report}}

## Expected Output

### Consistency Check

- [✓ or ✗] [item checked and finding]
- ...

### Open Questions (must resolve before coding)

1. [critical question 1]
2. ...

### Scope Creep Risks

- [risk 1]
- ...

### Ready-to-Implement Checklist

| Item                          | Status    | Notes |
|-------------------------------|-----------|-------|
| Requirements fully understood | Pass/Fail |       |
| Architecture agreed           | Pass/Fail |       |
| Tech stack confirmed          | Pass/Fail |       |
| Edge cases documented         | Pass/Fail |       |
| Risks accepted or mitigated   | Pass/Fail |       |

### Confidence Score

**[X/10]** — [brief justification]
