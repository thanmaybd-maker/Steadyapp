package com.thanu.steady.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.platform.DialerAdapter

/** No repository, authentication or personal fields enter this public component. */
@Composable fun PublicSafetyPanel(country: String = "IN", onPrivate: (() -> Unit)? = null) {
    val context = LocalContext.current
    var dialFailed by remember { mutableStateOf(false) }
    SectionCard(R.string.public_help) {
        Text(stringResource(R.string.public_directory_reviewed))
        if (country != "IN") Text(stringResource(R.string.directory_region_limit))
        Text(stringResource(R.string.india_directory))
        PrimaryAction(R.string.dial_telemanas) { dialFailed = !DialerAdapter(context).openDialer("14416") }
        PrimaryAction(R.string.dial_emergency) { dialFailed = !DialerAdapter(context).openDialer("112") }
        if (dialFailed) Text(stringResource(R.string.dialer_unavailable), color = MaterialTheme.colorScheme.error)
        onPrivate?.let { SecondaryAction(R.string.open_private_plan, onClick = it) }
    }
}
