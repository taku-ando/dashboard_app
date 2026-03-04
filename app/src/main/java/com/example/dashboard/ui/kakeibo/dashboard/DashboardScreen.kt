package com.example.dashboard.ui.kakeibo.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dashboard.domain.usecase.BudgetStatus
import com.example.dashboard.domain.usecase.CategoryAmount
import com.example.dashboard.domain.usecase.MonthlyTotal
import com.example.dashboard.ui.components.TopBar
import java.text.NumberFormat
import java.util.Locale

private val CHART_COLORS = listOf(
    Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04),
    Color(0xFF34A853), Color(0xFFAB47BC), Color(0xFFEF5350),
    Color(0xFF26A69A)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopBar(title = "ダッシュボード") }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val summary = state.summary
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 月切り替えヘッダー
            MonthSelector(
                yearMonth = "%d年%d月".format(state.yearMonth.year, state.yearMonth.monthValue),
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )

            // 当月合計カード
            TotalAmountCard(totalAmount = summary?.totalAmount ?: 0)

            // 未分類バッジ
            if ((summary?.unclassifiedCount ?: 0) > 0) {
                UnclassifiedBadge(count = summary!!.unclassifiedCount)
            }

            // カテゴリ別ドーナツグラフ
            if (!summary?.categoryBreakdown.isNullOrEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("カテゴリ別内訳", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        DonutChartSection(items = summary!!.categoryBreakdown)
                    }
                }
            }

            // 月別推移棒グラフ
            if (!summary?.monthlyTotals.isNullOrEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("月別推移（直近12ヶ月）", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        MonthlyBarChart(totals = summary!!.monthlyTotals)
                    }
                }
            }

            // 予算アラート
            if (!summary?.budgets.isNullOrEmpty()) {
                val alerts = summary!!.budgets.filter { it.ratio >= 0.8f }
                if (alerts.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "予算アラート",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            alerts.forEach { BudgetAlertRow(it) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 月切り替えヘッダー
// ---------------------------------------------------------------------------

@Composable
private fun MonthSelector(
    yearMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "前月")
        }
        Text(
            text = yearMonth,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "次月")
        }
    }
}

// ---------------------------------------------------------------------------
// 当月合計カード
// ---------------------------------------------------------------------------

@Composable
private fun TotalAmountCard(totalAmount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "当月支出合計",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatAmount(totalAmount),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 未分類バッジ
// ---------------------------------------------------------------------------

@Composable
private fun UnclassifiedBadge(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = "⚠ 未分類の取引が ${count} 件あります",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

// ---------------------------------------------------------------------------
// ドーナツグラフ
// ---------------------------------------------------------------------------

@Composable
private fun DonutChartSection(items: List<CategoryAmount>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ドーナツ
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            val ratios = items.map { it.ratio }
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                var startAngle = -90f
                ratios.forEachIndexed { i, ratio ->
                    val sweep = ratio * 360f
                    drawArc(
                        color = CHART_COLORS[i % CHART_COLORS.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = size.minDimension * 0.22f),
                        size = Size(size.minDimension, size.minDimension),
                        topLeft = Offset(
                            (size.width - size.minDimension) / 2,
                            (size.height - size.minDimension) / 2
                        )
                    )
                    startAngle += sweep
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // 凡例
        Column(
            modifier = Modifier.weight(1.5f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.take(6).forEachIndexed { i, item ->
                LegendRow(
                    color = CHART_COLORS[i % CHART_COLORS.size],
                    label = item.category.name,
                    amount = item.amount,
                    ratio = item.ratio
                )
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, amount: Int, ratio: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color)
        }
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text(
                "${formatAmount(amount)} (${(ratio * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 月別棒グラフ（Canvas実装）
// ---------------------------------------------------------------------------

@Composable
private fun MonthlyBarChart(totals: List<MonthlyTotal>) {
    val maxAmount = totals.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val barWidth = size.width / (totals.size * 1.5f)
        val gap = barWidth * 0.5f
        val chartHeight = size.height - 24.dp.toPx()

        totals.forEachIndexed { i, item ->
            val barHeight = (item.amount.toFloat() / maxAmount) * chartHeight
            val x = i * (barWidth + gap) + gap / 2
            val y = chartHeight - barHeight

            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }

    // 月ラベル（最初・中間・最後のみ表示）
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            totals.firstOrNull()?.yearMonth?.takeLast(2)?.let { "${it}月" } ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            totals.getOrNull(totals.size / 2)?.yearMonth?.takeLast(2)?.let { "${it}月" } ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            totals.lastOrNull()?.yearMonth?.takeLast(2)?.let { "${it}月" } ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// 予算アラート行
// ---------------------------------------------------------------------------

@Composable
private fun BudgetAlertRow(status: BudgetStatus) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                status.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                "${formatAmount(status.spent)} / ${formatAmount(status.budget.limitAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { status.ratio.coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (status.ratio >= 1f) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary
        )
    }
}

// ---------------------------------------------------------------------------
// ユーティリティ
// ---------------------------------------------------------------------------

private fun formatAmount(amount: Int): String =
    "¥" + NumberFormat.getNumberInstance(Locale.JAPAN).format(amount)
