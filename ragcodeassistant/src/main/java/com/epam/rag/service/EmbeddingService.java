package com.epam.rag.service;

import ai.onnxruntime.*;
import com.epam.rag.config.EmbeddingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@Slf4j
public class EmbeddingService {
    
    private final EmbeddingConfig embeddingConfig;
    private OrtEnvironment env;
    private OrtSession session;
    
    @Autowired
    public EmbeddingService(EmbeddingConfig embeddingConfig) {
        this.embeddingConfig = embeddingConfig;
        initializeModel();
    }
    
    private void initializeModel() {
        try {
            env = OrtEnvironment.getEnvironment();
            Path path = Paths.get(embeddingConfig.getModelPath());
            if (Files.exists(path)) {
                session = env.createSession(embeddingConfig.getModelPath(), new OrtSession.SessionOptions());
                log.info("ONNX model loaded successfully from: {}", embeddingConfig.getModelPath());
            } else {
                log.warn("ONNX model not found at: {}. Using stub implementation.", embeddingConfig.getModelPath());
            }
        } catch (Exception e) {
            log.error("Failed to initialize ONNX model: {}", e.getMessage());
        }
    }
    
    public float[] generateEmbedding(String text) {
        if (session == null) {
            return generateStubEmbedding(text);
        }
        
        try {
            long[] inputIds = tokenize(text);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, new long[][]{inputIds});
            Map<String, OnnxTensor> inputs = Map.of("input_ids", inputTensor);
            
            OrtSession.Result result = session.run(inputs);
            float[][] embeddings = (float[][]) result.get(0).getValue();
            
            inputTensor.close();
            result.close();
            
            return embeddings[0];
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return generateStubEmbedding(text);
        }
    }
    
    private long[] tokenize(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        long[] tokens = new long[Math.min(words.length, embeddingConfig.getMaxSequenceLength())];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = Math.abs(words[i].hashCode()) % 30000;
        }
        return tokens;
    }
    
    private float[] generateStubEmbedding(String text) {
        log.debug("Generating stub embedding for text of length: {}", text.length());
        float[] embedding = new float[embeddingConfig.getDimension()];
        int hash = text.hashCode();
        for (int i = 0; i < embeddingConfig.getDimension(); i++) {
            embedding[i] = (float) Math.sin(hash + i) * 0.1f;
        }
        return embedding;
    }
    
    public float[] generateCodeEmbedding(String code, String language) {
        log.debug("Generating code embedding for {} code", language);
        String processedCode = preprocessCode(code, language);
        return generateEmbedding(processedCode);
    }
    
    private String preprocessCode(String code, String language) {
        if (code == null) {
            return "";
        }
        
        // Remove comments and normalize whitespace
        String processed = code
                .replaceAll("//.*", "")
                .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                .replaceAll("\\s+", " ")
                .trim();
        
        // Language-specific preprocessing
        if ("java".equalsIgnoreCase(language)) {
            // Remove package and import statements for better semantic matching
            processed = processed.replaceAll("package\\s+[\\w\\.]+;?", "")
                               .replaceAll("import\\s+[\\w\\.\\*]+;?", "")
                               .trim();
        }
        
        return processed;
    }
    
    public double calculateSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null) {
            return 0.0;
        }
        
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have the same dimension");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0.0 ? 0.0 : dotProduct / denominator;
    }
    
    @PreDestroy
    public void close() {
        try {
            if (session != null) {
                session.close();
                log.info("ONNX session closed");
            }
            if (env != null) {
                env.close();
                log.info("ONNX environment closed");
            }
        } catch (Exception e) {
            log.error("Error closing ONNX resources: {}", e.getMessage());
        }
    }
}