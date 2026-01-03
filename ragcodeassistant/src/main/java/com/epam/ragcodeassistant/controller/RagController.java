/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.epam.ragcodeassistant.controller;


import com.epam.ragcodeassistant.model.CodeRequest;
import com.epam.ragcodeassistant.model.SearchRequest;
import com.epam.ragcodeassistant.service.CodeRetriever;
import com.epam.ragcodeassistant.service.CodeReviewService;
import com.epam.ragcodeassistant.service.JavaCodeParser;
import com.epam.ragcodeassistant.service.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final CodeRetriever codeRetriever;
    private final JavaCodeParser javaCodeParser;
    private final LlmService llmService;
    private final CodeReviewService codeReviewService;

    @PostMapping("/search")
    public String search(@RequestBody SearchRequest request) {
        return codeRetriever.searchCode(request.getQuery());
    }

    @PostMapping("/analyze")
    public String analyze(@RequestBody CodeRequest request) {
        return javaCodeParser.analyzeCode(request.getCode());
    }

    @PostMapping("/refactor")
    public String refactor(@RequestBody CodeRequest request) {
        return llmService.generateSuggestion(request.getPrompt(), request.getCode());
    }

    @PostMapping("/review")
    public String review(@RequestBody CodeRequest request) {
        return codeReviewService.reviewCode(request.getCode());
    }
}