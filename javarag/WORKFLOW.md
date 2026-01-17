# Java RAG Code Review System - Workflow Guide

## Overview
This document explains the complete workflow of the Java RAG (Retrieval-Augmented Generation) Code Review System, from input to output.

## System Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Java File     │    │  Configuration   │    │ Knowledge Base  │
│   (Input)       │    │     Files        │    │   (JSON Files)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MAIN ORCHESTRATOR                            │
│                   (com.epam.Main)                               │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                 STEP 1: KNOWLEDGE INDEXING                      │
│              (KnowledgeBaseIndexer)                             │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                 STEP 2: STATIC ANALYSIS                         │
│           (CheckstyleAnalyzer + PMDAnalyzer)                    │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                 STEP 3: RAG PIPELINE                            │
│        (KnowledgeBaseSearcher + FeedbackGenerator)              │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    FEEDBACK OUTPUT                              │
└─────────────────────────────────────────────────────────────────┘
```

## Detailed Workflow

### Phase 1: Input Validation and Setup

**1.1 User Input Processing**
- User provides Java file to analyze
- System validates file existence and accessibility
- Configuration files (Checkstyle XML, PMD ruleset) are validated
- Knowledge base directory is verified

**1.2 Initialization**
```java
// Entry point
java com.epam.Main [JavaFile] [CheckstyleConfig] [PMDRuleset] [KnowledgeBaseDir] [IndexDir]

// Or run with test samples
java com.epam.Main
```

### Phase 2: Knowledge Base Preparation (RAG - Retrieval Setup)

**2.1 Knowledge Base Indexing**
```
Input: JSON files in knowledgebase/ directory
├── vector_vs_arraylist.json
├── enumeration_vs_iterator.json
└── synchronized_vs_concurrent.json

Process:
1. KnowledgeBaseIndexer reads each JSON file
2. Parses KnowledgeEntry objects (title, type, description, example, reference, tags)
3. Creates Lucene documents with searchable fields
4. Builds inverted index for fast text search
5. Stores index in specified directory

Output: Lucene index ready for search queries
```

**2.2 Knowledge Entry Structure**
```json
{
  "title": "Rule or Best Practice Title",
  "type": "Best Practice|Anti-pattern|Enhancement", 
  "description": "Detailed explanation",
  "example": "Code example with solution",
  "reference": "Source documentation",
  "tags": ["keyword1", "keyword2", "keyword3"]
}
```

### Phase 3: Static Code Analysis

**3.1 Checkstyle Analysis**
```
Input: Java source file + checkstyle.xml configuration

Process:
1. CheckstyleAnalyzer loads configuration rules
2. Creates Checker instance with TreeWalker modules
3. Processes Java file against configured rules
4. Collects violations via AuditListener
5. Converts violations to AnalysisFinding objects

Output: List<AnalysisFinding> with Checkstyle violations
```

**3.2 PMD Analysis**
```
Input: Java source file + pmd-ruleset.xml configuration

Process:
1. PMDAnalyzer creates PMDConfiguration
2. Loads ruleset and sets up PmdAnalysis
3. Performs static analysis on Java file
4. Extracts rule violations from Report
5. Converts violations to AnalysisFinding objects

Output: List<AnalysisFinding> with PMD violations
```

**3.3 Finding Consolidation**
```
Combined Results:
- Checkstyle findings (naming, formatting, structure)
- PMD findings (best practices, performance, bugs)
- Total findings count reported to user
```

### Phase 4: RAG Pipeline Execution

**4.1 Retrieval Phase**
```
For each AnalysisFinding:
1. Extract issue description/rule name
2. KnowledgeBaseSearcher queries Lucene index
3. Search across title, description, and tags fields
4. Return top matching KnowledgeEntry (if any)
5. Relevance scoring determines best match
```

**4.2 Generation Phase**
```
FeedbackGenerator combines:
- Static analysis finding (issue + location)
- Retrieved knowledge entry (guidance + examples)
- Formatted output with actionable advice

Two output types:
1. Rich feedback (finding + knowledge match)
2. Basic feedback (finding only, no knowledge match)
```

### Phase 5: Output Generation

**5.1 Feedback Format**
```
=== CODE REVIEW FEEDBACK ===
Issue Detected: [Rule/Issue Name]
Location: [File:Line - Description]

