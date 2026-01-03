/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.epam.ragcodeassistant.service;


import org.springframework.stereotype.Service;

@Service
public class CodeReviewService {
    public String reviewCode(String code) {
        if (code.contains("Thread.stop()")) {
            return "Warning: Deprecated API usage detected!";
        }
        if (code.length() > 500) {
            return "Warning: Method is too long, consider refactoring.";
        }
        return "Code review passed.";
    }
}
