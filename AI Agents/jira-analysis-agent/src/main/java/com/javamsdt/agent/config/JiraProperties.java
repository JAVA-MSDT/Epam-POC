package com.javamsdt.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jira")
public class JiraProperties {

    private String baseUrl = "";
    private String username = "";
    private String apiToken = "";

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && username != null && !username.isBlank()
                && apiToken != null && !apiToken.isBlank();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
}
