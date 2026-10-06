package com.thanu.steady.di

import android.content.Context
import androidx.room.Room
import com.thanu.steady.data.SteadyDatabase
import com.thanu.steady.platform.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

import com.thanu.steady.platform.AlarmAdapter
import com.thanu.steady.platform.NotificationAdapter

class AppContainer(private val context: Context) {
    val keyManager by lazy { DatabaseKeyManager(context) }
    val alarmAdapter by lazy { AlarmAdapter(context) }
    val activityAlarms by lazy { com.thanu.steady.platform.ActivityAlarmAdapter(context) }
    val routineReminders by lazy { com.thanu.steady.platform.RoutineReminders(context,{ database },clock) }
    val notificationAdapter by lazy { NotificationAdapter(context) }
    val documentAdapter by lazy { com.thanu.steady.platform.DocumentAdapter(context) }
    val bootstrap by lazy { com.thanu.steady.platform.BootstrapStore(context) }
    val clock: java.time.Clock = java.time.Clock.systemUTC()
    val recoveryRepository by lazy { com.thanu.steady.data.RecoveryRepository { database } }
    val preferencesRepository by lazy { com.thanu.steady.data.PreferencesRepository { database } }
    val privateKeyManager by lazy { DatabaseKeyManager(context, "SteadyPrivateSafetyKey", "safety_secret", "steady_safety.db") }
    private var openSafetyDatabase: com.thanu.steady.data.PrivateSafetyDatabase? = null
    val safetyDatabase: com.thanu.steady.data.PrivateSafetyDatabase
        @Synchronized get() {
            openSafetyDatabase?.let { return it }
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, com.thanu.steady.data.PrivateSafetyDatabase::class.java, "steady_safety.db")
                .openHelperFactory(SupportOpenHelperFactory(privateKeyManager.getOrGenerateDatabasePassphrase()))
                .addMigrations(com.thanu.steady.data.SAFETY_MIGRATION_1_2)
                .build().also { openSafetyDatabase = it }
        }
    val privateSafetyRepository by lazy { com.thanu.steady.data.PrivateSafetyRepository({ safetyDatabase }, { database }) }
    val expandedRepository by lazy { com.thanu.steady.data.ExpandedRepository({ database }, clock, preferencesRepository) }
    val activityRepository by lazy { com.thanu.steady.data.ActivityRepository({ database }, {
        com.thanu.steady.domain.ActivityClock(clock.millis(), android.os.SystemClock.elapsedRealtime(),
            android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, 0).toLong())
    }, preferencesRepository) }
    @Volatile var isForeground = false
    val timerRepository by lazy { com.thanu.steady.data.TimerRepository({ database }) {
        com.thanu.steady.data.TimerTime(clock.instant(), android.os.SystemClock.elapsedRealtime(),
            android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, 0).toLong())
    } }
    
    private var openDatabase: SteadyDatabase? = null
    val database: SteadyDatabase
        @Synchronized get() {
        openDatabase?.let { return it }
        System.loadLibrary("sqlcipher")
        val passphrase = keyManager.getOrGenerateDatabasePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(context, SteadyDatabase::class.java, "steady_encrypted.db")
            .openHelperFactory(factory)
            .addMigrations(SteadyDatabase.MIGRATION_1_2, SteadyDatabase.MIGRATION_2_3, com.thanu.steady.data.EXPANDED_MIGRATION_3_4)
            .build().also { openDatabase = it }
    }

    @Synchronized
    fun deleteLocalData() {
        routineReminders.cancelAll()
        openDatabase?.close()
        openDatabase = null
        openSafetyDatabase?.close()
        openSafetyDatabase = null
        val databaseFile = context.getDatabasePath("steady_encrypted.db")
        if (databaseFile.exists()) check(context.deleteDatabase("steady_encrypted.db"))
        if (context.getDatabasePath("steady_safety.db").exists()) check(context.deleteDatabase("steady_safety.db"))
        keyManager.deleteKeyMaterial()
        privateKeyManager.deleteKeyMaterial()
        notificationAdapter.cancelAll()
    }
}
