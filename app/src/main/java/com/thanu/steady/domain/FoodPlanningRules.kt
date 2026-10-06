package com.thanu.steady.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Serializable data class RecipeContext(val mealType: String? = null, val context: String = "") {
    fun validate() {
        require(mealType == null || mealType in setOf("BREAKFAST","LUNCH","DINNER","SNACK"))
        require(context.length <= 1000)
    }
}
data class MealPlanProposal(val id: String, val recipeId: String, val day: String, val title: String,
    val notes: String, val minute: Int?, val minutes: Int, val zone: String, val boundary: Int) {
    val planId get() = UUID.nameUUIDFromBytes("meal-plan:$id".toByteArray(Charsets.UTF_8)).toString()
    fun validate() {
        require(UUID.fromString(id).toString() == id && UUID.fromString(recipeId).toString() == recipeId)
        LocalDate.parse(day); ZoneId.of(zone)
        require(title.isNotBlank() && title.length <= 500 && notes.length <= 100_000)
        require(minute == null || minute in 0..1439)
        require(minutes in 1..1440 && boundary in 0..1439)
    }
}
@Serializable data class MealPlanAssociation(val id: String, val recipeId: String, val planId: String) {
    fun validate() {
        require(UUID.fromString(id).toString() == id && UUID.fromString(recipeId).toString() == recipeId)
        require(planId == UUID.nameUUIDFromBytes("meal-plan:$id".toByteArray(Charsets.UTF_8)).toString())
    }
}
object PantryRules {
    fun entries(text: String) = text.split(',', '\n').map(String::trim).filter(String::isNotEmpty)
    fun selected(text: String, item: String) = entries(text).any { it.equals(item,ignoreCase=true) }
    fun toggle(text: String, item: String, checked: Boolean): String {
        require(item.isNotBlank() && item.length <= 100 && ',' !in item && '\n' !in item)
        val existing = entries(text)
        return (if(checked && !selected(text,item)) existing + item else if(!checked) existing.filterNot { it.equals(item,ignoreCase=true) } else existing).joinToString("\n")
    }
}
