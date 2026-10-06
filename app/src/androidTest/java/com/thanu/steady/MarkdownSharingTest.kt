package com.thanu.steady

import android.content.*
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.platform.MarkdownSharingAdapter
import org.junit.Test
import org.junit.Assert.*

class MarkdownSharingTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun exactUnicodePreviewBuildsSensitiveClipAndRecipientFreeChooserWithoutSending() {
        check(context.packageName == "com.thanu.steady.qa")
        val text="# Synthetic organiser\nα² + β² = γ²\n"
        var clip: ClipData?=null; var chooser: Intent?=null
        val adapter=MarkdownSharingAdapter(context,{ clip=it },{ chooser=it })
        assertTrue(adapter.copyPreview(text)); assertTrue(clip?.getItemAt(0)?.text?.toString() == text)
        if(Build.VERSION.SDK_INT >= 33) assertTrue(clip?.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true)
        assertTrue(adapter.sharePreview(text)); assertEquals(Intent.ACTION_CHOOSER,chooser?.action)
        @Suppress("DEPRECATION") val send=chooser?.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertEquals(Intent.ACTION_SEND,send?.action); assertEquals("text/plain",send?.type)
        assertTrue(send?.getStringExtra(Intent.EXTRA_TEXT) == text)
        assertFalse(send?.hasExtra(Intent.EXTRA_EMAIL) == true || send?.hasExtra(Intent.EXTRA_STREAM) == true)
        assertTrue(chooser!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
    @Test fun unavailableClipboardOrChooserAndOversizedUtf8NeverReportSuccessOrTruncate() {
        val adapter=MarkdownSharingAdapter(context,{ error("Synthetic clipboard failure") },{ throw ActivityNotFoundException("Synthetic handler missing") })
        assertFalse(adapter.copyPreview("Synthetic preview")); assertFalse(adapter.sharePreview("Synthetic preview"))
        var copies=0; var launches=0
        val bounded=MarkdownSharingAdapter(context,{ copies++ },{ launches++ })
        val large="α".repeat(MarkdownSharingAdapter.MAX_BYTES/2+1)
        assertFalse(bounded.copyPreview(large)); assertFalse(bounded.sharePreview(large))
        assertEquals(0,copies); assertEquals(0,launches)
    }
}
