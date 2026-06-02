# Agent: Visual Analysis Report

## Purpose

Transform the deep dive analysis into a clear HTML report that developers and stakeholders
can open in a browser. The output must be valid HTML body content (no doctype or html/head
tags — the surrounding page shell is added automatically).

## Instructions

1. Review the deep dive analysis, including the codebase gap analysis.
2. Produce a structured HTML body with the following sections:
   - **Summary** — 2–3 sentence overview of the analysis findings.
   - **Codebase Gap Analysis** — table showing each requirement, whether it exists, and what action is needed.
   - **Architecture Diagram** — an ASCII diagram inside a `<pre>` block showing components and their relationships.
   - **Data Flow** — an ASCII diagram inside a `<pre>` block showing how data flows through the system.
   - **Key Findings** — a prioritised table (High / Medium / Low impact) of the most important findings.
   - **Effort vs. Impact Matrix** — table of proposed strategies with effort, impact, and recommendation.
   - **Risks** — bullet list of identified risks and mitigations.
3. Use semantic HTML: `<h2>`, `<h3>`, `<table>`, `<ul>`, `<pre>`, `<p>`, `<code>`.
4. For priority/severity badges use: `<span class="badge high">High</span>`,
   `<span class="badge medium">Medium</span>`, `<span class="badge low">Low</span>`.
5. Wrap each major section in `<div class="section">`.
6. Do NOT include `<!DOCTYPE>`, `<html>`, `<head>`, `<body>`, or `<style>` tags.

## Input

Deep Dive Analysis:
{{deep_dive}}

## Expected Output

Valid HTML body content only, starting with a `<div class="section">` block.

Example structure:
```html
<div class="section">
  <h2>Summary</h2>
  <p>...</p>
</div>

<div class="section">
  <h2>Codebase Gap Analysis</h2>
  <table>
    <thead><tr><th>Requirement</th><th>Exists?</th><th>File</th><th>Action</th></tr></thead>
    <tbody>
      <tr><td>...</td><td>No</td><td>—</td><td>Create</td></tr>
    </tbody>
  </table>
</div>

<div class="section">
  <h2>Architecture Diagram</h2>
  <pre>...</pre>
</div>
```
