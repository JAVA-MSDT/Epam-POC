# Java RAG System - Architecture Guide

## Overview

This is a TRUE Retrieval-Augmented Generation (RAG) system for Java code review that combines:
- Static analysis (Checkstyle + PMD)
- Knowledge base retrieval (Lucene)
- LLM generation (Ollama)

---

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    USER INPUT                           │
│              File + Natural Language Query              │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  STATIC ANALYSIS                        │
│              (Checkstyle + PMD)                         │
│  • Detects code issues                                  │
│  • Generates findings                                   │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
                    Findings List
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              RAG PIPELINE (3 Steps)                     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  STEP 1: RETRIEVAL                                      │
│  ┌──────────────────────────────────────┐              │
│  │  Knowledge Base Searcher             │              │
│  │  • Lucene-powered search             │              │
│  │  • Finds relevant KB entries         │              │
│  │  • Returns top matches               │              │
│  └──────────────────────────────────────┘              │
│                    ↓                                    │
│              Context Retrieved                          │
│                    ↓                                    │
│  STEP 2: AUGMENTATION                                   │
│  ┌──────────────────────────────────────┐              │
│  │  Prompt Builder                      │              │
│  │  • Combines user query               │              │
│  │  • Adds findings                     │              │
│  │  • Adds KB context                   │              │
│  │  • Adds code snippet                 │              │
│  └──────────────────────────────────────┘              │
│                    ↓                                    │
│              Enriched Prompt                            │
│                    ↓                                    │
│  STEP 3: GENERATION                                     │
│  ┌──────────────────────────────────────┐              │
│  │  Ollama Client                       │              │
│  │  • Sends prompt to LLM               │              │
│  │  • CodeLlama 7B generates response   │              │
│  │  • Returns natural language feedback │              │
│  └──────────────────────────────────────┘              │
│                                                         │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    OUTPUT                               │
│         Natural Language Feedback to User               │
└─────────────────────────────────────────────────────────┘
```

---

## Component Details

### 1. Static Analysis Layer

**Purpose**: Detect code issues using rule-based tools

**Components**:
- `CheckstyleAnalyzer.java`: Style and convention checks
- `PMDAnalyzer.java`: Bug detection and code quality

**Output**: List of `AnalysisFinding` objects

---

### 2. Knowledge Base Layer

**Purpose**: Store and retrieve best practices

**Components**:
- `KnowledgeBaseIndexer.java`: Indexes JSON entries with Lucene
- `KnowledgeBaseSearcher.java`: Searches for relevant entries
- JSON files in `resources/knowledgebase/`

**Technology**: Apache Lucene for fast text search

---

### 3. RAG Layer (Core)

#### 3.1 Retrieval
**Class**: `KnowledgeBaseSearcher`
- Searches KB based on findings
- Returns top 3 relevant entries
- Uses keyword matching

#### 3.2 Augmentation
**Class**: `PromptBuilder`
- Constructs prompts with:
  - System instructions
  - User query
  - Code context
  - Findings
  - KB entries
- Truncates code to avoid token limits

#### 3.3 Generation
**Class**: `OllamaClient`
- Connects to Ollama (localhost:11434)
- Sends prompt to CodeLlama 7B
- Returns LLM-generated response
- Handles errors gracefully

---

### 4. Orchestration Layer

**Class**: `RAGGenerator`
- Coordinates the 3-step RAG pipeline
- Two modes:
  - `generateFeedback()`: With static analysis
  - `answerQuestion()`: Direct Q&A
- Error handling and logging

**Class**: `Main`
- Entry point
- Two modes:
  - Test mode (no args)
  - RAG mode (file + query)
- User interface

---

## Data Flow

### Example: "Find bugs in MyCode.java"

```
1. User Input
   ├─ File: MyCode.java
   └─ Query: "Find bugs in this code"

2. Static Analysis
   ├─ Checkstyle: 2 issues found
   ├─ PMD: 1 issue found
   └─ Total: 3 findings

3. RAG Pipeline
   │
   ├─ RETRIEVAL
   │  ├─ Search KB for "Vector"
   │  ├─ Search KB for "Enumeration"
   │  └─ Found: 2 relevant entries
   │
   ├─ AUGMENTATION
   │  ├─ Build prompt:
   │  │  "You are a Java expert..."
   │  │  "User asked: Find bugs..."
   │  │  "Issues: Vector usage, Enumeration..."
   │  │  "KB Context: Vector is legacy..."
   │  └─ Prompt ready (500 tokens)
   │
   └─ GENERATION
      ├─ Send to Ollama
      ├─ CodeLlama processes (10s)
      └─ Response: "I found 3 issues..."

