package com.ivanarroyo.commands;

import com.ivanarroyo.core.ObjectStore;
import com.ivanarroyo.core.Index;
import com.ivanarroyo.util.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class AddCommandTest {

    @TempDir
    Path tempDir;

    private ObjectStore store;
    private AddCommand addCommand;
    private File workingDir;

    @BeforeEach
    void setUp() throws IOException {
        workingDir = tempDir.toFile();
        System.setProperty("user.dir", workingDir.getAbsolutePath());
        
        File repoDir = tempDir.resolve(".opipop").toFile();
        repoDir.mkdirs();
        store = new ObjectStore(repoDir.getPath());
        store.getObjectsDir().mkdirs();
        
        addCommand = new AddCommand(store);
    }

    @Test
    void testAddFileNoArguments() {
        assertDoesNotThrow(() -> addCommand.execute(new String[]{}));
    }

    @Test
    void testAddNonExistentFile() {
        assertDoesNotThrow(() -> addCommand.execute(new String[]{"nonexistent.txt"}));
    }

    @Test
    void testAddFile() throws Exception {
        File testFile = new File(workingDir, "test.txt");
        String content = "test content";
        Files.writeString(testFile.toPath(), content);
        
        addCommand.execute(new String[]{"test.txt"});
        
        String hash = HashUtils.sha1(content.getBytes());
        File objectFile = store.getObjectFile(hash);
        assertTrue(objectFile.exists());
        
        String storedContent = Files.readString(objectFile.toPath());
        assertEquals(content, storedContent);
    }

    @Test
    void testAddMultipleFiles() throws Exception {
        File file1 = new File(workingDir, "file1.txt");
        File file2 = new File(workingDir, "file2.txt");
        Files.writeString(file1.toPath(), "content1");
        Files.writeString(file2.toPath(), "content2");
        
        addCommand.execute(new String[]{"file1.txt", "file2.txt"});
        
        String hash1 = HashUtils.sha1("content1".getBytes());
        String hash2 = HashUtils.sha1("content2".getBytes());
        
        assertTrue(store.getObjectFile(hash1).exists());
        assertTrue(store.getObjectFile(hash2).exists());
    }

    @Test
    void testAddFileAlreadyAdded() throws Exception {
        File testFile = new File(workingDir, "test.txt");
        Files.writeString(testFile.toPath(), "content");
        
        addCommand.execute(new String[]{"test.txt"});
        
        assertDoesNotThrow(() -> addCommand.execute(new String[]{"test.txt"}));
    }

    @Test
    void testAddCreatesObjectDirectory() throws Exception {
        File testFile = new File(workingDir, "test.txt");
        Files.writeString(testFile.toPath(), "content");
        
        File objectsDir = store.getObjectsDir();
        if (objectsDir.exists()) {
            deleteDirectory(objectsDir);
        }
        
        addCommand.execute(new String[]{"test.txt"});
        
        assertTrue(objectsDir.exists());
    }

    @Test
    void testAddBinaryFile() throws Exception {
        File testFile = new File(workingDir, "binary.dat");
        byte[] binaryContent = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF};
        Files.write(testFile.toPath(), binaryContent);
        
        addCommand.execute(new String[]{"binary.dat"});
        
        String hash = HashUtils.sha1(binaryContent);
        File objectFile = store.getObjectFile(hash);
        assertTrue(objectFile.exists());
        
        byte[] storedContent = Files.readAllBytes(objectFile.toPath());
        assertArrayEquals(binaryContent, storedContent);
    }

    @Test
    void testAddDirectory() throws Exception {
        File dir = new File(workingDir, "testdir");
        dir.mkdirs();
        
        assertDoesNotThrow(() -> addCommand.execute(new String[]{"testdir"}));
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                }
                file.delete();
            }
        }
        dir.delete();
    }
}
