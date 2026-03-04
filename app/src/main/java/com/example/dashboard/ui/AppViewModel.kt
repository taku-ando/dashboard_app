package com.example.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dashboard.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Activity スコープで保持する共有 ViewModel。
 * ボトムナビのバッジ表示など、複数画面にまたがる状態を管理する。
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {

    /** 未分類取引件数（ボトムナビ「明細」タブのバッジ用）。 */
    val unclassifiedCount: StateFlow<Int> =
        transactionRepository.getUnclassified()
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
