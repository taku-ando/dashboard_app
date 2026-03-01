package com.example.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val shopName: String,
    val matchType: String,
    val categoryId: String,
    val createdAt: String,
    val updatedAt: String
)
