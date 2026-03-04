package com.example.dashboard.ui.kakeibo.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.DismissValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dashboard.domain.model.Category
import com.example.dashboard.domain.model.Transaction
import com.example.dashboard.ui.components.TopBar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var searchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(title = "取引一覧") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 検索バー
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onSearch = { searchActive = false },
                active = searchActive,
                onActiveChange = { searchActive = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("店名で検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            ) {}

            // カテゴリフィルターチップ
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text("すべて") }
                    )
                }
                items(state.categories) { cat ->
                    FilterChip(
                        selected = state.selectedCategoryId == cat.id,
                        onClick = { viewModel.onCategorySelected(cat.id) },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (state.transactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "取引がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // グルーピングして表示（日付ごと）
                val grouped = state.transactions.groupBy { it.date }.toSortedMap(reverseOrder())

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (date, txList) ->
                        item {
                            Text(
                                text = formatDate(date),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 16.dp, end = 16.dp,
                                    top = 12.dp, bottom = 4.dp
                                )
                            )
                        }
                        items(
                            items = txList,
                            key = { it.id }
                        ) { tx ->
                            SwipeToDeleteItem(
                                transaction = tx,
                                category = state.categories.find { it.id == tx.categoryId },
                                allCategories = state.categories,
                                onDeleteRequest = { viewModel.requestDelete(tx) },
                                onCategoryChange = { catId ->
                                    viewModel.updateCategory(tx, catId)
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        // 削除確認ダイアログ
        state.deletePendingTransaction?.let { tx ->
            AlertDialog(
                onDismissRequest = viewModel::cancelDelete,
                title = { Text("取引を削除") },
                text = { Text("「${tx.name}」を削除しますか？この操作は元に戻せません。") },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDelete) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDelete) { Text("キャンセル") }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// スワイプ削除アイテム
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    transaction: Transaction,
    category: Category?,
    allCategories: List<Category>,
    onDeleteRequest: () -> Unit,
    onCategoryChange: (String?) -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            if (value == DismissValue.DismissedToStart) {
                onDeleteRequest()
            }
            false  // dismiss はダイアログ確認後なので false
        }
    )

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            val color by animateColorAsState(
                if (dismissState.targetValue == DismissValue.DismissedToStart)
                    MaterialTheme.colorScheme.errorContainer
                else Color.Transparent,
                label = "swipe_bg"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissContent = {
            TransactionItem(
                transaction = transaction,
                category = category,
                allCategories = allCategories,
                onCategoryChange = onCategoryChange
            )
        }
    )
}

// ---------------------------------------------------------------------------
// 取引アイテム
// ---------------------------------------------------------------------------

@Composable
private fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    allCategories: List<Category>,
    onCategoryChange: (String?) -> Unit
) {
    var showCategoryMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        onClick = { showCategoryMenu = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // カテゴリアイコン
            Text(
                text = category?.icon ?: "📦",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category?.name ?: "未分類",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatAmount(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (transaction.amount < 0)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }

        // カテゴリ変更ドロップダウン
        DropdownMenu(
            expanded = showCategoryMenu,
            onDismissRequest = { showCategoryMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("未分類") },
                onClick = {
                    onCategoryChange(null)
                    showCategoryMenu = false
                }
            )
            allCategories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text("${cat.icon} ${cat.name}") },
                    onClick = {
                        onCategoryChange(cat.id)
                        showCategoryMenu = false
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ユーティリティ
// ---------------------------------------------------------------------------

private fun formatAmount(amount: Int): String =
    (if (amount < 0) "-" else "") +
    "¥" + NumberFormat.getNumberInstance(Locale.JAPAN).format(Math.abs(amount))

private fun formatDate(date: String): String {
    val parts = date.split("-")
    if (parts.size < 3) return date
    return "${parts[0]}年${parts[1].trimStart('0')}月${parts[2].trimStart('0')}日"
}
