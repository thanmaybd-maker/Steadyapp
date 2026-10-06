package com.thanu.steady

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.thanu.steady.ui.SteadyAppNavigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.thanu.steady.di.AppContainer

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer
    private lateinit var accessModel: com.thanu.steady.ui.AccessViewModel
    private var authenticatedAction: (() -> Unit)? = null
    private val credential = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) { accessModel.authenticated(); authenticatedAction?.invoke() }
        authenticatedAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        appContainer = (application as SteadyApplication).container
        accessModel = androidx.lifecycle.ViewModelProvider(this, object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return com.thanu.steady.ui.AccessViewModel(appContainer.bootstrap) as T
            }
        })[com.thanu.steady.ui.AccessViewModel::class.java]
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            val access by accessModel.state.collectAsState()
            var profile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.thanu.steady.data.ExpandedProfile()) }
            var safetyVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            androidx.compose.runtime.SideEffect {
                if (!access.canOpenPrivate || access.bootstrap.hideRecents || safetyVisible) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
            val saved = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
            com.thanu.steady.ui.SteadyTheme(profile = if (access.canOpenPrivate) profile else com.thanu.steady.data.ExpandedProfile(theme = "DARK", highContrast = true)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (access.canOpenPrivate) saved.SaveableStateProvider("private_navigation") {
                        com.thanu.steady.ui.ExpandedNavigation(appContainer, access, ::authenticate, { profile = it }, { safetyVisible = it })
                    } else com.thanu.steady.ui.ExpandedPage {
                        com.thanu.steady.ui.PublicSafetyPanel(access.bootstrap.country)
                        if (access.loading) Text(getString(R.string.loading_access))
                        else if (access.failed) Text(getString(R.string.access_bootstrap_failed))
                        else com.thanu.steady.ui.PrimaryAction(R.string.unlock_app) { authenticate(null) }
                    }
                }
            }
        }
    }
    private fun authenticate(onSuccess: (() -> Unit)?) {
        val keyguard = getSystemService(android.app.KeyguardManager::class.java)
        if (!keyguard.isDeviceSecure) {
            android.widget.Toast.makeText(this, R.string.device_lock_required, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        authenticatedAction = onSuccess
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val prompt = android.hardware.biometrics.BiometricPrompt.Builder(this)
                .setTitle(getString(R.string.unlock_app))
                .setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()
            prompt.authenticate(android.os.CancellationSignal(), mainExecutor, object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult) {
                    accessModel.authenticated(); authenticatedAction?.invoke(); authenticatedAction = null
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { authenticatedAction = null }
            })
        } else {
            @Suppress("DEPRECATION")
            val intent = keyguard.createConfirmDeviceCredentialIntent(getString(R.string.unlock_app), getString(R.string.unlock_description))
            if (intent != null) credential.launch(intent)
        }
    }
    override fun onStart() { super.onStart(); if (::appContainer.isInitialized) appContainer.isForeground = true }
    override fun onStop() {
        if (::appContainer.isInitialized) appContainer.isForeground = false
        if (::accessModel.isInitialized) accessModel.relock()
        super.onStop()
    }
}
