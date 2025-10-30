package com.ivanarroyo.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Index {
    private final File indexFile;
    private final Map<String, String> entries; // path -> hash

    public Index(File indexFile) {
        this.indexFile = indexFile;
        this.entries = new HashMap<>();
        load();
    }

    public void add(String path, String hash) {
        entries.put(path, hash);
    }

    public void remove(String path) {
        entries.remove(path);
    }

    public Map<String, String> getEntries() {
        return new HashMap<>(entries);
    }

    public String getHash(String path) {
        return entries.get(path);
    }

    public boolean contains(String path) {
        return entries.containsKey(path);
    }

    public void save() throws IOException {
        indexFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                writer.write(entry.getValue() + " " + entry.getKey());
                writer.newLine();
            }
        }
    }

    public void load() {
        if (!indexFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 2);
                if (parts.length == 2) {
                    entries.put(parts[1], parts[0]);
                }
            }
        } catch (IOException e) {
            // Ignore, start with empty index
        }
    }

    public void clear() {
        entries.clear();
    }
}
