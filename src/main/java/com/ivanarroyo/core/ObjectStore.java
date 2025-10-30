package com.ivanarroyo.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ObjectStore {
    private final File repoDir;
    
    public ObjectStore(String repoPath) {
        this.repoDir = new File(repoPath);
    }

    public File getRepoDir() {
        return repoDir;
    }

    public File getObjectsDir() {
        return new File(repoDir, "objects");
    }

    public File getRefsDir() {
        return new File(repoDir, "refs");
    }

    public File getObjectFile(String hash) {
        return new File(getObjectsDir(), hash);
    }

    public File getIndexFile() {
        return new File(repoDir, "index");
    }

    public File getHeadFile() {
        return new File(repoDir, "HEAD");
    }

    public File getStashFile() {
        return new File(repoDir, "stash");
    }

    public String getCurrentBranch() throws IOException {
        File head = getHeadFile();
        if (!head.exists()) {
            return "main";
        }
        String content = Files.readString(head.toPath()).trim();
        if (content.startsWith("ref: refs/heads/")) {
            return content.substring(16);
        }
        return null;
    }

    public String getHeadCommit() throws IOException {
        String branch = getCurrentBranch();
        if (branch == null) {
            return Files.readString(getHeadFile().toPath()).trim();
        }
        File branchFile = new File(getRefsDir(), "heads/" + branch);
        if (!branchFile.exists()) {
            return null;
        }
        return Files.readString(branchFile.toPath()).trim();
    }

    public void updateHead(String commitHash) throws IOException {
        String branch = getCurrentBranch();
        if (branch != null) {
            File branchFile = new File(getRefsDir(), "heads/" + branch);
            branchFile.getParentFile().mkdirs();
            Files.writeString(branchFile.toPath(), commitHash);
        } else {
            Files.writeString(getHeadFile().toPath(), commitHash);
        }
    }

    public void setCurrentBranch(String branch) throws IOException {
        Files.writeString(getHeadFile().toPath(), "ref: refs/heads/" + branch);
    }
}
