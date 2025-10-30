package com.ivanarroyo.commands;

import com.ivanarroyo.core.Index;
import com.ivanarroyo.core.ObjectStore;
import com.ivanarroyo.util.HashUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AddCommand implements Command {
    private final ObjectStore store; 

    public AddCommand(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: opipop add <file>");
            return;
        }

        Index index = new Index(store.getIndexFile());
        
        for (String arg : args) {
            addFile(arg);
        }
        
        index.save();
    }

    private void addFile(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            System.out.println("File not found: " + filePath);
            return;
        }

        byte[] content = Files.readAllBytes(path);
        String hash = HashUtils.sha1(content);
        File objectFile = store.getObjectFile(hash);
        if (objectFile.exists()) {
            System.out.println("File already added: " + filePath);
            return;
        }

        objectFile.getParentFile().mkdirs();

        Path tmp = Files.createTempFile(objectFile.getParentFile().toPath(), "opipop-obj-", ".tmp");
        try {
            Files.write(tmp, content);
            Files.move(tmp, objectFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException e) {
                // Ignore
            }
        }

        System.out.println("Added file: " + filePath + " (hash: " + hash + ")");
    }
}
