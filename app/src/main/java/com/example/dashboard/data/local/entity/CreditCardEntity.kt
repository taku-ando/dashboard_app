package com.example.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val name: String,
    val issuer: String,
    val colorCode: String,
    val createdAt: String
)
