package com.thanu.steady

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.platform.DocumentAdapter
import com.thanu.steady.data.PortableCodec
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.io.RandomAccessFile

class DocumentProviderTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun uri(name: String) = Uri.parse("content://${context.packageName}.synthetic.documents/$name")
    @Before fun isolate() { check(context.packageName.endsWith(".qa")) }
    @After fun cleanup() { listOf("roundtrip","oversized").forEach { File(context.cacheDir,"qa-document-$it.bin").delete() } }
    @Test fun unicodeMarkdownAndEncryptedBytesRoundTripThroughContentResolver() {
        val adapter = DocumentAdapter(context)
        val text = "# Synthetic scratchpad\nα² + β² = γ²\nಕನ್ನಡ\n"
        assertTrue(adapter.writeMarkdownToUri(uri("roundtrip"),text))
        assertEquals(text,context.contentResolver.openInputStream(uri("roundtrip"))!!.bufferedReader().use { it.readText() })
        val bytes = ByteArray(512) { (it % 251).toByte() }
        assertTrue(adapter.writeBackupToUri(uri("roundtrip"),bytes))
        assertArrayEquals(bytes,adapter.readBackupFromUri(uri("roundtrip")))
    }
    @Test fun unavailableProviderAndOversizedInputReturnVisibleFailureSignals() {
        val adapter = DocumentAdapter(context)
        assertFalse(adapter.writeMarkdownToUri(uri("missing"),"Synthetic"))
        assertFalse(adapter.writeBackupToUri(uri("missing"),byteArrayOf(1)))
        assertNull(adapter.readBackupFromUri(uri("missing")))
        RandomAccessFile(File(context.cacheDir,"qa-document-oversized.bin"),"rw").use { it.setLength(PortableCodec.MAX_BYTES.toLong()+61) }
        assertNull(adapter.readBackupFromUri(uri("oversized")))
    }
    @Test fun legacyJsonUsesStrictUtf8AndItsSmallerBoundWithoutReturningPartialInput() {
        val adapter=DocumentAdapter(context)
        val text="{\"plans\":[],\"synthetic\":\"α\"}"
        assertTrue(adapter.writeMarkdownToUri(uri("roundtrip"),text))
        assertEquals(text,adapter.readLegacyJsonFromUri(uri("roundtrip")))
        assertTrue(adapter.writeBackupToUri(uri("roundtrip"),byteArrayOf(0xC3.toByte(),0x28)))
        assertNull(adapter.readLegacyJsonFromUri(uri("roundtrip")))
        RandomAccessFile(File(context.cacheDir,"qa-document-oversized.bin"),"rw").use { it.setLength(com.thanu.steady.data.LegacyOgCodec.MAX_BYTES.toLong()+1) }
        assertNull(adapter.readLegacyJsonFromUri(uri("oversized")))
    }
}
