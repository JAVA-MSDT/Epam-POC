package com.javamsdt.aidevworkflow.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Creates GitHub pull requests and fetches PR review comments via the GitHub REST API v3.
 *
 * Required environment variables:
 *   GITHUB_TOKEN — personal access token or fine-grained token with repo scope
 *   GITHUB_REPO  — owner/repo, e.g. "my-org/my-service"
 */
public class GitHubClient {

    private static final String API_BASE = "https://api.github.com";

    private final String repo;
    private final String token;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitHubClient(String repo, String token) {
        this.repo = repo;
        this.token = token;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Convenience factory — reads credentials from environment variables.
     */
    public static GitHubClient fromEnv() {
        String repo = requireEnv("GITHUB_REPO");
        String token = requireEnv("GITHUB_TOKEN");
        return new GitHubClient(repo, token);
    }

    /**
     * Creates a pull request and returns its URL.
     *
     * @param title      PR title
     * @param body       PR description (markdown)
     * @param head       source branch name
     * @param base       target branch name (e.g. "main")
     */
    public String createPullRequest(String title, String body, String head, String base) {
        Map<String, Object> payload = Map.of(
                "title", title,
                "body", body,
                "head", head,
                "base", base
        );
        String responseBody = post("/repos/" + repo + "/pulls", payload);
        try {
            Map<?, ?> response = objectMapper.readValue(responseBody, Map.class);
            Object url = response.get("html_url");
            return url != null ? url.toString() : "";
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse PR creation response: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches all review comments on the given PR number.
     * Returns a formatted string ready for injection into an LLM prompt.
     */
    public String fetchPrComments(int prNumber) {
        String responseBody = get("/repos/" + repo + "/pulls/" + prNumber + "/comments");
        try {
            List<?> comments = objectMapper.readValue(responseBody, List.class);
            if (comments.isEmpty()) return "(no review comments yet)";

            StringBuilder sb = new StringBuilder();
            for (Object c : comments) {
                if (c instanceof Map<?, ?> comment) {
                    String user = extractString(comment, "user", "login");
                    String path = comment.getOrDefault("path", "").toString();
                    String commentBody = comment.getOrDefault("body", "").toString();
                    sb.append("- **").append(user).append("** on `").append(path).append("`:\n");
                    sb.append("  ").append(commentBody).append("\n");
                }
            }
            return sb.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse PR comments: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the PR number from a GitHub PR URL.
     * e.g. "https://github.com/org/repo/pull/42" → 42
     */
    public static int prNumberFromUrl(String prUrl) {
        String[] parts = prUrl.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private String get(String path) {
        Request request = new Request.Builder()
                .url(API_BASE + path)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        return execute(request);
    }

    private String post(String path, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(json,
                    MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(API_BASE + path)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .post(requestBody)
                    .build();
            return execute(request);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    private String execute(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("GitHub API error " + response.code() + ": " + body);
            }
            return body;
        } catch (IOException e) {
            throw new RuntimeException("GitHub API call failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractString(Map<?, ?> map, String key, String nestedKey) {
        Object value = map.get(key);
        if (value instanceof Map<?, ?> nested) return nested.getOrDefault(nestedKey, "").toString();
        return "";
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is not set.");
        }
        return value;
    }
}
