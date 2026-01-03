# Rag Code Assistant

## Architecture
```text
ragcodeassistant/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── epam/
│       │           └── rag/
│       │               ├── RagServiceApplication.java
│       │               ├── controller/
│       │               │   └── RagController.java
│       │               ├── service/
│       │               │   ├── CodeRetriever.java
│       │               │   ├── JavaCodeParser.java
│       │               │   ├── LlmService.java
│       │               │   └── CodeReviewService.java
│       │               └── model/
│       │                   ├── CodeRequest.java
│       │                   └── SearchRequest.java
│       └── resources/
│           └── application.yml
├── pom.xml
└── README.md
```
