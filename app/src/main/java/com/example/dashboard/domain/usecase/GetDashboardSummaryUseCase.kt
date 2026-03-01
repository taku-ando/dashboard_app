package com.example.dashboard.domain.usecase

import com.example.dashboard.data.repository.BudgetRepository
import com.example.dashboard.data.repository.CategoryRepository
import com.example.dashboard.data.repository.TransactionRepository
import com.example.dashboard.domain.model.Budget
import com.example.dashboard.domain.model.Category
import com.example.dashboard.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// 出力型
// ---------------------------------------------------------------------------

data class DashboardSummary(
    val yearMonth: String,                       // "YYYY-MM"
    val totalAmount: Int,                        // 当月支出合計
    val categoryBreakdown: List<CategoryAmount>, // カテゴリ別内訳
    val budgets: List<BudgetStatus>,             // 予算消化状況
    val unclassifiedCount: Int,                  // 未分類件数（全期間）
    val monthlyTotals: List<MonthlyTotal>        // 直近12ヶ月推移（グラフ用）
)

data class CategoryAmount(
    val category: Category,
    val amount: Int,
    val ratio: Float   // 当月合計に対する割合 (0.0〜1.0)
)

data class BudgetStatus(
    val budget: Budget,
    val categoryName: String,
    val spent: Int,
    val ratio: Float   // spent / limitAmount（1.0 超えで予算オーバー）
)

data class MonthlyTotal(
    val yearMonth: String,   // "YYYY-MM"
    val amount: Int
)

// ---------------------------------------------------------------------------
// UseCase
// ---------------------------------------------------------------------------

/**
 * ダッシュボード表示に必要なデータをリアクティブに返す UseCase。
 *
 * DB が更新されるたびに [DashboardSummary] が再計算される。
 */
@Singleton
class GetDashboardSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(year: Int, month: Int): Flow<DashboardSummary> {
        val yearMonth = "%04d-%02d".format(year, month)

        return combine(
            transactionRepository.getByMonth(yearMonth),
            transactionRepository.getAll(),
            categoryRepository.getAll(),
            budgetRepository.getByMonth(year, month)
        ) { monthTx, allTx, categories, budgets ->
            buildSummary(yearMonth, monthTx, allTx, categories, budgets)
        }
    }

    // -----------------------------------------------------------------------
    // 集計ロジック
    // -----------------------------------------------------------------------

    private fun buildSummary(
        yearMonth: String,
        monthTx: List<Transaction>,
        allTx: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>
    ): DashboardSummary {
        val categoryMap = categories.associateBy { it.id }

        // 当月合計（支出のみ・正の金額）
        val totalAmount = monthTx.filter { it.amount > 0 }.sumOf { it.amount }

        // カテゴリ別内訳
        val categoryBreakdown = buildCategoryBreakdown(monthTx, categoryMap, totalAmount)

        // 予算消化状況
        val budgetStatuses = buildBudgetStatuses(monthTx, budgets, categoryMap)

        // 未分類件数（全期間）
        val unclassifiedCount = allTx.count {
            it.classifiedBy == "unclassified" || it.categoryId == null
        }

        // 直近12ヶ月推移
        val monthlyTotals = buildMonthlyTotals(yearMonth, allTx)

        return DashboardSummary(
            yearMonth          = yearMonth,
            totalAmount        = totalAmount,
            categoryBreakdown  = categoryBreakdown,
            budgets            = budgetStatuses,
            unclassifiedCount  = unclassifiedCount,
            monthlyTotals      = monthlyTotals
        )
    }

    private fun buildCategoryBreakdown(
        monthTx: List<Transaction>,
        categoryMap: Map<String, Category>,
        totalAmount: Int
    ): List<CategoryAmount> {
        return monthTx
            .filter { it.amount > 0 && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapNotNull { (categoryId, txList) ->
                val cat = categoryMap[categoryId] ?: return@mapNotNull null
                val amount = txList.sumOf { it.amount }
                CategoryAmount(
                    category = cat,
                    amount   = amount,
                    ratio    = if (totalAmount > 0) amount.toFloat() / totalAmount else 0f
                )
            }
            .sortedByDescending { it.amount }
    }

    private fun buildBudgetStatuses(
        monthTx: List<Transaction>,
        budgets: List<Budget>,
        categoryMap: Map<String, Category>
    ): List<BudgetStatus> {
        val spentByCategory = monthTx
            .filter { it.amount > 0 && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, txList) -> txList.sumOf { it.amount } }

        return budgets.mapNotNull { budget ->
            val cat = categoryMap[budget.categoryId] ?: return@mapNotNull null
            val spent = spentByCategory[budget.categoryId] ?: 0
            BudgetStatus(
                budget       = budget,
                categoryName = cat.name,
                spent        = spent,
                ratio        = if (budget.limitAmount > 0) spent.toFloat() / budget.limitAmount else 0f
            )
        }
    }

    private fun buildMonthlyTotals(
        currentYearMonth: String,
        allTx: List<Transaction>
    ): List<MonthlyTotal> {
        // 当月を含む直近12ヶ月のリストを生成
        val (y, m) = currentYearMonth.split("-").map { it.toInt() }
        val ym = YearMonth.of(y, m)
        val months = (11 downTo 0).map { offset ->
            ym.minusMonths(offset.toLong()).toString()   // "YYYY-MM"
        }

        // 全取引を月別に集計
        val amountByMonth = allTx
            .filter { it.amount > 0 }
            .groupBy { it.date.take(7) }                // "YYYY-MM"
            .mapValues { (_, txList) -> txList.sumOf { it.amount } }

        return months.map { month ->
            MonthlyTotal(yearMonth = month, amount = amountByMonth[month] ?: 0)
        }
    }
}
