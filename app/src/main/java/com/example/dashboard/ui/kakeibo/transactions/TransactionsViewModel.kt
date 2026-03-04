package com.example.dashboard.ui.kakeibo.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dashboard.data.repository.CategoryRepository
import com.example.dashboard.data.repository.TransactionRepository
import com.example.dashboard.domain.model.Category
import com.example.dashboard.domain.model.Transaction
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
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,   // null = 全カテゴリ
    val isLoading: Boolean = true,
    val deletePendingTransaction: Transaction? = null  // 削除確認ダイアログ用
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _deletePending = MutableStateFlow<Transaction?>(null)

    // 検索クエリに応じて Flow を切り替え
    private val _transactions = _searchQuery.flatMapLatest { q ->
        if (q.isBlank()) transactionRepository.getAll()
        else transactionRepository.search(q)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        _transactions,
        categoryRepository.getAll(),
        _searchQuery,
        _selectedCategoryId,
        _deletePending
    ) { transactions, categories, query, catId, deletePending ->
        val filtered = if (catId == null) transactions
                       else transactions.filter { it.categoryId == catId }
        TransactionsUiState(
            transactions = filtered,
            categories = categories,
            searchQuery = query,
            selectedCategoryId = catId,
            isLoading = false,
            deletePendingTransaction = deletePending
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }

    fun onCategorySelected(categoryId: String?) {
        _selectedCategoryId.update { categoryId }
    }

    fun requestDelete(transaction: Transaction) {
        _deletePending.update { transaction }
    }

    fun cancelDelete() {
        _deletePending.update { null }
    }

    fun confirmDelete() {
        val tx = _deletePending.value ?: return
        _deletePending.update { null }
        viewModelScope.launch {
            transactionRepository.delete(tx)
        }
    }

    fun updateCategory(transaction: Transaction, categoryId: String?) {
        viewModelScope.launch {
            transactionRepository.insert(
                transaction.copy(
                    categoryId = categoryId,
                    classifiedBy = "manual"
                )
            )
        }
    }
}
