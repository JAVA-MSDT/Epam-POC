package com.epam.web;

import com.epam.analysis.CheckstyleAnalyzer;
import com.epam.analysis.PMDAnalyzer;
import com.epam.augmentation.PromptBuilder;
import com.epam.generation.RAGPipeline;
import com.epam.llm.OllamaClient;
import com.epam.model.AnalysisFinding;
import com.epam.retrieval.KnowledgeBaseIndexer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * HTTP server for the RAG web interface.
 * Uses the JDK built-in HttpServer — no additional dependencies are required.
 * <p>
 * Routes:
 *   GET / → serves index.html from classpath (/web/index.html)
 *   POST /api/run → runs the selected pipeline and returns JSON result
 */
public class RagWebServer {

    private static final String KB_DIR      = "src/main/resources/knowledgebase";
    private static final String INDEX_DIR   = "index";
    private static final String CHECKSTYLE  = "src/main/resources/checkstyle.xml";
    private static final String PMD_RULES   = "src/main/resources/pmd-ruleset.xml";

    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();

    public RagWebServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handlePage);
        server.createContext("/api/run", this::handleApiRun);
        server.createContext("/static", this::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    // ── Static page handler ────────────────────────────────────────────────

    private void handlePage(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.equals("/")) {
            sendText(exchange, 404, "text/plain", "Not Found");
            return;
        }
        try (exchange; InputStream stream = getClass().getResourceAsStream("/web/index.html")) {
            if (stream == null) {
                sendText(exchange, 404, "text/plain", "index.html not found in classpath");
                return;
            }
            byte[] content = stream.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
        }
    }

    // ── Static handler ────────────────────────────────────────────────────────
    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath(); // e.g. /static/icon/robot.png
        String resourcePath = path.replaceFirst("/static", ""); // → /icon/robot.png

        try (InputStream stream = getClass().getResourceAsStream("/static" + resourcePath)) {
            if (stream == null) {
                sendText(exchange, 404, "text/plain", "Not Found");
                return;
            }

            byte[] content = stream.readAllBytes();

            // Guess MIME type
            String mime = Files.probeContentType(Path.of(resourcePath));
            if (mime == null) mime = "application/octet-stream";

            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
        }
    }


    // ── API handler ────────────────────────────────────────────────────────

    private void handleApiRun(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode req  = mapper.readTree(body);

            String runner = req.path("runner").asText("rag");
            String code   = req.path("code").asText("").trim();
            String query  = req.path("query").asText("Review this code for issues").trim();
            String kb     = req.path("kb").asText("").trim();

            if (code.isEmpty()) {
                sendJson(exchange, 400, Map.of("error", "No code provided"));
                return;
            }

            String result = dispatch(runner, code, query, kb);
            sendJson(exchange, 200, Map.of("result", result, "status", "success"));

        } catch (OllamaClient.OllamaException e) {
            sendJson(exchange, 503, Map.of(
                "error", "Ollama is not available. Ensure Ollama is running (ollama serve) " +
                          "and the model is pulled.",
                "status", "error"
            ));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage() != null ? e.getMessage() : "Unexpected error",
                                           "status", "error"));
        }
    }

    // ── Pipeline dispatch ──────────────────────────────────────────────────

    private String dispatch(String runner, String code, String query, String kb) throws Exception {
        return switch (runner) {
            case "llm-only"          -> runLlmOnly(code, query);
            case "static-analysis"   -> runStaticAnalysis(code);
            default                  -> runRag(code, query, kb);
        };
    }

    private String runLlmOnly(String code, String query) {
        OllamaClient client = new OllamaClient();
        PromptBuilder pb    = new PromptBuilder();
        return client.generate(pb.buildSimplePrompt(query, code));
    }

    private String runStaticAnalysis(String code) throws Exception {
        File temp = writeTempJava(code);
        try {
            List<AnalysisFinding> findings = collectFindings(temp);
            if (findings.isEmpty()) return "No issues found by static analysis.";
            StringBuilder sb = new StringBuilder("Static Analysis Results:\n\n");
            findings.forEach(f -> sb.append("• [").append(f.issue()).append("]\n  ")
                                    .append(f.details()).append("\n\n"));
            return sb.toString().trim();
        } finally {
            temp.delete();
        }
    }

    private String runRag(String code, String query, String kb) throws Exception {
        File temp         = writeTempJava(code);
        File tempKbDir    = null;
        File tempIndexDir = null;
        String indexDir   = INDEX_DIR;

        try {
            // Use custom KB if provided, otherwise ensure the default index exists
            if (!kb.isEmpty()) {
                tempKbDir    = Files.createTempDirectory("rag-kb").toFile();
                tempIndexDir = Files.createTempDirectory("rag-idx").toFile();
                writeKbEntries(kb, tempKbDir);
                new KnowledgeBaseIndexer().indexKnowledgeBase(tempKbDir.getPath(), tempIndexDir.getPath());
                indexDir = tempIndexDir.getPath();
            } else {
                File defaultIndex = new File(INDEX_DIR);
                if (!defaultIndex.exists()) {
                    new KnowledgeBaseIndexer().indexKnowledgeBase(KB_DIR, INDEX_DIR);
                }
            }

            List<AnalysisFinding> findings = collectFindings(temp);
            return new RAGPipeline(indexDir).generateFeedback(query, findings, code);

        } finally {
            temp.delete();
            if (tempKbDir    != null) deleteDir(tempKbDir);
            if (tempIndexDir != null) deleteDir(tempIndexDir);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private List<AnalysisFinding> collectFindings(File javaFile) throws Exception {
        List<AnalysisFinding> findings = new ArrayList<>();
        findings.addAll(new CheckstyleAnalyzer().analyze(javaFile, new File(CHECKSTYLE)));
        findings.addAll(new PMDAnalyzer().analyze(javaFile, new File(PMD_RULES)));
        return findings;
    }

    private File writeTempJava(String code) throws IOException {
        File temp = File.createTempFile("rag-code-", ".java");
        Files.writeString(temp.toPath(), code);
        return temp;
    }

    /**
     * Writes KB JSON to individual files in tempKbDir.
     * Supports both a single JSON object and a JSON array of objects.
     */
    private void writeKbEntries(String kb, File dir) throws Exception {
        JsonNode node = mapper.readTree(kb);
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                mapper.writeValue(new File(dir, "entry_" + i + ".json"), node.get(i));
            }
        } else {
            mapper.writeValue(new File(dir, "entry_0.json"), node);
        }
    }

    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) deleteDir(f);
            }
        }
        dir.delete();
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendText(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void sendJson(HttpExchange exchange, int status, Map<String, ?> payload) throws IOException {
        sendText(exchange, status, "application/json; charset=UTF-8", mapper.writeValueAsString(payload));
    }
}
