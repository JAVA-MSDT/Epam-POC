#!/bin/bash

# Script to start Ollama locally and pull the mistral model

echo "Setting up Ollama local LLM..."

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "Ollama is not installed. Installing Ollama..."
    
    # Install Ollama (Linux/macOS)
    if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "darwin"* ]]; then
        curl -fsSL https://ollama.ai/install.sh | sh
    else
        echo "Please install Ollama manually from https://ollama.ai"
        echo "For Windows, download the installer from the website."
        exit 1
    fi
fi

# Check if Ollama service is running
if ! pgrep -f "ollama serve" > /dev/null; then
    echo "Starting Ollama service..."
    ollama serve &
    OLLAMA_PID=$!
    echo "Ollama service started with PID: $OLLAMA_PID"
    
    # Wait for service to be ready
    echo "Waiting for Ollama service to be ready..."
    sleep 5
else
    echo "Ollama service is already running"
fi

# Wait for Ollama to be ready
echo "Checking Ollama availability..."
for i in {1..30}; do
    if curl -s http://localhost:11434/api/tags > /dev/null; then
        echo "✅ Ollama is ready!"
        break
    fi
    echo "Waiting for Ollama... ($i/30)"
    sleep 2
done

# Check if mistral model is already available
if ollama list | grep -q "mistral"; then
    echo "✅ Mistral model is already available"
else
    echo "Pulling mistral model (this may take a while)..."
    ollama pull mistral
    
    if [ $? -eq 0 ]; then
        echo "✅ Mistral model pulled successfully!"
    else
        echo "❌ Failed to pull mistral model"
        exit 1
    fi
fi

# Test the model
echo "Testing Ollama with mistral model..."
response=$(ollama run mistral "Hello, respond with just 'OK' if you're working" --timeout 30s 2>/dev/null)

if [[ $response == *"OK"* ]]; then
    echo "✅ Ollama and mistral model are working correctly!"
else
    echo "⚠️  Ollama is running but model test failed. This might be normal on first run."
fi

echo ""
echo "Ollama Setup Complete!"
echo "   - API endpoint: http://localhost:11434"
echo "   - Available models: $(ollama list | grep -v NAME | awk '{print $1}' | tr '\n' ' ')"
echo ""
echo "Usage:"
echo "   - Chat: ollama run mistral"
echo "   - API: curl http://localhost:11434/api/generate -d '{\"model\":\"mistral\",\"prompt\":\"Hello\"}'"
echo ""
echo "To stop Ollama: pkill -f 'ollama serve'"