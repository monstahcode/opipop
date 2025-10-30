package com.ivanarroyo.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

class IndexTest {

    @TempDir
    Path tempDir;

    private File indexFile;
    private Index index;

    @BeforeEach
    void setUp() {
        indexFile = tempDir.resolve("index").toFile();
        index = new Index(indexFile);
    }

    @Test
    void testAddEntry() {
        index.add("file.txt", "hash123");
        
        assertTrue(index.contains("file.txt"));
        assertEquals("hash123", index.getHash("file.txt"));
    }

    @Test
    void testAddMultipleEntries() {
        index.add("file1.txt", "hash1");
        index.add("file2.txt", "hash2");
        index.add("file3.txt", "hash3");
        
        Map<String, String> entries = index.getEntries();
        assertEquals(3, entries.size());
        assertEquals("hash1", entries.get("file1.txt"));
        assertEquals("hash2", entries.get("file2.txt"));
        assertEquals("hash3", entries.get("file3.txt"));
    }

    @Test
    void testRemoveEntry() {
        index.add("file.txt", "hash123");
        assertTrue(index.contains("file.txt"));
        
        index.remove("file.txt");
        assertFalse(index.contains("file.txt"));
        assertNull(index.getHash("file.txt"));
    }

    @Test
    void testGetEntriesReturnsImmutableCopy() {
        index.add("file.txt", "hash1");
        Map<String, String> entries = index.getEntries();
        entries.put("newfile.txt", "hash2");
        
        assertEquals(1, index.getEntries().size());
    }

    @Test
    void testSaveAndLoad() throws IOException {
        index.add("file1.txt", "hash1");
        index.add("file2.txt", "hash2");
        index.save();
        
        Index loadedIndex = new Index(indexFile);
        Map<String, String> entries = loadedIndex.getEntries();
        
        assertEquals(2, entries.size());
        assertEquals("hash1", entries.get("file1.txt"));
        assertEquals("hash2", entries.get("file2.txt"));
    }

    @Test
    void testLoadNonExistentFile() {
        File nonExistent = tempDir.resolve("nonexistent").toFile();
        Index newIndex = new Index(nonExistent);
        
        assertTrue(newIndex.getEntries().isEmpty());
    }

    @Test
    void testClear() {
        index.add("file1.txt", "hash1");
        index.add("file2.txt", "hash2");
        assertEquals(2, index.getEntries().size());
        
        index.clear();
        assertTrue(index.getEntries().isEmpty());
    }

    @Test
    void testUpdateEntry() {
        index.add("file.txt", "hash1");
        assertEquals("hash1", index.getHash("file.txt"));
        
        index.add("file.txt", "hash2");
        assertEquals("hash2", index.getHash("file.txt"));
    }

    @Test
    void testContainsNonExistentEntry() {
        assertFalse(index.contains("nonexistent.txt"));
    }

    @Test
    void testGetHashNonExistentEntry() {
        assertNull(index.getHash("nonexistent.txt"));
    }

    @Test
    void testSaveCreatesParentDirectory() throws IOException {
        File nestedIndex = tempDir.resolve("nested/dir/index").toFile();
        Index nestedIndexObj = new Index(nestedIndex);
        nestedIndexObj.add("file.txt", "hash1");
        
        nestedIndexObj.save();
        
        assertTrue(nestedIndex.exists());
        assertTrue(nestedIndex.getParentFile().exists());
    }

    @Test
    void testEmptyIndexSave() throws IOException {
        index.save();
        assertTrue(indexFile.exists());
        
        Index loadedIndex = new Index(indexFile);
        assertTrue(loadedIndex.getEntries().isEmpty());
    }
}
