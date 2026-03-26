package com.epam.main;

import com.epam.analysis.CheckstyleAnalyzer;
import com.epam.analysis.PMDAnalyzer;
import com.epam.constant.AppConstant;
import com.epam.generation.RAGPipeline;
import com.epam.llm.OllamaClient;
import com.epam.model.AnalysisFinding;
import com.epam.retrieval.KnowledgeBaseIndexer;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RAG runner — static analysis + KB retrieval + LLM.
 * Mirrors LLMOnlyRunner structure; the only difference is RAGPipeline
 * retrieves KB entries from Lucene before building the prompt.
 *
 * Compare against:
 *   - StaticAnalysisRunner: no LLM at all
 *   - LLMOnlyRunner:        LLM with findings, no KB
 *
 * Run: mvn exec:java -Dexec.mainClass=com.epam.main.RAGRunner
 */
@SuppressWarnings("java:S106")
public class RAGRunner {

    private static final String TEST_FILE = "samples/KnowledgeBaseTestExample.java";
    private static final String CHECKSTYLE_CONFIG = "src/main/resources/checkstyle.xml";
    private static final String PMD_RULESET = "src/main/resources/pmd-ruleset.xml";
    private static final String KB_DIR = "src/main/resources/knowledgebase";
    private static final String INDEX_DIR = "index";

    private static final String[] TEST_QUERIES = {
        "Find and explain all code quality issues",
        "What are the security concerns in this code?",
        "Suggest performance improvements"
    };

    public static void main(String[] args) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("RAG Runner (static analysis + KB retrieval + LLM)");
        System.out.println("=".repeat(60));

        // Index KB once before the query loop
        KnowledgeBaseIndexer indexer = new KnowledgeBaseIndexer();
        indexer.indexKnowledgeBase(KB_DIR, INDEX_DIR);
        System.out.println("Knowledge base indexed.");

        File javaFile = new File(TEST_FILE);
        if (!javaFile.exists()) {
            System.err.println("File not found: " + TEST_FILE);
            return;
        }

        String sourceCode = Files.readString(javaFile.toPath());
        List<AnalysisFinding> findings = runStaticAnalysis(
                javaFile, new File(CHECKSTYLE_CONFIG), new File(PMD_RULESET));

        RAGPipeline rag = new RAGPipeline(INDEX_DIR);

        for (int i = 0; i < TEST_QUERIES.length; i++) {
            System.out.println("\n" + "-".repeat(60));
            System.out.printf("Query %d/%d: %s%n", i + 1, TEST_QUERIES.length, TEST_QUERIES[i]);
            System.out.println("-".repeat(60));

            try {
                String feedback = rag.generateFeedback(TEST_QUERIES[i], findings, sourceCode);

                System.out.println("\n" + "=".repeat(60));
                System.out.println("RAG FEEDBACK");
                System.out.println("=".repeat(60));
                System.out.println(feedback);
                System.out.println("=".repeat(60));

            } catch (OllamaClient.OllamaException e) {
                System.err.println("Error generating response: " + e.getMessage());
                System.err.println("Ensure Ollama is running: ollama serve");
                System.err.println("Model must be downloaded: ollama pull " + AppConstant.OLLAMA_MODEL);
            }

            if (i < TEST_QUERIES.length - 1) {
                System.out.println("\nWaiting 2 seconds before next query...");
                Thread.sleep(2000);
            }
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("RAG run complete.");
        System.out.println("=".repeat(60));
    }

    private static List<AnalysisFinding> runStaticAnalysis(
            File javaFile, File checkstyleConfig, File pmdRuleset) throws Exception {

        System.out.println("Running static analysis...");
        CheckstyleAnalyzer checkstyle = new CheckstyleAnalyzer();
        List<AnalysisFinding> checkstyleFindings = checkstyle.analyze(javaFile, checkstyleConfig);
        System.out.println("Checkstyle: " + checkstyleFindings.size() + " issues.");

        PMDAnalyzer pmd = new PMDAnalyzer();
        List<AnalysisFinding> pmdFindings = pmd.analyze(javaFile, pmdRuleset);
        System.out.println("PMD: " + pmdFindings.size() + " issues.");

        List<AnalysisFinding> all = new ArrayList<>(checkstyleFindings);
        all.addAll(pmdFindings);
        System.out.println("Total: " + all.size() + " findings.");
        return all;
    }
}
