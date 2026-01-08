package com.epam.rag.service;

import com.epam.rag.model.CodeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CodeAnalysisService {
    
    public Map<String, Object> analyzeCode(String code, String language) {
        log.info("Analyzing {} code", language);
        
        Map<String, Object> analysis = new HashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        if ("java".equalsIgnoreCase(language)) {
            issues.addAll(analyzeJavaCode(code));
        } else {
            issues.add("Language " + language + " analysis not fully implemented yet");
        }
        
        // Add deprecated API detection
        issues.addAll(detectDeprecatedApis(code));
        
        analysis.put("issues", issues);
        analysis.put("suggestions", suggestions);
        analysis.put("language", language);
        analysis.put("codeLength", code.length());
        analysis.put("lineCount", code.split("\n").length);
        
        return analysis;
    }
    
    private List<String> analyzeJavaCode(String code) {
        List<String> issues = new ArrayList<>();
        
        // Basic pattern-based analysis
        if (code.contains("System.out.println")) {
            issues.add("Consider using a proper logging framework instead of System.out.println");
        }
        
        if (code.contains("catch (Exception e) {}")) {
            issues.add("Empty catch blocks should be avoided");
        }
        
        if (code.matches(".*public\\s+class\\s+\\w+\\s*\\{[\\s\\S]*?public\\s+static\\s+void\\s+main[\\s\\S]*?")) {
            issues.add("Consider separating main method from business logic");
        }
        
        // Check for long lines
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > 120) {
                issues.add("Line " + (i + 1) + " is too long (" + lines[i].length() + " characters)");
            }
        }
        
        return issues;
    }
    
    public List<String> detectDeprecatedApis(String code) {
        log.info("Detecting deprecated APIs");
        
        List<String> deprecatedUsages = new ArrayList<>();
        
        if (code.contains("new Date()")) {
            deprecatedUsages.add("Usage of deprecated 'new Date()' constructor. Consider using LocalDateTime or Instant.");
        }
        
        if (code.contains("StringBuffer")) {
            deprecatedUsages.add("Consider using StringBuilder instead of StringBuffer for better performance in single-threaded scenarios.");
        }
        
        if (code.contains("Vector")) {
            deprecatedUsages.add("Vector is legacy. Consider using ArrayList or other modern collections.");
        }
        
        if (code.contains("Hashtable")) {
            deprecatedUsages.add("Hashtable is legacy. Consider using HashMap or ConcurrentHashMap.");
        }
        
        return deprecatedUsages;
    }
}