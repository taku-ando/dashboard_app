package com.example.dashboard.ui.kakeibo.csvimport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dashboard.data.local.entity.ImportHistoryEntity
import com.example.dashboard.data.repository.CardRepository
import com.example.dashboard.data.repository.ImportHistoryRepository
import com.example.dashboard.data.repository.TransactionRepository
import com.example.dashboard.domain.model.CreditCard
import com.example.dashboard.domain.usecase.ImportCsvUseCase
import com.example.dashboard.domain.usecase.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val cards: List<CreditCard> = emptyList(),
    val selectedCardId: String? = null,
    val history: List<ImportHistoryEntity> = emptyList(),
    // インポート処理状態
    val isImporting: Boolean = false,
    val pendingUri: Uri? = null,
    val pendingFileName: String = "",
    val pendingBytes: ByteArray? = null,
    // 重複ファイル警告
    val duplicateWarning: ImportHistoryEntity? = null,
    // 結果メッセージ
    val resultMessage: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importCsvUseCase: ImportCsvUseCase,
    private val importHistoryRepository: ImportHistoryRepository,
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _selected = MutableStateFlow<String?>(null)
    private val _isImporting = MutableStateFlow(false)
    private val _pendingUri = MutableStateFlow<Uri?>(null)
    private val _pendingFileName = MutableStateFlow("")
    private val _pendingBytes = MutableStateFlow<ByteArray?>(null)
    private val _duplicateWarning = MutableStateFlow<ImportHistoryEntity?>(null)
    private val _resultMessage = MutableStateFlow<String?>(null)
    private val _isError = MutableStateFlow(false)

    val uiState: StateFlow<ImportUiState> = combine(
        cardRepository.getAll(),
        importHistoryRepository.getAll(),
        _selected,
        _isImporting,
        _duplicateWarning
    ) { cards, history, selected, importing, duplicate ->
        ImportUiState(
            cards = cards,
            selectedCardId = selected,
            history = history,
            isImporting = importing,
            duplicateWarning = duplicate,
            pendingFileName = _pendingFileName.value,
            pendingBytes = _pendingBytes.value,
            resultMessage = _resultMessage.value,
            isError = _isError.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ImportUiState()
    )

    fun onCardSelected(cardId: String?) {
        _selected.update { cardId }
    }

    /** ファイルピッカーで選択されたバイト列をセット。重複チェックを先行実施。 */
    fun onFilePicked(bytes: ByteArray, fileName: String) {
        _pendingBytes.update { bytes }
        _pendingFileName.update { fileName }
        _pendingUri.update { null }
        _resultMessage.update { null }
        _isError.update { false }

        viewModelScope.launch {
            val hash = com.example.dashboard.util.CsvParser.sha256(bytes)
            val existing = importHistoryRepository.findByHash(hash)
            if (existing != null) {
                _duplicateWarning.update { existing }
            }
            // 重複がなければ自動的にインポートは実行しない（BottomSheet で確認後に execute）
        }
    }

    /** 重複警告を無視してインポート実行。 */
    fun forceImport() {
        _duplicateWarning.update { null }
        executeImport(skipDuplicateCheck = true)
    }

    fun cancelImport() {
        _duplicateWarning.update { null }
        _pendingBytes.update { null }
        _pendingFileName.update { "" }
    }

    /** 通常インポート実行。 */
    fun executeImport(skipDuplicateCheck: Boolean = false) {
        val bytes = _pendingBytes.value ?: return
        val fileName = _pendingFileName.value

        _isImporting.update { true }
        viewModelScope.launch {
            val result = importCsvUseCase(
                bytes = bytes,
                fileName = fileName,
                cardId = _selected.value,
                skipDuplicateCheck = skipDuplicateCheck
            )
            _isImporting.update { false }
            _pendingBytes.update { null }
            _pendingFileName.update { "" }

            when (result) {
                is ImportResult.Success -> {
                    _resultMessage.update { "${result.count}件のデータをインポートしました" }
                    _isError.update { false }
                }
                is ImportResult.DuplicateFile -> {
                    _duplicateWarning.update { result.existing }
                }
                is ImportResult.ParseError -> {
                    _resultMessage.update { "エラー: ${result.message}" }
                    _isError.update { true }
                }
            }
        }
    }

    fun clearResultMessage() {
        _resultMessage.update { null }
    }

    /** インポート履歴単位で全取引を削除 */
    fun deleteImportHistory(history: ImportHistoryEntity) {
        viewModelScope.launch {
            transactionRepository.deleteByImportId(history.id)
            importHistoryRepository.delete(history)
        }
    }
}
