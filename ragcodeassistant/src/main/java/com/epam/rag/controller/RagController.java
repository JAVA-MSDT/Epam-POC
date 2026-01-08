package com.epam.rag.controller;

import com.epam.rag.model.CodeDocument;
import com.epam.rag.model.SearchRequest;
import com.epam.rag.service.RefactoringService;
import com.epam.rag.service.VectorDbService;
import com.epam.rag.service.CodeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RagController {

    private final VectorDbService vectorDbService;
    private final RefactoringService refactoringService;
    private final CodeAnalysisService codeAnalysisService;

    @PostMapping("/documents")
    public ResponseEntity<String> addDocument(@RequestBody CodeDocument document) {
        try {
            String id = vectorDbService.addDocument(document);
            return ResponseEntity.ok(id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error adding document: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<CodeDocument>> search(@RequestParam String query,
                                                   @RequestParam(defaultValue = "10") int limit,
                                                   @RequestParam(defaultValue = "0.7") double threshold) {
        try {
            SearchRequest request = new SearchRequest(query, null, limit, threshold);
            List<CodeDocument> results = vectorDbService.search(request);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/review")
    public ResponseEntity<Map<String, Object>> reviewCode(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String language = request.getOrDefault("language", "java");
            Map<String, Object> review = codeAnalysisService.analyzeCode(code, language);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refactor")
    public ResponseEntity<Map<String, Object>> refactorCode(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String language = request.getOrDefault("language", "java");
            Map<String, Object> suggestions = refactoringService.generateRefactoringSuggestions(code, language);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/documents/{id}")
    public ResponseEntity<String> updateDocument(@PathVariable String id, @RequestBody CodeDocument document) {
        try {
            document.setId(id);
            vectorDbService.updateDocument(document);
            return ResponseEntity.ok("Document updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating document: " + e.getMessage());
        }
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable String id) {
        try {
            vectorDbService.deleteDocument(id);
            return ResponseEntity.ok("Document deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting document: " + e.getMessage());
        }
    }
}