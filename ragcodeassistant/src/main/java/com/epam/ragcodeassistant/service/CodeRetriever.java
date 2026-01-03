/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.epam.ragcodeassistant.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CodeRetriever {

    private final ElasticsearchClient client;

    public CodeRetriever(ElasticsearchClient client) {
        this.client = client;
    }

    public String searchCode(String query) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("codebase")
                    .query(q -> q
                            .match(m -> m
                                    .field("content")
                                    .query(query)
                            )
                    )
                    .size(5)
            );
            SearchResponse<Object> response = client.search(searchRequest, Object.class);
            if (response.hits().hits().isEmpty()) {
                return "No results found.";
            }
            return response.hits().hits().stream()
                    .map(Hit::source).filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.joining("\n---\n"));
        } catch (IOException e) {
            return "Error searching code: " + e.getMessage();
        }
    }
}