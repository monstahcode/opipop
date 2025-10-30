package com.ivanarroyo.commands;

import com.ivanarroyo.core.Index;
import com.ivanarroyo.core.ObjectStore;
import com.ivanarroyo.core.Tree;
import com.ivanarroyo.core.Commit;
import com.ivanarroyo.util.HashUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StatusCommand implements Command {
    private final ObjectStore store;

    public StatusCommand(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void execute(String[] args) throws Exception {
        String branch = store.getCurrentBranch();
        System.out.println("On branch " + branch);
        System.out.println();

        Index index = new Index(store.getIndexFile());
        Map<String, String> indexEntries = index.getEntries();

        Map<String, String> headTree = getHeadTree();
        Map<String, String> workingDir = getWorkingDirectory();

        Set<String> stagedForCommit = new HashSet<>();
        Set<String> modified = new HashSet<>();
        Set<String> deleted = new HashSet<>();
        Set<String> untracked = new HashSet<>();

        // Check staged changes
        for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
            String path = entry.getKey();
            String indexHash = entry.getValue();
            String headHash = headTree.get(path);

            if (!indexHash.equals(headHash)) {
                stagedForCommit.add(path);
            }
        }

        // Check working directory changes
        for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
            String path = entry.getKey();
            String indexHash = entry.getValue();
            String workHash = workingDir.get(path);

            if (workHash == null) {
                deleted.add(path);
            } else if (!workHash.equals(indexHash)) {
                modified.add(path);
            }
        }

        // Check untracked files
        for (String path : workingDir.keySet()) {
            if (!indexEntries.containsKey(path)) {
                untracked.add(path);
            }
        }

        if (!stagedForCommit.isEmpty()) {
            System.out.println("Changes to be committed:");
            for (String path : stagedForCommit) {
                if (headTree.containsKey(path)) {
                    System.out.println("  modified: " + path);
                } else {
                    System.out.println("  new file: " + path);
                }
            }
            System.out.println();
        }

        if (!modified.isEmpty() || !deleted.isEmpty()) {
            System.out.println("Changes not staged for commit:");
            for (String path : modified) {
                System.out.println("  modified: " + path);
            }
            for (String path : deleted) {
                System.out.println("  deleted: " + path);
            }
            System.out.println();
        }

        if (!untracked.isEmpty()) {
            System.out.println("Untracked files:");
            for (String path : untracked) {
                System.out.println("  " + path);
            }
            System.out.println();
        }

        if (stagedForCommit.isEmpty() && modified.isEmpty() && deleted.isEmpty() && untracked.isEmpty()) {
            System.out.println("nothing to commit, working tree clean");
        }
    }

    private Map<String, String> getHeadTree() throws IOException {
        String commitHash = store.getHeadCommit();
        if (commitHash == null) {
            return new HashMap<>();
        }

        File commitFile = store.getObjectFile(commitHash);
        if (!commitFile.exists()) {
            return new HashMap<>();
        }

        byte[] commitData = Files.readAllBytes(commitFile.toPath());
        Commit commit = Commit.deserialize(commitData);

        File treeFile = store.getObjectFile(commit.getTreeHash());
        if (!treeFile.exists()) {
            return new HashMap<>();
        }

        byte[] treeData = Files.readAllBytes(treeFile.toPath());
        Tree tree = Tree.deserialize(treeData);
        return tree.getEntries();
    }

    private Map<String, String> getWorkingDirectory() throws IOException {
        Map<String, String> files = new HashMap<>();
        File currentDir = new File(".");
        scanDirectory(currentDir, "", files);
        return files;
    }

    private void scanDirectory(File dir, String prefix, Map<String, String> files) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            String name = child.getName();
            if (name.equals(".opipop") || name.startsWith(".")) continue;

            String path = prefix.isEmpty() ? name : prefix + "/" + name;

            if (child.isDirectory()) {
                scanDirectory(child, path, files);
            } else if (child.isFile()) {
                byte[] content = Files.readAllBytes(child.toPath());
                String hash = HashUtils.sha1(content);
                files.put(path, hash);
            }
        }
    }
}
