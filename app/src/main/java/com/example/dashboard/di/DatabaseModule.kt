package com.example.dashboard.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dashboard.data.local.AppDatabase
import com.example.dashboard.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return Room.databaseBuilder(ctx, AppDatabase::class.java, "dashboard.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("""
                            INSERT INTO categories (id, userId, name, icon, color, isDefault) VALUES
                            ('cat_food',          'local', '食費',        '🛒', '#4CAF50', 1),
                            ('cat_transport',     'local', '交通費',      '🚃', '#2196F3', 1),
                            ('cat_entertainment', 'local', '娯楽',        '🎮', '#9C27B0', 1),
                            ('cat_shopping',      'local', 'ショッピング', '🛍', '#FF9800', 1),
                            ('cat_medical',       'local', '医療・健康',  '💊', '#F44336', 1),
                            ('cat_utilities',     'local', '光熱費・通信','💡', '#607D8B', 1),
                            ('cat_other',         'local', 'その他',      '📦', '#9E9E9E', 1)
                    """.trimIndent())
                }
            })
            .build()
    }

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideCreditCardDao(db: AppDatabase): CreditCardDao = db.creditCardDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideCategoryRuleDao(db: AppDatabase): CategoryRuleDao = db.categoryRuleDao()

    @Provides
    fun provideImportHistoryDao(db: AppDatabase): ImportHistoryDao = db.importHistoryDao()
}
