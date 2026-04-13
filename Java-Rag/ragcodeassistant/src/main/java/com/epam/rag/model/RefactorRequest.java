package com.epam.rag.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefactorRequest {
    private String code;
    private String language;
    private String fileName;
    private String filePath;
    private String context;
    private boolean includeStaticAnalysis = true;
    private boolean includeLlmSuggestions = true;
    private boolean findSimilarPatterns = true;
}