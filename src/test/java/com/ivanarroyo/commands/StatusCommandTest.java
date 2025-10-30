package com.ivanarroyo.commands;

import com.ivanarroyo.core.ObjectStore;
import com.ivanarroyo.core.Index;
import com.ivanarroyo.core.Tree;
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

class StatusCommandTest {

    @TempDir
    Path tempDir;

    private ObjectStore store;
    private StatusCommand statusCommand;
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
        
        statusCommand = new StatusCommand(store);
    }

    @Test
    void testStatusEmptyRepository() {
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }

    @Test
    void testStatusCleanWorkingTree() throws Exception {
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }

    @Test
    void testStatusWithUntrackedFiles() throws Exception {
        File file = new File(workingDir, "untracked.txt");
        Files.writeString(file.toPath(), "content");
        
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }

    @Test
    void testStatusWithStagedFiles() throws Exception {
        Index index = new Index(store.getIndexFile());
        index.add("staged.txt", "hash123");
        index.save();
        
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }

    @Test
    void testStatusWithModifiedFiles() throws Exception {
        File file = new File(workingDir, "file.txt");
        Files.writeString(file.toPath(), "original");
        String hash = HashUtils.sha1("original".getBytes());
        
        Index index = new Index(store.getIndexFile());
        index.add("file.txt", hash);
        index.save();
        
        Files.writeString(file.toPath(), "modified");
        
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }

    @Test
    void testStatusWithDeletedFiles() throws Exception {
        Index index = new Index(store.getIndexFile());
        index.add("deleted.txt", "hash123");
        index.save();
        
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }

    @Test
    void testStatusWithCommit() throws Exception {
        File file = new File(workingDir, "file.txt");
        String content = "content";
        Files.writeString(file.toPath(), content);
        String hash = HashUtils.sha1(content.getBytes());
        
        File objectFile = store.getObjectFile(hash);
        objectFile.getParentFile().mkdirs();
        Files.write(objectFile.toPath(), content.getBytes());
        
        Tree tree = new Tree();
        tree.addEntry("file.txt", hash);
        byte[] treeData = tree.serialize();
        String treeHash = HashUtils.sha1(treeData);
        
        File treeFile = store.getObjectFile(treeHash);
        treeFile.getParentFile().mkdirs();
        Files.write(treeFile.toPath(), treeData);
        
        Commit commit = new Commit(treeHash, null, "Initial", "test");
        byte[] commitData = commit.serialize();
        String commitHash = HashUtils.sha1(commitData);
        
        File commitFile = store.getObjectFile(commitHash);
        commitFile.getParentFile().mkdirs();
        Files.write(commitFile.toPath(), commitData);
        
        File branchFile = new File(store.getRefsDir(), "heads/main");
        Files.writeString(branchFile.toPath(), commitHash);
        
        Index index = new Index(store.getIndexFile());
        index.add("file.txt", hash);
        index.save();
        
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }

    @Test
    void testStatusIgnoresHiddenFiles() throws Exception {
        File hiddenFile = new File(workingDir, ".hidden");
        Files.writeString(hiddenFile.toPath(), "hidden content");
        
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }

    @Test
    void testStatusWithNestedDirectories() throws Exception {
        File dir = new File(workingDir, "subdir");
        dir.mkdirs();
        File file = new File(dir, "file.txt");
        Files.writeString(file.toPath(), "content");
        
        assertDoesNotThrow(() -> statusCommand.execute(new String[]{}));
    }
}
