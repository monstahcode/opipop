package com.ivanarroyo.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashUtilsTest {

    @Test
    void testSha1EmptyString() {
        byte[] data = "".getBytes();
        String hash = HashUtils.sha1(data);
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hash);
    }

    @Test
    void testSha1SimpleString() {
        byte[] data = "hello world".getBytes();
        String hash = HashUtils.sha1(data);
        assertEquals("2aae6c35c94fcfb415dbe95f408b9ce91ee846ed", hash);
    }

    @Test
    void testSha1Consistency() {
        byte[] data = "test content".getBytes();
        String hash1 = HashUtils.sha1(data);
        String hash2 = HashUtils.sha1(data);
        assertEquals(hash1, hash2);
    }

    @Test
    void testSha1DifferentContent() {
        byte[] data1 = "content1".getBytes();
        byte[] data2 = "content2".getBytes();
        String hash1 = HashUtils.sha1(data1);
        String hash2 = HashUtils.sha1(data2);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void testSha1HashLength() {
        byte[] data = "any content".getBytes();
        String hash = HashUtils.sha1(data);
        assertEquals(40, hash.length());
    }

    @Test
    void testSha1WithBinaryData() {
        byte[] data = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF};
        String hash = HashUtils.sha1(data);
        assertNotNull(hash);
        assertEquals(40, hash.length());
    }
}
