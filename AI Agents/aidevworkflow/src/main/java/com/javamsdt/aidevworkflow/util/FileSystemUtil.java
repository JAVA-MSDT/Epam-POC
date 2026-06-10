package com.javamsdt.aidevworkflow.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public final class FileSystemUtil {

    private FileSystemUtil() {
    }

    /**
     * Creates a report folder under the given base directory, named after the ticket ID.
     * Returns the absolute path of the created folder.
     * <p>
     * Example: createReportFolder("/tmp/reports", "PROJ-123")
     * → "/tmp/reports/PROJ-123"
     */
    public static String createReportFolder(String baseDir, String ticketId) {
        String safeName = ticketId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        Path folder = Paths.get(baseDir, safeName);
        try {
            Files.createDirectories(folder);
            return folder.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create report folder: " + folder, e);
        }
    }

    /**
     * Writes content to a file, creating parent directories if needed.
     * Overwrites any existing file at the same path.
     */
    public static void writeFile(String filePath, String content) {
        Path path = Paths.get(filePath);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    /**
     * Reads and returns the content of a file as a UTF-8 string.
     */
    public static String readFile(String filePath) {
        try {
            return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    /**
     * Recursively collects all source files under rootPath matching the given extensions.
     * Returns a list of [relative/path/to/File.java]\n[file content] blocks,
     * suitable for injecting into an LLM prompt.
     * <p>
     * Example: readSourceFiles("/my/project/src", List.of(".java", ".xml"))
     */
    public static String readSourceFiles(String rootPath, List<String> extensions, int maxFiles) {
        Path root = Paths.get(rootPath);
        List<String> blocks = new ArrayList<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (blocks.size() >= maxFiles) return FileVisitResult.TERMINATE;
                    String name = file.getFileName().toString();
                    boolean matches = extensions.stream().anyMatch(name::endsWith);
                    if (matches) {
                        try {
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            String relative = root.relativize(file).toString().replace('\\', '/');
                            blocks.add("### " + relative + "\n```\n" + content + "\n```");
                        } catch (IOException ignored) {
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan source files under: " + rootPath, e);
        }

        return String.join("\n\n", blocks);
    }
}
