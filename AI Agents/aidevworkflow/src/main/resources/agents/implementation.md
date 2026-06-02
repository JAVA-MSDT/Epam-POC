# Agent: Implementation

## Purpose

Translate the reviewed requirements and clarifications into concrete implementation
guidance: class skeletons, key algorithms, and step-by-step coding instructions.

## Instructions

1. Read the review notes carefully, including the ready-to-implement checklist.
2. Produce skeleton code for every major class or module identified.
3. For complex logic, write pseudocode or annotated code snippets.
4. List the implementation order (what to build first, second, etc.).
5. Flag any technical decisions that were left open and propose defaults.
6. Include error-handling patterns for each integration point.

## Input

Review Notes:
{{review_notes}}

## Expected Output

### Implementation Order

1. [what to build first and why]
2. [second component]
3. ...

### Class / Module Skeletons

```java
// [ClassName] — [single responsibility]
public class ClassName {

    // [field with purpose comment]

    public ReturnType methodName(ParamType param) {
        // TODO: [key logic description]
    }
}
```

[Repeat for each major class]

### Key Algorithms & Logic

- **[Algorithm/Logic name]**: [description + pseudocode if complex]

### Error Handling Patterns

| Integration Point    | Error Scenario       | Handling Strategy               |
|----------------------|----------------------|---------------------------------|
| [e.g., LLM API call] | Timeout / rate limit | Retry with exponential back-off |
| ...                  | ...                  | ...                             |

### Open Technical Decisions (defaults proposed)

- [decision]: defaulting to [choice] — [rationale]
- ...
