package com.example.dashboard.ui.kakeibo.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dashboard.data.repository.CardRepository
import com.example.dashboard.data.repository.CategoryRepository
import com.example.dashboard.data.repository.RuleRepository
import com.example.dashboard.domain.model.Category
import com.example.dashboard.domain.model.CategoryRule
import com.example.dashboard.domain.model.CreditCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val cards: List<CreditCard> = emptyList(),
    val categories: List<Category> = emptyList(),
    val rules: List<CategoryRule> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: RuleRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        cardRepository.getAll(),
        categoryRepository.getAll(),
        ruleRepository.getAll()
    ) { cards, categories, rules ->
        SettingsUiState(cards = cards, categories = categories, rules = rules)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    // ── カード ────────────────────────────────────────────────────────────────

    fun addCard(name: String, issuer: String, colorCode: String) {
        viewModelScope.launch {
            cardRepository.insert(
                CreditCard(
                    id        = UUID.randomUUID().toString(),
                    name      = name.trim(),
                    issuer    = issuer.trim(),
                    colorCode = colorCode
                )
            )
        }
    }

    fun deleteCard(card: CreditCard) {
        viewModelScope.launch { cardRepository.delete(card) }
    }

    // ── カテゴリ ──────────────────────────────────────────────────────────────

    fun addCategory(name: String, icon: String, color: String) {
        viewModelScope.launch {
            categoryRepository.insert(
                Category(
                    id        = UUID.randomUUID().toString(),
                    name      = name.trim(),
                    icon      = icon.trim(),
                    color     = color,
                    isDefault = false
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        if (category.isDefault) return  // デフォルトカテゴリは削除不可
        viewModelScope.launch { categoryRepository.delete(category) }
    }

    // ── 仕分けルール ──────────────────────────────────────────────────────────

    fun addRule(shopName: String, matchType: String, categoryId: String) {
        val now = Instant.now().toString()
        viewModelScope.launch {
            ruleRepository.insert(
                CategoryRule(
                    id         = UUID.randomUUID().toString(),
                    shopName   = shopName.trim(),
                    matchType  = matchType,
                    categoryId = categoryId
                )
            )
        }
    }

    fun deleteRule(rule: CategoryRule) {
        viewModelScope.launch { ruleRepository.delete(rule) }
    }
}
