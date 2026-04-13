package com.epam.rag.service;

import com.epam.rag.model.CodeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    
    private final VectorDbService vectorDbService;
    private final EmbeddingService embeddingService;
    
    public String addDocument(CodeDocument document) {
        log.info("Adding document through DocumentService: {}", document.getFileName());
        return vectorDbService.addDocument(document);
    }
    
    public CodeDocument getDocument(String id) {
        log.info("Retrieving document: {}", id);
        return vectorDbService.getDocument(id);
    }
    
    public void updateDocument(CodeDocument document) {
        log.info("Updating document through DocumentService: {}", document.getId());
        vectorDbService.updateDocument(document);
    }
    
    public void deleteDocument(String id) {
        log.info("Deleting document through DocumentService: {}", id);
        vectorDbService.deleteDocument(id);
    }
    
    public void reindexDocument(String id) {
        log.info("Reindexing document: {}", id);
        CodeDocument document = vectorDbService.getDocument(id);
        if (document != null && document.getContent() != null) {
            float[] newEmbeddings = embeddingService.generateCodeEmbedding(
                document.getContent(), 
                document.getLanguage()
            );
            document.setEmbeddings(newEmbeddings);
            vectorDbService.updateDocument(document);
        }
    }
    
    public List<CodeDocument> getAllDocuments() {
        log.info("Retrieving all documents");
        return vectorDbService.getAllDocuments();
    }
    
    public void reindexAllDocuments() {
        log.info("Reindexing all documents");
        List<CodeDocument> documents = vectorDbService.getAllDocuments();
        for (CodeDocument document : documents) {
            if (document.getContent() != null) {
                float[] newEmbeddings = embeddingService.generateCodeEmbedding(
                    document.getContent(), 
                    document.getLanguage()
                );
                document.setEmbeddings(newEmbeddings);
                vectorDbService.updateDocument(document);
            }
        }
    }
}