package com.thanu.steady.domain

object ReflectionDraftRules {
    fun rating(value: String): Int? = value.trim().takeIf(String::isNotEmpty)?.let {
        require(it.matches(Regex("[1-5]"))); it.toInt()
    }
}
