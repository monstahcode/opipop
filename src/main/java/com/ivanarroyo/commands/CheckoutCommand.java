package com.ivanarroyo.commands;

import com.ivanarroyo.core.*;
import com.ivanarroyo.util.HashUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class CheckoutCommand implements Command {
    private final ObjectStore store;
    private final File workingDir;

    public CheckoutCommand(ObjectStore store, File workingDir) {
        this.store = store;
        this.workingDir = workingDir;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: opipop checkout <branch>");
            return;
        }

        String branchName = args[0];
        File branchFile = new File(store.getRefsDir(), "heads/" + branchName);

        if (!branchFile.exists()) {
            System.out.println("Branch '" + branchName + "' does not exist");
            return;
        }

        String currentBranch = store.getCurrentBranch();
        if (branchName.equals(currentBranch)) {
            System.out.println("Already on '" + branchName + "'");
            return;
        }

        // Check for uncommitted changes
        if (hasUncommittedChanges()) {
            System.out.println("Error: You have uncommitted changes. Commit or stash them first.");
            return;
        }

        // Get commit hash for the branch
        String commitHash = Files.readString(branchFile.toPath()).trim();

        // Load the tree from commit
        File commitFile = store.getObjectFile(commitHash);
        byte[] commitData = Files.readAllBytes(commitFile.toPath());
        Commit commit = Commit.deserialize(commitData);

        File treeFile = store.getObjectFile(commit.getTreeHash());
        byte[] treeData = Files.readAllBytes(treeFile.toPath());
        Tree tree = Tree.deserialize(treeData);

        updateWorkingDirectorySafe(workingDir, tree.getEntries());

        // Update index
        Index index = new Index(store.getIndexFile());
        index.clear();
        for (Map.Entry<String, String> entry : tree.getEntries().entrySet()) {
            index.add(entry.getKey(), entry.getValue());
        }
        index.save();

        // Update HEAD
        store.setCurrentBranch(branchName);

        System.out.println("Switched to branch '" + branchName + "'");
    }

    private boolean hasUncommittedChanges() throws IOException {
        Index index = new Index(store.getIndexFile());

        for (Map.Entry<String, String> entry : index.getEntries().entrySet()) {
            File file = new File(entry.getKey());
            if (!file.exists()) {
                return true;
            }
            byte[] content = Files.readAllBytes(file.toPath());
            String hash = HashUtils.sha1(content);
            if (!hash.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private void updateWorkingDirectorySafe(File workingDir, Map<String, String> treeEntries) throws IOException {
        for (Map.Entry<String, String> entry : treeEntries.entrySet()) {
            File targetFile = new File(workingDir, entry.getKey());
            targetFile.getParentFile().mkdirs();
            byte[] content = Files.readAllBytes(store.getObjectFile(entry.getValue()).toPath());
            Files.write(targetFile.toPath(), content);
        }

        deleteUntrackedFiles(workingDir, treeEntries);
    }

    private void deleteUntrackedFiles(File dir, Map<String, String> treeEntries) throws IOException {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                deleteUntrackedFiles(f, treeEntries);
                if (f.list().length == 0) f.delete();
            } else {
                String relativePath = workingDir.toPath().relativize(f.toPath()).toString();
                if (!treeEntries.containsKey(relativePath)) {
                    f.delete();
                }
            }
        }
    }
}
