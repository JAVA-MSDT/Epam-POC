package com.javamsdt.aidevworkflow.github;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around the local git CLI for commit, branch, and push operations.
 * <p>
 * Requires git to be installed and available on PATH.
 * All operations run inside the provided projectRootPath.
 */
public class GitClient {

    private final String projectRootPath;

    public GitClient(String projectRootPath) {
        this.projectRootPath = projectRootPath;
    }

    /**
     * Creates a new branch off the current HEAD.
     * Returns the branch name.
     */
    public String createBranch(String branchName) {
        run("git", "checkout", "-b", branchName);
        return branchName;
    }

    /**
     * Stages all modified and new tracked files, then commits with the given message.
     */
    public void commitAll(String message) {
        run("git", "add", "-A");
        run("git", "commit", "-m", message);
    }

    /**
     * Stages only the listed relative paths and commits with the given message.
     * Paths must be relative to the project root (e.g., "src/main/java/Foo.java").
     *
     * @param relativePaths files to stage; must be non-empty
     * @param message       the commit message
     * @throws RuntimeException if git add or commit exits with a non-zero code
     */
    public void commitFiles(List<String> relativePaths, String message) {
        List<String> addCmd = new ArrayList<>();
        addCmd.add("git");
        addCmd.add("add");
        addCmd.addAll(relativePaths);
        run(addCmd.toArray(new String[0]));
        run("git", "commit", "-m", message);
    }

    /**
     * Pushes the current branch to the given remote (typically "origin").
     * Uses --set-upstream on the first push.
     */
    public void push(String remote, String branchName) {
        run("git", "push", "--set-upstream", remote, branchName);
    }

    /**
     * Returns the current branch name.
     */
    public String currentBranch() {
        return runCapture("git", "rev-parse", "--abbrev-ref", "HEAD").trim();
    }

    /**
     * Returns the remote URL for the given remote name.
     */
    public String remoteUrl(String remote) {
        return runCapture("git", "remote", "get-url", remote).trim();
    }

    private void run(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(new File(projectRootPath))
                    .redirectErrorStream(true)
                    .start();
            String output = readOutput(process);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("git command failed (exit " + exit + "): "
                        + List.of(command) + "\n" + output);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to run git command: " + List.of(command), e);
        }
    }

    private String runCapture(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(new File(projectRootPath))
                    .redirectErrorStream(true)
                    .start();
            String output = readOutput(process);
            process.waitFor();
            return output;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to run git command: " + List.of(command), e);
        }
    }

    private String readOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
