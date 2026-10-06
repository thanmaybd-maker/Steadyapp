package com.thanu.steady.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom
import android.util.Base64

class DatabaseKeyManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "SteadyDbKeyAlias"
        private const val DB_SECRET_FILE = "db_secret.enc"
        private const val DB_IV_FILE = "db_secret_iv.enc"
    }

    fun getOrGenerateDatabasePassphrase(): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val secretFile = File(context.filesDir, DB_SECRET_FILE)
        val ivFile = File(context.filesDir, DB_IV_FILE)
        val databaseExists = context.getDatabasePath("steady_encrypted.db").exists()
        val hasMetadata = secretFile.exists() || ivFile.exists()
        if (hasMetadata || databaseExists) {
            check(secretFile.exists() && ivFile.exists() && keyStore.containsAlias(ALIAS)) {
                "Encrypted storage key is unavailable; existing records were preserved"
            }
            val existingKey = keyStore.getKey(ALIAS, null) as SecretKey
            return unwrapSecret(existingKey, secretFile.readBytes(), ivFile.readBytes())
        }
        
        if (!keyStore.containsAlias(ALIAS)) {
            generateKeystoreKey()
        }
        
        val secretKey = keyStore.getKey(ALIAS, null) as SecretKey
        
        
        if (secretFile.exists() && ivFile.exists()) {
            return unwrapSecret(secretKey, secretFile.readBytes(), ivFile.readBytes())
        } else {
            val generatedSecret = ByteArray(32)
            SecureRandom().nextBytes(generatedSecret)
            val (wrappedSecret, iv) = wrapSecret(secretKey, generatedSecret)
            fun writeAtomic(file: File, bytes: ByteArray) {
                val atomic = android.util.AtomicFile(file)
                val stream = atomic.startWrite()
                try { stream.write(bytes); atomic.finishWrite(stream) }
                catch (failure: Exception) { atomic.failWrite(stream); throw failure }
            }
            writeAtomic(ivFile, iv)
            writeAtomic(secretFile, wrappedSecret)
            return generatedSecret
        }
    }

    fun deleteKeyMaterial() {
        listOf(DB_SECRET_FILE, DB_IV_FILE).forEach {
            val file = File(context.filesDir, it)
            if (file.exists()) check(file.delete())
            val backup = File(context.filesDir, "$it.bak")
            if (backup.exists()) check(backup.delete())
        }
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
    }

    private fun generateKeystoreKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun wrapSecret(keystoreKey: SecretKey, plainSecret: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainSecret)
        return Pair(encrypted, iv)
    }

    private fun unwrapSecret(keystoreKey: SecretKey, wrappedSecret: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey, spec)
        return cipher.doFinal(wrappedSecret)
    }
}
