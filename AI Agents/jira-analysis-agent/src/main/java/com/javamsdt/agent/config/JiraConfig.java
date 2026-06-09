package com.javamsdt.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Configuration
public class JiraConfig {

    @Bean
    public WebClient jiraWebClient(JiraProperties jiraProperties) {
        WebClient.Builder builder = WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json");

        if (jiraProperties.isConfigured()) {
            String credentials = jiraProperties.getUsername() + ":" + jiraProperties.getApiToken();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
            builder.baseUrl(jiraProperties.getBaseUrl())
                   .defaultHeader("Authorization", "Basic " + encoded);
        }

        return builder.build();
    }
}
