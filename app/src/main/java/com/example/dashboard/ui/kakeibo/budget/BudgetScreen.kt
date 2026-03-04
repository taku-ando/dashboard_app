package com.example.dashboard.ui.kakeibo.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dashboard.ui.components.TopBar
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var editTarget by remember { mutableStateOf<BudgetRowState?>(null) }

    Scaffold(
        topBar = { TopBar(title = "予算管理") }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 月切り替えヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "前月")
                }
                Text(
                    text = "%d年%d月".format(
                        state.yearMonth.year,
                        state.yearMonth.monthValue
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "次月")
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.rows) { row ->
                    BudgetRowItem(
                        row = row,
                        onEditClick = { editTarget = row }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        // 予算編集ダイアログ
        editTarget?.let { target ->
            BudgetEditDialog(
                row = target,
                onConfirm = { amount ->
                    if (amount <= 0 && target.budget != null) {
                        viewModel.deleteBudget(target.budget)
                    } else if (amount > 0) {
                        viewModel.saveBudget(target.category.id, amount)
                    }
                    editTarget = null
                },
                onDismiss = { editTarget = null }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 予算行
// ---------------------------------------------------------------------------

@Composable
private fun BudgetRowItem(
    row: BudgetRowState,
    onEditClick: () -> Unit
) {
    val limitAmount = row.budget?.limitAmount ?: 0
    val isOverBudget = row.usageRate > 1f
    val progressColor = when {
        isOverBudget  -> MaterialTheme.colorScheme.error
        row.usageRate >= 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.category.icon,
                    style = MaterialTheme.typography.titleLarge
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = row.category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (limitAmount > 0) {
                        Text(
                            text = "${formatAmount(row.spent)} / ${formatAmount(limitAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "予算未設定（支出: ${formatAmount(row.spent)}）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "予算を編集")
                }
            }

            if (limitAmount > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { row.usageRate.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor
                )
                if (isOverBudget) {
                    Text(
                        text = "予算超過 ${(row.usageRate * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 予算編集ダイアログ
// ---------------------------------------------------------------------------

@Composable
private fun BudgetEditDialog(
    row: BudgetRowState,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember {
        mutableStateOf(row.budget?.limitAmount?.toString() ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${row.category.icon} ${row.category.name}の予算") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("月間予算（円）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("0または空白で予算を削除") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(text.replace(",", "").toIntOrNull() ?: 0)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

private fun formatAmount(amount: Int): String =
    "¥" + NumberFormat.getNumberInstance(Locale.JAPAN).format(amount)
