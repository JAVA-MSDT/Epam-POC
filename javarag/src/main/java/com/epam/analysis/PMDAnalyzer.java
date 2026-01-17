package com.epam.analysis;

import com.epam.model.AnalysisFinding;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.lang.LanguageRegistry;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Integrates PMD static analysis tool to analyze Java code and collect findings.
 * Runs PMD rules against Java files and converts results to AnalysisFinding objects.
 */
public class PMDAnalyzer {

    /**
     * Analyzes a Java file using PMD rules and returns findings.
     * 
     * @param javaFile The Java source file to analyze
     * @param rulesetFile The PMD ruleset file containing analysis rules
     * @return List of analysis findings discovered by PMD
     * @throws Exception If analysis fails due to configuration or file issues
     */
    public List<AnalysisFinding> analyze(File javaFile, File rulesetFile) throws Exception {
        List<AnalysisFinding> findings = new ArrayList<>();
        
        // Configure PMD
        PMDConfiguration configuration = new PMDConfiguration();
        configuration.addInputPath(Path.of(javaFile.getAbsolutePath()));
        configuration.addRuleSet(rulesetFile.getAbsolutePath());
        configuration.setDefaultLanguageVersion(
            LanguageRegistry.findLanguageByTerseName("java").getDefaultVersion()
        );

        // Run PMD analysis
        try (PmdAnalysis pmd = PmdAnalysis.create(configuration)) {
            Report report = pmd.performAnalysis();
            
            // Convert PMD violations to AnalysisFinding objects
            for (RuleViolation violation : report.getViolations()) {
                findings.add(new AnalysisFinding(
                    violation.getRule().getName(),
                    violation.getFilename() + ":" + violation.getBeginLine() + " - " + violation.getDescription()
                ));
            }
        }

        return findings;
    }
}