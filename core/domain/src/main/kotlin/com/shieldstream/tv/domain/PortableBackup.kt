package com.shieldstream.tv.domain

interface PortableBackup {
    val allowed: Boolean
    fun export(password: CharArray): ByteArray
    /** Authenticate and validate all data before replacing local preferences. Restart immediately afterward. */
    fun restore(encrypted: ByteArray, password: CharArray)
}
