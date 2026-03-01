package com.example.dashboard.data.repository

import com.example.dashboard.domain.model.CategoryRule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun getAll(): Flow<List<CategoryRule>>
    suspend fun insert(rule: CategoryRule)
    suspend fun delete(rule: CategoryRule)
}
