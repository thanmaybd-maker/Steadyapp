package com.thanu.steady.domain

data class FoodTemplate(
    val id: String,
    val title: String,
    val ingredients: List<String>,
    val isVegetarian: Boolean,
    val source: String = "Steady Nutrition Reference 1.0"
)

data class FoodPreference(
    val dietPattern: String = "mixed",
    val avoidedIngredients: List<String> = emptyList()
)

object StaticFoodReference {
    val templates = listOf(
        FoodTemplate(
            id = "bf_1",
            title = "Idli or Dosa",
            ingredients = listOf("Idli/Dosa", "Sambar", "Curd"),
            isVegetarian = true
        ),
        FoodTemplate(
            id = "bf_2",
            title = "Oats and Banana",
            ingredients = listOf("Oats", "Milk or fortified soy drink", "Banana"),
            isVegetarian = true
        ),
        FoodTemplate(
            id = "lunch_1",
            title = "Standard Lunch",
            ingredients = listOf("Rice or Chapati", "Dal/Sambar", "Vegetable preparation", "Curd"),
            isVegetarian = true
        ),
        FoodTemplate(
            id = "lunch_mixed",
            title = "Mixed Lunch",
            ingredients = listOf("Rice", "Fish or Chicken", "Vegetable preparation"),
            isVegetarian = false
        )
    )
}
