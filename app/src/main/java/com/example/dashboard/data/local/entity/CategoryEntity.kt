package com.example.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Int
)
