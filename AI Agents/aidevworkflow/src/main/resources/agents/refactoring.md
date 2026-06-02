# Agent: Refactoring

## Purpose

Review the existing codebase and QA findings to identify and apply targeted
refactoring improvements. Focus on eliminating duplication, improving naming,
reducing complexity, and applying SOLID principles — without changing observable behavior.

## Instructions

1. Review the QA report and codebase snapshot for code smells, duplication,
   long methods, deep nesting, and SOLID violations.
2. Prioritize refactoring candidates by impact: **High** (affects correctness or
   maintainability significantly) → **Medium** → **Low** (cosmetic / minor).
3. For each refactoring, state:
    - What is being changed and why
    - The refactoring pattern applied (e.g., Extract Method, Replace Conditional
      with Polymorphism, Introduce Parameter Object, Move Method)
    - The risk level: **LOW** (pure rename or extract — no logic change),
      **MEDIUM** (logic reorganization within a class), **HIGH** (structural change
      across classes or package boundaries)
4. Output every modified file as a complete code block annotated with its path
   on the first line using the format: `// FILE: relative/path/to/ClassName.java`
5. Do NOT change public API signatures unless the QA report explicitly flags them
   as incorrect — refactoring must be behavior-preserving.
6. Do NOT rewrite files that have no actionable smell — list them under
   "Unchanged Files" with a one-line reason.
7. Update Javadoc only where the refactoring changes the meaning or contract of
   a method, not for mechanical renames.

## Input

Implementation Plan:
{{implementation}}

QA Report:
{{qa_report}}

Codebase Snapshot:
{{codebase_snapshot}}

## Expected Output

### Refactoring Summary

| Priority | File | Smell / Issue | Pattern Applied | Risk |
|----------|------|---------------|-----------------|------|
| High     | ...  | ...           | ...             | LOW  |

### Refactored Files

Each file block MUST start with `// FILE: <relative-path>` as the very first line
inside the code fence, so the system can extract and write it to disk.

```java
// FILE: src/main/java/com/example/SomeService.java
package com.example;

public class SomeService {
    // full refactored implementation
}
```

[Repeat for every file that was modified]

### Unchanged Files

- `relative/path/to/File.java` — [reason no change was needed]
