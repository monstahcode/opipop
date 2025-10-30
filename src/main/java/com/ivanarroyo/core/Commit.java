package com.ivanarroyo.core;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commit {
    private String hash;
    private String treeHash;
    private String parentHash;
    private String message;
    private long timestamp;
    private String author;

    public Commit(String treeHash, String parentHash, String message, String author) {
        this.treeHash = treeHash;
        this.parentHash = parentHash;
        this.message = message;
        this.timestamp = Instant.now().getEpochSecond();
        this.author = author;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getTreeHash() {
        return treeHash;
    }

    public String getParentHash() {
        return parentHash;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAuthor() {
        return author;
    }

    public byte[] serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(treeHash).append("\n");
        if (parentHash != null) {
            sb.append("parent ").append(parentHash).append("\n");
        }
        sb.append("author ").append(author).append(" ").append(timestamp).append("\n");
        sb.append("\n");
        sb.append(message);
        return sb.toString().getBytes();
    }

    public static Commit deserialize(byte[] data) {
        String content = new String(data);
        String[] lines = content.split("\n");

        String treeHash = null;
        String parentHash = null;
        String author = null;
        long timestamp = 0;
        StringBuilder message = new StringBuilder();
        boolean inMessage = false;

        for (String line : lines) {
            if (inMessage) {
                if(message.length() > 0) {
                    message.append("\n");
                }
                message.append(line);
            } else if (line.startsWith("tree ")) {
                treeHash = line.substring(5).trim();
            } else if (line.startsWith("parent ")) {
                parentHash = line.substring(7).trim();
            } else if (line.startsWith("author ")) {
                String[] parts = line.substring(7).trim().split(" ");
                author = parts[0];
                if (parts.length > 1) {
                    timestamp = Long.parseLong(parts[1]);
                }
            }
        }

        Commit commit = new Commit(treeHash, parentHash, message.toString(), author);
        commit.timestamp = timestamp;
        return commit;
    } 
}
