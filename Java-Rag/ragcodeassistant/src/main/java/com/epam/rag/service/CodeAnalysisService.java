package com.epam.rag.service;

import com.epam.rag.model.CodeDocument;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CodeAnalysisService {
    
    private final JavaParser javaParser = new JavaParser();
    
    public Map<String, Object> analyzeCode(String code, String language) {
        log.info("Analyzing {} code", language);
        
        Map<String, Object> analysis = new HashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        if ("java".equalsIgnoreCase(language)) {
            issues.addAll(analyzeJavaCodeWithParser(code));
            issues.addAll(analyzeJavaCodePatterns(code));
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
    
    private List<String> analyzeJavaCodeWithParser(String code) {
        List<String> issues = new ArrayList<>();
        
        try {
            CompilationUnit cu = javaParser.parse(code).getResult().orElse(null);
            if (cu == null) {
                issues.add("Failed to parse Java code - syntax errors may be present");
                return issues;
            }
            
            // Analyze using JavaParser AST
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration md, Void arg) {
                    super.visit(md, arg);
                    
                    // Check method length
                    if (md.getBody().isPresent()) {
                        int lineCount = md.getEnd().get().line - md.getBegin().get().line + 1;
                        if (lineCount > 50) {
                            issues.add("Method '" + md.getNameAsString() + "' is too long (" + lineCount + " lines). Consider breaking it down.");
                        }
                    }
                    
                    // Check parameter count
                    if (md.getParameters().size() > 5) {
                        issues.add("Method '" + md.getNameAsString() + "' has too many parameters (" + md.getParameters().size() + "). Consider using a parameter object.");
                    }
                }
                
                @Override
                public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                    super.visit(cid, arg);
                    
                    // Check class size
                    int methodCount = cid.getMethods().size();
                    if (methodCount > 20) {
                        issues.add("Class '" + cid.getNameAsString() + "' has too many methods (" + methodCount + "). Consider splitting responsibilities.");
                    }
                }
                
                @Override
                public void visit(MethodCallExpr mce, Void arg) {
                    super.visit(mce, arg);
                    
                    // Check for System.out.println usage
                    if ("System".equals(mce.getScope().map(Object::toString).orElse("")) && 
                        "out".equals(mce.getName().getIdentifier()) ||
                        "println".equals(mce.getName().getIdentifier())) {
                        issues.add("Avoid using System.out.println. Use a proper logging framework instead.");
                    }
                }
            }, null);
            
        } catch (Exception e) {
            log.error("Error parsing Java code: {}", e.getMessage());
            issues.add("Failed to analyze code structure: " + e.getMessage());
        }
        
        return issues;
    }
    
    private List<String> analyzeJavaCodePatterns(String code) {
        List<String> issues = new ArrayList<>();
        
        // Basic pattern-based analysis
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
        
        // Check for magic numbers
        if (code.matches(".*\\b\\d{2,}\\b.*")) {
            issues.add("Consider extracting magic numbers into named constants");
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
        
        if (code.contains("@SuppressWarnings")) {
            deprecatedUsages.add("Review @SuppressWarnings usage - ensure warnings are properly addressed.");
        }
        
        return deprecatedUsages;
    }
    
    public Map<String, Object> analyzeCodeComplexity(String code) {
        Map<String, Object> complexity = new HashMap<>();
        
        try {
            CompilationUnit cu = javaParser.parse(code).getResult().orElse(null);
            if (cu != null) {
                int[] methodCount = {0};
                int[] classCount = {0};
                int[] totalComplexity = {0};
                
                cu.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(MethodDeclaration md, Void arg) {
                        super.visit(md, arg);
                        methodCount[0]++;
                        // Simple cyclomatic complexity estimation
                        String methodBody = md.toString();
                        int complexity = 1; // Base complexity
                        complexity += countOccurrences(methodBody, "if");
                        complexity += countOccurrences(methodBody, "while");
                        complexity += countOccurrences(methodBody, "for");
                        complexity += countOccurrences(methodBody, "case");
                        complexity += countOccurrences(methodBody, "catch");
                        totalComplexity[0] += complexity;
                    }
                    
                    @Override
                    public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                        super.visit(cid, arg);
                        classCount[0]++;
                    }
                }, null);
                
                complexity.put("methodCount", methodCount[0]);
                complexity.put("classCount", classCount[0]);
                complexity.put("averageComplexity", methodCount[0] > 0 ? totalComplexity[0] / methodCount[0] : 0);
                complexity.put("totalComplexity", totalComplexity[0]);
            }
        } catch (Exception e) {
            log.error("Error analyzing code complexity: {}", e.getMessage());
        }
        
        return complexity;
    }
    
    private int countOccurrences(String text, String pattern) {
        return text.split("\\b" + pattern + "\\b").length - 1;
    }
}