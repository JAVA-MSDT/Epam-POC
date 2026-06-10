package com.javamsdt.aidevworkflow.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Fetches Jira ticket data via the Jira REST API v3.
 * <p>
 * Required environment variables:
 * JIRA_BASE_URL   — e.g. https://your-org.atlassian.net
 * JIRA_USER_EMAIL — the account email used for basic auth
 * JIRA_API_TOKEN  — API token generated at id.atlassian.com
 */
public class JiraClient {

    private final String baseUrl;
    private final String authHeader;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JiraClient(String baseUrl, String userEmail, String apiToken) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        String credentials = userEmail + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Convenience factory — reads credentials from environment variables.
     */
    public static JiraClient fromEnv() {
        String baseUrl = requireEnv("JIRA_BASE_URL");
        String email = requireEnv("JIRA_USER_EMAIL");
        String token = requireEnv("JIRA_API_TOKEN");
        return new JiraClient(baseUrl, email, token);
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is not set.");
        }
        return value;
    }

    /**
     * Fetches a Jira issue and returns a structured {@link JiraTicket}.
     *
     * @param issueKey e.g. "PROJ-123"
     */
    public JiraTicket fetchTicket(String issueKey) {
        String url = baseUrl + "/rest/api/3/issue/" + issueKey
                + "?fields=summary,description,issuetype,status,assignee,reporter,priority,labels,comment";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("Jira API error " + response.code() + " for " + issueKey + ": " + body);
            }
            return parseTicket(issueKey, body);
        } catch (IOException e) {
            throw new RuntimeException("Jira API call failed for " + issueKey + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private JiraTicket parseTicket(String issueKey, String json) throws IOException {
        Map<?, ?> root = objectMapper.readValue(json, Map.class);
        Map<?, ?> fields = (Map<?, ?>) root.get("fields");

        String summary = stringOrEmpty(fields.get("summary"));
        String description = extractDescription(fields.get("description"));
        String issueType = extractNestedName(fields.get("issuetype"));
        String status = extractNestedName(fields.get("status"));
        String assignee = extractDisplayName(fields.get("assignee"));
        String reporter = extractDisplayName(fields.get("reporter"));
        String priority = extractNestedName(fields.get("priority"));

        List<?> labelsList = fields.get("labels") instanceof List<?> l ? l : List.of();
        String labels = String.join(", ", labelsList.stream().map(Object::toString).toList());

        String comments = extractComments(fields.get("comment"));

        return new JiraTicket(issueKey, summary, description, issueType,
                status, assignee, reporter, priority, labels, comments);
    }

    private String extractDescription(Object descObj) {
        if (descObj == null) return "";
        // Jira ADF (Atlassian Document Format) — extract plain text from content nodes
        if (descObj instanceof Map<?, ?> doc) {
            return extractAdfText(doc);
        }
        return descObj.toString();
    }

    private String extractAdfText(Map<?, ?> node) {
        StringBuilder sb = new StringBuilder();
        Object text = node.get("text");
        if (text != null) sb.append(text);
        Object content = node.get("content");
        if (content instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> child) {
                    String childText = extractAdfText(child);
                    if (!childText.isBlank()) sb.append(childText).append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private String extractComments(Object commentObj) {
        if (!(commentObj instanceof Map<?, ?> commentMap)) return "";
        Object comments = commentMap.get("comments");
        if (!(comments instanceof List<?> list)) return "";
        StringBuilder sb = new StringBuilder();
        for (Object c : list) {
            if (c instanceof Map<?, ?> comment) {
                String author = extractDisplayName(comment.get("author"));
                String body = extractDescription(comment.get("body"));
                sb.append("- ").append(author).append(": ").append(body).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String extractNestedName(Object obj) {
        if (obj instanceof Map<?, ?> m) return stringOrEmpty(m.get("name"));
        return "";
    }

    private String extractDisplayName(Object obj) {
        if (obj instanceof Map<?, ?> m) return stringOrEmpty(m.get("displayName"));
        return "Unassigned";
    }

    private String stringOrEmpty(Object obj) {
        return obj != null ? obj.toString() : "";
    }
}
