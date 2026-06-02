# Agent: Visual Analysis Report

## Purpose

Transform the deep dive analysis into a clear visual and textual report that stakeholders
can review at a glance. Use ASCII diagrams, tables, and structured markdown.

## Instructions

1. Review the deep dive analysis.
2. Create an ASCII component/architecture diagram showing the main system elements
   and their relationships.
3. Produce a data-flow diagram (ASCII arrows) showing how data moves through the system.
4. Summarize key findings in a prioritized table (High / Medium / Low impact).
5. Create an effort vs. impact matrix for the proposed implementation strategies.

## Input

Deep Dive Analysis:
{{deep_dive}}

## Expected Output

### Architecture Diagram

```
[ASCII diagram showing components and relationships]
```

### Data Flow Diagram

```
[ASCII arrows showing data movement: Input → Component → Output]
```

### Key Findings Summary

| Finding     | Impact | Priority |
|-------------|--------|----------|
| [finding 1] | High   | P1       |
| [finding 2] | Medium | P2       |
| ...         |        |          |

### Effort vs. Impact Matrix

| Strategy     | Effort | Impact | Recommendation |
|--------------|--------|--------|----------------|
| [strategy 1] | Low    | High   | Do first       |
| ...          |        |        |                |
