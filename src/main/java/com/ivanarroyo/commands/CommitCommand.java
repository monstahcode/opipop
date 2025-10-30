package com.ivanarroyo.commands;

import com.ivanarroyo.core.Commit;
import com.ivanarroyo.core.Index;
import com.ivanarroyo.core.ObjectStore;
import com.ivanarroyo.core.Tree;
import com.ivanarroyo.util.HashUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class CommitCommand implements Command {
    private final ObjectStore store;

    public CommitCommand(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: opipop commit -m <message>");
            return;
        }

        String message = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-m") && i + 1 < args.length) {
                message = args[i + 1];
                break;
            }
        }

        if (message == null) {
            System.out.println("Usage: opipop commit -m <message>");
            return;
        }

        Index index = new Index(store.getIndexFile());
        if (index.getEntries().isEmpty()) {
            System.out.println("nothing to commit");
            return;
        }

        // Create tree from index
        Tree tree = new Tree();
        for (Map.Entry<String, String> entry : index.getEntries().entrySet()) {
            tree.addEntry(entry.getKey(), entry.getValue());
        }

        byte[] treeData = tree.serialize();
        String treeHash = HashUtils.sha1(treeData);
        writeObject(treeHash, treeData);

        // Get parent commit
        String parentHash = store.getHeadCommit();

        // Create commit
        String author = System.getProperty("user.name", "unknown");
        Commit commit = new Commit(treeHash, parentHash, message, author);
        byte[] commitData = commit.serialize();
        String commitHash = HashUtils.sha1(commitData);
        commit.setHash(commitHash);

        writeObject(commitHash, commitData);

        // Update HEAD
        store.updateHead(commitHash);

        System.out.println("[" + store.getCurrentBranch() + " " + commitHash.substring(0, 7) + "] " + message);
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
}
