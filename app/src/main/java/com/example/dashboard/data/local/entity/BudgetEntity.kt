package com.example.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val categoryId: String,
    val year: Int,
    val month: Int,
    val limitAmount: Int
)
