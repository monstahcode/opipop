package com.ivanarroyo.commands;

import com.ivanarroyo.core.ObjectStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class BranchCommand implements Command {
    private final ObjectStore store;

    public BranchCommand(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            listBranches();
        } else {
            String branchName = args[0];
            createBranch(branchName);
        }
    }

    private void listBranches() throws IOException {
        String currentBranch = store.getCurrentBranch();
        File headsDir = new File(store.getRefsDir(), "heads");
        
        if (!headsDir.exists() || headsDir.listFiles() == null) {
            System.out.println("* " + currentBranch);
            return;
        }

        File[] branches = headsDir.listFiles();
        if (branches == null || branches.length == 0) {
            System.out.println("* " + currentBranch);
            return;
        }

        for (File branch : branches) {
            String name = branch.getName();
            if (name.equals(currentBranch)) {
                System.out.println("* " + name);
            } else {
                System.out.println("  " + name);
            }
        }
    }

    private void createBranch(String branchName) throws IOException {
        File branchFile = new File(store.getRefsDir(), "heads/" + branchName);
        
        if (branchFile.exists()) {
            System.out.println("Branch '" + branchName + "' already exists");
            return;
        }

        String headCommit = store.getHeadCommit();
        if (headCommit == null) {
            System.out.println("Cannot create branch: no commits yet");
            return;
        }

        branchFile.getParentFile().mkdirs();
        Files.writeString(branchFile.toPath(), headCommit);
        System.out.println("Created branch '" + branchName + "'");
    }
}
