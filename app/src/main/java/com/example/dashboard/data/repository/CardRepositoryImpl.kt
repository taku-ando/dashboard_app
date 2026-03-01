package com.example.dashboard.data.repository

import com.example.dashboard.data.local.dao.CreditCardDao
import com.example.dashboard.data.local.entity.CreditCardEntity
import com.example.dashboard.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val dao: CreditCardDao
) : CardRepository {

    override fun getAll(): Flow<List<CreditCard>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(card: CreditCard) {
        dao.insert(card.toEntity())
    }

    override suspend fun update(card: CreditCard) {
        dao.update(card.toEntity())
    }

    override suspend fun delete(card: CreditCard) {
        dao.delete(card.toEntity())
    }

    private fun CreditCardEntity.toDomain() = CreditCard(
        id = id,
        name = name,
        issuer = issuer,
        colorCode = colorCode
    )

    private fun CreditCard.toEntity() = CreditCardEntity(
        id = id,
        userId = "local",
        name = name,
        issuer = issuer,
        colorCode = colorCode,
        createdAt = Instant.now().toString()
    )
}
