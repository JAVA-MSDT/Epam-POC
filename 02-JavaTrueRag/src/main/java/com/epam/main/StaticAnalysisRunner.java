package com.epam.main;

import com.epam.analysis.CheckstyleAnalyzer;
import com.epam.analysis.PMDAnalyzer;
import com.epam.model.AnalysisFinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runner that performs ONLY static analysis (Checkstyle + PMD) with no LLM or RAG.
 * Use this to establish a baseline of raw tool findings for comparison against
 * LLMOnlyRunner and the full RAG pipeline in Main.
 *
 * Run: mvn exec:java -Dexec.mainClass=com.epam.main.StaticAnalysisRunner
 */
@SuppressWarnings("java:S106")
public class StaticAnalysisRunner {

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
        System.out.println("Static Analysis Runner (no LLM, no RAG)");
        System.out.println("=".repeat(60));

        File javaFile = new File(TEST_FILE);
        if (!javaFile.exists()) {
            System.err.println("File not found: " + TEST_FILE);
            return;
        }

        File checkstyleConfig = new File(CHECKSTYLE_CONFIG);
        File pmdRuleset = new File(PMD_RULESET);

        List<AnalysisFinding> findings = runStaticAnalysis(javaFile, checkstyleConfig, pmdRuleset);

        // Print findings once — they are the same regardless of query
        System.out.println("\n" + "=".repeat(60));
        System.out.println("STATIC ANALYSIS FINDINGS");
        System.out.println("=".repeat(60));

        if (findings.isEmpty()) {
            System.out.println("No issues detected.");
        } else {
            for (int i = 0; i < findings.size(); i++) {
                AnalysisFinding f = findings.get(i);
                System.out.printf("%d. %s%n   %s%n", i + 1, f.issue(), f.details());
            }
        }

        // Show how findings map to each query (no LLM — user interprets them)
        System.out.println("\n" + "=".repeat(60));
        System.out.println("QUERY MAPPING (raw findings, no LLM interpretation)");
        System.out.println("=".repeat(60));

        for (int i = 0; i < TEST_QUERIES.length; i++) {
            System.out.println("\n" + "-".repeat(60));
            System.out.printf("Query %d/%d: %s%n", i + 1, TEST_QUERIES.length, TEST_QUERIES[i]);
            System.out.println("-".repeat(60));
            System.out.println("Total findings available: " + findings.size());
            System.out.println("(No LLM — see findings above for raw output)");
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Static analysis complete.");
        System.out.println("=".repeat(60));
    }

    private static List<AnalysisFinding> runStaticAnalysis(
            File javaFile, File checkstyleConfig, File pmdRuleset) throws Exception {

        System.out.println("Running Checkstyle...");
        CheckstyleAnalyzer checkstyle = new CheckstyleAnalyzer();
        List<AnalysisFinding> checkstyleFindings = checkstyle.analyze(javaFile, checkstyleConfig);
        System.out.println("Checkstyle found " + checkstyleFindings.size() + " issues.");

        System.out.println("Running PMD...");
        PMDAnalyzer pmd = new PMDAnalyzer();
        List<AnalysisFinding> pmdFindings = pmd.analyze(javaFile, pmdRuleset);
        System.out.println("PMD found " + pmdFindings.size() + " issues.");

        List<AnalysisFinding> all = new ArrayList<>(checkstyleFindings);
        all.addAll(pmdFindings);
        System.out.println("Total findings: " + all.size());
        return all;
    }
}
