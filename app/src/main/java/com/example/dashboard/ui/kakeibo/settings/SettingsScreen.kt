package com.example.dashboard.ui.kakeibo.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dashboard.domain.model.Category
import com.example.dashboard.domain.model.CategoryRule
import com.example.dashboard.domain.model.CreditCard
import com.example.dashboard.ui.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(title = "設定") },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "追加")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("カード") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("カテゴリ") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("仕分けルール") }
                )
            }

            when (selectedTab) {
                0 -> CardsTab(
                    cards = state.cards,
                    onDelete = viewModel::deleteCard
                )
                1 -> CategoriesTab(
                    categories = state.categories,
                    onDelete = viewModel::deleteCategory
                )
                2 -> RulesTab(
                    rules = state.rules,
                    categories = state.categories,
                    onDelete = viewModel::deleteRule
                )
            }
        }

        // 追加ダイアログ
        if (showAddDialog) {
            when (selectedTab) {
                0 -> AddCardDialog(
                    onConfirm = { name, issuer, color ->
                        viewModel.addCard(name, issuer, color)
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
                1 -> AddCategoryDialog(
                    onConfirm = { name, icon, color ->
                        viewModel.addCategory(name, icon, color)
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
                2 -> AddRuleDialog(
                    categories = state.categories,
                    onConfirm = { shopName, matchType, categoryId ->
                        viewModel.addRule(shopName, matchType, categoryId)
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// カードタブ
// ---------------------------------------------------------------------------

@Composable
private fun CardsTab(
    cards: List<CreditCard>,
    onDelete: (CreditCard) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(cards) { card ->
            ListItem(
                title = card.name,
                subtitle = card.issuer,
                onDelete = { onDelete(card) }
            )
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// カテゴリタブ
// ---------------------------------------------------------------------------

@Composable
private fun CategoriesTab(
    categories: List<Category>,
    onDelete: (Category) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(categories) { cat ->
            ListItem(
                title = "${cat.icon} ${cat.name}",
                subtitle = if (cat.isDefault) "デフォルト（削除不可）" else null,
                onDelete = if (!cat.isDefault) ({ onDelete(cat) }) else null
            )
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// 仕分けルールタブ
// ---------------------------------------------------------------------------

@Composable
private fun RulesTab(
    rules: List<CategoryRule>,
    categories: List<Category>,
    onDelete: (CategoryRule) -> Unit
) {
    val catMap = categories.associateBy { it.id }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rules) { rule ->
            ListItem(
                title = rule.shopName,
                subtitle = "${matchTypeLabel(rule.matchType)} → ${catMap[rule.categoryId]?.name ?: "不明"}",
                onDelete = { onDelete(rule) }
            )
            HorizontalDivider()
        }
    }
}

private fun matchTypeLabel(matchType: String) = when (matchType) {
    "exact"    -> "完全一致"
    "prefix"   -> "前方一致"
    "contains" -> "含む"
    else       -> matchType
}

// ---------------------------------------------------------------------------
// 共通リストアイテム
// ---------------------------------------------------------------------------

@Composable
private fun ListItem(
    title: String,
    subtitle: String?,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// カード追加ダイアログ
// ---------------------------------------------------------------------------

@Composable
private fun AddCardDialog(
    onConfirm: (name: String, issuer: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("カードを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("カード名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("発行会社") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, issuer, "#607D8B") },
                enabled = name.isNotBlank()
            ) { Text("追加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

// ---------------------------------------------------------------------------
// カテゴリ追加ダイアログ
// ---------------------------------------------------------------------------

@Composable
private fun AddCategoryDialog(
    onConfirm: (name: String, icon: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("📦") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("カテゴリを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("カテゴリ名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("アイコン（絵文字）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, icon, "#607D8B") },
                enabled = name.isNotBlank()
            ) { Text("追加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

// ---------------------------------------------------------------------------
// 仕分けルール追加ダイアログ
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    categories: List<Category>,
    onConfirm: (shopName: String, matchType: String, categoryId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf("contains") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var matchExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val matchOptions = listOf("exact" to "完全一致", "prefix" to "前方一致", "contains" to "含む")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("仕分けルールを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("店名・キーワード") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // マッチタイプ
                ExposedDropdownMenuBox(
                    expanded = matchExpanded,
                    onExpandedChange = { matchExpanded = it }
                ) {
                    OutlinedTextField(
                        value = matchOptions.find { it.first == matchType }?.second ?: matchType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("マッチタイプ") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(matchExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = matchExpanded,
                        onDismissRequest = { matchExpanded = false }
                    ) {
                        matchOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { matchType = value; matchExpanded = false }
                            )
                        }
                    }
                }

                // カテゴリ
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    val selCat = categories.find { it.id == selectedCategoryId }
                    OutlinedTextField(
                        value = selCat?.let { "${it.icon} ${it.name}" } ?: "カテゴリ選択",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("振り分け先カテゴリ") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${cat.name}") },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (shopName.isNotBlank() && selectedCategoryId.isNotBlank()) {
                        onConfirm(shopName, matchType, selectedCategoryId)
                    }
                },
                enabled = shopName.isNotBlank() && selectedCategoryId.isNotBlank()
            ) { Text("追加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}
