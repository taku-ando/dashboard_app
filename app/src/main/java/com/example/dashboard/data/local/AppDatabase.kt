package com.example.dashboard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.dashboard.data.local.dao.*
import com.example.dashboard.data.local.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CreditCardEntity::class,
        BudgetEntity::class,
        CategoryRuleEntity::class,
        ImportHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun importHistoryDao(): ImportHistoryDao
}
