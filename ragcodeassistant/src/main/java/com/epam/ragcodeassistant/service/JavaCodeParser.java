/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.epam.ragcodeassistant.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.springframework.stereotype.Service;

@Service
public class JavaCodeParser {
    private final JavaParser javaParser = new JavaParser();

    public String analyzeCode(String code) {
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            CompilationUnit cu = result.getResult().get();
            boolean hasDeprecated = cu.findAll(MethodCallExpr.class)
                    .stream()
                    .anyMatch(m -> m.getNameAsString().equals("stop"));
            if (hasDeprecated) {
                return "Warning: Deprecated API usage detected!";
            }
            return "No deprecated API usage found.";
        } else {
            return "Failed to parse code.";
        }
    }
}
