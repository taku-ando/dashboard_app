package com.example.dashboard.data.repository

import com.example.dashboard.data.local.dao.BudgetDao
import com.example.dashboard.data.local.entity.BudgetEntity
import com.example.dashboard.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun getByMonth(year: Int, month: Int): Flow<List<Budget>> =
        dao.getByMonth(year, month).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(budget: Budget) {
        dao.insert(budget.toEntity())
    }

    override suspend fun update(budget: Budget) {
        dao.update(budget.toEntity())
    }

    override suspend fun delete(budget: Budget) {
        dao.delete(budget.toEntity())
    }

    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        categoryId = categoryId,
        year = year,
        month = month,
        limitAmount = limitAmount
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        userId = "local",
        categoryId = categoryId,
        year = year,
        month = month,
        limitAmount = limitAmount
    )
}
