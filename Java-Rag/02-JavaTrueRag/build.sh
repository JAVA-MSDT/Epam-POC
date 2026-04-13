#!/bin/bash

# Java RAG System - Build and Run Script

echo "🔨 Building Java RAG System..."
echo ""

# Find Maven
if command -v mvn &> /dev/null; then
    MVN="mvn"
elif [ -f "/usr/local/bin/mvn" ]; then
    MVN="/usr/local/bin/mvn"
elif [ -f "$HOME/.sdkman/candidates/maven/current/bin/mvn" ]; then
    MVN="$HOME/.sdkman/candidates/maven/current/bin/mvn"
else
    echo "❌ Maven not found. Please install Maven first."
    exit 1
fi

echo "Using Maven: $MVN"
echo ""

# Clean and compile
$MVN clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "To run the system:"
    echo ""
    echo "  Test Mode:"
    echo "    $MVN exec:java"
    echo ""
    echo "  RAG Mode:"
    echo "    $MVN exec:java -Dexec.args=\"samples/KnowledgeBaseTestExample.java 'Find bugs'\""
    echo ""
else
    echo ""
    echo "❌ Build failed. Check errors above."
    exit 1
fi
