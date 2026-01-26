package com.epam.generation;

import com.epam.augmentation.PromptBuilder;
import com.epam.llm.OllamaClient;
import com.epam.model.AnalysisFinding;
import com.epam.model.KnowledgeEntry;
import com.epam.retrieval.KnowledgeBaseSearcher;

import java.util.List;

/**
 * RAG (Retrieval-Augmented Generation) pipeline orchestrator.
 * Coordinates the three RAG steps: Retrieval, Augmentation, and Generation.
 */
public class RAGPipeline {
    private final OllamaClient ollamaClient;
    private final PromptBuilder promptBuilder;
    private final KnowledgeBaseSearcher searcher;
    
    /**
     * Creates a RAG pipeline with default Ollama model.
     * 
     * @param indexDir Directory containing the knowledge base index
     */
    public RAGPipeline(String indexDir) {
        this(indexDir, "codellama:7b");
    }
    
    /**
     * Creates a RAG pipeline with specified Ollama model.
     * 
     * @param indexDir Directory containing the knowledge base index
     * @param modelName Ollama model name
     */
    public RAGPipeline(String indexDir, String modelName) {
        this.ollamaClient = new OllamaClient(modelName);
        this.promptBuilder = new PromptBuilder();
        this.searcher = new KnowledgeBaseSearcher(indexDir);
    }
    
    /**
     * Generates feedback for code issues using the complete RAG pipeline.
     * 
     * @param userQuery User's question or intent (e.g., "Find bugs", "Explain issues")
     * @param findings List of code issues detected
     * @param codeSnippet The code being analyzed
     * @return LLM-generated feedback
     * @throws Exception if generation fails
     */
    public String generateFeedback(
        String userQuery,
        List<AnalysisFinding> findings,
        String codeSnippet
    ) throws Exception {
        
        System.out.println("\n=== RAG PIPELINE ===");
        
        // Step 1: RETRIEVAL - Get relevant knowledge entries
        System.out.println("📚 Step 1: Retrieving relevant knowledge...");
        List<KnowledgeEntry> knowledgeEntries = retrieveKnowledge(findings);
        System.out.println("   Found " + knowledgeEntries.size() + " relevant knowledge entries");
        
        // Step 2: AUGMENTATION - Build prompt with context
        System.out.println("🔧 Step 2: Building prompt with context...");
        String prompt = promptBuilder.buildPrompt(userQuery, findings, knowledgeEntries, codeSnippet);
        
        // Step 3: GENERATION - LLM generates response
        System.out.println("🤖 Step 3: Generating response with LLM...");
        String response = ollamaClient.generate(prompt);
        
        System.out.println("=== RAG COMPLETE ===\n");
        
        return response;
    }
    
    /**
     * Generates a response for a general code question without static analysis.
     * 
     * @param userQuery User's question
     * @param codeSnippet Code to analyze
     * @return LLM-generated response
     */
    public String answerQuestion(String userQuery, String codeSnippet) {
        System.out.println("\n=== RAG QUERY MODE ===");
        
        // Build simple prompt
        String prompt = promptBuilder.buildSimplePrompt(userQuery, codeSnippet);
        
        // Generate response
        String response = ollamaClient.generate(prompt);
        
        System.out.println("=== RAG COMPLETE ===\n");
        
        return response;
    }
    
    /**
     * Retrieves relevant knowledge entries based on findings.
     * This is the RETRIEVAL step of RAG.
     */
    private List<KnowledgeEntry> retrieveKnowledge(List<AnalysisFinding> findings) throws Exception {
        // Search for knowledge entries related to each finding
        for (AnalysisFinding finding : findings) {
            List<KnowledgeEntry> entries = searcher.search(finding.issue(), 3);
            if (!entries.isEmpty()) {
                return entries; // Return first match for simplicity
            }
        }
        
        // If no specific matches, return general entries
        return searcher.search("best practices", 2);
    }
    
    /**
     * Checks if Ollama is available.
     * 
     * @return true if Ollama is running and responding
     */
    public boolean isOllamaAvailable() {
        return ollamaClient.isAvailable();
    }
}
