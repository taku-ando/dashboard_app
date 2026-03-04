package com.example.dashboard.ui.kakeibo.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dashboard.domain.usecase.DashboardSummary
import com.example.dashboard.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

data class DashboardUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val summary: DashboardSummary? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<DashboardUiState> = _yearMonth
        .flatMapLatest { ym ->
            getDashboardSummary(ym.year, ym.monthValue)
                .map { summary ->
                    DashboardUiState(yearMonth = ym, summary = summary, isLoading = false)
                }
                .onStart { emit(DashboardUiState(yearMonth = ym, isLoading = true)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )

    fun previousMonth() {
        _yearMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        _yearMonth.update { it.plusMonths(1) }
    }
}
