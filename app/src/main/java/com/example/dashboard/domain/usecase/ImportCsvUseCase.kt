package com.example.dashboard.domain.usecase

import com.example.dashboard.data.local.entity.ImportHistoryEntity
import com.example.dashboard.data.repository.ImportHistoryRepository
import com.example.dashboard.data.repository.RuleRepository
import com.example.dashboard.data.repository.TransactionRepository
import com.example.dashboard.domain.model.Transaction
import com.example.dashboard.util.CsvParser
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// 結果型
// ---------------------------------------------------------------------------

sealed class ImportResult {
    data class Success(val count: Int) : ImportResult()

    /** 同一ファイル（SHA-256一致）が既にインポート済み。AlertDialog で確認を促す。 */
    data class DuplicateFile(val existing: ImportHistoryEntity) : ImportResult()

    data class ParseError(val message: String) : ImportResult()
}

// ---------------------------------------------------------------------------
// UseCase
// ---------------------------------------------------------------------------

/**
 * CSVバイト列を受け取り、解析→重複チェック→自動仕分け→DB保存 を一括実行する。
 *
 * 重複ファイルを検出した場合は [ImportResult.DuplicateFile] を返す。
 * ユーザーが「それでも取り込む」を選んだ場合は [skipDuplicateCheck]=true で再呼び出し。
 */
@Singleton
class ImportCsvUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val importHistoryRepository: ImportHistoryRepository,
    private val ruleRepository: RuleRepository,
    private val classifyUseCase: ClassifyTransactionUseCase
) {
    suspend operator fun invoke(
        bytes: ByteArray,
        fileName: String,
        cardId: String?,
        skipDuplicateCheck: Boolean = false
    ): ImportResult {

        // 1. CSV パース
        val parsed = try {
            CsvParser.parse(bytes, fileName)
        } catch (e: Exception) {
            return ImportResult.ParseError(e.message ?: "CSVの解析に失敗しました")
        }

        if (parsed.transactions.isEmpty()) {
            return ImportResult.ParseError("取引データが見つかりませんでした")
        }

        // 2. 重複ファイルチェック（SHA-256）
        if (!skipDuplicateCheck) {
            val existing = importHistoryRepository.findByHash(parsed.fileHash)
            if (existing != null) return ImportResult.DuplicateFile(existing)
        }

        // 3. ルールを一括取得してバッチ分類
        val rules = ruleRepository.getAll().first()
        val importId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        val transactions = parsed.transactions.map { p ->
            val result = classifyUseCase(p.name, rules)
            Transaction(
                id           = UUID.randomUUID().toString(),
                cardId       = cardId,
                importId     = importId,
                date         = p.date,
                name         = p.name,
                categoryId   = result.categoryId,
                amount       = p.amount,
                memo         = p.memo,
                source       = "csv",
                classifiedBy = result.classifiedBy
            )
        }

        // 4. インポート履歴を保存
        importHistoryRepository.insert(
            ImportHistoryEntity(
                id         = importId,
                fileName   = fileName,
                fileHash   = parsed.fileHash,
                cardId     = cardId,
                count      = transactions.size,
                importedAt = now
            )
        )

        // 5. 取引を保存（同一日・同名・同額でも全件取り込む）
        transactions.forEach { transactionRepository.insert(it) }

        return ImportResult.Success(transactions.size)
    }
}
