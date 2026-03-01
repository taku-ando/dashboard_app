package com.example.dashboard.domain.model

data class Budget(
    val id: String,
    val categoryId: String,
    val year: Int,
    val month: Int,
    val limitAmount: Int
)
