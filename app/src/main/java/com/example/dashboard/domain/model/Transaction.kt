package com.example.dashboard.domain.model

data class Transaction(
    val id: String,
    val cardId: String?,
    val importId: String?,
    val date: String,
    val name: String,
    val categoryId: String?,
    val amount: Int,
    val memo: String?,
    val source: String,
    val classifiedBy: String
)
