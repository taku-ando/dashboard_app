package com.example.dashboard.data.repository

import com.example.dashboard.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getByMonth(year: Int, month: Int): Flow<List<Budget>>
    suspend fun insert(budget: Budget)
    suspend fun update(budget: Budget)
    suspend fun delete(budget: Budget)
}
