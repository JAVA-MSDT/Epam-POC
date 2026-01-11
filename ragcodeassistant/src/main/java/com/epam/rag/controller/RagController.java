package com.epam.rag.controller;

import com.epam.rag.model.CodeDocument;
import com.epam.rag.model.SearchRequest;
import com.epam.rag.model.ReviewRequest;
import com.epam.rag.model.RefactorRequest;
import com.epam.rag.service.RefactoringService;
import com.epam.rag.service.VectorDbService;
import com.epam.rag.service.CodeAnalysisService;
import com.epam.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final VectorDbService vectorDbService;
    private final DocumentService documentService;
    private final RefactoringService refactoringService;
    private final CodeAnalysisService codeAnalysisService;

    @PostMapping("/documents")
    public ResponseEntity<Map<String, String>> addDocument(@RequestBody CodeDocument document) {
        try {
            log.info("Adding document: {}", document.getFileName());
            String id = documentService.addDocument(document);
            return ResponseEntity.ok(Map.of("id", id, "message", "Document added successfully"));
        } catch (Exception e) {
            log.error("Error adding document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error adding document: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<CodeDocument>> search(@RequestParam String query,
                                                   @RequestParam(required = false) String language,
                                                   @RequestParam(defaultValue = "10") int limit,
                                                   @RequestParam(defaultValue = "0.7") double threshold) {
        try {
            log.info("Searching documents with query: {}", query);
            SearchRequest request = new SearchRequest(query, language, limit, threshold);
            List<CodeDocument> results = vectorDbService.search(request);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching documents: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/search")
    public ResponseEntity<List<CodeDocument>> searchPost(@RequestBody SearchRequest request) {
        try {
            log.info("Searching documents with query: {}", request.getQuery());
            List<CodeDocument> results = vectorDbService.search(request);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching documents: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<CodeDocument> getDocument(@PathVariable String id) {
        try {
            log.info("Retrieving document: {}", id);
            CodeDocument document = documentService.getDocument(id);
            if (document != null) {
                return ResponseEntity.ok(document);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error retrieving document: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<CodeDocument>> getAllDocuments() {
        try {
            log.info("Retrieving all documents");
            List<CodeDocument> documents = documentService.getAllDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error retrieving documents: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/review")
    public ResponseEntity<Map<String, Object>> reviewCode(@RequestBody ReviewRequest request) {
        try {
            log.info("Performing code review for {} code", request.getLanguage());
            Map<String, Object> review = refactoringService.performCodeReview(request.getCode(), request.getLanguage());
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("Error performing code review: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/review/simple")
    public ResponseEntity<Map<String, Object>> reviewCodeSimple(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String language = request.getOrDefault("language", "java");
            log.info("Performing simple code review for {} code", language);
            Map<String, Object> review = codeAnalysisService.analyzeCode(code, language);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("Error performing code review: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refactor")
    public ResponseEntity<Map<String, Object>> refactorCode(@RequestBody RefactorRequest request) {
        try {
            log.info("Generating refactoring suggestions for {} code", request.getLanguage());
            Map<String, Object> suggestions = refactoringService.generateRefactoringSuggestions(request.getCode(), request.getLanguage());
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error generating refactoring suggestions: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refactor/simple")
    public ResponseEntity<Map<String, Object>> refactorCodeSimple(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String language = request.getOrDefault("language", "java");
            log.info("Generating simple refactoring suggestions for {} code", language);
            Map<String, Object> suggestions = refactoringService.generateRefactoringSuggestions(code, language);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error generating refactoring suggestions: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/explain")
    public ResponseEntity<Map<String, String>> explainCode(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            log.info("Generating code explanation");
            String explanation = refactoringService.explainCode(code);
            return ResponseEntity.ok(Map.of("explanation", explanation));
        } catch (Exception e) {
            log.error("Error generating code explanation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/documents/{id}")
    public ResponseEntity<Map<String, String>> updateDocument(@PathVariable String id, @RequestBody CodeDocument document) {
        try {
            log.info("Updating document: {}", id);
            document.setId(id);
            documentService.updateDocument(document);
            return ResponseEntity.ok(Map.of("message", "Document updated successfully"));
        } catch (Exception e) {
            log.error("Error updating document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error updating document: " + e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String id) {
        try {
            log.info("Deleting document: {}", id);
            documentService.deleteDocument(id);
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error deleting document: " + e.getMessage()));
        }
    }

    @PostMapping("/documents/{id}/reindex")
    public ResponseEntity<Map<String, String>> reindexDocument(@PathVariable String id) {
        try {
            log.info("Reindexing document: {}", id);
            documentService.reindexDocument(id);
            return ResponseEntity.ok(Map.of("message", "Document reindexed successfully"));
        } catch (Exception e) {
            log.error("Error reindexing document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error reindexing document: " + e.getMessage()));
        }
    }

    @PostMapping("/documents/reindex-all")
    public ResponseEntity<Map<String, String>> reindexAllDocuments() {
        try {
            log.info("Reindexing all documents");
            documentService.reindexAllDocuments();
            return ResponseEntity.ok(Map.of("message", "All documents reindexed successfully"));
        } catch (Exception e) {
            log.error("Error reindexing all documents: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error reindexing documents: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "RAG Code Assistant",
            "timestamp", System.currentTimeMillis(),
            "documentCount", vectorDbService.getDocumentCount()
        ));
    }
}