# Agent: AI-Powered Deep Dive

## Purpose

Perform a thorough technical analysis by comparing the ticket requirements against the
existing codebase. Surface architectural gaps, edge cases, and implementation strategies are 
grounded in what is already in the project.

## Instructions

1. Review the ticket summary and project setup plan.
2. If codebase context is provided, scan the existing source files to identify:
    - Code that already addresses part of the ticket requirements.
    - Code that conflicts with or must be modified to satisfy the requirements.
    - Missing components that need to be created from scratch.
3. Analyze core technical challenges and non-functional requirements (performance,
   security, scalability, maintainability).
4. Identify the main architectural patterns suitable for this change.
5. Surface potential edge cases and failure modes.
6. Propose concrete implementation strategies for the highest-risk areas.
7. List third-party libraries or services that would speed up development.

## Input

Ticket Summary:
{{ticket_summary}}

Project Setup Plan:
{{project_setup}}

Existing Codebase (scanned source files):
{{codebase_context}}

## Expected Output

### Codebase Gap Analysis

| Requirement     | Already Exists?    | File / Class     | Action Needed          |
|-----------------|--------------------|------------------|------------------------|
| [requirement 1] | Yes / No / Partial | [file if exists] | Create / Modify / None |
| ...             |                    |                  |                        |

### Technical Challenges

- [challenge 1 with brief analysis]
- ...

### Non-Functional Requirements

| NFR             | Target / Expectation |
|-----------------|----------------------|
| Performance     |                      |
| Security        |                      |
| Scalability     |                      |
| Maintainability |                      |

### Recommended Architecture

[Describe the primary architectural pattern(s) and why they fit this project]

### Edge Cases & Failure Modes

- [edge case / failure mode 1]
- ...

### Implementation Strategies

1. [strategy for highest-risk area]
2. ...

### Recommended Libraries / Services

- **[Library/Service]**: [purpose and benefit]
- ...
