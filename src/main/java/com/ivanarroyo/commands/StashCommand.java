package com.ivanarroyo.commands;

import com.ivanarroyo.core.Index;
import com.ivanarroyo.core.ObjectStore;
import com.ivanarroyo.util.HashUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StashCommand implements Command {
    private final ObjectStore store;

    public StashCommand(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            stashChanges();
        } else {
            String subcommand = args[0];
            switch (subcommand) {
                case "pop":
                    popStash();
                    break;
                case "list":
                    listStash();
                    break;
                case "clear":
                    clearStash();
                    break;
                default:
                    System.out.println("Unknown stash command: " + subcommand);
                    System.out.println("Usage: opipop stash [pop|list|clear]");
            }
        }
    }

    private void stashChanges() throws IOException {
        Index index = new Index(store.getIndexFile());
        Map<String, String> indexEntries = index.getEntries();

        if (indexEntries.isEmpty()) {
            System.out.println("No changes to stash");
            return;
        }

        // Check for modified files
        List<StashEntry> modifiedFiles = new ArrayList<>();
        for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
            String path = entry.getKey();
            File file = new File(path);
            
            if (!file.exists()) {
                continue;
            }

            byte[] content = Files.readAllBytes(file.toPath());
            String currentHash = HashUtils.sha1(content);

            if (!currentHash.equals(entry.getValue())) {
                modifiedFiles.add(new StashEntry(path, currentHash, content));
            }
        }

        if (modifiedFiles.isEmpty()) {
            System.out.println("No changes to stash");
            return;
        }

        // Save stash
        List<StashEntry> existingStash = loadStash();
        existingStash.addAll(0, modifiedFiles);
        saveStash(existingStash);

        // Save objects for stashed files
        for (StashEntry stashEntry : modifiedFiles) {
            writeObject(stashEntry.hash, stashEntry.content);
        }

        // Restore files to indexed state
        for (StashEntry stashEntry : modifiedFiles) {
            String path = stashEntry.path;
            String indexHash = indexEntries.get(path);
            File objectFile = store.getObjectFile(indexHash);
            byte[] content = Files.readAllBytes(objectFile.toPath());
            Files.write(new File(path).toPath(), content);
        }

        System.out.println("Saved working directory state (stashed " + modifiedFiles.size() + " file(s))");
    }

    private void popStash() throws IOException {
        List<StashEntry> stash = loadStash();
        
        if (stash.isEmpty()) {
            System.out.println("No stash entries found");
            return;
        }

        // Find the most recent stash group (all entries with same timestamp/batch)
        List<StashEntry> toRestore = new ArrayList<>();
        for (StashEntry entry : stash) {
            toRestore.add(entry);
            if (entry.isBatchEnd) {
                break;
            }
        }

        // Restore files
        for (StashEntry entry : toRestore) {
            File objectFile = store.getObjectFile(entry.hash);
            if (!objectFile.exists()) {
                System.out.println("Warning: stashed object not found for " + entry.path);
                continue;
            }
            byte[] content = Files.readAllBytes(objectFile.toPath());
            File targetFile = new File(entry.path);
            targetFile.getParentFile().mkdirs();
            Files.write(targetFile.toPath(), content);
        }

        // Remove from stash
        stash.subList(0, toRestore.size()).clear();
        saveStash(stash);

        System.out.println("Restored " + toRestore.size() + " file(s) from stash");
    }

    private void listStash() throws IOException {
        List<StashEntry> stash = loadStash();
        
        if (stash.isEmpty()) {
            System.out.println("No stash entries");
            return;
        }

        System.out.println("Stashed changes:");
        int batchNum = 0;
        for (StashEntry entry : stash) {
            System.out.println("  " + entry.path + " (" + entry.hash.substring(0, 7) + ")");
            if (entry.isBatchEnd) {
                batchNum++;
            }
        }
    }

    private void clearStash() throws IOException {
        File stashFile = store.getStashFile();
        if (stashFile.exists()) {
            stashFile.delete();
        }
        System.out.println("Stash cleared");
    }

    private List<StashEntry> loadStash() throws IOException {
        List<StashEntry> entries = new ArrayList<>();
        File stashFile = store.getStashFile();
        
        if (!stashFile.exists()) {
            return entries;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(stashFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 3);
                if (parts.length >= 2) {
                    boolean isBatchEnd = parts.length > 2 && parts[2].equals("END");
                    entries.add(new StashEntry(parts[1], parts[0], null, isBatchEnd));
                }
            }
        }

        return entries;
    }

    private void saveStash(List<StashEntry> entries) throws IOException {
        File stashFile = store.getStashFile();
        stashFile.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(stashFile))) {
            for (int i = 0; i < entries.size(); i++) {
                StashEntry entry = entries.get(i);
                writer.write(entry.hash + " " + entry.path);
                
                // Mark the last entry of each batch
                if (i == entries.size() - 1 || entries.get(i).isBatchEnd) {
                    writer.write(" END");
                }
                
                writer.newLine();
            }
        }
    }

    private void writeObject(String hash, byte[] data) throws IOException {
        File objectFile = store.getObjectFile(hash);
        if (objectFile.exists()) {
            return;
        }

        objectFile.getParentFile().mkdirs();

        Path tmp = Files.createTempFile(objectFile.getParentFile().toPath(), "opipop-obj-", ".tmp");
        try {
            Files.write(tmp, data);
            Files.move(tmp, objectFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static class StashEntry {
        String path;
        String hash;
        byte[] content;
        boolean isBatchEnd;

        StashEntry(String path, String hash, byte[] content) {
            this(path, hash, content, true);
        }

        StashEntry(String path, String hash, byte[] content, boolean isBatchEnd) {
            this.path = path;
            this.hash = hash;
            this.content = content;
            this.isBatchEnd = isBatchEnd;
        }
    }
}
