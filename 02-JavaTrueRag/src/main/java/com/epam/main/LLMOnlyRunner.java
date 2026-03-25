package com.epam.main;

import com.epam.analysis.CheckstyleAnalyzer;
import com.epam.analysis.PMDAnalyzer;
import com.epam.augmentation.PromptBuilder;
import com.epam.constant.AppConstant;
import com.epam.llm.OllamaClient;
import com.epam.model.AnalysisFinding;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runner that uses Ollama LLM with static analysis findings but WITHOUT RAG
 * (no knowledge base retrieval, no Lucene index).
 *
 * Pipeline: static analysis → prompt(query + code + findings) → LLM
 * Compare against:
 *   - StaticAnalysisRunner: no LLM at all
 *   - Main (RAG mode):       adds KB retrieval between analysis and LLM
 *
 * Run: mvn exec:java -Dexec.mainClass=com.epam.main.LLMOnlyRunner
 */
@SuppressWarnings("java:S106")
public class LLMOnlyRunner {

    private static final String TEST_FILE = "samples/KnowledgeBaseTestExample.java";
    private static final String CHECKSTYLE_CONFIG = "src/main/resources/checkstyle.xml";
    private static final String PMD_RULESET = "src/main/resources/pmd-ruleset.xml";

    private static final String[] TEST_QUERIES = {
        "Find and explain all code quality issues",
        "What are the security concerns in this code?",
        "Suggest performance improvements"
    };

    public static void main(String[] args) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("LLM-Only Runner (static analysis + LLM, no RAG)");
        System.out.println("=".repeat(60));

        File javaFile = new File(TEST_FILE);
        if (!javaFile.exists()) {
            System.err.println("File not found: " + TEST_FILE);
            return;
        }

        String sourceCode = Files.readString(javaFile.toPath());
        List<AnalysisFinding> findings = runStaticAnalysis(
                javaFile, new File(CHECKSTYLE_CONFIG), new File(PMD_RULESET));

        OllamaClient llm = new OllamaClient();
        PromptBuilder promptBuilder = new PromptBuilder();

        for (int i = 0; i < TEST_QUERIES.length; i++) {
            System.out.println("\n" + "-".repeat(60));
            System.out.printf("Query %d/%d: %s%n", i + 1, TEST_QUERIES.length, TEST_QUERIES[i]);
            System.out.println("-".repeat(60));

            // Augment with findings but NO knowledge base entries (empty list)
            String prompt = promptBuilder.buildPrompt(
                    TEST_QUERIES[i],
                    findings,
                    Collections.emptyList(),
                    sourceCode
            );

            try {
                String response = llm.generate(prompt);

                System.out.println("\n" + "=".repeat(60));
                System.out.println("LLM FEEDBACK (no RAG context)");
                System.out.println("=".repeat(60));
                System.out.println(response);
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
        System.out.println("LLM-only run complete.");
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
