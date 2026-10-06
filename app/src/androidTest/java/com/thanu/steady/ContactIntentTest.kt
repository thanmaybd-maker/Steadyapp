package com.thanu.steady

import android.content.*
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.platform.DialerAdapter
import org.junit.Assert.*
import org.junit.Test

class ContactIntentTest {
    @Test fun smsOpensOnlyAComposerAndMissingHandlerDoesNotPretendToSend() {
        var launched: Intent?=null
        val recording=object: ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun startActivity(intent: Intent) { launched=intent }
        }
        assertTrue(DialerAdapter(recording).openSmsComposer("(010) 123-456"))
        assertTrue(launched?.action == Intent.ACTION_SENDTO && launched?.data?.scheme == "smsto")
        assertFalse(launched?.hasExtra("sms_body") ?: true)
        assertFalse(DialerAdapter(recording).openSmsComposer("invalid;number"))
        val unavailable=object: ContextWrapper(recording) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException() }
        }
        assertFalse(DialerAdapter(unavailable).openSmsComposer("010123456"))
    }
}
