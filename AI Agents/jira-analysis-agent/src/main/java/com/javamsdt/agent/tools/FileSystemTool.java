package com.javamsdt.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Component
public class FileSystemTool {

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

    /** Returns this instance as a Spring AI tool object (objects with @Tool methods are accepted directly). */
    public FileSystemTool asFunctionCallback() {
        return this;
    }
}
