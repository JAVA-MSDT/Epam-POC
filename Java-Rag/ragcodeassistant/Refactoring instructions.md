# Rag Code Assistant

## prompt
refactor the Java Spring Boot project named `ragcodeassistant` with the following structure:

src/main/java/com/epam/rag/
- RagApplication.java: Main Spring Boot application class.
- controller/RagController.java: REST controller exposing endpoints for document management, semantic search, code review, and refactoring.
- service/VectorDbService.java: Service for CRUD and search operations with a local vector database (Qdrant, using its Java client).
- service/EmbeddingService.java: Service for generating vector embeddings for code/text using ONNX Runtime Java API and a local transformer model (e.g., MiniLM or BERT).
- service/LlmService.java: Service for interacting with a local LLM (Ollama) via REST API to get code explanations and refactoring suggestions.
- service/CodeAnalysisService.java: Service for analyzing Java code using JavaParser to detect patterns, deprecated APIs, and code smells.
- service/RefactoringService.java: Service for orchestrating code refactoring suggestions using analysis and LLM feedback.
- model/CodeDocument.java: Model representing a code file or snippet with metadata.
- model/SearchRequest.java: Model representing a semantic search query.

Include a `README.md` with setup instructions, a `pom.xml` with all necessary dependencies (Spring Boot, JavaParser, ONNX Runtime, Qdrant Java client, etc.), and two shell scripts in a `scripts/` folder:
- `run_qdrant.sh`: Script to start Qdrant locally via Docker.
- `run_ollama.sh`: Script to start Ollama locally and pull a model (e.g., mistral).

The project should be fully local, with no cloud dependencies or API keys required. All data and processing must remain on the user's machine. Implement basic CRUD endpoints, semantic search, code analysis, and LLM integration. Provide stub implementations for services where external setup is required (e.g., ONNX model loading, Qdrant connection, Ollama REST calls), and document any manual steps in the README.

Generate all necessary classes and configuration to make the project compile and run, with clear separation of concerns and best practices for a modern Spring Boot application.

## Details

1. Layered Structure
   Controller Layer: Exposes REST API endpoints for user interaction.
   Service Layer: Contains business logic for embedding, vector search, code analysis, LLM interaction, and document management.
   Repository Layer: Handles persistence and retrieval from the vector database (Qdrant).
   Integration Layer: Connects to external local services (Qdrant, Ollama).

2. Package Structure
## Architecture
```text
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
│                       │   └── RefactoringService.java
│                       └── model/
│                           ├── CodeDocument.java
│                           └── SearchRequest.java
├── README.md
├── pom.xml
└── scripts/
    ├── run_qdrant.sh
    └── run_ollama.sh
```

3. Component Responsibilities
   A. Controller Layer
   RagController.java
   Exposes endpoints:
   POST /api/documents — Add code document
   GET /api/search — Semantic search
   POST /api/review — Automated code review
   POST /api/refactor — Refactoring suggestion
   PUT /api/documents/{id} — Update document
   DELETE /api/documents/{id} — Delete document

   B. Service Layer
   EmbeddingService.java
   Generates vector embeddings for code/text using ONNX Runtime (Java).
   VectorDbService.java
   Handles CRUD and search operations with Qdrant via Java client.
   CodeAnalysisService.java
   Uses JavaParser to analyze code for patterns, smells, and standards.
   LlmService.java
   Sends code and prompts to Ollama (local LLM) via REST API, receives responses.
   DocumentService.java
   Orchestrates document management: add, update, delete, re-index.

   C. Repository Layer
   VectorDbRepository.java
   Directly interacts with Qdrant for storing and retrieving vectors/documents.

   D. Model Layer
   CodeDocument.java: Represents a code file/snippet with metadata.
   SearchRequest.java: Encapsulates search queries.
   ReviewRequest.java: Encapsulates code review requests.
   RefactorRequest.java: Encapsulates refactoring requests.

   E. Config Layer
   QdrantConfig.java
   Configures Qdrant client bean for dependency injection.

## project workflow
``` text
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

[//]: # (in the  Refactoring instructions.md there are instructions of how to build the project as it should be, can you double check it then complete what is not finished yet)
