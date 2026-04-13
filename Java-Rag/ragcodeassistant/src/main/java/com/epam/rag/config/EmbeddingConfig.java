package com.epam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {
    
    private String modelPath = "models/sentence-transformers-all-MiniLM-L6-v2.onnx";
    private int dimension = 384;
    private int maxSequenceLength = 512;
    
    public String getModelPath() {
        return modelPath;
    }
    
    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }
    
    public int getDimension() {
        return dimension;
    }
    
    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
    
    public int getMaxSequenceLength() {
        return maxSequenceLength;
    }
    
    public void setMaxSequenceLength(int maxSequenceLength) {
        this.maxSequenceLength = maxSequenceLength;
    }
}