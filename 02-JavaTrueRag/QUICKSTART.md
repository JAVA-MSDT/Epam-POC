# Quick Start Guide

Get the Java RAG system running in 5 minutes.

---

## Step 1: Install Ollama (2 minutes)

```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Windows: Download from https://ollama.ai/download
```

---

## Step 2: Start Ollama & Download Model (3 minutes)

```bash
# Start Ollama service
ollama serve &

# Download CodeLlama model (~3.8GB)
ollama pull codellama:7b

# Test it works
ollama run codellama:7b "Hello"
# Press Ctrl+D to exit
```

---

## Step 3: Build the Project (1 minute)

```bash
cd javatruerag

# Build
mvn clean compile
```

---

## Step 4: Run It! (30 seconds)

### Option A: Test Mode (Automated)
```bash
mvn exec:java
```

This runs 3 predefined queries and shows the RAG pipeline in action.

### Option B: RAG Mode (Interactive)
```bash
mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Find bugs'"
```

---

## Example Queries

```bash
# Find bugs
mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Find bugs in this code'"

# Security review
mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Is this code secure?'"

# Performance analysis
mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'What are performance issues?'"

# Explain issues
mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Explain the problems'"

# Best practices
mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Suggest improvements'"
```

---

## Troubleshooting

### "Connection refused" or "Cannot connect to Ollama"
```bash
# Start Ollama
ollama serve &

# Verify it's running
curl http://localhost:11434/api/tags
```

### "Model not found"
```bash
# Download the model
ollama pull codellama:7b

# Verify it's downloaded
ollama list
```

### Slow first query
This is normal! The model needs to load into memory (10-30 seconds).
Subsequent queries are faster (2-10 seconds).

**Speed it up**: Pre-warm the model before demos:
```bash
ollama run codellama:7b "test" > /dev/null
```

---

## What You'll See

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
🔧 Step 2: Building prompt with context...
🤖 Step 3: Generating response with LLM...
✅ Response generated successfully
=== RAG COMPLETE ===

============================================================
💡 RAG-GENERATED FEEDBACK
============================================================

I've analyzed your code and found several issues:

1. Vector Usage (Line 8)
   You're using the legacy Vector class...
   [Natural language explanation continues...]
============================================================
```

---

## Next Steps

- **Learn more**: Read [ARCHITECTURE.md](ARCHITECTURE.md)
- **Present it**: See [PRESENTATION.md](PRESENTATION.md)
- **Customize**: Add your own knowledge base entries
- **Extend**: Swap components (see [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md))

---

## Need Help?

- **Setup issues**: See [OLLAMA_SETUP.md](OLLAMA_SETUP.md)
- **Architecture questions**: See [ARCHITECTURE.md](ARCHITECTURE.md)
- **Implementation details**: See [IMPLEMENTATION_COMPLETE.md](IMPLEMENTATION_COMPLETE.md)

---

**That's it! You now have a working TRUE RAG system.** 🎉
