# Ollama Setup Guide for Java RAG System

This guide will help you set up Ollama for local LLM integration in the Java RAG Code Review system.

---

## What is Ollama?

Ollama is a tool that lets you run large language models (LLMs) locally on your machine:

- ✅ **Completely free** – No API keys or subscriptions
- ✅ **Runs offline** – No internet required after download
- ✅ **Privacy-focused** - Your code never leaves your machine
- ✅ **Easy to use** – Simple CLI interface
- ✅ **Multiple models** – Choose the best model for your needs

---

## System Requirements

### Minimum Requirements:

- **RAM**: 8GB (16GB recommended)
- **Disk Space**: 5–10GB free (for model storage)
- **OS**: macOS, Linux, or Windows
- **CPU**: Modern multicore processor (GPU optional but faster)

### Recommended for Best Performance:

- **RAM**: 16GB or more
- **GPU**: NVIDIA GPU with 8GB+ VRAM (optional)
- **Disk**: SSD for faster model loading

---

## Installation

### macOS

#### Option 1: Using Homebrew (Recommended)

```bash
# Install Ollama
brew install ollama

# Verify installation
ollama --version
```

#### Option 2: Direct Download

1. Download from: https://ollama.ai/download
2. Open the downloaded `.dmg` file
3. Drag Ollama to the Applications folder
4. Open Ollama from Applications

### Linux

```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Verify installation
ollama --version
```

### Windows

1. Download the installer from: https://ollama.ai/download
2. Run the installer
3. Follow the installation wizard
4. Verify installation:

```powershell
ollama --version
```

---

## Starting Ollama Service

Ollama runs as a background service that listens on `http://localhost:11434`

### macOS/Linux

```bash
# Start Ollama service (runs in foreground)
ollama serve

# Or run in background
nohup ollama serve > /dev/null 2>&1 &
```

### Windows

Ollama starts automatically as a service after installation.

### Verify Service is Running

```bash
# Check if Ollama is responding
curl http://localhost:11434/api/tags

# Should return JSON with available models
```

---

## Choosing a Model

### Recommended Models for Code Review

| Model             | Size  | RAM Needed | Speed     | Quality   | Best For                  |
|-------------------|-------|------------|-----------|-----------|---------------------------|
| **codellama:7b**  | 3.8GB | 8GB        | Fast      | Good      | Code-specific tasks ⭐     |
| **llama3:8b**     | 4.7GB | 8GB        | Fast      | Excellent | General purpose ⭐         |
| **mistral:7b**    | 4.1GB | 8GB        | Very Fast | Good      | Quick responses           |
| **codellama:13b** | 7.3GB | 16GB       | Medium    | Better    | More accurate code review |
| **llama3:70b**    | 40GB  | 64GB       | Slow      | Best      | Production quality        |

**For this demo, we recommend: `codellama:7b`** ⭐

---

## Downloading Models

### Download CodeLlama (Recommended)

```bash
# Download CodeLlama 7B (best for code review)
ollama pull codellama:7b

# This will download ~3.8GB
# Takes 5-15 minutes depending on internet speed
```

### Download Llama 3 (Alternative)

```bash
# Download Llama 3 8B (excellent general purpose)
ollama pull llama3:8b

# This will download ~4.7GB
```

### Download Mistral (Fastest)

```bash
# Download Mistral 7B (fastest responses)
ollama pull mistral:7b

# This will download ~4.1GB
```

### Check Downloaded Models

```bash
# List all downloaded models
ollama list

# Example output:
# NAME              ID              SIZE      MODIFIED
# codellama:7b      8fdf8f752f6e    3.8 GB    2 hours ago
# llama3:8b         a6990ed6be41    4.7 GB    1 day ago
```

---

## Testing Ollama

### Test 1: Basic Interaction

```bash
# Run interactive chat with CodeLlama
ollama run codellama:7b

# You'll see a prompt like:
# >>> 

# Try asking:
>>> Write a hello world program in Java

# Press Ctrl+D or type /bye to exit
```

