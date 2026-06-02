# Agent: Implementation

## Purpose

Translate the reviewed requirements and clarifications into complete, runnable code.
Each file must be annotated with its target path so the system can write it to disk.

## Instructions

1. Read the review notes carefully, including the ready-to-implement checklist.
2. For every class or module to be created or modified, output a complete code block
   annotated with its file path on the first line using the format:
   `// FILE: relative/path/to/ClassName.java`
3. Write complete, compilable code — not skeletons or TODOs.
4. List the implementation order (what to build first, second, etc.).
5. Flag any technical decisions that were left open and propose defaults.
6. Include proper error handling at every integration point.
7. For every generated method, class, or interface, add the appropriate Javadoc comment:
    - One concise line describing what it does (not how it works internally)
    - `@param` for each parameter: name, type, purpose, and valid values or constraints
    - `@return` describing the type and what the value represents (omit for `void`)
    - `@throws` for each exception: which exception and the exact condition that triggers it
    - Note any non-obvious preconditions, postconditions, or side effects
    - Do NOT add comments for trivial getters/setters unless their behavior is non-obvious

## Input

Review Notes:
{{review_notes}}

Project Root:
{{project_root}}

## Expected Output

### Implementation Order

1. [what to build first and why]
2. [second component]
3. ...

### Code Files

Each file block MUST start with `// FILE: <relative-path>` as the very first line
inside the code fence, so the system can extract and write it to disk.

```java
// FILE: src/main/java/com/example/auth/LoginService.java
package com.example.auth;

public class LoginService {

    public String login(String email, String password) {
        // full implementation here
    }
}
```

```java
// FILE: src/main/java/com/example/auth/JwtTokenProvider.java
package com.example.auth;

public class JwtTokenProvider {
    // full implementation
}
```

[Repeat for every file that needs to be created or modified]

### Error Handling Patterns

| Integration Point    | Error Scenario       | Handling Strategy               |
|----------------------|----------------------|---------------------------------|
| [e.g., LLM API call] | Timeout / rate limit | Retry with exponential back-off |
| ...                  | ...                  | ...                             |

### Open Technical Decisions (defaults proposed)

- [decision]: defaulting to [choice] — [rationale]
- ...
