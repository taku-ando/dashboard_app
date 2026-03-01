package com.example.dashboard.data.local.dao

import androidx.room.*
import com.example.dashboard.data.local.entity.ImportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(h: ImportHistoryEntity)

    @Delete
    suspend fun delete(h: ImportHistoryEntity)

    @Query("SELECT * FROM import_history ORDER BY importedAt DESC")
    fun getAll(): Flow<List<ImportHistoryEntity>>

    @Query("SELECT * FROM import_history WHERE fileHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): ImportHistoryEntity?
}
