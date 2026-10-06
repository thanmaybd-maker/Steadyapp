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
    val notificationAdapter by lazy { NotificationAdapter(context) }
    val documentAdapter by lazy { com.thanu.steady.platform.DocumentAdapter(context) }
    val clock: java.time.Clock = java.time.Clock.systemUTC()
    val recoveryRepository by lazy { com.thanu.steady.data.RecoveryRepository { database } }
    
    private var openDatabase: SteadyDatabase? = null
    val database: SteadyDatabase
        @Synchronized get() {
        openDatabase?.let { return it }
        System.loadLibrary("sqlcipher")
        val passphrase = keyManager.getOrGenerateDatabasePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(context, SteadyDatabase::class.java, "steady_encrypted.db")
            .openHelperFactory(factory)
            .addMigrations(SteadyDatabase.MIGRATION_1_2)
            .build().also { openDatabase = it }
    }

    @Synchronized
    fun deleteLocalData() {
        openDatabase?.close()
        openDatabase = null
        val databaseFile = context.getDatabasePath("steady_encrypted.db")
        if (databaseFile.exists()) check(context.deleteDatabase("steady_encrypted.db"))
        keyManager.deleteKeyMaterial()
        notificationAdapter.cancelAll()
    }
}
