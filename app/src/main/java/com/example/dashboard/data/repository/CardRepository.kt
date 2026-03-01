package com.example.dashboard.data.repository

import com.example.dashboard.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getAll(): Flow<List<CreditCard>>
    suspend fun insert(card: CreditCard)
    suspend fun update(card: CreditCard)
    suspend fun delete(card: CreditCard)
}
