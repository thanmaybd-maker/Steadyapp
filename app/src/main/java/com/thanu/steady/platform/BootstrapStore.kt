package com.thanu.steady.platform

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.bootstrapDataStore by preferencesDataStore(name = "steady_bootstrap")
data class BootstrapState(val locked: Boolean = false, val hideRecents: Boolean = true, val country: String = "IN")

/** Only nonsensitive access/bootstrap flags. No names, notes, health records or timer tokens. */
class BootstrapStore(context: Context) {
    private val store = context.applicationContext.bootstrapDataStore
    private val lock = booleanPreferencesKey("require_device_auth")
    private val recents = booleanPreferencesKey("hide_recents")
    private val country = stringPreferencesKey("public_directory_country")
    val states = store.data.map { BootstrapState(it[lock] ?: false, it[recents] ?: true, it[country] ?: "IN") }
    suspend fun setLock(value: Boolean) { store.edit { it[lock] = value } }
    suspend fun setRecents(value: Boolean) { store.edit { it[recents] = value } }
    suspend fun setCountry(value: String) { require(value in setOf("IN", "OTHER")); store.edit { it[country] = value } }
    suspend fun clear() { store.edit { it.clear() } }
}
