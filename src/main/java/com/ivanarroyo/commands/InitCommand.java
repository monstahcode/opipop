package com.ivanarroyo.commands;

import com.ivanarroyo.core.ObjectStore;

import java.io.File;
import java.nio.file.Files;

public class InitCommand implements Command {
    private final ObjectStore store;

    public InitCommand(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void execute(String[] args) throws Exception {
        File repo = store.getRepoDir();
        if (repo.exists()) {
            System.out.println("Repository already exists at " + repo.getAbsolutePath());
            return;
        }

        boolean created = repo.mkdirs();
        if(!created) {
            throw new IllegalStateException("Could not create repository directory at " + repo.getAbsolutePath());
        }

        store.getObjectsDir().mkdirs();
        store.getRefsDir().mkdirs();
        new File(store.getRefsDir(), "heads").mkdirs();

        // Create HEAD pointing to main branch
        Files.writeString(store.getHeadFile().toPath(), "ref: refs/heads/main");

        System.out.println("Initialized empty repository in " + repo.getAbsolutePath());
    }
}
