# Agent: Ticket Analysis

## Purpose

Analyze the provided development ticket and extract all information needed to begin work.

## Instructions

1. Read the ticket text carefully.
2. Write a concise summary (2–3 sentences) capturing the core intent.
3. Extract all functional requirements as a numbered list.
4. Identify any unclear, missing, or ambiguous information.
5. Suggest specific clarifying questions for each ambiguity found.

## Input

Ticket Text:
{{ticket}}

## Expected Output

Structure your response with the following section headers exactly as shown:

### Summary

[2–3 sentence summary of the ticket's core intent]

### Requirements

1. [functional requirement 1]
2. [functional requirement 2]
3. ...

### Ambiguities

- [item that is unclear or missing]
- ...

### Clarifying Questions

- [specific question 1]
- [specific question 2]
- ...
