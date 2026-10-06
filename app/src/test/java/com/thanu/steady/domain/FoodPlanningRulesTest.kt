package com.thanu.steady.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class FoodPlanningRulesTest {
    @Test fun pantrySelectionRetainsCustomEntriesAndDoesNotDuplicateChoices() {
        val text = "Rice, Custom ingredient\nEggs"
        assertTrue(PantryRules.selected(text,"rice"))
        assertEquals(listOf("Rice","Custom ingredient","Eggs"),PantryRules.entries(PantryRules.toggle(text,"Rice",true)))
        assertEquals(listOf("Custom ingredient","Eggs"),PantryRules.entries(PantryRules.toggle(text,"rice",false)))
        assertEquals(listOf("Rice","Custom ingredient","Eggs","Lentils"),PantryRules.entries(PantryRules.toggle(text,"Lentils",true)))
    }
    @Test fun adoptionIdentityIsIndependentOfEditsAndInvalidPlanOrContextIsRejected() {
        val plan = MealPlanProposal(UUID.randomUUID().toString(),UUID.randomUUID().toString(),"2026-01-05","Synthetic meal","Synthetic notes",600,15,"UTC",240)
        plan.validate()
        assertEquals(plan.planId,plan.copy(title="Renamed",day="2026-01-06").planId)
        MealPlanAssociation(plan.id,plan.recipeId,plan.planId).validate()
        assertTrue(runCatching { plan.copy(minutes=0).validate() }.isFailure)
        assertTrue(runCatching { RecipeContext("UNKNOWN").validate() }.isFailure)
        assertTrue(runCatching { MealPlanAssociation(plan.id,plan.recipeId,UUID.randomUUID().toString()).validate() }.isFailure)
    }
}
