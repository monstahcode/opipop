package com.ivanarroyo.core;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Tree {
    private final Map<String, String> entries; // path -> hash

    public Tree() {
        this.entries = new TreeMap<>();
    }

    public void addEntry(String path, String hash) {
        entries.put(path, hash);
    }

    public Map<String, String> getEntries() {
        return new HashMap<>(entries);
    }

    public byte[] serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append(entry.getValue()).append(" ").append(entry.getKey()).append("\n");
        }
        return sb.toString().getBytes();
    }

    public static Tree deserialize(byte[] data) {
        Tree tree = new Tree();
        String content = new String(data);
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (line.isEmpty()) continue;
            String[] parts = line.split(" ", 2);
            if (parts.length == 2) {
                tree.addEntry(parts[1], parts[0]);
            }
        }
        
        return tree;
    }
}
