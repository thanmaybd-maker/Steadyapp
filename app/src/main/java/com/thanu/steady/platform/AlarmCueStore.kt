package com.thanu.steady.platform

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.thanu.steady.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val Context.alarmCueDataStore by preferencesDataStore(name = "steady_alarm_bootstrap")

/** Separate from personal stores. Excluded from ordinary files and OS backup. */
class AlarmCueStore(context: Context, val time: () -> ActivityClock = {
    ActivityClock(System.currentTimeMillis(), android.os.SystemClock.elapsedRealtime(),
        android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, 0).toLong())
}) {
    private val store = context.applicationContext.alarmCueDataStore
    private val key = stringPreferencesKey("opaque_cues")
    private fun decode(value: String?): Map<String, AlarmCueToken> {
        if(value == null) return emptyMap()
        require(value.length <= 100_000)
        return Json.decodeFromString<Map<String, AlarmCueToken>>(value).also { tokens ->
            require(tokens.size <= 64)
            tokens.forEach { (key, token) -> token.validate(); require(key == token.key) }
        }
    }
    suspend fun tokens(): List<AlarmCueToken> = decode(store.data.first()[key]).values.toList()
    suspend fun register(token: AlarmCueToken) {
        token.validate()
        store.edit { prefs ->
            val entries = decode(prefs[key]).filterValues { it.expiresWall > time().wall || it.delivered && it.routine }.toMutableMap()
            val old = entries[token.key]
            entries[token.key] = if(old != null && old.copy(delivered = false, expiresWall = token.expiresWall) == token.copy(delivered = false)) old else token
            require(entries.size <= 64)
            prefs[key] = Json.encodeToString(entries)
        }
    }
    suspend fun claim(kind: String, id: String, generation: Int, day: String? = null, wallAt: Long? = null): AlarmCueToken? {
        UUIDCheck(id)
        var claimed: AlarmCueToken? = null
        store.edit { prefs ->
            val entries = decode(prefs[key]).toMutableMap()
            val tokenKey = if(day != null) "$kind|$id|$day" else "$kind|$id"
            val token = entries[tokenKey]
            if(token != null && AlarmCueRules.eligible(token, time(), generation, wallAt)) {
                claimed = token
                entries[tokenKey] = token.copy(delivered = true)
                prefs[key] = Json.encodeToString(entries)
            }
        }
        return claimed
    }
    suspend fun cancel(kind: String, id: String) {
        store.edit { prefs -> prefs[key] = Json.encodeToString(decode(prefs[key]).filterValues { it.kind != kind || it.id != id }) }
    }
    suspend fun clearRoutineScheduled() {
        store.edit { prefs -> prefs[key] = Json.encodeToString(decode(prefs[key]).filterValues { !it.routine || it.delivered }) }
    }
    suspend fun acknowledgeRoutines(receipts: List<AlarmCueToken>) {
        val acknowledged = receipts.filter { it.routine && it.delivered }.associateBy { it.key }
        store.edit { prefs -> prefs[key] = Json.encodeToString(decode(prefs[key]).filter { (id, token) -> acknowledged[id] != token }) }
    }
    suspend fun clear() { store.edit { it.clear() } }
    suspend fun invalidateScheduled() {
        store.edit { prefs -> prefs[key] = Json.encodeToString(decode(prefs[key]).filterValues { it.routine && it.delivered }) }
    }
    private fun UUIDCheck(id: String) { java.util.UUID.fromString(id) }
}

/** A locked/background delivery never invokes the private completion callback. */
class BackgroundCueDelivery(private val store: AlarmCueStore, private val privateAccess: () -> Boolean,
    private val completePrivate: suspend (String, String, Int) -> Boolean, private val notify: (AlarmCueToken) -> Unit) {
    suspend fun timer(kind: String, id: String, generation: Int): Boolean {
        if(privateAccess() && !completePrivate(kind, id, generation)) return false
        val token = store.claim(kind, id, generation) ?: return false
        notify(token)
        return true
    }
}