### Test 2: Single Query

```bash
# Run a single query without interactive mode
ollama run codellama:7b "Explain what a Vector is in Java"

# Should return a detailed explanation
```

### Test 3: API Test (What Our Java Code Will Use)

```bash
# Test the API endpoint
curl http://localhost:11434/api/generate -d '{
  "model": "codellama:7b",
  "prompt": "Why is using Vector bad in Java?",
  "stream": false
}'

# Should return JSON with the response
```

### Test 4: Java Integration Test

Create a simple test file:

```bash
cat > TestOllama.java << 'EOF'
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestOllama {
    public static void main(String[] args) throws Exception {
        String json = """
            {
              "model": "codellama:7b",
              "prompt": "Say hello in Java",
              "stream": false
            }
            """;
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }
}
EOF

# Compile and run
javac TestOllama.java
java TestOllama

# Should print the LLM response
```

---

## Performance Expectations

### First Query (Cold Start)

- **Time**: 10–30 seconds
- **Reason**: Model needs to load into memory
- **Solution**: Keep Ollama running, or pre-warm with a dummy query

### Subsequent Queries (Warm)

- **Time**: 2–10 seconds
- **Reason**: Model already in memory
- **Varies by**: Model size, hardware, prompt complexity

### Token Generation Speed

- **CPU**: 5-20 tokens/second
- **GPU**: 50-200 tokens/second
- **Typical response**: 100–300 tokens (5–60 seconds)

---

## Troubleshooting

### Issue 1: "Connection refused" or "Cannot connect to Ollama"

**Cause**: Ollama service is not running

**Solution**:

```bash
# Start Ollama service
ollama serve

# In a new terminal, verify it's running
curl http://localhost:11434/api/tags
```

---

### Issue 2: "Model not found"

**Cause**: Model hasn't been downloaded

**Solution**:

```bash
# List downloaded models
ollama list

# If empty, download a model
ollama pull codellama:7b
```

---

### Issue 3: Slow responses or timeouts

**Cause**: Insufficient RAM or CPU resources

**Solutions**:

1. Use a smaller model:
   ```bash
   ollama pull codellama:7b  # Instead of 13b or 70b
   ```

2. Close other applications to free RAM

