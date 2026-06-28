# Skill: generate_md_report

Generate a Markdown analysis report.

## Required Inputs

- `ticket`: Structured ticket data (id, title, requirements, acceptance_criteria)
- `analysis`: Codebase analysis output (affected_files, risks, edge_cases, patterns_found)
- `implementation_plan`: Array of step objects
- `open_questions`: Array of unanswered questions
- `review_iteration`: Integer (0 = initial, 1+ = updated)
- `output_path`: Where to write the file
- `test_strategy`: Object with `detected_convention`, `test_framework`, `patterns_summary`,
  `steps_needing_tests[]` (each: step, test_type, test_file, test_guidance)

## Markdown Structure

```markdown
# Analysis: {ticket.id} — {ticket.title}

**Generated**: {date}
**Review Iteration**: {review_iteration}
**Implementation Steps**: {count}
**Affected Files**: {count}

---

## Ticket Summary

{ticket.description}

### Requirements

- {requirement 1}
- {requirement 2}

### Acceptance Criteria

- [ ] {criterion 1}
- [ ] {criterion 2}

---

## Codebase Analysis

| File              | Role        | Impact      |
| ----------------- | ----------- | ----------- |
| `path/to/file.ts` | description | description |

### Relevant Patterns Found

- {pattern 1}

---

## Risk Assessment

| Risk        | Severity                  | Mitigation |
| ----------- | ------------------------- | ---------- |
| description | 🔴 High / 🟡 Medium / 🟢 Low | mitigation |

### Edge Cases

- {edge case 1}

---

## Implementation Plan

### Step 1: {title}

**Description**: {description}

**Files**:
- `path/to/file.ts`

**Test**: `{test_command}`

**Test Type**: `{test_type}`

**Test Guidance**: {test_guidance — omit this line when test_type is "none" or null}

**Commit**: `{commit_message}`

---

### Step 2: ...

---

## Open Questions

- {question 1}
- {question 2}

---

## Test Strategy

**Framework**: {test_strategy.test_framework or "Unknown"}
**Convention**: {test_strategy.detected_convention or "Not detected"}

### Pattern Summary

{test_strategy.patterns_summary}

### Coverage by Step

| Step   | Test Type   | Test File     | Approach                         |
| ------ | ----------- | ------------- | -------------------------------- |
| {step} | {test_type} | `{test_file}` | {test_guidance — first sentence} |

{if test_strategy.detected_convention is null}
> **Warning**: No existing tests found. Confirm test framework before implementing test steps.
{/if}

---

## Review History

### Iteration 1 (if applicable)
- Addressed: {finding}
- Updated: {what changed}
```

## Writing the File

Write the Markdown **incrementally** — one section at a time — rather than generating the entire file in a single Write
call. This keeps each write focused and prevents content from being dropped under context pressure.

### Step 1 — Write the header block

Write the file with the title, metadata lines (`Generated`, `Review Iteration`, `Implementation Steps`,
`Affected Files`), and the first `---` divider. Use the Write tool.

### Step 2 — Append sections one at a time

Use the Edit tool to append each section in order. After inserting each section, re-read it to confirm content is
present and correct before continuing.

1. **Ticket Summary** — description, Requirements list, Acceptance Criteria checklist
2. **Codebase Analysis** — affected files table + Relevant Patterns list
3. **Risk Assessment** — risk table + Edge Cases list
4. **Implementation Plan** — one `### Step N` block per step; include `**Test Type**` and `**Test Guidance**` lines on
   steps where `test_type != "none"`
5. **Open Questions** — only if `open_questions.length > 0`
6. **Test Strategy** — framework, convention, coverage table; warning block if no tests found
7. **Review History** — only if `review_iteration > 0`

### Step 3 — Final verification

After all sections are appended, read the full file and verify:
- Every acceptance criterion appears in the Ticket Summary
- Every affected file appears in the Codebase Analysis table
- Every implementation step has a `### Step N` block in the Implementation Plan
- Step count in the metadata header matches the actual number of step blocks

If anything is missing, insert it with Edit before returning.

### For updates (review_iteration > 0)

Edit the existing file in place — add a "Review Iteration N" block at the top of the Review History section and
update only the affected sections. Apply the same per-section verify-after-edit discipline.
