package com.thanu.steady.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SafetyDao {
    @Query("SELECT * FROM safety_plan WHERE id = 1")
    suspend fun getPlan(): SafetyPlanEntity?

    @Query("SELECT * FROM support_contact WHERE planId = 1 ORDER BY sortOrder ASC")
    suspend fun getContacts(): List<SupportContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SafetyPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<SupportContactEntity>)

    @Query("DELETE FROM support_contact WHERE planId = 1")
    suspend fun clearContacts()

    @Query("DELETE FROM safety_plan WHERE id = 1")
    suspend fun clearPlan()

    @Transaction
    suspend fun saveFullPlan(plan: SafetyPlanEntity, contacts: List<SupportContactEntity>) {
        insertPlan(plan)
        clearContacts()
        insertContacts(contacts)
    }
}
