package com.epam.analysis;

import com.epam.model.AnalysisFinding;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
     */
    public List<AnalysisFinding> analyze(File javaFile, File rulesetFile) {
        List<AnalysisFinding> findings = new ArrayList<>();
        
        try {
            PMDConfiguration configuration = new PMDConfiguration();
            configuration.addInputPath(Path.of(javaFile.getAbsolutePath()));
            configuration.addRuleSet(rulesetFile.getAbsolutePath());
            configuration.setDefaultLanguageVersion(
                Objects.requireNonNull(LanguageRegistry.PMD.getLanguageById("java")).getDefaultVersion()
            );
            
            try (PmdAnalysis pmd = PmdAnalysis.create(configuration)) {
                Report report = pmd.performAnalysisAndCollectReport();
                
                report.getViolations().forEach(violation -> findings.add(new AnalysisFinding(
                    violation.getRule().getName(),
                    javaFile.getName() + ":" + violation.getBeginLine() + " - " + violation.getDescription()
                )));
            }
        } catch (Exception e) {
            findings.add(new AnalysisFinding(
                "PMD Analysis Error", 
                "Could not analyze " + javaFile.getName() + ": " + e.getMessage()
            ));
        }

        return findings;
    }
}