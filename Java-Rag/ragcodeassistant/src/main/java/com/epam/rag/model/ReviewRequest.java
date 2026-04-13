package com.epam.rag.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    private String code;
    private String language;
    private String fileName;
    private String filePath;
    private boolean includeStaticAnalysis = true;
    private boolean includeLlmReview = true;
    private boolean includeDeprecatedApiCheck = true;
}