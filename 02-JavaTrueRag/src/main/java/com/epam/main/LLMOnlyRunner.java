package com.epam.main;

import com.epam.augmentation.PromptBuilder;
import com.epam.constant.AppConstant;
import com.epam.llm.OllamaClient;

import java.io.File;
import java.nio.file.Files;

/**
 * True LLM-only runner — raw source code + query sent directly to the LLM.
 * No static analysis tools, no knowledge base.
 *
 * Pipeline: code → prompt(query + code) → LLM
 * Compare against:
 *   - StaticAnalysisRunner: tools only, no LLM
 *   - RAGRunner:            code → tools → KB retrieval → LLM
 *
 * Run: mvn exec:java -Dexec.mainClass=com.epam.main.LLMOnlyRunner
 */
@SuppressWarnings("java:S106")
public class LLMOnlyRunner {

    private static final String TEST_FILE = "samples/BadCodeExample.java";

    private static final String[] TEST_QUERIES = {
        "Find and explain all code quality issues",
        "What are the security concerns in this code?",
        "Suggest performance improvements"
    };

    public static void main(String[] args) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("LLM-Only Runner (no tools, no RAG)");
        System.out.println("=".repeat(60));

        File javaFile = new File(TEST_FILE);
        if (!javaFile.exists()) {
            System.err.println("File not found: " + TEST_FILE);
            return;
        }

        String sourceCode = Files.readString(javaFile.toPath());
        OllamaClient llm = new OllamaClient();
        PromptBuilder promptBuilder = new PromptBuilder();

        for (int i = 0; i < TEST_QUERIES.length; i++) {
            System.out.println("\n" + "-".repeat(60));
            System.out.printf("Query %d/%d: %s%n", i + 1, TEST_QUERIES.length, TEST_QUERIES[i]);
            System.out.println("-".repeat(60));

            String prompt = promptBuilder.buildSimplePrompt(TEST_QUERIES[i], sourceCode);

            try {
                String response = llm.generate(prompt);

                System.out.println("\n" + "=".repeat(60));
                System.out.println("LLM FEEDBACK (no tools, no RAG)");
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
}