4. Output
   └─ Natural language explanation with:
      ├─ Why issues matter
      ├─ Best practices
      └─ Concrete suggestions
```

---

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Analysis** | Checkstyle, PMD | Rule-based code analysis |
| **Storage** | JSON files | Knowledge base entries |
| **Indexing** | Apache Lucene | Fast text search |
| **Retrieval** | Lucene queries | Find relevant context |
| **Generation** | Ollama + CodeLlama | LLM-powered responses |
| **Integration** | LangChain4j | Java LLM framework |
| **Build** | Maven | Dependency management |

---

## Key Design Decisions

### 1. Local-First Architecture
- No cloud dependencies
- No API keys required
- Privacy-focused
- Runs offline

### 2. Modular Design
- Each component is swappable
- Clear interfaces
- Easy to extend

### 3. Two-Mode Operation
- Test mode: Automated demos
- RAG mode: Interactive queries

### 4. Graceful Degradation
- Clear error messages
- Fallback handling
- User-friendly feedback

---

## Extension Points

### Easy to Replace:

| Component | Current | Alternatives |
|-----------|---------|-------------|
| **Static Analysis** | Checkstyle/PMD | SpotBugs, SonarQube |
| **Knowledge Store** | JSON files | Database, API |
| **Search** | Lucene | Elasticsearch, Vector DB |
| **LLM** | Ollama/CodeLlama | OpenAI, Claude, Llama3 |
| **Embeddings** | None | Sentence-transformers |

### Easy to Add:

- Vector embeddings for semantic search
- Re-ranking for better retrieval
- Streaming responses
- Multi-file analysis
- Custom knowledge sources
- REST API layer

---

## Performance Characteristics

### Static Analysis
- **Speed**: 100-500ms per file
- **Deterministic**: Same input = same output
- **Scalable**: Can analyze many files

### Knowledge Retrieval
- **Speed**: 10-50ms per query
- **Accuracy**: Keyword-based matching
- **Scalable**: Lucene handles large KBs

### LLM Generation
- **First query**: 10-30 seconds (model loading)
- **Subsequent**: 2-10 seconds
- **Quality**: Good for code review
- **Resource**: 2-4GB RAM

---

## Security Considerations

### ✅ Secure:
- All processing local
- No data sent to cloud
- No API keys to leak
- Code stays on machine

### ⚠️ Consider:
- Input validation (file paths)
- Resource limits (prevent DoS)
- Sanitize LLM outputs
- Rate limiting for production

---

## Scalability

### Current Limitations:
- Single-threaded processing
- One file at a time
- Synchronous LLM calls
- In-memory knowledge base

### To Scale:
- Add async processing
- Batch file analysis
- Distributed knowledge base
- Load balancing for LLM

---

## Monitoring & Observability

### Current:
- Console logging
- Basic error messages
- Pipeline step indicators

### Production Needs:
- Structured logging (SLF4J)
- Metrics (Prometheus)
- Tracing (OpenTelemetry)
- Health checks

---

## Testing Strategy

### Unit Tests (TODO):
- Test each component independently
- Mock external dependencies
- Verify prompt construction

### Integration Tests (TODO):
- Test RAG pipeline end-to-end
- Verify Ollama integration
- Test error handling

### Manual Testing:
- Run test mode
- Try various queries
- Verify output quality

---

## Deployment

### Local Development:
```bash
mvn clean compile
mvn exec:java
```

### Production Considerations:
- Package as JAR
- Docker container
- Environment configuration
- Resource allocation
- Monitoring setup

---

## Future Enhancements

### Short Term:
1. Add vector embeddings
2. Improve prompt templates
3. Add more KB entries
4. Better error handling

### Medium Term:
1. REST API
2. Web UI
3. Multi-file analysis
4. Custom model support

### Long Term:
1. Fine-tuned models
2. Feedback loop
3. A/B testing
4. Enterprise features

---

## Comparison: Before vs After

### Before (Template-Based):
```
File → Analysis → Findings → Template Fill → Report
```
- Fast (100ms)
- Rigid output
- No user queries
- Not TRUE RAG

### After (LLM-Based):
```
Query + File → Analysis → Retrieval → Prompt → LLM → Response
```
- Slower (10s)
- Natural language
- User queries
- TRUE RAG ✅

---

## Summary

This is a **TRUE RAG system** because it has:

1. ✅ **User queries** (natural language input)
2. ✅ **Retrieval** (knowledge base search)
3. ✅ **Augmentation** (prompt with context)
4. ✅ **Generation** (LLM creates response)
5. ✅ **Local** (no cloud, no API keys)

**Perfect for learning, demos, and internal tools!**
