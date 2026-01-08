package com.epam.rag.service;

import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@Slf4j
public class EmbeddingService {
    
    private static final int EMBEDDING_DIMENSION = 384; // MiniLM embedding size
    private OrtEnvironment env;
    private OrtSession session;
    private final String modelPath = "models/sentence-transformers-all-MiniLM-L6-v2.onnx";
    
    public EmbeddingService() {
        initializeModel();
    }
    
    private void initializeModel() {
        try {
            env = OrtEnvironment.getEnvironment();
            Path path = Paths.get(modelPath);
            if (Files.exists(path)) {
                session = env.createSession(modelPath, new OrtSession.SessionOptions());
                log.info("ONNX model loaded successfully from: {}", modelPath);
            } else {
                log.warn("ONNX model not found at: {}. Using stub implementation.", modelPath);
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
        long[] tokens = new long[Math.min(words.length, 512)];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = Math.abs(words[i].hashCode()) % 30000;
        }
        return tokens;
    }
    
    private float[] generateStubEmbedding(String text) {
        log.info("Generating stub embedding for text of length: {}", text.length());
        float[] embedding = new float[EMBEDDING_DIMENSION];
        int hash = text.hashCode();
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] = (float) Math.sin(hash + i) * 0.1f;
        }
        return embedding;
    }
    
    public float[] generateCodeEmbedding(String code, String language) {
        log.info("Generating code embedding for {} code", language);
        String processedCode = preprocessCode(code, language);
        return generateEmbedding(processedCode);
    }
    
    private String preprocessCode(String code, String language) {
        return code.replaceAll("//.*", "")
                  .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                  .replaceAll("\\s+", " ")
                  .trim();
    }
    
    public double calculateSimilarity(float[] embedding1, float[] embedding2) {
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
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception e) {
            log.error("Error closing ONNX resources: {}", e.getMessage());
        }
    }
}