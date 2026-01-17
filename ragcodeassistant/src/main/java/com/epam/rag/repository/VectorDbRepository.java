package com.epam.rag.repository;

import com.epam.rag.model.CodeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class VectorDbRepository {
    
    private final Map<String, CodeDocument> documentStore = new ConcurrentHashMap<>();
    
    public String store(CodeDocument document) {
        log.debug("Storing document: {}", document.getId());
        documentStore.put(document.getId(), document);
        return document.getId();
    }
    
    public CodeDocument retrieve(String id) {
        return documentStore.get(id);
    }
    
    public void update(CodeDocument document) {
        documentStore.put(document.getId(), document);
    }
    
    public void delete(String id) {
        documentStore.remove(id);
    }
    
    public List<CodeDocument> findSimilar(float[] queryVector, int limit, double threshold) {
        if (queryVector == null) {
            return documentStore.values().stream().limit(limit).collect(Collectors.toList());
        }
        
        return documentStore.values().stream()
            .filter(doc -> doc.getEmbeddings() != null)
            .map(doc -> new SimilarityResult(doc, cosineSimilarity(queryVector, doc.getEmbeddings())))
            .filter(result -> result.similarity >= threshold)
            .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
            .limit(limit)
            .map(result -> result.document)
            .collect(Collectors.toList());
    }
    
    public List<CodeDocument> findAll() {
        return new ArrayList<>(documentStore.values());
    }
    
    public boolean exists(String id) {
        return documentStore.containsKey(id);
    }
    
    public long count() {
        return documentStore.size();
    }
    
    public void clear() {
        documentStore.clear();
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private static class SimilarityResult {
        final CodeDocument document;
        final double similarity;
        
        SimilarityResult(CodeDocument document, double similarity) {
            this.document = document;
            this.similarity = similarity;
        }
    }
}