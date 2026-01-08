package com.epam.rag.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeDocument {
    private String id;
    private String fileName;
    private String filePath;
    private String content;
    private String language;
    private String packageName;
    private String className;
    private long lastModified;
    private int lineCount;
    private float[] embeddings;
    private String summary;
}