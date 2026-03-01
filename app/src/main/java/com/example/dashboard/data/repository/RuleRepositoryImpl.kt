package com.example.dashboard.data.repository

import com.example.dashboard.data.local.dao.CategoryRuleDao
import com.example.dashboard.data.local.entity.CategoryRuleEntity
import com.example.dashboard.domain.model.CategoryRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RuleRepositoryImpl @Inject constructor(
    private val dao: CategoryRuleDao
) : RuleRepository {

    override fun getAll(): Flow<List<CategoryRule>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(rule: CategoryRule) {
        dao.insert(rule.toEntity())
    }

    override suspend fun delete(rule: CategoryRule) {
        dao.delete(rule.toEntity())
    }

    private fun CategoryRuleEntity.toDomain() = CategoryRule(
        id = id,
        shopName = shopName,
        matchType = matchType,
        categoryId = categoryId
    )

    private fun CategoryRule.toEntity(): CategoryRuleEntity {
        val now = Instant.now().toString()
        return CategoryRuleEntity(
            id = id,
            userId = "local",
            shopName = shopName,
            matchType = matchType,
            categoryId = categoryId,
            createdAt = now,
            updatedAt = now
        )
    }
}
