package com.epam.analysis;

import com.epam.model.AnalysisFinding;
import com.puppycrawl.tools.checkstyle.api.*;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Integrates Checkstyle static analysis tool to analyze Java code and collect findings.
 * Runs Checkstyle rules against Java files and converts results to AnalysisFinding objects.
 */
public class CheckstyleAnalyzer {

    /**
     * Analyzes a Java file using Checkstyle rules and returns findings.
     * 
     * @param javaFile The Java source file to analyze
     * @param configFile The Checkstyle configuration file containing rules
     * @return List of analysis findings discovered by Checkstyle
     * @throws Exception If analysis fails due to configuration or file issues
     */
    public List<AnalysisFinding> analyze(File javaFile, File configFile) throws Exception {
        List<AnalysisFinding> findings = new ArrayList<>();
        
        // Load Checkstyle configuration
        Configuration config = ConfigurationLoader.loadConfiguration(
            configFile.getAbsolutePath(),
            new PropertiesExpander(System.getProperties()),
            ConfigurationLoader.IgnoredModulesOptions.OMIT
        );
        
        // Create and configure Checkstyle checker
        Checker checker = new Checker();
        checker.setModuleClassLoader(Checker.class.getClassLoader());
        checker.configure(config);

        // Add listener to collect audit events as findings
        checker.addListener(new AuditListener() {
            @Override
            public void auditStarted(AuditEvent event) {}
            
            @Override
            public void auditFinished(AuditEvent event) {}
            
            @Override
            public void fileStarted(AuditEvent event) {}
            
            @Override
            public void fileFinished(AuditEvent event) {}
            
            @Override
            public void addError(AuditEvent event) {
                findings.add(new AnalysisFinding(
                    event.getViolation().getKey(), 
                    event.getFileName() + ":" + event.getLine() + " - " + event.getMessage()
                ));
            }
            
            @Override
            public void addException(AuditEvent event, Throwable throwable) {}
        });

        // Process the Java file
        checker.process(List.of(javaFile));
        checker.destroy();
        
        return findings;
    }
}