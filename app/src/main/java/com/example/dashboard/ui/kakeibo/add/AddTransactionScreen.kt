package com.example.dashboard.ui.kakeibo.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dashboard.ui.components.TopBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // 保存完了したら戻る
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) navController.popBackStack()
    }

    // エラー表示
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "手動入力",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 日付
            OutlinedTextField(
                value = formatDisplayDate(state.date),
                onValueChange = {},
                readOnly = true,
                label = { Text("日付") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("変更") }
                }
            )

            // 店名・内容
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("店名・内容") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 金額
            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("金額（円）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("支出は正の数、収入は負の数") }
            )

            // カテゴリ
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = state.categories.find { it.id == state.selectedCategoryId }?.let {
                        "${it.icon} ${it.name}"
                    } ?: "カテゴリを選択",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("カテゴリ（任意）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoryDropdown) }
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("未分類") },
                        onClick = {
                            viewModel.onCategorySelected(null)
                            showCategoryDropdown = false
                        }
                    )
                    state.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.icon} ${cat.name}") },
                            onClick = {
                                viewModel.onCategorySelected(cat.id)
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            // メモ
            OutlinedTextField(
                value = state.memo,
                onValueChange = viewModel::onMemoChanged,
                label = { Text("メモ（任意）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }

        // 日付ピッカー
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = parseDateToMillis(state.date)
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE)
                            viewModel.onDateChanged(date)
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        val d = LocalDate.parse(isoDate)
        "${d.year}年${d.monthValue}月${d.dayOfMonth}日"
    } catch (_: Exception) {
        isoDate
    }
}

private fun parseDateToMillis(isoDate: String): Long? {
    return try {
        LocalDate.parse(isoDate)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        null
    }
}
