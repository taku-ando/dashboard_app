package com.example.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val cardId: String?,
    val importId: String?,
    val date: String,
    val name: String,
    val categoryId: String?,
    val amount: Int,
    val memo: String?,
    val source: String,
    val classifiedBy: String,
    val createdAt: String
)
