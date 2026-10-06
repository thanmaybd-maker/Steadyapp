package com.thanu.steady.data

import androidx.room.*
import kotlinx.serialization.Serializable
import java.util.UUID

/** These types never enter organiser export/backup/AI contracts. */
@Serializable @Entity(tableName="private_safety_note")
data class PrivateSafetyNote(@PrimaryKey val id: String,val title: String="",val content: String="",
    val category: String="CONTACT",val pinned: Boolean=false,val created: Long,val updated: Long,val revision: Int=0) {
    fun validate(draft: Boolean=false) {
        require(UUID.fromString(id).toString() == id && revision in 0 until Int.MAX_VALUE && created >= 0 && updated >= created)
        require(title.length <= 500 && content.length <= 100_000)
        require(draft || title.isNotBlank() || content.isNotBlank())
        require(category in setOf("CONTACT","MEDICAL","ROUTE","CAMPUS","GROUNDING","OTHER"))
    }
}
@Entity(tableName="private_safety_note_draft")
data class PrivateSafetyNoteDraft(@PrimaryKey val id: Int=1,val content: String)
@Dao interface PrivateSafetyNotesDao {
    @Query("SELECT * FROM private_safety_note ORDER BY pinned DESC,updated DESC,id") suspend fun all(): List<PrivateSafetyNote>
    @Query("SELECT * FROM private_safety_note WHERE id = :id") suspend fun note(id: String): PrivateSafetyNote?
    @Query("SELECT COUNT(*) FROM private_safety_note") suspend fun count(): Int
    @Upsert suspend fun save(note: PrivateSafetyNote)
    @Query("DELETE FROM private_safety_note WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT * FROM private_safety_note_draft WHERE id = 1") suspend fun draft(): PrivateSafetyNoteDraft?
    @Upsert suspend fun save(draft: PrivateSafetyNoteDraft)
    @Query("DELETE FROM private_safety_note_draft") suspend fun clearDraft()
}
val SAFETY_MIGRATION_2_3 = object : androidx.room.migration.Migration(2,3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `private_safety_note` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `created` INTEGER NOT NULL, `updated` INTEGER NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `private_safety_note_draft` (`id` INTEGER NOT NULL, `content` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}
