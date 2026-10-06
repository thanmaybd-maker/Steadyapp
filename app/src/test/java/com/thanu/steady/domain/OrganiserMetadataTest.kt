package com.thanu.steady.domain

import com.thanu.steady.data.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class OrganiserMetadataTest {
    @Test fun invalidSettingsAndForgedAssociationsCannotCrossBackupValidation() {
        fun valid(id: String, json: String) = runCatching {
            PortableCodec.validate(ExpandedArchive(notes = listOf(SessionNote(id,null,json,1))))
        }.isSuccess
        assertTrue(valid("ambient:preferences", "{}"))
        assertTrue(valid("study-block-settings", "{\"reminders\":true}"))
        assertFalse(valid("study-block-settings", "{\"reminders\":\"true\"}"))
        assertFalse(valid("ambient:preferences", "{\"volume\":2}"))
        assertFalse(valid("ambient:preferences", "{\"autoPlay\":true,\"autoPlay\":false}"))
        assertFalse(valid("ambient:unknown", "{}"))
        val block = StudyBlockRules.templates(LocalDate.parse("2026-01-05"),"UTC",240,listOf("A","B","C"))[0]
        assertTrue(valid("study-block:${block.id}", Json.encodeToString(block)))
        assertFalse(valid("study-block:${block.id}", Json.encodeToString(block.copy(adoptedPlanId = java.util.UUID.randomUUID().toString()))))
        assertFalse(valid("study-block:${block.id}", Json.encodeToString(block.copy(leadMinutes = -1))))
    }
}