Knowledge Base Guidance:
Title: [Best Practice Title]
Type: [Best Practice|Anti-pattern|Enhancement]
Description: [Detailed explanation]

Example/Suggestion: [Code example with solution]

Reference: [Documentation source]
=============================
```

**5.2 Console Output Flow**
```
1. System startup messages
2. Knowledge base indexing progress
3. Static analysis results summary
4. Individual feedback entries
5. Completion status
```

## Component Interactions

### Data Flow Diagram
```
┌─────────────┐
│ Java File   │──┐
└─────────────┘  │
                 │    ┌─────────────────┐
┌─────────────┐  │    │                 │
│ Config Files│──┼───▶│ Static Analysis │
└─────────────┘  │    │ (Checkstyle+PMD)│
                 │    └─────────────────┘
┌─────────────┐  │              │
│ Knowledge   │──┘              │
│ Base (JSON) │                 ▼
└─────────────┘         ┌─────────────────┐
       │                │ AnalysisFinding │
       │                │    Objects      │
       ▼                └─────────────────┘
┌─────────────────┐              │
│ Lucene Index    │              │
│ (Searchable KB) │              ▼
└─────────────────┘     ┌─────────────────┐
       │                │ RAG Pipeline    │
       │                │ (Retrieval +    │
       └───────────────▶│  Generation)    │
                        └─────────────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │ Formatted       │
                        │ Feedback        │
                        └─────────────────┘
```

## Key Classes and Responsibilities

| Class                   | Responsibility               | Input               | Output               |
|-------------------------|------------------------------|---------------------|----------------------|
| `Main`                  | Orchestrates entire pipeline | Command line args   | Console output       |
| `KnowledgeBaseIndexer`  | Creates searchable index     | JSON files          | Lucene index         |
| `CheckstyleAnalyzer`    | Runs Checkstyle analysis     | Java file + config  | AnalysisFinding list |
| `PMDAnalyzer`           | Runs PMD analysis            | Java file + ruleset | AnalysisFinding list |
| `KnowledgeBaseSearcher` | Searches indexed knowledge   | Query string        | KnowledgeEntry list  |
| `FeedbackGenerator`     | Creates formatted feedback   | Finding + Knowledge | Formatted text       |

## Error Handling Strategy

**Graceful Degradation:**
- Configuration errors → Error findings with explanations
- Missing knowledge → Basic feedback without guidance
- Analysis failures → Error messages with context
- File access issues → Clear error messages with paths

**Continuation Policy:**
- A single tool failure doesn't stop an entire pipeline
- Missing knowledge entries don't prevent basic feedback
- Partial results are still useful for code review

## Performance Considerations

**Indexing Optimization:**
- One-time index creation per session
- Reuses existing index if available
- Incremental updates possible

**Search Efficiency:**
- Lucene provides fast text search
- Relevance scoring for best matches
- Configurable result limits

**Memory Management:**
- Streaming file processing
- Proper resource cleanup (try-with-resources)
- Bounded result sets

## Usage Examples

**Basic Usage:**
```bash
# Analyze specific file
java com.epam.Main MyClass.java checkstyle.xml pmd-rules.xml knowledgebase/ index/

# Test with samples
java com.epam.Main
```

**Expected Output:**
```
Running Java RAG Code Review with test samples...
=================================================

==================================================
Testing: samples/BadCodeExample.java
==================================================
Indexing knowledge base...
Knowledge base indexed successfully.
Running static analysis...
Checkstyle found 5 issues.
PMD found 3 issues.
Total findings: 8

Generating feedback using RAG pipeline...

=== CODE REVIEW FEEDBACK ===
Issue Detected: Vector
Location: BadCodeExample.java:11 - Using legacy Vector class
Knowledge Base Guidance:
Title: Vector is legacy, prefer ArrayList
Type: Anti-pattern
Description: Vector is synchronized by default which adds unnecessary overhead...
Example/Suggestion: Use ArrayList<String> list = new ArrayList<>();
Reference: Effective Java by Joshua Bloch, Item 6
=============================
```

This workflow ensures comprehensive code analysis with contextual, educational feedback powered by the RAG architecture.