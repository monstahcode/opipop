package com.ivanarroyo.commands;

import com.ivanarroyo.core.ObjectStore;
import com.ivanarroyo.core.Index;
import com.ivanarroyo.core.Commit;
import com.ivanarroyo.util.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class CommitCommandTest {

    @TempDir
    Path tempDir;

    private ObjectStore store;
    private CommitCommand commitCommand;
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
        
        commitCommand = new CommitCommand(store);
    }

    @Test
    void testCommitNoArguments() {
        assertDoesNotThrow(() -> commitCommand.execute(new String[]{}));
    }

    @Test
    void testCommitNoMessage() {
        assertDoesNotThrow(() -> commitCommand.execute(new String[]{"-m"}));
    }

    @Test
    void testCommitEmptyIndex() {
        assertDoesNotThrow(() -> commitCommand.execute(new String[]{"-m", "test"}));
    }

    @Test
    void testCommitWithFiles() throws Exception {
        Index index = new Index(store.getIndexFile());
        index.add("file1.txt", "hash1");
        index.add("file2.txt", "hash2");
        index.save();
        
        commitCommand.execute(new String[]{"-m", "Test commit"});
        
        String commitHash = store.getHeadCommit();
        assertNotNull(commitHash);
        
        File commitFile = store.getObjectFile(commitHash);
        assertTrue(commitFile.exists());
    }

    @Test
    void testCommitCreatesTree() throws Exception {
        Index index = new Index(store.getIndexFile());
        index.add("file.txt", "hash123");
        index.save();
        
        commitCommand.execute(new String[]{"-m", "Create tree"});
        
        String commitHash = store.getHeadCommit();
        byte[] commitData = Files.readAllBytes(store.getObjectFile(commitHash).toPath());
        Commit commit = Commit.deserialize(commitData);
        
        assertNotNull(commit.getTreeHash());
        File treeFile = store.getObjectFile(commit.getTreeHash());
        assertTrue(treeFile.exists());
    }

    @Test
    void testFirstCommitHasNoParent() throws Exception {
        Index index = new Index(store.getIndexFile());
        index.add("file.txt", "hash123");
        index.save();
        
        commitCommand.execute(new String[]{"-m", "First commit"});
        
        String commitHash = store.getHeadCommit();
        byte[] commitData = Files.readAllBytes(store.getObjectFile(commitHash).toPath());
        Commit commit = Commit.deserialize(commitData);
        
        assertNull(commit.getParentHash());
    }

    @Test
    void testSecondCommitHasParent() throws Exception {
        Index index = new Index(store.getIndexFile());
        index.add("file1.txt", "hash1");
        index.save();
        
        commitCommand.execute(new String[]{"-m", "First"});
        String firstCommit = store.getHeadCommit();
        
        index.add("file2.txt", "hash2");
        index.save();
        
        commitCommand.execute(new String[]{"-m", "Second"});
        String secondCommit = store.getHeadCommit();
        
        byte[] commitData = Files.readAllBytes(store.getObjectFile(secondCommit).toPath());
        Commit commit = Commit.deserialize(commitData);
        
        assertEquals(firstCommit, commit.getParentHash());
    }

    @Test
    void testCommitUpdatesBranch() throws Exception {
        Index index = new Index(store.getIndexFile());
        index.add("file.txt", "hash123");
        index.save();
        
        assertNull(store.getHeadCommit());
        
        commitCommand.execute(new String[]{"-m", "Update branch"});
        
        assertNotNull(store.getHeadCommit());
    }

    @Test
    void testCommitMessageParsing() throws Exception {
        Index index = new Index(store.getIndexFile());
        index.add("file.txt", "hash123");
        index.save();
        
        String message = "Multi word message";
        commitCommand.execute(new String[]{"-m", message});
        
        String commitHash = store.getHeadCommit();
        byte[] commitData = Files.readAllBytes(store.getObjectFile(commitHash).toPath());
        Commit commit = Commit.deserialize(commitData);
        
        assertEquals(message, commit.getMessage());
    }

    @Test
    void testCommitWithAuthor() throws Exception {
        System.setProperty("user.name", "testuser");
        
        Index index = new Index(store.getIndexFile());
        index.add("file.txt", "hash123");
        index.save();
        
        commitCommand.execute(new String[]{"-m", "Author test"});
        
        String commitHash = store.getHeadCommit();
        byte[] commitData = Files.readAllBytes(store.getObjectFile(commitHash).toPath());
        Commit commit = Commit.deserialize(commitData);
        
        assertEquals("testuser", commit.getAuthor());
    }
}
