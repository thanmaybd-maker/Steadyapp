package com.thanu.steady.di

import android.content.Context
import androidx.room.Room
import com.thanu.steady.data.SteadyDatabase
import com.thanu.steady.platform.DatabaseKeyManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

import com.thanu.steady.platform.AlarmAdapter
import com.thanu.steady.platform.NotificationAdapter

class AppContainer(private val context: Context) {
    val keyManager by lazy { DatabaseKeyManager(context) }
    val alarmAdapter by lazy { AlarmAdapter(context) }
    val notificationAdapter by lazy { NotificationAdapter(context) }
    val documentAdapter by lazy { com.thanu.steady.platform.DocumentAdapter(context) }
    
    val database: SteadyDatabase by lazy {
        val passphrase = keyManager.getOrGenerateDatabasePassphrase()
        val factory = SupportFactory(passphrase)
        Room.databaseBuilder(context, SteadyDatabase::class.java, "steady_encrypted.db")
            .openHelperFactory(factory)
            .addMigrations(SteadyDatabase.MIGRATION_1_2)
            .build()
    }
}
