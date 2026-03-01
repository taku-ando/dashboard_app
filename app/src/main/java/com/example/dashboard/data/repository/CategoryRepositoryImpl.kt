package com.example.dashboard.data.repository

import com.example.dashboard.data.local.dao.CategoryDao
import com.example.dashboard.data.local.entity.CategoryEntity
import com.example.dashboard.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(category: Category) {
        dao.insert(category.toEntity())
    }

    override suspend fun update(category: Category) {
        dao.update(category.toEntity())
    }

    override suspend fun delete(category: Category) {
        dao.delete(category.toEntity())
    }

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        icon = icon,
        color = color,
        isDefault = isDefault == 1
    )

    private fun Category.toEntity() = CategoryEntity(
        id = id,
        userId = "local",
        name = name,
        icon = icon,
        color = color,
        isDefault = if (isDefault) 1 else 0
    )
}
