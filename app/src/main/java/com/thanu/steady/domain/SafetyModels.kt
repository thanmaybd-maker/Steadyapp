package com.thanu.steady.domain

import java.time.Instant

data class SupportContact(
    val id: String,
    val role: String, // first, backup, clinic, other
    val displayName: String,
    val phone: String,
    val note: String? = null,
    val sortOrder: Int
)

data class SafetyPlan(
    val id: Int = 1,
    val warningSigns: String = "",
    val copingSteps: String = "",
    val safePeoplePlaces: String = "",
    val environmentSteps: String = "",
    val clinicName: String = "",
    val clinicPhone: String = "",
    val followUpAt: String? = null,
    val reviewedByUserAt: Instant? = null,
    val clinicianReviewStatus: String = "unreviewed",
    val updatedAt: Instant = Instant.now(),
    val contacts: List<SupportContact> = emptyList()
)

object PublicHelp {
    val verificationDate = "6 October 2026"
    val numbers = listOf(
        Pair("Tele-MANAS", "14416"),
        Pair("Emergency", "112")
    )
}
