# Skill: generate_html_report

Generate a complete, self-contained HTML analysis report with inline CSS.

## Required Inputs

- `ticket`: Structured ticket data (id, title, requirements, acceptance_criteria)
- `analysis`: Codebase analysis output (affected_files, risks, edge_cases, patterns_found)
- `implementation_plan`: Array of step objects
- `open_questions`: Array of unanswered questions
- `review_iteration`: Integer (0 = initial, 1+ = updated)
- `output_path`: Where to write the file
- `test_strategy`: Object with `detected_convention`, `test_framework`, `patterns_summary`,
  `steps_needing_tests[]` (each: step, test_type, test_file, test_guidance)

## HTML Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Analysis: {ticket.id} — {ticket.title}</title>
  <style>
    /* Inline all CSS here — no external resources */
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
           margin: 0; background: #f5f5f5; color: #333; }
    header { background: #1a1a2e; color: white; padding: 24px 32px; }
    header h1 { margin: 0; font-size: 1.4rem; }
    header .meta { opacity: 0.7; font-size: 0.85rem; margin-top: 4px; }
    main { max-width: 1100px; margin: 0 auto; padding: 32px; }
    section { background: white; border-radius: 8px; margin-bottom: 24px;
              padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    h2 { margin-top: 0; color: #1a1a2e; border-bottom: 2px solid #eee;
         padding-bottom: 8px; }
    code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px;
           font-family: 'Courier New', monospace; font-size: 0.85rem; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #f8f8f8; text-align: left; padding: 10px;
         border-bottom: 2px solid #ddd; }
    td { padding: 10px; border-bottom: 1px solid #eee; vertical-align: top; }
    .risk-low { border-left: 4px solid #28a745; padding-left: 12px; }
    .risk-medium { border-left: 4px solid #ffc107; padding-left: 12px; }
    .risk-high { border-left: 4px solid #dc3545; padding-left: 12px; }
    .step-card { border: 1px solid #e0e0e0; border-radius: 6px;
                 padding: 16px; margin-bottom: 16px; }
    .step-badge { display: inline-block; background: #1a1a2e; color: white;
                  border-radius: 50%; width: 28px; height: 28px;
                  text-align: center; line-height: 28px; font-weight: bold;
                  margin-right: 10px; font-size: 0.85rem; }
    .badge-updated { background: #ffc107; color: #333; padding: 2px 8px;
                     border-radius: 10px; font-size: 0.75rem; margin-left: 8px; }
    .test-badge { padding: 2px 8px; border-radius: 10px; font-size: 0.75rem; font-weight: bold; }
    .test-badge-unit { background: #0d6efd; color: white; }
    .test-badge-integration { background: #6610f2; color: white; }
    .test-badge-update { background: #fd7e14; color: white; }
    .test-guidance { background: #f0f4ff; border-left: 3px solid #0d6efd;
                     padding: 8px 12px; margin-top: 8px; font-size: 0.875rem; }
  </style>
</head>
<body>
  <header>
    <h1>{ticket.id}: {ticket.title}</h1>
    <div class="meta">Generated: {date} | Review iteration: {review_iteration}
         | {step_count} implementation steps | {affected_files_count} affected files</div>
  </header>
  <main>

    <!-- Section 1: Ticket Summary -->
    <section id="summary">
      <h2>Ticket Summary</h2>
      <p>{ticket.description}</p>
      <h3>Requirements</h3>
      <ul>{ticket.requirements as <li> items}</ul>
      <h3>Acceptance Criteria</h3>
      <ul>{ticket.acceptance_criteria as <li> items}</ul>
    </section>

    <!-- Section 2: Codebase Analysis -->
    <section id="codebase">
      <h2>Codebase Analysis</h2>
      <table>
        <tr><th>File</th><th>Role</th><th>Impact</th></tr>
        {analysis.affected_files as <tr><td><code>path</code></td><td>role</td><td>impact</td></tr>}
      </table>
      {if analysis.patterns_found}
      <h3>Relevant Patterns Found</h3>
      <ul>{patterns as <li> items}</ul>
      {/if}
    </section>

    <!-- Section 3: Risk Assessment -->
    <section id="risks">
      <h2>Risk Assessment</h2>
      {analysis.risks as divs with class risk-{severity}}
      {analysis.edge_cases as a list under "Edge Cases" heading}
    </section>

    <!-- Section 4: Implementation Plan -->
    <section id="plan">
      <h2>Implementation Plan</h2>
      {implementation_plan as step-cards with step-badge}
      Each card shows: title, description, files (as <code>), test_command, commit_message.
      If step.test_type is not null and not "none": render a
      <span class="test-badge test-badge-{test_type}">{test_type}</span> next to the step title, and a
      <div class="test-guidance">{step.test_guidance}</div> below the description.
    </section>

    <!-- Section 5: Test Strategy -->
    {if test_strategy and (test_strategy.detected_convention or test_strategy.steps_needing_tests.length > 0)}
    <section id="test-strategy">
      <h2>Test Strategy</h2>
      <p><strong>Framework:</strong> {test_strategy.test_framework or "Unknown"}</p>
      <p><strong>File naming convention:</strong> {test_strategy.detected_convention or "Not detected"}</p>
      <h3>Pattern Summary</h3>
      <p>{test_strategy.patterns_summary}</p>
      {if test_strategy.steps_needing_tests.length > 0}
      <h3>Coverage by Step</h3>
      <table>
        <tr><th>Step</th><th>Type</th><th>Test File</th><th>Approach</th></tr>
        {steps_needing_tests as <tr> rows — step number, badge span, <code>test_file</code>, test_guidance text}
      </table>
      {/if}
      {if test_strategy.detected_convention is null}
      <p class="risk-medium">No existing tests found. Confirm test framework before implementing test steps.</p>
      {/if}
    </section>
    {/if}

    <!-- Section 6: Open Questions -->
    {if open_questions.length > 0}
    <section id="questions">
      <h2>Open Questions</h2>
      <ul>{open_questions as <li> items}</ul>
    </section>
    {/if}

    <!-- Review Iterations (if iteration > 0) -->
    {if review_iteration > 0}
    <section id="iterations">
      <h2>Review History</h2>
      {list of what was addressed in each iteration}
    </section>
    {/if}

  </main>
</body>
</html>
```

## Writing the File

Write the HTML **incrementally** — one section at a time — rather than generating the entire file in a single Write
call. This keeps each write focused, prevents content from being dropped due to context pressure, and makes it easy to
verify completeness before moving on.

### Step 1 — Write the skeleton

Write the file with `<!DOCTYPE html>`, `<head>` (title + full `<style>` block), `<body>`, `<header>`, and an empty
`<main></main>`. Use the Write tool. The file now exists with correct structure and all CSS.

### Step 2 — Append sections one at a time

Use the Edit tool to insert each section into `<main>` in order. After inserting each section, re-read the relevant
portion of the file to confirm the content is present and correct before continuing.

1. **Ticket Summary** (`<section id="summary">`) — requirements and acceptance criteria
2. **Codebase Analysis** (`<section id="codebase">`) — affected files table + patterns
3. **Risk Assessment** (`<section id="risks">`) — risk rows and edge cases
4. **Implementation Plan** (`<section id="plan">`) — one step-card per implementation step; include test-type badge
   and test-guidance block on each step where `test_type != "none"`
5. **Test Strategy** (`<section id="test-strategy">`) — only if `test_strategy` is present; framework, convention,
   coverage table
6. **Open Questions** (`<section id="questions">`) — only if `open_questions.length > 0`
7. **Review History** (`<section id="iterations">`) — only if `review_iteration > 0`

### Step 3 — Final verification

After all sections are inserted, read the full file and verify:
- Every acceptance criterion appears in the Ticket Summary
- Every affected file appears in the Codebase Analysis table
- Every implementation step has a card in the Plan section
- Step count in the `<header>` meta line matches the actual number of step-cards

If anything is missing, insert the missing content with Edit before returning.
