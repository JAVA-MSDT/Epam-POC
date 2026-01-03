/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.epam.ragcodeassistant.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodebaseIndexer {


    private final ElasticsearchClient client;

    @Value("${codebase.path:src/main/java}")
    private String codebasePath;

    @PostConstruct
    public void indexCodebase() {
        try {
            Files.walk(Paths.get(codebasePath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(this::indexFile);
        } catch (IOException e) {
            log.error("Failed to index codebase: {}", e.getMessage());
        }
    }

    private void indexFile(Path path) {
        try {
            String content = Files.readString(path);
            client.index(IndexRequest.of(i -> i
                    .index("codebase")
                    .id(UUID.randomUUID().toString())
                    .document(new CodeDocument(path.toString(), content))
            ));
            log.info("Indexed: {}", path);
        } catch (Exception e) {
            log.error("Failed to index file {}: {}", path, e.getMessage());
        }
    }

    public record CodeDocument(String filename, String content) {}
}
