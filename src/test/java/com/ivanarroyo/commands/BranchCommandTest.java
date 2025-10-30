package com.ivanarroyo.commands;

import com.ivanarroyo.core.ObjectStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class BranchCommandTest {

    @TempDir
    Path tempDir;

    private ObjectStore store;
    private BranchCommand branchCommand;

    @BeforeEach
    void setUp() throws IOException {
        File repoDir = tempDir.resolve(".opipop").toFile();
        repoDir.mkdirs();
        store = new ObjectStore(repoDir.getPath());
        store.getRefsDir().mkdirs();
        new File(store.getRefsDir(), "heads").mkdirs();
        Files.writeString(store.getHeadFile().toPath(), "ref: refs/heads/main");
        
        branchCommand = new BranchCommand(store);
    }

    @Test
    void testListBranchesEmpty() {
        assertDoesNotThrow(() -> branchCommand.execute(new String[]{}));
    }

    @Test
    void testListBranchesWithBranches() throws Exception {
        File headsDir = new File(store.getRefsDir(), "heads");
        Files.writeString(new File(headsDir, "main").toPath(), "commit123");
        Files.writeString(new File(headsDir, "develop").toPath(), "commit456");
        
        assertDoesNotThrow(() -> branchCommand.execute(new String[]{}));
    }

    @Test
    void testCreateBranchNoCommits() {
        assertDoesNotThrow(() -> branchCommand.execute(new String[]{"feature"}));
    }

    @Test
    void testCreateBranch() throws Exception {
        File headsDir = new File(store.getRefsDir(), "heads");
        Files.writeString(new File(headsDir, "main").toPath(), "commit123");
        
        branchCommand.execute(new String[]{"feature"});
        
        File branchFile = new File(headsDir, "feature");
        assertTrue(branchFile.exists());
        String content = Files.readString(branchFile.toPath());
        assertEquals("commit123", content);
    }

    @Test
    void testCreateBranchAlreadyExists() throws Exception {
        File headsDir = new File(store.getRefsDir(), "heads");
        Files.writeString(new File(headsDir, "main").toPath(), "commit123");
        Files.writeString(new File(headsDir, "feature").toPath(), "commit123");
        
        assertDoesNotThrow(() -> branchCommand.execute(new String[]{"feature"}));
        
        String content = Files.readString(new File(headsDir, "feature").toPath());
        assertEquals("commit123", content);
    }

    @Test
    void testCreateMultipleBranches() throws Exception {
        File headsDir = new File(store.getRefsDir(), "heads");
        Files.writeString(new File(headsDir, "main").toPath(), "commit123");
        
        branchCommand.execute(new String[]{"feature1"});
        branchCommand.execute(new String[]{"feature2"});
        
        assertTrue(new File(headsDir, "feature1").exists());
        assertTrue(new File(headsDir, "feature2").exists());
    }

    @Test
    void testCreateBranchFromCurrentCommit() throws Exception {
        File headsDir = new File(store.getRefsDir(), "heads");
        String commitHash = "abc123def456";
        Files.writeString(new File(headsDir, "main").toPath(), commitHash);
        
        branchCommand.execute(new String[]{"hotfix"});
        
        File branchFile = new File(headsDir, "hotfix");
        String content = Files.readString(branchFile.toPath());
        assertEquals(commitHash, content);
    }
}
