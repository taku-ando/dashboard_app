package com.example.dashboard.data.local.dao

import androidx.room.*
import com.example.dashboard.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month")
    fun getByMonth(year: Int, month: Int): Flow<List<BudgetEntity>>
}
