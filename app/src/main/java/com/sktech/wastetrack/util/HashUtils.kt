package com.sktech.wastetrack.util

import java.security.MessageDigest

object HashUtils {
    /**
     * Generates a SHA-256 hash of the given content for audit trail immutability.
     */
    fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a content hash for a scrap entry for audit trail.
     */
    fun hashScrapEntry(
        id: String,
        category: String,
        weightKg: Float,
        factoryId: String,
        timestamp: Long
    ): String {
        val raw = "$id|$category|$weightKg|$factoryId|$timestamp"
        return sha256(raw)
    }

    /**
     * Generates a content hash for a transfer for audit trail.
     */
    fun hashTransfer(
        id: String,
        scrapEntryId: String,
        weightAtSource: Float,
        supervisorId: String,
        timestamp: Long
    ): String {
        val raw = "$id|$scrapEntryId|$weightAtSource|$supervisorId|$timestamp"
        return sha256(raw)
    }
}
