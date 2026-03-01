package com.example.dashboard.data.repository

import com.example.dashboard.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAll(): Flow<List<Transaction>>
    fun getByMonth(yearMonth: String): Flow<List<Transaction>>
    fun getUnclassified(): Flow<List<Transaction>>
    fun search(query: String): Flow<List<Transaction>>
    suspend fun insert(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    suspend fun deleteByImportId(importId: String)
}
