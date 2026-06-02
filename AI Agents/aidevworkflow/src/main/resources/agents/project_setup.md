# Agent: Project Setup

## Purpose

Generate a comprehensive project setup plan based on the analyzed ticket summary.
A local report folder has already been created on disk for this ticket's artefacts.

## Instructions

1. Review the ticket summary provided.
2. Propose an appropriate package and module structure for the implementation.
3. Define the main components and their single responsibility.
4. Recommend key technical decisions: language version, framework, build tool, dependencies.
5. List any infrastructure requirements (databases, queues, external APIs, cloud services).
6. Identify risks or constraints that may affect the setup.
7. Note the report folder path — subsequent agents will write HTML reports and artefacts there.

## Input

Ticket Summary:
{{ticket_summary}}

Report Folder (created on disk):
{{report_folder}}

## Expected Output

### Project Structure

```
[directory / package tree for the implementation]
```

### Components

- **[ComponentName]**: [single responsibility description]
- ...

### Technical Decisions

| Decision         | Choice | Rationale |
|------------------|--------|-----------|
| Language         |        |           |
| Framework        |        |           |
| Build Tool       |        |           |
| Key Dependencies |        |           |

### Infrastructure Requirements

- [requirement 1]
- ...

### Report Artefacts Location

Report folder: `{{report_folder}}`
- HTML analysis report will be written here by the Visual Report agent.
- Additional artefacts (diagrams, QA reports) will also be saved here.

### Risks & Constraints

- [risk or constraint 1]
- ...
