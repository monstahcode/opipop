package com.ivanarroyo.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class CommitTest {

    private String treeHash;
    private String parentHash;
    private String message;
    private String author;

    @BeforeEach
    void setUp() {
        treeHash = "abc123";
        parentHash = "def456";
        message = "Initial commit";
        author = "testuser";
    }

    @Test
    void testCreateCommitWithParent() {
        Commit commit = new Commit(treeHash, parentHash, message, author);
        
        assertEquals(treeHash, commit.getTreeHash());
        assertEquals(parentHash, commit.getParentHash());
        assertEquals(message, commit.getMessage());
        assertEquals(author, commit.getAuthor());
        assertTrue(commit.getTimestamp() > 0);
    }

    @Test
    void testCreateCommitWithoutParent() {
        Commit commit = new Commit(treeHash, null, message, author);
        
        assertEquals(treeHash, commit.getTreeHash());
        assertNull(commit.getParentHash());
        assertEquals(message, commit.getMessage());
        assertEquals(author, commit.getAuthor());
    }

    @Test
    void testSetAndGetHash() {
        Commit commit = new Commit(treeHash, parentHash, message, author);
        commit.setHash("newhash123");
        
        assertEquals("newhash123", commit.getHash());
    }

    @Test
    void testSerializeCommitWithParent() {
        Commit commit = new Commit(treeHash, parentHash, message, author);
        byte[] serialized = commit.serialize();
        String content = new String(serialized);
        
        assertTrue(content.contains("tree " + treeHash));
        assertTrue(content.contains("parent " + parentHash));
        assertTrue(content.contains("author " + author));
        assertTrue(content.contains(message));
    }

    @Test
    void testSerializeCommitWithoutParent() {
        Commit commit = new Commit(treeHash, null, message, author);
        byte[] serialized = commit.serialize();
        String content = new String(serialized);
        
        assertTrue(content.contains("tree " + treeHash));
        assertFalse(content.contains("parent"));
        assertTrue(content.contains("author " + author));
        assertTrue(content.contains(message));
    }

    @Test
    void testDeserializeCommitWithParent() {
        Commit original = new Commit(treeHash, parentHash, message, author);
        byte[] serialized = original.serialize();
        
        Commit deserialized = Commit.deserialize(serialized);
        
        assertEquals(original.getTreeHash(), deserialized.getTreeHash());
        assertEquals(original.getParentHash(), deserialized.getParentHash());
        assertEquals(original.getMessage(), deserialized.getMessage());
        assertEquals(original.getAuthor(), deserialized.getAuthor());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
    }

    @Test
    void testDeserializeCommitWithoutParent() {
        Commit original = new Commit(treeHash, null, message, author);
        byte[] serialized = original.serialize();
        
        Commit deserialized = Commit.deserialize(serialized);
        
        assertEquals(original.getTreeHash(), deserialized.getTreeHash());
        assertNull(deserialized.getParentHash());
        assertEquals(original.getMessage(), deserialized.getMessage());
        assertEquals(original.getAuthor(), deserialized.getAuthor());
    }

    @Test
    void testMultilineMessage() {
        String multilineMessage = "First line\nSecond line\nThird line";
        Commit commit = new Commit(treeHash, parentHash, multilineMessage, author);
        byte[] serialized = commit.serialize();
        
        Commit deserialized = Commit.deserialize(serialized);
        assertEquals(multilineMessage, deserialized.getMessage());
    }

    @Test
    void testTimestampIsSet() {
        long before = System.currentTimeMillis() / 1000;
        Commit commit = new Commit(treeHash, parentHash, message, author);
        long after = System.currentTimeMillis() / 1000;
        
        assertTrue(commit.getTimestamp() >= before);
        assertTrue(commit.getTimestamp() <= after);
    }
}
