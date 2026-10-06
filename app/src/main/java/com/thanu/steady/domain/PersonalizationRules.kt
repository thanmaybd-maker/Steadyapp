package com.thanu.steady.domain

object PersonalizationRules {
    val metrics = setOf("STEPS","FOCUS","HABITS","WATER","WORKOUTS","SLEEP")
    val reviewCards = setOf("FOCUS","HABITS","WORKOUTS","WATER","SLEEP")
    fun csv(value: String, allowed: Set<String>): List<String> {
        val keys = value.split(',').filter(String::isNotBlank)
        require(keys.distinct().size == keys.size && keys.all { it in allowed })
        return keys
    }
    fun waterQuantities(value: String): List<Int> {
        val numbers = value.split(',').map { it.trim().toInt() }
        require(numbers.size in 1..5 && numbers.distinct().size == numbers.size && numbers.all { it in 1..2000 })
        return numbers
    }
    fun validate(rings: String, quantities: String, unit: String, cards: String) {
        require(csv(rings,metrics).size == 3)
        csv(cards,reviewCards)
        waterQuantities(quantities)
        require(unit in setOf("ML","FLOZ"))
    }
}
