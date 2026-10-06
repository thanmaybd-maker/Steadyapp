package com.thanu.steady.domain

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupService {
    companion object {
        const val MAX_PAYLOAD_BYTES = 50 * 1024 * 1024
        const val MAX_ARCHIVE_BYTES = MAX_PAYLOAD_BYTES + 60
        const val ITERATIONS = 600_000
        private const val HEADER_BYTES = 44
        private val MAGIC = "STDYBK01".toByteArray(Charsets.US_ASCII)
    }

    fun createEncryptedBackup(jsonData: String, passphrase: CharArray): ByteArray {
        require(passphrase.size in 12..1024)
        val plaintext = jsonData.toByteArray(Charsets.UTF_8)
        try {
            require(plaintext.size <= MAX_PAYLOAD_BYTES)
            val random = SecureRandom()
            val salt = ByteArray(16).also(random::nextBytes)
            val nonce = ByteArray(12).also(random::nextBytes)
            val header = ByteBuffer.allocate(HEADER_BYTES).put(MAGIC).putInt(ITERATIONS)
                .put(salt).put(nonce).putInt(plaintext.size + 16).array()
            val key = derive(passphrase, salt)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                cipher.updateAAD(header)
                return header + cipher.doFinal(plaintext)
            } finally {
                key.fill(0)
            }
        } finally {
            plaintext.fill(0)
        }
    }

    fun restoreEncryptedBackup(data: ByteArray, passphrase: CharArray): String? {
        if (data.size !in 60..MAX_ARCHIVE_BYTES || passphrase.size !in 12..1024) return null
        val buffer = ByteBuffer.wrap(data)
        val magic = ByteArray(8).also(buffer::get)
        if (!magic.contentEquals(MAGIC) || buffer.int != ITERATIONS) return null
        val salt = ByteArray(16).also(buffer::get)
        val nonce = ByteArray(12).also(buffer::get)
        if (buffer.int != data.size - HEADER_BYTES) return null
        return try {
            val key = derive(passphrase, salt)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                cipher.updateAAD(data, 0, HEADER_BYTES)
                val plaintext = cipher.doFinal(data, HEADER_BYTES, data.size - HEADER_BYTES)
                try {
                    Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(plaintext)).toString()
                } finally {
                    plaintext.fill(0)
                }
            } finally {
                key.fill(0)
            }
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: java.nio.charset.CharacterCodingException) {
            null
        }
    }

    private fun derive(passphrase: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
