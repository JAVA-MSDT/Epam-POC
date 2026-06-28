# Skill: analyze_codebase

Analyze a codebase against a set of requirements. Returns affected areas, risks, and edge cases.

## Process

### 1. Map the Project Structure

Use Glob to understand the layout:

```
Glob("**/*.ts")       — TypeScript projects
Glob("**/*.py")       — Python projects
Glob("src/**/*")      — Common source layout
Glob("**/package.json") — Node.js dependencies
```

Identify: language, framework, test runner, entry points, key modules.

### 2. Find Relevant Code

For each requirement keyword, use Grep:

```
Grep("<feature_keyword>", path=codebase_path)
Grep("<entity_name>", type="ts|py|js")
Grep("class <Name>|def <name>|function <name>")
```

Then Read the most relevant files to understand the full implementation context.

### 3. Trace Dependencies

For each affected file:

- Find its importers (who calls it) using Grep for its module path
- Find its dependencies (what it imports) by reading the import block
- Note any shared state, global config, or database models involved

### 4. Assess Risks

For each affected area, classify:

- **Breaking change** (red): Changed function signatures, removed exports, schema changes, API contract changes
- **Medium risk** (yellow): New dependencies, modified shared utilities, changed behavior with existing callers
- **Low risk** (green): New files with no existing callers, isolated feature additions, test additions

### 5. Identify Edge Cases

Consider for each requirement:

- Null/undefined/empty inputs
- Empty collections or zero-length results
- Concurrent access or race conditions
- Large data sets or pagination
- Authentication/authorization boundaries
- Error states and partial failures
- Backwards compatibility with existing data

## Output Format

Return:

```json
{
  "affected_files": [
    { "path": "string", "role": "string", "impact": "string" }
  ],
  "risks": [
    { "description": "string", "severity": "low|medium|high", "mitigation": "string" }
  ],
  "edge_cases": ["string"],
  "patterns_found": ["description of relevant patterns already in the codebase"]
}
```
