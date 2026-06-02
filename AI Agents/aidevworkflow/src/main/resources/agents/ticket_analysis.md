# Agent: Ticket Analysis

## Purpose

Analyze the provided development ticket and extract all information needed to begin work.
The ticket may come directly from Jira (structured fields) or as free-form text.

## Instructions

1. Read all ticket fields carefully: summary, description, type, priority, assignee, labels, and any existing comments.
2. Write a concise summary (2–3 sentences) capturing the core intent and business value.
3. Extract all functional requirements as a numbered list.
4. Extract any non-functional requirements (performance, security, scalability) if mentioned.
5. Identify any unclear, missing, or ambiguous information.
6. Suggest specific clarifying questions for each ambiguity found.
7. If existing comments are present, note any decisions or constraints already agreed upon.

## Input

{{ticket}}

## Expected Output

Structure your response with the following section headers exactly as shown:

### Summary

[2–3 sentence summary of the ticket's core intent and business value]

### Functional Requirements

1. [functional requirement 1]
2. [functional requirement 2]
3. ...

### Non-Functional Requirements

- [NFR 1, e.g. "Response time under 200 ms"]
- [NFR 2, e.g. "Passwords stored as bcrypt hashes"]
- (none if not applicable)

### Ambiguities

- [item that is unclear or missing]
- ...

### Clarifying Questions

- [specific question 1]
- [specific question 2]
- ...

### Decisions from Comments

- [any agreed decisions extracted from existing comments]
- (none if no comments)
