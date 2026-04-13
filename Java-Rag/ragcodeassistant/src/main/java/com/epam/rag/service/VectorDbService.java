package com.epam.rag.service;

import com.epam.rag.model.CodeDocument;
import com.epam.rag.model.SearchRequest;
import com.epam.rag.repository.VectorDbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDbService {
    
    private final EmbeddingService embeddingService;
    private final VectorDbRepository vectorDbRepository;
    
    public String addDocument(CodeDocument document) {
        log.info("Adding document: {}", document.getFileName());
        if (document.getId() == null) {
            document.setId(UUID.randomUUID().toString());
        }
        
        // Generate embeddings if not present
        if (document.getEmbeddings() == null) {
            float[] embeddings = embeddingService.generateCodeEmbedding(document.getContent(), document.getLanguage());
            document.setEmbeddings(embeddings);
        }
        
        // Set additional metadata
        document.setLastModified(System.currentTimeMillis());
        document.setLineCount(document.getContent() != null ? document.getContent().split("\n").length : 0);
        
        // Store in repository
        return vectorDbRepository.store(document);
    }
    
    public CodeDocument getDocument(String id) {
        return vectorDbRepository.retrieve(id);
    }
    
    public void updateDocument(CodeDocument document) {
        log.info("Updating document: {}", document.getId());
        
        // Regenerate embeddings for updated content
        if (document.getContent() != null) {
            float[] embeddings = embeddingService.generateCodeEmbedding(document.getContent(), document.getLanguage());
            document.setEmbeddings(embeddings);
        }
        
        // Update metadata
        document.setLastModified(System.currentTimeMillis());
        document.setLineCount(document.getContent() != null ? document.getContent().split("\n").length : 0);
        
        // Update in repository
        vectorDbRepository.update(document);
    }
    
    public void deleteDocument(String id) {
        log.info("Deleting document: {}", id);
        vectorDbRepository.delete(id);
    }
    
    public List<CodeDocument> search(SearchRequest request) {
        log.info("Searching documents with query: {}", request.getQuery());
        
        // Generate embedding for search query
        float[] queryEmbedding = embeddingService.generateEmbedding(request.getQuery());
        
        // Search using repository
        List<CodeDocument> results = vectorDbRepository.findSimilar(queryEmbedding, request.getLimit() * 2, request.getThreshold());
        
        // Apply additional filters and sorting
        return results.stream()
                .filter(doc -> request.getLanguage() == null || request.getLanguage().equals(doc.getLanguage()))
                .filter(doc -> doc.getEmbeddings() != null)
                .filter(doc -> {
                    double similarity = embeddingService.calculateSimilarity(queryEmbedding, doc.getEmbeddings());
                    return similarity >= request.getThreshold();
                })
                .sorted((doc1, doc2) -> {
                    double sim1 = embeddingService.calculateSimilarity(queryEmbedding, doc1.getEmbeddings());
                    double sim2 = embeddingService.calculateSimilarity(queryEmbedding, doc2.getEmbeddings());
                    return Double.compare(sim2, sim1); // Descending order
                })
                .limit(request.getLimit())
                .collect(Collectors.toList());
    }
    
    public List<CodeDocument> searchSimilar(float[] queryEmbedding, int limit, double threshold) {
        log.info("Searching for similar documents with limit: {} and threshold: {}", limit, threshold);
        
        List<CodeDocument> results = vectorDbRepository.findSimilar(queryEmbedding, limit * 2, threshold);
        
        return results.stream()
                .filter(doc -> doc.getEmbeddings() != null)
                .filter(doc -> {
                    double similarity = embeddingService.calculateSimilarity(queryEmbedding, doc.getEmbeddings());
                    return similarity >= threshold;
                })
                .sorted((doc1, doc2) -> {
                    double sim1 = embeddingService.calculateSimilarity(queryEmbedding, doc1.getEmbeddings());
                    double sim2 = embeddingService.calculateSimilarity(queryEmbedding, doc2.getEmbeddings());
                    return Double.compare(sim2, sim1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<CodeDocument> getAllDocuments() {
        return vectorDbRepository.findAll();
    }
    
    public boolean documentExists(String id) {
        return vectorDbRepository.exists(id);
    }
    
    public long getDocumentCount() {
        return vectorDbRepository.count();
    }
}