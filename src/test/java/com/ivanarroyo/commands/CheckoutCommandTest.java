package com.ivanarroyo.commands;

import com.ivanarroyo.core.*;
import com.ivanarroyo.util.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class CheckoutCommandTest {

    @TempDir
    Path tempDir;

    private ObjectStore store;
    private CheckoutCommand checkoutCommand;
    private File workingDir;

    @BeforeEach
    void setUp() throws IOException {
        workingDir = tempDir.toFile();
        System.setProperty("user.dir", workingDir.getAbsolutePath());
        
        File repoDir = tempDir.resolve(".opipop").toFile();
        repoDir.mkdirs();
        store = new ObjectStore(repoDir.getPath());
        store.getObjectsDir().mkdirs();
        store.getRefsDir().mkdirs();
        new File(store.getRefsDir(), "heads").mkdirs();
        Files.writeString(store.getHeadFile().toPath(), "ref: refs/heads/main");
        
        checkoutCommand = new CheckoutCommand(store);
    }

    @Test
    void testCheckoutNoArguments() {
        assertDoesNotThrow(() -> checkoutCommand.execute(new String[]{}));
    }

    @Test
    void testCheckoutNonExistentBranch() {
        assertDoesNotThrow(() -> checkoutCommand.execute(new String[]{"nonexistent"}));
    }

    @Test
    void testCheckoutCurrentBranch() throws Exception {
        File branchFile = new File(store.getRefsDir(), "heads/main");
        branchFile.getParentFile().mkdirs();
        Files.writeString(branchFile.toPath(), "commit123");
        
        assertDoesNotThrow(() -> checkoutCommand.execute(new String[]{"main"}));
    }

    @Test
    void testCheckoutWithUncommittedChanges() throws Exception {
        File file = new File(workingDir, "file.txt");
        Files.writeString(file.toPath(), "original");
        String hash = HashUtils.sha1("original".getBytes());
        
        Index index = new Index(store.getIndexFile());
        index.add("file.txt", hash);
        index.save();
        
        Files.writeString(file.toPath(), "modified");
        
        File branchFile = new File(store.getRefsDir(), "heads/develop");
        Files.writeString(branchFile.toPath(), "commit456");
        
        assertDoesNotThrow(() -> checkoutCommand.execute(new String[]{"develop"}));
    }

    @Test
    void testCheckoutBranch() throws Exception {
        String content = "test content";
        byte[] contentBytes = content.getBytes();
        String fileHash = HashUtils.sha1(contentBytes);
        
        File objectFile = store.getObjectFile(fileHash);
        objectFile.getParentFile().mkdirs();
        Files.write(objectFile.toPath(), contentBytes);
        
        Tree tree = new Tree();
        tree.addEntry("test.txt", fileHash);
        byte[] treeData = tree.serialize();
        String treeHash = HashUtils.sha1(treeData);
        
        File treeFile = store.getObjectFile(treeHash);
        Files.write(treeFile.toPath(), treeData);
        
        Commit commit = new Commit(treeHash, null, "Test commit", "test");
        byte[] commitData = commit.serialize();
        String commitHash = HashUtils.sha1(commitData);
        
        File commitFile = store.getObjectFile(commitHash);
        Files.write(commitFile.toPath(), commitData);
        
        File branchFile = new File(store.getRefsDir(), "heads/develop");
        Files.writeString(branchFile.toPath(), commitHash);
        
        checkoutCommand.execute(new String[]{"develop"});
        
        assertEquals("develop", store.getCurrentBranch());
        assertTrue(new File(workingDir, "test.txt").exists());
        String restoredContent = Files.readString(new File(workingDir, "test.txt").toPath());
        assertEquals(content, restoredContent);
    }

    @Test
    void testCheckoutClearsWorkingDirectory() throws Exception {
        File existingFile = new File(workingDir, "existing.txt");
        Files.writeString(existingFile.toPath(), "existing");
        
        Tree tree = new Tree();
        byte[] treeData = tree.serialize();
        String treeHash = HashUtils.sha1(treeData);
        
        File treeFile = store.getObjectFile(treeHash);
        treeFile.getParentFile().mkdirs();
        Files.write(treeFile.toPath(), treeData);
        
        Commit commit = new Commit(treeHash, null, "Empty commit", "test");
        byte[] commitData = commit.serialize();
        String commitHash = HashUtils.sha1(commitData);
        
        File commitFile = store.getObjectFile(commitHash);
        Files.write(commitFile.toPath(), commitData);
        
        File branchFile = new File(store.getRefsDir(), "heads/develop");
        Files.writeString(branchFile.toPath(), commitHash);
        
        checkoutCommand.execute(new String[]{"develop"});
        
        assertFalse(existingFile.exists());
    }

    @Test
    void testCheckoutUpdatesIndex() throws Exception {
        String content = "content";
        String hash = HashUtils.sha1(content.getBytes());
        
        File objectFile = store.getObjectFile(hash);
        objectFile.getParentFile().mkdirs();
        Files.write(objectFile.toPath(), content.getBytes());
        
        Tree tree = new Tree();
        tree.addEntry("file.txt", hash);
        byte[] treeData = tree.serialize();
        String treeHash = HashUtils.sha1(treeData);
        
        File treeFile = store.getObjectFile(treeHash);
        Files.write(treeFile.toPath(), treeData);
        
        Commit commit = new Commit(treeHash, null, "Commit", "test");
        byte[] commitData = commit.serialize();
        String commitHash = HashUtils.sha1(commitData);
        
        File commitFile = store.getObjectFile(commitHash);
        Files.write(commitFile.toPath(), commitData);
        
        File branchFile = new File(store.getRefsDir(), "heads/feature");
        Files.writeString(branchFile.toPath(), commitHash);
        
        checkoutCommand.execute(new String[]{"feature"});
        
        Index index = new Index(store.getIndexFile());
        assertTrue(index.contains("file.txt"));
        assertEquals(hash, index.getHash("file.txt"));
    }

    @Test
    void testCheckoutWithNestedDirectories() throws Exception {
        String content = "nested content";
        String hash = HashUtils.sha1(content.getBytes());
        
        File objectFile = store.getObjectFile(hash);
        objectFile.getParentFile().mkdirs();
        Files.write(objectFile.toPath(), content.getBytes());
        
        Tree tree = new Tree();
        tree.addEntry("dir/subdir/file.txt", hash);
        byte[] treeData = tree.serialize();
        String treeHash = HashUtils.sha1(treeData);
        
        File treeFile = store.getObjectFile(treeHash);
        Files.write(treeFile.toPath(), treeData);
        
        Commit commit = new Commit(treeHash, null, "Nested", "test");
        byte[] commitData = commit.serialize();
        String commitHash = HashUtils.sha1(commitData);
        
        File commitFile = store.getObjectFile(commitHash);
        Files.write(commitFile.toPath(), commitData);
        
        File branchFile = new File(store.getRefsDir(), "heads/nested");
        Files.writeString(branchFile.toPath(), commitHash);
        
        checkoutCommand.execute(new String[]{"nested"});
        
        File nestedFile = new File(workingDir, "dir/subdir/file.txt");
        assertTrue(nestedFile.exists());
        assertEquals(content, Files.readString(nestedFile.toPath()));
    }
}
