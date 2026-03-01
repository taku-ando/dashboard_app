package com.example.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val fileName: String,
    val fileHash: String,
    val cardId: String?,
    val count: Int,
    val importedAt: String
)
