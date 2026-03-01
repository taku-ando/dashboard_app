package com.example.dashboard.data.local.dao

import androidx.room.*
import com.example.dashboard.data.local.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CreditCardEntity)

    @Update
    suspend fun update(card: CreditCardEntity)

    @Delete
    suspend fun delete(card: CreditCardEntity)

    @Query("SELECT * FROM credit_cards ORDER BY name ASC")
    fun getAll(): Flow<List<CreditCardEntity>>
}
