package com.example.dashboard.data.local.dao

import androidx.room.*
import com.example.dashboard.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cat: CategoryEntity)

    @Update
    suspend fun update(cat: CategoryEntity)

    @Delete
    suspend fun delete(cat: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<CategoryEntity>>
}
