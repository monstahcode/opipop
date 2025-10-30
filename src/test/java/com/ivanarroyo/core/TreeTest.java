package com.ivanarroyo.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

class TreeTest {

    private Tree tree;

    @BeforeEach
    void setUp() {
        tree = new Tree();
    }

    @Test
    void testAddEntry() {
        tree.addEntry("file.txt", "abc123");
        Map<String, String> entries = tree.getEntries();
        assertEquals(1, entries.size());
        assertEquals("abc123", entries.get("file.txt"));
    }

    @Test
    void testAddMultipleEntries() {
        tree.addEntry("file1.txt", "hash1");
        tree.addEntry("file2.txt", "hash2");
        tree.addEntry("file3.txt", "hash3");
        
        Map<String, String> entries = tree.getEntries();
        assertEquals(3, entries.size());
        assertEquals("hash1", entries.get("file1.txt"));
        assertEquals("hash2", entries.get("file2.txt"));
        assertEquals("hash3", entries.get("file3.txt"));
    }

    @Test
    void testSerializeDeserialize() {
        tree.addEntry("file1.txt", "hash1");
        tree.addEntry("file2.txt", "hash2");
        
        byte[] serialized = tree.serialize();
        Tree deserialized = Tree.deserialize(serialized);
        
        Map<String, String> original = tree.getEntries();
        Map<String, String> restored = deserialized.getEntries();
        
        assertEquals(original.size(), restored.size());
        assertEquals(original.get("file1.txt"), restored.get("file1.txt"));
        assertEquals(original.get("file2.txt"), restored.get("file2.txt"));
    }

    @Test
    void testSerializeEmptyTree() {
        byte[] serialized = tree.serialize();
        assertEquals(0, serialized.length);
    }

    @Test
    void testDeserializeEmptyData() {
        Tree deserialized = Tree.deserialize(new byte[0]);
        assertTrue(deserialized.getEntries().isEmpty());
    }

    @Test
    void testGetEntriesReturnsImmutableCopy() {
        tree.addEntry("file.txt", "hash1");
        Map<String, String> entries = tree.getEntries();
        entries.put("newfile.txt", "hash2");
        
        assertEquals(1, tree.getEntries().size());
    }

    @Test
    void testOverwriteEntry() {
        tree.addEntry("file.txt", "hash1");
        tree.addEntry("file.txt", "hash2");
        
        Map<String, String> entries = tree.getEntries();
        assertEquals(1, entries.size());
        assertEquals("hash2", entries.get("file.txt"));
    }

    @Test
    void testSerializeFormat() {
        tree.addEntry("file.txt", "abc123");
        byte[] serialized = tree.serialize();
        String content = new String(serialized);
        assertTrue(content.contains("abc123"));
        assertTrue(content.contains("file.txt"));
    }
}
