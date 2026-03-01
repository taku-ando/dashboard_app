package com.example.dashboard.data.repository

import com.example.dashboard.data.local.dao.ImportHistoryDao
import com.example.dashboard.data.local.entity.ImportHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ImportHistoryRepositoryImpl @Inject constructor(
    private val dao: ImportHistoryDao
) : ImportHistoryRepository {

    override fun getAll(): Flow<List<ImportHistoryEntity>> = dao.getAll()

    override suspend fun insert(history: ImportHistoryEntity) {
        dao.insert(history)
    }

    override suspend fun delete(history: ImportHistoryEntity) {
        dao.delete(history)
    }

    override suspend fun findByHash(hash: String): ImportHistoryEntity? =
        dao.findByHash(hash)
}
