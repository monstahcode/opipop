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

        // Update working directory (SIN BORRAR - solo actualiza archivos tracked)
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

    /**
     * Actualiza el working directory de forma SEGURA
     * Solo modifica archivos que están en el índice (tracked)
     * NO borra archivos no rastreados
     */
    private void updateWorkingDirectorySafe(File workingDir, Map<String, String> treeEntries) throws IOException {
        // Obtener archivos actualmente en el índice
        Index currentIndex = new Index(store.getIndexFile());
        Map<String, String> currentFiles = currentIndex.getEntries();

        // 1. Actualizar/crear archivos de la nueva rama
        for (Map.Entry<String, String> entry : treeEntries.entrySet()) {
            String path = entry.getKey();
            String hash = entry.getValue();

            File objectFile = store.getObjectFile(hash);
            byte[] content = Files.readAllBytes(objectFile.toPath());

            File targetFile = new File(workingDir, path);
            targetFile.getParentFile().mkdirs();
            Files.write(targetFile.toPath(), content);
        }

        // 2. Solo borrar archivos que estaban tracked y ya no están
        for (String oldPath : currentFiles.keySet()) {
            if (!treeEntries.containsKey(oldPath)) {
                File oldFile = new File(oldPath);
                if (oldFile.exists()) {
                    oldFile.delete();
                    System.out.println("Removed: " + oldPath);
                }
            }
        }
    }
}
