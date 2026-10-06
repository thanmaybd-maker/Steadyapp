package com.thanu.steady.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun LicensesScreen(onBack: () -> Unit, onSafety: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        text = null; failed = false
        selected?.let { asset ->
            try { text = withContext(Dispatchers.IO) { context.assets.open("licenses/$asset").bufferedReader().use { it.readText() } } }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
        }
    }
    ExpandedPage {
        SecondaryAction(R.string.safety_action,onClick = onSafety)
        SecondaryAction(R.string.back_action) { if(selected == null) onBack() else selected = null }
        SectionCard(R.string.third_party_notices) {
            if(selected == null) {
                Text(stringResource(R.string.licences_description))
                listOf("runtime-inventory.csv" to R.string.runtime_dependencies,
                    "apache-2.0.txt" to R.string.apache_licence,
                    "sqlcipher-android.txt" to R.string.sqlcipher_android_notice,
                    "sqlcipher-community.txt" to R.string.sqlcipher_core_notice,
                    "openssl-3.0.16.txt" to R.string.openssl_notice,
                    "steady-og-source.txt" to R.string.og_source_notice).forEach { (asset,label) ->
                    SecondaryAction(label) { selected = asset }
                }
            } else if(failed) {
                Text(stringResource(R.string.notice_load_failed))
                SecondaryAction(R.string.retry) { selected = null }
            } else text?.let { Text(it) } ?: Text(stringResource(R.string.loading_records))
        }
    }
}
