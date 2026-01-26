# Java RAG Code Review System

A TRUE Retrieval-Augmented Generation (RAG) system for Java code review that combines static analysis tools (Checkstyle and PMD) with a local knowledge base and Ollama LLM to provide intelligent, contextual feedback.

## What Makes This TRUE RAG?

✅ **User Queries**: Accept natural language questions about code  
✅ **Retrieval**: Search knowledge base for relevant context  
✅ **LLM Generation**: Use Ollama (local LLM) to generate intelligent responses  
✅ **Completely Local**: No API keys, no cloud services, runs offline  
✅ **Privacy-First**: Your code never leaves your machine

## Project Structure

```
javatruerag/
├── pom.xml                                  # Maven build file
├── OLLAMA_SETUP.md                          # Ollama installation guide
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── epam/
│   │   │           ├── analysis/            # Static analysis components
│   │   │           │   ├── CheckstyleAnalyzer.java
│   │   │           │   └── PMDAnalyzer.java
│   │   │           ├── retrieval/           # RAG: Retrieval
│   │   │           │   ├── KnowledgeBaseIndexer.java
│   │   │           │   └── KnowledgeBaseSearcher.java
│   │   │           ├── augmentation/        # RAG: Augmentation
│   │   │           │   └── PromptBuilder.java
│   │   │           ├── llm/                 # RAG: LLM Client
│   │   │           │   └── OllamaClient.java
│   │   │           ├── generation/          # RAG: Generation Pipeline
│   │   │           │   └── RAGPipeline.java
│   │   │           ├── model/               # Data models
│   │   │           │   ├── AnalysisFinding.java
│   │   │           │   └── KnowledgeEntry.java
│   │   │           └── Main.java            # Main entry point
│   │   └── resources/
│   │       ├── checkstyle.xml               # Checkstyle configuration
│   │       ├── pmd-ruleset.xml              # PMD ruleset configuration
│   │       └── knowledgebase/               # Knowledge base entries
│   │           ├── vector_vs_arraylist.json
│   │           ├── enumeration_vs_iterator.json
│   │           └── synchronized_vs_concurrent.json
└── samples/                                 # Sample files for testing
```

## Features

- **TRUE RAG Implementation**: Complete RAG pipeline with LLM generation
- **Local LLM**: Uses Ollama (CodeLlama, Llama3, or Mistral) - no API keys needed
- **User Queries**: Ask questions in natural language about your code
- **Static Analysis Integration**: Checkstyle and PMD for comprehensive code analysis
- **Knowledge Base Retrieval**: Lucene-powered search for relevant best practices
- **Intelligent Feedback**: LLM generates contextual, educational responses
- **Privacy-First**: Everything runs locally, code never leaves your machine
- **Extensible**: Easy to swap models, add knowledge, customize analysis

## Build and Run

## Prerequisites

### Required:
- **Java 21** or higher
- **Maven 3.6** or higher
- **Ollama** installed and running
- **CodeLlama model** downloaded

### Setup Ollama (5 minutes):

```bash
# 1. Install Ollama
brew install ollama

# 2. Start Ollama service
ollama serve &

# 3. Download CodeLlama model (~3.8GB)
ollama pull codellama:7b

# 4. Test it works
ollama run codellama:7b "Hello"
```

**For detailed setup instructions, see [OLLAMA_SETUP.md](OLLAMA_SETUP.md)**

### Build
```bash
cd javatruerag
mvn clean compile
```

### Run Test Mode (Automated Tests)
```bash
mvn exec:java
```

### Run RAG Mode (Interactive)
```bash
# Ask questions about your code
mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Find bugs in this code'"

mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Explain the issues'"

mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Suggest improvements'"
```

## Usage

### Test Mode (No Arguments)
Runs predefined test queries:
```bash
mvn exec:java
```

### RAG Mode (File + Query)
Analyze specific files with custom queries:
```bash
mvn exec:java -Dexec.args="<file> '<query>'"
```

**Example Queries:**
- `"Find all bugs and security issues"`
- `"Explain why this code is problematic"`
- `"What are the performance concerns?"`
- `"Suggest best practices improvements"`
- `"Is this code thread-safe?"`

## Adding Knowledge Base Entries

Create JSON files in the `knowledgebase` directory with this structure:

```json
{
  "title": "Rule or Best Practice Title",
  "type": "Best Practice|Anti-pattern|Enhancement",
  "description": "Detailed explanation of the issue and why it matters",
  "example": "Code example showing the problem and solution",
  "reference": "Source or reference documentation",
  "tags": ["keyword1", "keyword2", "keyword3"]
}
```

## Customizing Rules

- **Checkstyle**: Modify `src/main/resources/checkstyle.xml`
- **PMD**: Modify `src/main/resources/pmd-ruleset.xml`

## How It Works (TRUE RAG Pipeline)

```
User Query + Code File
        ↓
1. INDEXING (Offline)
   Knowledge Base → Lucene Index
        ↓
2. STATIC ANALYSIS
   Checkstyle + PMD → Findings
        ↓
3. RETRIEVAL
   Search KB for relevant context
        ↓
4. PROMPT CONSTRUCTION
   Combine: Query + Findings + KB Context
        ↓
5. LLM GENERATION
   Ollama generates intelligent response
        ↓
   Natural Language Feedback
```

## Example Output

```
🚀 Java RAG Code Review System - TRUE RAG Mode
============================================================

📄 File: samples/KnowledgeBaseTestExample.java
❓ Query: Find bugs in this code

Running static analysis...
Checkstyle found 2 issues.
PMD found 1 issues.

=== RAG PIPELINE ===
📚 Step 1: Retrieving relevant knowledge...
   Found 3 relevant knowledge entries
🔧 Step 2: Building prompt with context...
🤖 Step 3: Generating response with LLM...
✅ Response generated successfully
=== RAG COMPLETE ===

============================================================
💡 RAG-GENERATED FEEDBACK
============================================================

I've analyzed your code and found several issues that need attention:

1. **Vector Usage (Line 8)**
   You're using the legacy Vector class. This is an anti-pattern in modern
   Java. Vector is synchronized by default, which adds unnecessary overhead
   in single-threaded scenarios. 
   
   Recommendation: Replace with ArrayList:
   ```java
   // Instead of:
   Vector<String> items = new Vector<>();
   
   // Use:
   ArrayList<String> items = new ArrayList<>();
   ```

2. **Performance Concern**
   The synchronization overhead of Vector can impact performance...

[Full LLM-generated explanation continues...]
============================================================
```