3. Increase timeout in Java code (we'll handle this)

---

### Issue 4: "Out of memory" errors

**Cause**: Model too large for available RAM

**Solution**:

```bash
# Check your RAM
# macOS/Linux:
free -h

# Use smaller model
ollama pull mistral:7b  # Smaller than codellama
```

---

### Issue 5: Port 11434 already in use

**Cause**: Another process using the port

**Solution**:

```bash
# Find process using port 11434
lsof -i :11434

# Kill the process
kill -9 <PID>

# Or change Ollama port
OLLAMA_HOST=0.0.0.0:11435 ollama serve
```

---

### Issue 6: Model generates poor quality responses

**Cause**: Model not suitable for a task, or poor prompt

**Solutions**:

1. Try a different model:
   ```bash
   ollama pull llama3:8b  # Better general purpose
   ```

2. Improve prompts (we'll handle this in code)

3. Use a larger model if you have RAM:
   ```bash
   ollama pull codellama:13b
   ```

---

## Advanced Configuration

### Change Ollama Host/Port

```bash
# Set custom host and port
export OLLAMA_HOST=0.0.0.0:8080
ollama serve
```

### Set GPU Layers (If you have GPU)

```bash
# Use GPU acceleration
export OLLAMA_NUM_GPU=1
ollama serve
```

### Adjust Context Window

```bash
# Increase context size (uses more RAM)
ollama run codellama:7b --ctx-size 4096
```

---

## Model Management

### List All Models

```bash
ollama list
```

### Remove a Model

```bash
# Free up disk space by removing unused models
ollama rm codellama:13b
```

### Update a Model

```bash
# Get latest version of a model
ollama pull codellama:7b
```

### Show Model Info

```bash
# See model details
ollama show codellama:7b
```

---

## Pre-Warming for Demos

To avoid slowing the first query during demos:

```bash
# Start Ollama
ollama serve &

# Pre-warm the model with a dummy query
ollama run codellama:7b "hello" > /dev/null

# Now the model is loaded and ready
# Your demo queries will be fast!
```

Or create a script:

```bash
cat > warm-ollama.sh << 'EOF'
#!/bin/bash
echo "Warming up Ollama..."
ollama run codellama:7b "test" > /dev/null 2>&1
echo "Ollama is ready!"
EOF

chmod +x warm-ollama.sh
./warm-ollama.sh
```

---

## Integration Checklist

Before running the Java RAG system, verify:

- [ ] Ollama is installed (`ollama --version`)
- [ ] Ollama service is running (`curl http://localhost:11434/api/tags`)
- [ ] Model is downloaded (`ollama list` shows codellama:7b)
- [ ] Model responds to queries (`ollama run codellama:7b "test"`)
- [ ] API endpoint works (the curl test above)
- [ ] Java can connect (TestOllama.java works)

---

## Quick Start Commands

```bash
# Complete setup in 4 commands:

# 1. Install Ollama
brew install ollama

# 2. Start service
ollama serve &

# 3. Download model
ollama pull codellama:7b

# 4. Test it works
ollama run codellama:7b "Hello"

# ✅ Ready to run Java RAG system!
```

---

## Recommended Models by Use Case

### For This Demo (Code Review):

```bash
ollama pull codellama:7b
```

**Why**: Best balance of speed, quality, and code understanding

### For Production (Better Quality):

```bash
ollama pull codellama:13b
```

**Why**: More accurate, better reasoning (needs 16GB RAM)

### For Fast Demos (Speed Priority):

```bash
ollama pull mistral:7b
```

**Why**: Fastest responses, good enough quality

### For Best Quality (If you have resources):

```bash
ollama pull llama3:70b
```

**Why**: Best quality, but needs 64GB RAM and is slow

---

## Performance Tuning Tips

### 1. Keep Ollama Running

Don't start/stop Ollama between queries. Keep it running:

```bash
ollama serve &
```

### 2. Pre-warm Before Demos

Run a dummy query to load the model:

```bash
ollama run codellama:7b "test" > /dev/null
```

### 3. Use Smaller Models for Demos

7B models are fast enough and impressive:

```bash
ollama pull codellama:7b  # Not 13b or 70b
```

### 4. Close Other Apps

Free up RAM for better performance

### 5. Use SSD

Store models on SSD for faster loading

---

## Comparison: Ollama vs Cloud LLMs

| Feature      | Ollama (Local) | OpenAI/Claude (Cloud)  |
|--------------|----------------|------------------------|
| **Cost**     | Free           | $0.01-0.10 per request |
| **Privacy**  | Complete       | Data sent to cloud     |
| **Speed**    | 5-30 seconds   | 1-5 seconds            |
| **Quality**  | Good           | Excellent              |
| **Setup**    | 15 minutes     | 5 minutes              |
| **Internet** | Not needed     | Required               |
| **API Keys** | None           | Required               |

**For demos and learning: Ollama is perfect!** ✅

---

## Next Steps

Once Ollama is set up and tested:

1. ✅ Verify all checklist items above
2. ✅ Keep Ollama running (`ollama serve`)
3. ✅ Pre-warm the model for demos
4. ✅ Ready for Java RAG implementation!

---

## Support Resources

- **Ollama Documentation**: https://github.com/ollama/ollama
- **Model Library**: https://ollama.ai/library
- **Discord Community**: https://discord.gg/ollama
- **GitHub Issues**: https://github.com/ollama/ollama/issues

---

## Estimated Setup Time

- **Installation**: 5 minutes
- **Model Download**: 10–15 minutes (depends on internet)
- **Testing**: 5 minutes
- **Total**: ~20–25 minutes

**You're now ready to implement TRUE RAG!** 🚀
