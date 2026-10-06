package com.thanu.steady.domain

import org.junit.Assert.*
import org.junit.Test

class BackupServiceTest {
    private val crypto = BackupService()
    private val password = "synthetic password for tests".toCharArray()
    private val json = "{\"schema\":1,\"plans\":[],\"reviews\":[],\"timers\":[]}"

    @Test fun roundTripWithoutDeviceKey() {
        val archive = crypto.createEncryptedBackup(json, password)
        assertEquals(json, BackupService().restoreEncryptedBackup(archive, password))
        assertFalse(String(archive, Charsets.ISO_8859_1).contains("schema"))
    }
    @Test fun samePayloadProducesDifferentArchives() {
        assertFalse(crypto.createEncryptedBackup(json, password).contentEquals(crypto.createEncryptedBackup(json, password)))
    }
    @Test fun wrongPasswordCannotAuthenticate() {
        assertNull(crypto.restoreEncryptedBackup(crypto.createEncryptedBackup(json, password), "another synthetic password".toCharArray()))
    }
    @Test fun modifiedCiphertextAndAuthenticatedHeaderAreRejected() {
        val archive = crypto.createEncryptedBackup(json, password)
        val modified = archive.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertNull(crypto.restoreEncryptedBackup(modified, password))
        val header = archive.copyOf().also { it[20] = (it[20].toInt() xor 1).toByte() }
        assertNull(crypto.restoreEncryptedBackup(header, password))
    }
    @Test fun truncationTrailingBytesAndUnknownVersionAreRejected() {
        val archive = crypto.createEncryptedBackup(json, password)
        assertNull(crypto.restoreEncryptedBackup(archive.copyOf(archive.size - 1), password))
        assertNull(crypto.restoreEncryptedBackup(archive + byteArrayOf(0), password))
        assertNull(crypto.restoreEncryptedBackup(archive.copyOf().also { it[7] = '9'.code.toByte() }, password))
    }
    @Test fun hostileWorkFactorAndOldPlaintextStubAreRejected() {
        val archive = crypto.createEncryptedBackup(json, password).also { it[8] = 127 }
        assertNull(crypto.restoreEncryptedBackup(archive, password))
        assertNull(crypto.restoreEncryptedBackup("ENCRYPTED_BACKUP_STUB:payload".toByteArray(), password))
    }
    @Test(expected = IllegalArgumentException::class) fun shortPasswordIsRejected() {
        crypto.createEncryptedBackup(json, "short".toCharArray())
    }
    @Test fun oversizedArchiveIsRejectedBeforeDerivation() {
        assertNull(crypto.restoreEncryptedBackup(ByteArray(BackupService.MAX_ARCHIVE_BYTES + 1), password))
    }
}
