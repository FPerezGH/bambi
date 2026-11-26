package com.shieldstream.tv.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupCipherTest {
    @Test fun roundTripAndWrongPassword() {
        val password = "a-strong-test-password".toCharArray()
        val plain = "private preference data".toByteArray()
        val a = BackupCipher.encrypt(plain, password)
        val b = BackupCipher.encrypt(plain, password)
        assertFalse(a.contentEquals(b))
        assertArrayEquals(plain, BackupCipher.decrypt(a, password))
        assertFalse(runCatching { BackupCipher.decrypt(a, "wrong-password".toCharArray()) }.isSuccess)
        a[a.lastIndex] = (a.last().toInt() xor 1).toByte()
        assertFalse(runCatching { BackupCipher.decrypt(a, password) }.isSuccess)
    }
}
