package com.epam;

import com.epam.analysis.CheckstyleAnalyzer;
import com.epam.analysis.PMDAnalyzer;
import com.epam.feedback.FeedbackGenerator;
import com.epam.model.AnalysisFinding;
import com.epam.model.KnowledgeEntry;
import com.epam.retrieval.KnowledgeBaseIndexer;
import com.epam.retrieval.KnowledgeBaseSearcher;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for the Java RAG Code Review system.
 * Orchestrates the complete pipeline: static analysis, knowledge retrieval, and feedback generation.
 */
public class Main {

    public static void main(String[] args) throws Exception {
            runTestSamples();
    }
    
    /**
     * Runs test samples when no arguments are provided.
     */
    private static void runTestSamples() throws Exception {
        System.out.println("Running Java RAG Code Review with test samples...");
        System.out.println("=================================================");
        
        String[] testFiles = {
            "samples/KnowledgeBaseTestExample.java",
//            "samples/BadCodeExample.java",
//            "samples/AnotherBadExample.java",
//            "samples/GoodCodeExample.java",
//            "samples/TestClass.java"
        };
        
        String checkstyleConfig = "src/main/resources/checkstyle.xml";
        String pmdRuleset = "src/main/resources/pmd-ruleset.xml";
        String kbDir = "src/main/resources/knowledgebase";
        String indexDir = "index";

        // Step 1: Index knowledge base (RAG - Retrieval preparation)
        // This part for demo, but usually you will have all the indexes in some embedded DB, documents, etc.
        indexKnowledgeBase(kbDir, indexDir);



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

                // Step 2: Run static analysis (Checkstyle + PMD) LLM like.
                List<AnalysisFinding> findings = runStaticAnalysis(javaFile, new File(checkstyleConfig), new File(pmdRuleset));

                // Step 2.5: Run KB-driven analysis (RAG proactive detection) Retrieval like
                List<AnalysisFinding> kbFindings = runKBAnalysis(javaFile, indexDir);
                findings.addAll(kbFindings);

                // Step 3: Generate feedback using RAG (Retrieval + Generation)
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
    
    /**
     * Runs KB-driven analysis by searching for knowledge base patterns directly in the code.
     * 
     * @param javaFile The Java source file to analyze
     * @param indexDir Directory containing the Lucene index
     * @return List of findings based on knowledge base patterns
     * @throws Exception If analysis fails
     */
    private static List<AnalysisFinding> runKBAnalysis(File javaFile, String indexDir) throws Exception {
        System.out.println("Running KB-driven analysis...");
        String sourceCode = Files.readString(javaFile.toPath());
        KnowledgeBaseSearcher searcher = new KnowledgeBaseSearcher(indexDir);
        List<AnalysisFinding> findings = searcher.searchInCode(sourceCode, javaFile.getName());
        System.out.println("KB analysis found " + findings.size() + " issues.");
        findings.forEach(f -> System.out.println("KB-driven - " + f.issue()));
        return findings;
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
        
        if (findings.isEmpty()) {
            System.out.println("No issues found - code looks good!");
            return;
        }

        List<AnalysisFinding> distinctFindings = findings.stream()
                .distinct()
                .toList();

        KnowledgeBaseSearcher searcher = new KnowledgeBaseSearcher(indexDir);
        FeedbackGenerator feedbackGen = new FeedbackGenerator();
        
        int knowledgeMatches = 0;
        
        for (AnalysisFinding finding : distinctFindings) {
            System.out.println("\n--- Processing finding: " + finding.issue() + " ---");
            
            // RAG: Retrieve relevant knowledge entries
            List<KnowledgeEntry> entries = searcher.search(finding.issue(), 1);
            
            // RAG: Generate feedback combining finding + knowledge
            if (!entries.isEmpty()) {
                knowledgeMatches++;
                KnowledgeEntry entry = entries.getFirst();
                System.out.println("✓ Knowledge base match found for: " + finding.issue());
                String feedback = feedbackGen.generateFeedback(finding, entry);
                System.out.println(feedback);
            } else {
                System.out.println("✗ No knowledge base match for: " + finding.issue());
                
                // Get available topics for context
                List<String> availableTopics = searcher.getAllTopics();
                String basicFeedback = feedbackGen.generateBasicFeedback(finding, availableTopics);
                System.out.println(basicFeedback);
            }
        }
        
        System.out.println("\n=== RAG PIPELINE SUMMARY ===");
        System.out.println("Total distinctFindings: " + distinctFindings.size());
        System.out.println("Knowledge base matches: " + knowledgeMatches);
        System.out.println("Match rate: " + (!distinctFindings.isEmpty() ? (knowledgeMatches * 100 / distinctFindings.size()) : 0) + "%");
    }

}