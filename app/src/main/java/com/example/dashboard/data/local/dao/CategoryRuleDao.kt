package com.example.dashboard.data.local.dao

import androidx.room.*
import com.example.dashboard.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRuleEntity)

    @Delete
    suspend fun delete(rule: CategoryRuleEntity)

    @Query("""
        SELECT * FROM category_rules
        ORDER BY CASE matchType
            WHEN 'exact' THEN 0
            WHEN 'prefix' THEN 1
            ELSE 2
        END
    """)
    fun getAll(): Flow<List<CategoryRuleEntity>>
}
