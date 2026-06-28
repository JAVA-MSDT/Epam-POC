# Model Configuration

Single reference for model assignments across all agents.
**When a new model ships, update this file AND the frontmatter in the corresponding agent file.**

| Tier   | Current Model                    | Used by                                                           |
| ------ | -------------------------------- | ----------------------------------------------------------------- |
| light  | `claude-haiku-4-5-20251001`      | `ticket_analysis_agent` — fetch + parse only                      |
| heavy  | `claude-sonnet-4-6`              | `report_generator_agent` — codebase analysis + plan generation    |
| heavy  | `claude-sonnet-4-6`              | `reanalysis_agent` — snapshot patching + targeted re-analysis     |
| heavy  | `claude-sonnet-4-6`              | `implementation_agent` — code edits + test execution              |

## Why these tiers?

- **light (Haiku):** ticket_analysis_agent does structured extraction — read input, identify ticket type,
  parse acceptance criteria, return JSON. No deep reasoning needed. Haiku handles this well at lower cost.

- **heavy (Sonnet):** report_generator_agent, reanalysis_agent, and implementation_agent all require
  multi-step reasoning, codebase pattern recognition, risk assessment, or reliable code generation.
  Sonnet provides the quality needed here.

## How to update models

1. Edit the table above with the new model ID.
2. Open the agent file listed in the "Used by" column.
3. Update the `model:` field in the YAML frontmatter at the top of that file.
4. Both must match — this file is the reference, the frontmatter is what Claude Code actually uses.
