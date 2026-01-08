package com.epam.rag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class QdrantConfig {
    
    @Value("${qdrant.host:localhost}")
    private String qdrantHost;
    
    @Value("${qdrant.port:6333}")
    private int qdrantPort;
    
    @Value("${qdrant.collection:code_documents}")
    private String collectionName;
    
    // Stub implementation - in real scenario, this would create actual Qdrant client
    @Bean
    public String qdrantClient() {
        log.info("Initializing Qdrant client for {}:{}", qdrantHost, qdrantPort);
        log.warn("Using stub Qdrant client. To use real Qdrant, implement actual client configuration.");
        return "stub-qdrant-client";
    }
    
    @Bean
    public String qdrantCollectionName() {
        return collectionName;
    }
}