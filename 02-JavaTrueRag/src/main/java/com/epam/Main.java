package com.epam;

import com.epam.analysis.CheckstyleAnalyzer;
import com.epam.analysis.PMDAnalyzer;
import com.epam.generation.RAGPipeline;
import com.epam.model.AnalysisFinding;
import com.epam.retrieval.KnowledgeBaseIndexer;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for the Java RAG Code Review system.
 * TRUE RAG implementation using Ollama for LLM-powered feedback generation.
 */
@SuppressWarnings("java:S106")
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            runTestSamples();
        } else if (args.length == 2) {
            // RAG Mode: java -jar app.jar <file> <query>
            runRAGMode(args[0], args[1]);
        } else {
            printUsage();
        }
    }
    
    private static void printUsage() {
        System.out.println("""
            Usage:
            
            Test Mode (no arguments):
              mvn exec:java
            
            RAG Mode (file + query):
              mvn exec:java -Dexec.args="<file> '<query>'"
            
            Examples:
              mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Find bugs'"
              mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Explain the issues'"
              mvn exec:java -Dexec.args="samples/KnowledgeBaseTestExample.java 'Suggest improvements'"
            """);
    }
    
    /**
     * RAG Mode: Analyzes a file with a user query using a TRUE RAG pipeline.
     */
    private static void runRAGMode(String filePath, String userQuery) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🚀 Java RAG Code Review System - TRUE RAG Mode");
        System.out.println("=".repeat(60));
        
        String kbDir = "src/main/resources/knowledgebase";
        String indexDir = "index";
        String checkstyleConfig = "src/main/resources/checkstyle.xml";
        String pmdRuleset = "src/main/resources/pmd-ruleset.xml";
        
        // Index knowledge base
        indexKnowledgeBase(kbDir, indexDir);
        
        // Load file
        File javaFile = new File(filePath);
        if (!javaFile.exists()) {
            System.err.println("❌ File not found: " + filePath);
            return;
        }
        
        String sourceCode = Files.readString(javaFile.toPath());
        
        System.out.println("\n📄 File: " + filePath);
        System.out.println("❓ Query: " + userQuery);
        System.out.println();
        
        // Run static analysis
        List<AnalysisFinding> findings = runStaticAnalysis(javaFile, 
            new File(checkstyleConfig), new File(pmdRuleset));
        
        if (findings.isEmpty()) {
            System.out.println("✅ No issues detected by static analysis.");
        }
        
        // TRUE RAG: Generate feedback using LLM
        RAGPipeline ragPipeline = new RAGPipeline(indexDir);
        
        try {
            String feedback = ragPipeline.generateFeedback(userQuery, findings, sourceCode);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("💡 RAG-GENERATED FEEDBACK");
            System.out.println("=".repeat(60));
            System.out.println(feedback);
            System.out.println("=".repeat(60));
            
        } catch (Exception e) {
            System.err.println("\n❌ Error generating feedback: " + e.getMessage());
            System.err.println("\nPlease ensure:");
            System.err.println("  1. Ollama is running: ollama serve");
            System.err.println("  2. Model is downloaded: ollama pull codellama:7b");
            System.err.println("  3. Test with: ollama run codellama:7b 'hello'");
        }
    }
    
    /**
     * Test Mode: Runs predefined test samples.
     */
    private static void runTestSamples() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🧪 Java RAG Code Review System - Test Mode");
        System.out.println("=".repeat(60));
        
        String testFile = "samples/KnowledgeBaseTestExample.java";
        String[] testQueries = {
            "Find and explain all code quality issues",
            "What are the security concerns in this code?",
            "Suggest performance improvements"
        };
        
        System.out.println("\nRunning " + testQueries.length + " test queries on: " + testFile);
        System.out.println();
        
        for (int i = 0; i < testQueries.length; i++) {
            System.out.println("\n" + "─".repeat(60));
            System.out.println("Test " + (i + 1) + "/" + testQueries.length);
            System.out.println("─".repeat(60));
            
            try {
                runRAGMode(testFile, testQueries[i]);
                
                if (i < testQueries.length - 1) {
                    System.out.println("\n⏳ Waiting 2 seconds before next query...");
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                System.err.println("❌ Test failed: " + e.getMessage());
            throw new InterruptedException("Interrupted");
            }
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ All tests completed!");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Indexes the knowledge base for fast retrieval during feedback generation.
     * 
     * @param kbDir Directory containing JSON knowledge base files
     * @param indexDir Directory where Lucene index will be stored
     * @throws Exception If indexing fails
     */
    private static void indexKnowledgeBase(String kbDir, String indexDir) throws Exception {
        System.out.println("Indexing knowledge base...");
        File kbDirectory = new File(kbDir);
        File[] jsonFiles = kbDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (jsonFiles != null && jsonFiles.length > 0) {
            System.out.println("Found " + jsonFiles.length + " knowledge base files:");
            for (File file : jsonFiles) {
                System.out.println("  - " + file.getName());
            }
        } else {
            System.out.println("WARNING: No JSON files found in knowledge base directory: " + kbDir);
        }
        
        KnowledgeBaseIndexer indexer = new KnowledgeBaseIndexer();
        indexer.indexKnowledgeBase(kbDir, indexDir);
        System.out.println("Knowledge base indexed successfully.");
    }
    
    /**
     * Runs static analysis using both Checkstyle and PMD tools.
     * 
     * @param javaFile The Java source file to analyze
     * @param checkstyleConfig Checkstyle configuration file
     * @param pmdRuleset PMD ruleset file
     * @return Combined list of findings from both tools
     * @throws Exception If analysis fails
     */
    private static List<AnalysisFinding> runStaticAnalysis(File javaFile, File checkstyleConfig, File pmdRuleset) throws Exception {
        System.out.println("Running static analysis...");

        // Run Checkstyle analysis
        CheckstyleAnalyzer checkstyle = new CheckstyleAnalyzer();
        List<AnalysisFinding> checkstyleFindings = checkstyle.analyze(javaFile, checkstyleConfig);
        List<AnalysisFinding> allFindings = new ArrayList<>(checkstyleFindings);
        System.out.println("Checkstyle found " + checkstyleFindings.size() + " issues.");
        
        // Run PMD analysis
        PMDAnalyzer pmd = new PMDAnalyzer();
        List<AnalysisFinding> pmdFindings = pmd.analyze(javaFile, pmdRuleset);
        allFindings.addAll(pmdFindings);
        System.out.println("PMD found " + pmdFindings.size() + " issues.");
        
        System.out.println("Total findings: " + allFindings.size());
        return allFindings;
    }
    
}