package com.ivanarroyo.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class ObjectStoreTest {

    @TempDir
    Path tempDir;

    private ObjectStore store;
    private File repoDir;

    @BeforeEach
    void setUp() {
        repoDir = tempDir.resolve(".opipop").toFile();
        store = new ObjectStore(repoDir.getPath());
    }

    @Test
    void testGetRepoDir() {
        assertEquals(repoDir, store.getRepoDir());
    }

    @Test
    void testGetObjectsDir() {
        File objectsDir = store.getObjectsDir();
        assertEquals(new File(repoDir, "objects"), objectsDir);
    }

    @Test
    void testGetRefsDir() {
        File refsDir = store.getRefsDir();
        assertEquals(new File(repoDir, "refs"), refsDir);
    }

    @Test
    void testGetObjectFile() {
        String hash = "abc123def456";
        File objectFile = store.getObjectFile(hash);
        assertEquals(new File(repoDir, "objects/" + hash), objectFile);
    }

    @Test
    void testGetIndexFile() {
        File indexFile = store.getIndexFile();
        assertEquals(new File(repoDir, "index"), indexFile);
    }

    @Test
    void testGetHeadFile() {
        File headFile = store.getHeadFile();
        assertEquals(new File(repoDir, "HEAD"), headFile);
    }

    @Test
    void testGetStashFile() {
        File stashFile = store.getStashFile();
        assertEquals(new File(repoDir, "stash"), stashFile);
    }

    @Test
    void testGetCurrentBranchWithoutHead() throws IOException {
        String branch = store.getCurrentBranch();
        assertEquals("main", branch);
    }

    @Test
    void testGetCurrentBranchWithHead() throws IOException {
        repoDir.mkdirs();
        Files.writeString(store.getHeadFile().toPath(), "ref: refs/heads/develop");
        
        String branch = store.getCurrentBranch();
        assertEquals("develop", branch);
    }

    @Test
    void testGetCurrentBranchDetachedHead() throws IOException {
        repoDir.mkdirs();
        Files.writeString(store.getHeadFile().toPath(), "abc123def456");
        
        String branch = store.getCurrentBranch();
        assertNull(branch);
    }

    @Test
    void testGetHeadCommitNoBranch() throws IOException {
        String commit = store.getHeadCommit();
        assertNull(commit);
    }

    @Test
    void testGetHeadCommitWithBranch() throws IOException {
        repoDir.mkdirs();
        File headsDir = new File(store.getRefsDir(), "heads");
        headsDir.mkdirs();
        
        File branchFile = new File(headsDir, "main");
        Files.writeString(branchFile.toPath(), "commit123");
        Files.writeString(store.getHeadFile().toPath(), "ref: refs/heads/main");
        
        String commit = store.getHeadCommit();
        assertEquals("commit123", commit);
    }

    @Test
    void testGetHeadCommitDetached() throws IOException {
        repoDir.mkdirs();
        Files.writeString(store.getHeadFile().toPath(), "detached123");
        
        String commit = store.getHeadCommit();
        assertEquals("detached123", commit);
    }

    @Test
    void testUpdateHeadOnBranch() throws IOException {
        repoDir.mkdirs();
        File headsDir = new File(store.getRefsDir(), "heads");
        headsDir.mkdirs();
        
        Files.writeString(store.getHeadFile().toPath(), "ref: refs/heads/main");
        
        store.updateHead("newcommit123");
        
        File branchFile = new File(headsDir, "main");
        String content = Files.readString(branchFile.toPath());
        assertEquals("newcommit123", content);
    }

    @Test
    void testUpdateHeadDetached() throws IOException {
        repoDir.mkdirs();
        Files.writeString(store.getHeadFile().toPath(), "oldcommit");
        
        store.updateHead("newcommit456");
        
        String content = Files.readString(store.getHeadFile().toPath());
        assertEquals("newcommit456", content);
    }

    @Test
    void testSetCurrentBranch() throws IOException {
        repoDir.mkdirs();
        
        store.setCurrentBranch("develop");
        
        String content = Files.readString(store.getHeadFile().toPath());
        assertEquals("ref: refs/heads/develop", content);
    }

    @Test
    void testSetCurrentBranchOverwrite() throws IOException {
        repoDir.mkdirs();
        Files.writeString(store.getHeadFile().toPath(), "ref: refs/heads/main");
        
        store.setCurrentBranch("feature");
        
        String content = Files.readString(store.getHeadFile().toPath());
        assertEquals("ref: refs/heads/feature", content);
    }
}
