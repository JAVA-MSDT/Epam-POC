package com.javamsdt.agent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Component
public class FileSystemTool {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemTool.class);

    private final String outputDirectory;

    public FileSystemTool(@Value("${agent.output-directory:./analysis-output}") String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Tool(description = "Read the contents of a file at the given path")
    public String readFile(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "List all files in a directory and return their paths")
    public String listDirectory(String directoryPath) {
        try {
            return Files.list(Paths.get(directoryPath))
                    .map(Path::toString)
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "Error listing directory: " + e.getMessage();
        }
    }

    @Tool(name = "create_ticket_folder",
            description = "Create a dedicated folder for a Jira ticket to store analysis results and artifacts. Returns the absolute path of the created folder.")
    public String createTicketFolder(String ticketId) {
        try {
            Path folder = Paths.get(outputDirectory, ticketId);
            Files.createDirectories(folder);
            logger.info("Created ticket folder: {}", folder.toAbsolutePath());
            return folder.toAbsolutePath().toString();
        } catch (IOException e) {
            logger.error("Failed to create ticket folder for {}: {}", ticketId, e.getMessage());
            return "Error creating folder: " + e.getMessage();
        }
    }

    public FileSystemTool asFunctionCallback() {
        return this;
    }
}
