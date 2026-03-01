package com.example.dashboard.data.repository

import com.example.dashboard.data.local.entity.ImportHistoryEntity
import kotlinx.coroutines.flow.Flow

interface ImportHistoryRepository {
    fun getAll(): Flow<List<ImportHistoryEntity>>
    suspend fun insert(history: ImportHistoryEntity)
    suspend fun delete(history: ImportHistoryEntity)
    suspend fun findByHash(hash: String): ImportHistoryEntity?
}
