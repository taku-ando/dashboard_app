package com.example.dashboard.ui.kakeibo.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dashboard.data.repository.BudgetRepository
import com.example.dashboard.data.repository.CategoryRepository
import com.example.dashboard.data.repository.TransactionRepository
import com.example.dashboard.domain.model.Budget
import com.example.dashboard.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

data class BudgetRowState(
    val category: Category,
    val budget: Budget?,        // null = 未設定
    val spent: Int,
    val usageRate: Float        // spent / limitAmount
)

data class BudgetUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val rows: List<BudgetRowState> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<BudgetUiState> = _yearMonth.flatMapLatest { ym ->
        val yearMonth = "%04d-%02d".format(ym.year, ym.monthValue)
        combine(
            budgetRepository.getByMonth(ym.year, ym.monthValue),
            categoryRepository.getAll(),
            transactionRepository.getByMonth(yearMonth)
        ) { budgets, categories, transactions ->
            val budgetMap = budgets.associateBy { it.categoryId }
            val spentMap = transactions
                .filter { it.amount > 0 }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val rows = categories.map { cat ->
                val budget = budgetMap[cat.id]
                val spent = spentMap[cat.id] ?: 0
                BudgetRowState(
                    category = cat,
                    budget = budget,
                    spent = spent,
                    usageRate = if ((budget?.limitAmount ?: 0) > 0)
                        spent.toFloat() / budget!!.limitAmount else 0f
                )
            }
            BudgetUiState(yearMonth = ym, rows = rows, isLoading = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState()
    )

    fun previousMonth() { _yearMonth.update { it.minusMonths(1) } }
    fun nextMonth()     { _yearMonth.update { it.plusMonths(1) } }

    fun saveBudget(categoryId: String, limitAmount: Int) {
        val ym = _yearMonth.value
        viewModelScope.launch {
            val existing = uiState.value.rows
                .firstOrNull { it.category.id == categoryId }?.budget
            if (existing != null) {
                budgetRepository.update(existing.copy(limitAmount = limitAmount))
            } else {
                budgetRepository.insert(
                    Budget(
                        id           = UUID.randomUUID().toString(),
                        categoryId   = categoryId,
                        year         = ym.year,
                        month        = ym.monthValue,
                        limitAmount  = limitAmount
                    )
                )
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { budgetRepository.delete(budget) }
    }
}
