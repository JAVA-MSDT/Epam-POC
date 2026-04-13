package com.epam.web;

import java.io.IOException;

/**
 * Entry point for the RAG Web Server.
 * Provides a visual web interface for the RAG pipeline.
 * <p>
 * Run with: mvn exec:java -Dexec.mainClass=com.epam.web.WebRunner
 */
public class WebRunner {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        RagWebServer server = new RagWebServer(port);
        server.start();
        System.out.println("RAG Web Server started at http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");
    }
}
