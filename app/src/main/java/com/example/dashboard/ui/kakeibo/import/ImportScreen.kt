package com.example.dashboard.ui.kakeibo.import

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dashboard.data.local.entity.ImportHistoryEntity
import com.example.dashboard.ui.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    // スナックバー表示
    LaunchedEffect(state.resultMessage) {
        state.resultMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResultMessage()
        }
    }

    // SAF ファイルピッカー
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes() ?: return@let
            val fileName = context.getFileName(it)
            viewModel.onFilePicked(bytes, fileName)
        }
    }

    Scaffold(
        topBar = { TopBar(title = "インポート") },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    text = { Text("CSVインポート") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("インポート履歴") }
                )
            }

            when (selectedTab) {
                0 -> ImportTab(
                    state = state,
                    onPickFile = { filePicker.launch("text/*") },
                    onCardSelected = viewModel::onCardSelected,
                    onExecuteImport = { viewModel.executeImport() }
                )
                1 -> HistoryTab(
                    history = state.history,
                    onDelete = viewModel::deleteImportHistory
                )
            }
        }

        // 重複ファイル警告ダイアログ
        state.duplicateWarning?.let { existing ->
            AlertDialog(
                onDismissRequest = viewModel::cancelImport,
                title = { Text("同一ファイルを検出") },
                text = {
                    Text(
                        "このファイルは「${existing.fileName}」として" +
                        "${existing.importedAt.take(10)}にインポート済みです。\n" +
                        "それでも取り込みますか？"
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::forceImport) {
                        Text("取り込む")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelImport) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// CSVインポートタブ
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportTab(
    state: ImportUiState,
    onPickFile: () -> Unit,
    onCardSelected: (String?) -> Unit,
    onExecuteImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // カード選択
        var expanded by remember { mutableStateOf(false) }
        val selectedCard = state.cards.find { it.id == state.selectedCardId }

        Text("カード選択（任意）", style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedCard?.name ?: "カードなし",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("カードなし") },
                    onClick = { onCardSelected(null); expanded = false }
                )
                state.cards.forEach { card ->
                    DropdownMenuItem(
                        text = { Text(card.name) },
                        onClick = { onCardSelected(card.id); expanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ファイル選択ボタン
        Button(
            onClick = onPickFile,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isImporting
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("CSVファイルを選択")
        }

        // 選択済みファイル情報 + インポート実行ボタン
        if (state.pendingBytes != null && state.pendingFileName.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("選択済み", style = MaterialTheme.typography.labelMedium)
                    Text(
                        state.pendingFileName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onExecuteImport,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isImporting
                    ) {
                        if (state.isImporting) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        }
                        Text("インポート実行")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// インポート履歴タブ
// ---------------------------------------------------------------------------

@Composable
private fun HistoryTab(
    history: List<ImportHistoryEntity>,
    onDelete: (ImportHistoryEntity) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<ImportHistoryEntity?>(null) }

    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("インポート履歴がありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(history.sortedByDescending { it.importedAt }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.fileName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${item.importedAt.take(10)} • ${item.count}件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { deleteTarget = item }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    // 削除確認ダイアログ
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("インポート履歴を削除") },
            text = {
                Text(
                    "「${target.fileName}」のインポート履歴と\n" +
                    "関連する ${target.count} 件の取引をすべて削除しますか？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    deleteTarget = null
                }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("キャンセル") }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// ファイル名取得ヘルパー
// ---------------------------------------------------------------------------

private fun Context.getFileName(uri: Uri): String {
    var name = "import.csv"
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
    }
    return name
}
