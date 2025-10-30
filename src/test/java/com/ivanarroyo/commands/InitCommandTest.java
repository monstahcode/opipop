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

class InitCommandTest {

    @TempDir
    Path tempDir;

    private ObjectStore store;
    private InitCommand initCommand;

    @BeforeEach
    void setUp() {
        File repoDir = tempDir.resolve(".opipop").toFile();
        store = new ObjectStore(repoDir.getPath());
        initCommand = new InitCommand(store);
    }

    @Test
    void testInitCreatesRepository() throws Exception {
        initCommand.execute(new String[]{});
        
        assertTrue(store.getRepoDir().exists());
        assertTrue(store.getRepoDir().isDirectory());
    }

    @Test
    void testInitCreatesObjectsDir() throws Exception {
        initCommand.execute(new String[]{});
        
        assertTrue(store.getObjectsDir().exists());
        assertTrue(store.getObjectsDir().isDirectory());
    }

    @Test
    void testInitCreatesRefsDir() throws Exception {
        initCommand.execute(new String[]{});
        
        assertTrue(store.getRefsDir().exists());
        assertTrue(store.getRefsDir().isDirectory());
    }

    @Test
    void testInitCreatesHeadsDir() throws Exception {
        initCommand.execute(new String[]{});
        
        File headsDir = new File(store.getRefsDir(), "heads");
        assertTrue(headsDir.exists());
        assertTrue(headsDir.isDirectory());
    }

    @Test
    void testInitCreatesHeadFile() throws Exception {
        initCommand.execute(new String[]{});
        
        assertTrue(store.getHeadFile().exists());
        String content = Files.readString(store.getHeadFile().toPath());
        assertEquals("ref: refs/heads/main", content);
    }

    @Test
    void testInitAlreadyExists() throws Exception {
        store.getRepoDir().mkdirs();
        
        assertDoesNotThrow(() -> initCommand.execute(new String[]{}));
        assertTrue(store.getRepoDir().exists());
    }

    @Test
    void testInitWithArguments() throws Exception {
        assertDoesNotThrow(() -> initCommand.execute(new String[]{"extra", "args"}));
        assertTrue(store.getRepoDir().exists());
    }
}
