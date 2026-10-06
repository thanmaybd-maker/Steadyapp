package com.thanu.steady.domain

import com.thanu.steady.data.PrivateSafetyNote
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PrivateNoteValidationTest {
    @Test fun incompleteDraftIsAllowedButEmptySavedNoteAndInvalidCategoryAreRejected() {
        val draft=PrivateSafetyNote(UUID.randomUUID().toString(),created=1,updated=1)
        draft.validate(draft=true)
        assertTrue(runCatching { draft.validate() }.isFailure)
        draft.copy(content="Synthetic fixture",category="GROUNDING").validate()
        assertTrue(runCatching { draft.copy(content="Synthetic fixture",category="UNKNOWN").validate() }.isFailure)
        assertTrue(runCatching { draft.copy(content="Synthetic fixture",revision=Int.MAX_VALUE).validate() }.isFailure)
        assertTrue(runCatching { draft.copy(content="Synthetic fixture",updated=0).validate() }.isFailure)
    }
}
