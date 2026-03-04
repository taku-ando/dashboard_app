package com.example.dashboard.ui.kakeibo.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dashboard.data.repository.CategoryRepository
import com.example.dashboard.data.repository.TransactionRepository
import com.example.dashboard.domain.model.Category
import com.example.dashboard.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class AddTransactionUiState(
    val categories: List<Category> = emptyList(),
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val name: String = "",
    val amountText: String = "",
    val selectedCategoryId: String? = null,
    val memo: String = "",
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _form = MutableStateFlow(AddTransactionUiState())

    val uiState: StateFlow<AddTransactionUiState> = combine(
        _form,
        categoryRepository.getAll()
    ) { form, categories ->
        form.copy(categories = categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddTransactionUiState()
    )

    fun onDateChanged(date: String) { _form.update { it.copy(date = date) } }
    fun onNameChanged(name: String) { _form.update { it.copy(name = name) } }
    fun onAmountChanged(text: String) { _form.update { it.copy(amountText = text) } }
    fun onCategorySelected(id: String?) { _form.update { it.copy(selectedCategoryId = id) } }
    fun onMemoChanged(memo: String) { _form.update { it.copy(memo = memo) } }

    fun save() {
        val form = _form.value
        val amount = form.amountText.replace(",", "").toIntOrNull()

        if (form.name.isBlank()) {
            _form.update { it.copy(errorMessage = "店名・内容を入力してください") }
            return
        }
        if (amount == null || amount == 0) {
            _form.update { it.copy(errorMessage = "金額を正しく入力してください") }
            return
        }

        viewModelScope.launch {
            transactionRepository.insert(
                Transaction(
                    id           = UUID.randomUUID().toString(),
                    cardId       = null,
                    importId     = null,
                    date         = form.date,
                    name         = form.name.trim(),
                    categoryId   = form.selectedCategoryId,
                    amount       = amount,
                    memo         = form.memo.trim().ifBlank { null },
                    source       = "manual",
                    classifiedBy = if (form.selectedCategoryId != null) "manual" else "unclassified"
                )
            )
            _form.update { it.copy(isSaved = true) }
        }
    }

    fun clearError() { _form.update { it.copy(errorMessage = null) } }
}
