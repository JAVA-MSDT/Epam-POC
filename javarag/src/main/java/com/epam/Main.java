package com.epam;

import com.epam.analysis.CheckstyleAnalyzer;
import com.epam.analysis.PMDAnalyzer;
import com.epam.model.AnalysisFinding;
import com.epam.model.KnowledgeEntry;
import com.epam.retrieval.KnowledgeBaseIndexer;
import com.epam.retrieval.KnowledgeBaseSearcher;
import com.epam.feedback.FeedbackGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for the Java RAG Code Review system.
 * Orchestrates the complete pipeline: static analysis, knowledge retrieval, and feedback generation.
 */
public class Main {
    
    /**
     * Main method that runs the complete RAG pipeline for code review.
     * 
     * @param args Command line arguments: [JavaFile] [CheckstyleConfig] [PMDRuleset] [KnowledgeBaseDir] [IndexDir]
     */
    public static void main(String[] args) {
        // If no arguments provided, run with default test samples
        if (args.length == 0) {
            runTestSamples();
            return;
        }
        
        if (args.length < 5) {
            printUsage();
            return;
        }
        
        try {
            // Parse command line arguments
            File javaFile = new File(args[0]);
            File checkstyleConfig = new File(args[1]);
            File pmdRuleset = new File(args[2]);
            String kbDir = args[3];
            String indexDir = args[4];
            
            // Validate input files
            validateInputs(javaFile, checkstyleConfig, pmdRuleset, kbDir);
            
            System.out.println("Starting Java RAG Code Review Pipeline...");
            
            // Step 1: Index knowledge base (RAG - Retrieval preparation)
            indexKnowledgeBase(kbDir, indexDir);
            
            // Step 2: Run static analysis (Checkstyle + PMD)
            List<AnalysisFinding> findings = runStaticAnalysis(javaFile, checkstyleConfig, pmdRuleset);
            
            // Step 3: Generate feedback using RAG (Retrieval + Generation)
            generateFeedback(findings, indexDir);
            
            System.out.println("\nCode review completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during code review: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Runs test samples when no arguments are provided.
     */
    private static void runTestSamples() {
        System.out.println("Running Java RAG Code Review with test samples...");
        System.out.println("=================================================");
        
        String[] testFiles = {
            "samples/BadCodeExample.java",
            "samples/AnotherBadExample.java", 
            "samples/GoodCodeExample.java"
        };
        
        String checkstyleConfig = "src/main/resources/checkstyle.xml";
        String pmdRuleset = "src/main/resources/pmd-ruleset.xml";
        String kbDir = "src/main/resources/knowledgebase";
        String indexDir = "index";
        
        for (String testFile : testFiles) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Testing: " + testFile);
            System.out.println("=".repeat(50));
            
            try {
                File javaFile = new File(testFile);
                if (!javaFile.exists()) {
                    System.out.println("Test file not found: " + testFile);
                    continue;
                }
                
                // Index knowledge base
                indexKnowledgeBase(kbDir, indexDir);
                
                // Run analysis
                List<AnalysisFinding> findings = runStaticAnalysis(javaFile, new File(checkstyleConfig), new File(pmdRuleset));
                
                // Generate feedback
                generateFeedback(findings, indexDir);
                
            } catch (Exception e) {
                System.err.println("Error analyzing " + testFile + ": " + e.getMessage());
            }
        }
        
        System.out.println("\nAll test samples completed!");
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
        List<AnalysisFinding> allFindings = new ArrayList<>();
        
        // Run Checkstyle analysis
        CheckstyleAnalyzer checkstyle = new CheckstyleAnalyzer();
        List<AnalysisFinding> checkstyleFindings = checkstyle.analyze(javaFile, checkstyleConfig);
        allFindings.addAll(checkstyleFindings);
        System.out.println("Checkstyle found " + checkstyleFindings.size() + " issues.");
        
        // Run PMD analysis
        PMDAnalyzer pmd = new PMDAnalyzer();
        List<AnalysisFinding> pmdFindings = pmd.analyze(javaFile, pmdRuleset);
        allFindings.addAll(pmdFindings);
        System.out.println("PMD found " + pmdFindings.size() + " issues.");
        
        System.out.println("Total findings: " + allFindings.size());
        return allFindings;
    }
    
    /**
     * Generates actionable feedback by combining findings with knowledge base entries.
     * 
     * @param findings List of static analysis findings
     * @param indexDir Directory containing the Lucene index
     * @throws Exception If feedback generation fails
     */
    private static void generateFeedback(List<AnalysisFinding> findings, String indexDir) throws Exception {
        System.out.println("\nGenerating feedback using RAG pipeline...");
        
        KnowledgeBaseSearcher searcher = new KnowledgeBaseSearcher(indexDir);
        FeedbackGenerator feedbackGen = new FeedbackGenerator();
        
        for (AnalysisFinding finding : findings) {
            // RAG: Retrieve relevant knowledge entries
            List<KnowledgeEntry> entries = searcher.search(finding.getIssue(), 1);
            
            // RAG: Generate feedback combining finding + knowledge
            if (!entries.isEmpty()) {
                KnowledgeEntry entry = entries.get(0);
                String feedback = feedbackGen.generateFeedback(finding, entry);
                System.out.println(feedback);
            } else {
                String basicFeedback = feedbackGen.generateBasicFeedback(finding);
                System.out.println(basicFeedback);
            }
        }
    }
    
    /**
     * Validates that all required input files and directories exist.
     * 
     * @param javaFile Java source file to analyze
     * @param checkstyleConfig Checkstyle configuration file
     * @param pmdRuleset PMD ruleset file
     * @param kbDir Knowledge base directory
     * @throws IllegalArgumentException If any required input is missing
     */
    private static void validateInputs(File javaFile, File checkstyleConfig, File pmdRuleset, String kbDir) {
        if (!javaFile.exists()) {
            throw new IllegalArgumentException("Java file not found: " + javaFile.getAbsolutePath());
        }
        if (!checkstyleConfig.exists()) {
            throw new IllegalArgumentException("Checkstyle config not found: " + checkstyleConfig.getAbsolutePath());
        }
        if (!pmdRuleset.exists()) {
            throw new IllegalArgumentException("PMD ruleset not found: " + pmdRuleset.getAbsolutePath());
        }
        if (!new File(kbDir).exists()) {
            throw new IllegalArgumentException("Knowledge base directory not found: " + kbDir);
        }
    }
    
    /**
     * Prints usage instructions for the application.
     */
    private static void printUsage() {
        System.out.println("Java RAG Code Review System");
        System.out.println("Usage: java com.epam.Main [JavaFile] [CheckstyleConfig] [PMDRuleset] [KnowledgeBaseDir] [IndexDir]");
        System.out.println();
        System.out.println("Run without arguments to test with sample files:");
        System.out.println("  java com.epam.Main");
        System.out.println();
        System.out.println("Or specify custom files:");
        System.out.println("Arguments:");
        System.out.println("  JavaFile         - Path to Java source file to analyze");
        System.out.println("  CheckstyleConfig - Path to Checkstyle configuration XML file");
        System.out.println("  PMDRuleset      - Path to PMD ruleset XML file");
        System.out.println("  KnowledgeBaseDir - Directory containing JSON knowledge base files");
        System.out.println("  IndexDir        - Directory for Lucene index (will be created if not exists)");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -jar javarag.jar src/main/java/Test.java src/main/resources/checkstyle.xml src/main/resources/pmd-ruleset.xml src/main/resources/knowledgebase index");
    }
}