package com.thanu.steady

import androidx.activity.ComponentActivity

/** Test-only host: it never opens the owner's app container or private Safety. */
class UiHarnessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
