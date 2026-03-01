package com.example.dashboard.data.repository

import com.example.dashboard.data.local.dao.TransactionDao
import com.example.dashboard.data.local.entity.TransactionEntity
import com.example.dashboard.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByMonth(yearMonth: String): Flow<List<Transaction>> =
        dao.getByMonth(yearMonth).map { list -> list.map { it.toDomain() } }

    override fun getUnclassified(): Flow<List<Transaction>> =
        dao.getUnclassified().map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Transaction>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(transaction: Transaction) {
        dao.insert(transaction.toEntity())
    }

    override suspend fun delete(transaction: Transaction) {
        dao.delete(transaction.toEntity())
    }

    override suspend fun deleteByImportId(importId: String) {
        dao.deleteByImportId(importId)
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        cardId = cardId,
        importId = importId,
        date = date,
        name = name,
        categoryId = categoryId,
        amount = amount,
        memo = memo,
        source = source,
        classifiedBy = classifiedBy
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        userId = "local",
        cardId = cardId,
        importId = importId,
        date = date,
        name = name,
        categoryId = categoryId,
        amount = amount,
        memo = memo,
        source = source,
        classifiedBy = classifiedBy,
        createdAt = Instant.now().toString()
    )
}
