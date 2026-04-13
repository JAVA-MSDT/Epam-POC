# RAG Code Assistant

A local RAG (Retrieval-Augmented Generation) system for code analysis, semantic search, and refactoring suggestions using Spring Boot.

## Features

- **Document Management**: Add, update, delete, and search code documents
- **Semantic Search**: Find similar code patterns using vector embeddings
- **Code Analysis**: Detect code smells, deprecated APIs, and patterns using JavaParser
- **AI-Powered Refactoring**: Get refactoring suggestions from local LLM (Ollama)
- **Code Review**: Automated code review with static analysis and AI insights
- **Fully Local**: No cloud dependencies, all processing happens on your machine

## Architecture

```
[User/Developer]
      |
      v
[Web UI or CLI or REST API]
      |
      v
[Spring Boot RAG Service]
      |         |         |         |
      |         |         |         |
[Embedding] [VectorDB] [LLM] [Code Analysis]
   |           |         |         |
[ONNX]     [Qdrant]  [Ollama] [JavaParser]
```

## Prerequisites

- Java 21+
- Maven 3.6+
- Docker (for Qdrant)
- Ollama (for local LLM)

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd ragcodeassistant
mvn clean install
```

### 2. Start External Services

Start Qdrant (vector database):
```bash
./scripts/run_qdrant.sh
```

Start Ollama (local LLM):
```bash
./scripts/run_ollama.sh
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Document Management
- `POST /api/documents` - Add a code document
- `PUT /api/documents/{id}` - Update a document
- `DELETE /api/documents/{id}` - Delete a document

### Search and Analysis
- `GET /api/search?query={query}&limit={limit}&threshold={threshold}` - Semantic search
- `POST /api/review` - Code review analysis
- `POST /api/refactor` - Get refactoring suggestions

## Configuration

### Application Properties

Create `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

qdrant:
  host: localhost
  port: 6333
  collection: code_documents

ollama:
  url: http://localhost:11434
  model: mistral

logging:
  level:
    com.epam.rag: INFO
```

### ONNX Model Setup (Optional)

For better embeddings, download a sentence transformer model:

1. Create `models/` directory in project root
2. Download ONNX model (e.g., sentence-transformers/all-MiniLM-L6-v2)
3. Place the `.onnx` file in `models/sentence-transformers-all-MiniLM-L6-v2.onnx`

**Note**: The application works with stub embeddings if no ONNX model is provided.

## External Services Setup

### Qdrant Vector Database

Qdrant runs in Docker and provides vector storage and similarity search:

```bash
# Start Qdrant
docker run -p 6333:6333 qdrant/qdrant

# Or use the provided script
./scripts/run_qdrant.sh
```

### Ollama Local LLM

Install and run Ollama for AI-powered code analysis:

```bash
# Install Ollama (see https://ollama.ai)
curl -fsSL https://ollama.ai/install.sh | sh

# Pull a model
ollama pull mistral

# Start Ollama service
ollama serve

# Or use the provided script
./scripts/run_ollama.sh
```

## Usage Examples

### Add a Code Document

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "Example.java",
    "filePath": "/src/Example.java",
    "content": "public class Example { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
    "language": "java",
    "className": "Example"
  }'
```

### Search for Similar Code

```bash
curl "http://localhost:8080/api/search?query=main method&limit=5"
```

### Get Code Review

```bash
curl -X POST http://localhost:8080/api/review \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Test { public static void main(String[] args) { System.out.println(\"test\"); } }",
    "language": "java"
  }'
```

### Get Refactoring Suggestions

```bash
curl -X POST http://localhost:8080/api/refactor \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Test { public static void main(String[] args) { System.out.println(\"test\"); } }",
    "language": "java"
  }'
```

## Project Structure

```
ragcodeassistant/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── epam/
│                   └── rag/
│                       ├── RagApplication.java
│                       ├── controller/
│                       │   └── RagController.java
│                       ├── service/
│                       │   ├── VectorDbService.java
│                       │   ├── EmbeddingService.java
│                       │   ├── LlmService.java
│                       │   ├── CodeAnalysisService.java
│                       │   ├── RefactoringService.java
│                       │   └── DocumentService.java
│                       ├── repository/
│                       │   └── VectorDbRepository.java
│                       ├── model/
│                       │   ├── CodeDocument.java
│                       │   ├── SearchRequest.java
│                       │   ├── ReviewRequest.java
│                       │   └── RefactorRequest.java
│                       └── config/
│                           └── QdrantConfig.java
├── scripts/
│   ├── run_qdrant.sh
│   └── run_ollama.sh
├── README.md
└── pom.xml
```

## Development

### Adding New Code Analysis Rules

Extend `CodeAnalysisService.java` to add new static analysis rules:

```java
private List<String> analyzeJavaCode(String code) {
    List<String> issues = new ArrayList<>();
    
    // Add your custom rules here
    if (code.contains("your-pattern")) {
        issues.add("Your custom issue description");
    }
    
    return issues;
}
```

### Extending Language Support

Currently, supports Java analysis. To add support for other languages:

1. Extend `CodeAnalysisService` with language-specific analyzers
2. Update `EmbeddingService` preprocessing for the new language
3. Add language-specific patterns and rules

## Troubleshooting

### Common Issues

1. **Qdrant Connection Failed**: Ensure Qdrant is running on port 6333
2. **Ollama Not Responding**: Check if Ollama service is running and model is pulled
3. **ONNX Model Not Found**: Application works with stub embeddings, but download a model for better results
4. **Out of Memory**: Increase JVM heap size: `-Xmx4g`

### Logs

Check application logs for detailed error information:

```bash
tail -f logs/application.log
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.