package com.example.dashboard.util

import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Calendar

// ---------------------------------------------------------------------------
// データクラス
// ---------------------------------------------------------------------------

/** CSV から解析した 1 件の取引。 */
data class ParsedTransaction(
    val date: String,       // "YYYY-MM-DD"
    val name: String,       // 店名（正規化前の生テキスト）
    val amount: Int,        // 円単位（支出は正、収入は負）
    val memo: String? = null
)

/** CSV パース結果。 */
data class CsvParseResult(
    val transactions: List<ParsedTransaction>,
    val fileHash: String,   // SHA-256 hex（重複チェック用）
    val format: CsvFormat
)

enum class CsvFormat { AEON_CARD, GENERIC }

// ---------------------------------------------------------------------------
// CsvParser
// ---------------------------------------------------------------------------

/**
 * CSV ファイルをパースして [CsvParseResult] を返す。
 *
 * 対応フォーマット:
 *   - イオンカード公式 CSV（ご利用日 / ご利用店名・商品名 / ご利用金額（円））
 *   - 汎用 CSV（列名キーワードで自動判別）
 *
 * 文字コード: UTF-8 (BOM あり / なし) / Shift-JIS を自動判別。
 */
object CsvParser {

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    fun parse(bytes: ByteArray, fileName: String): CsvParseResult {
        val hash = sha256(bytes)
        val charset = detectCharset(bytes)

        // BOM を除いてデコード
        val text = if (charset == Charsets.UTF_8 &&
            bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            String(bytes, 3, bytes.size - 3, charset)
        } else {
            String(bytes, charset)
        }

        val rows = parseCsvText(text)
        if (rows.isEmpty()) return CsvParseResult(emptyList(), hash, CsvFormat.GENERIC)

        // ヘッダー行を探す（前後にある説明行・空行をスキップ）
        val headerIndex = findHeaderIndex(rows)
        if (headerIndex < 0) return CsvParseResult(emptyList(), hash, CsvFormat.GENERIC)

        val headers = rows[headerIndex].map { it.trim() }
        val dataRows = rows.drop(headerIndex + 1)

        val format = detectFormat(headers)
        val transactions = when (format) {
            CsvFormat.AEON_CARD -> parseAeon(dataRows, headers)
            CsvFormat.GENERIC   -> parseGeneric(dataRows, headers)
        }

        return CsvParseResult(transactions, hash, format)
    }

    /** SHA-256 ハッシュを 16 進文字列で返す。 */
    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    // -----------------------------------------------------------------------
    // 文字コード判別
    // -----------------------------------------------------------------------

    private fun detectCharset(bytes: ByteArray): Charset {
        // UTF-8 BOM (EF BB BF)
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) return Charsets.UTF_8

        // 置換文字 (U+FFFD) が出なければ有効な UTF-8 とみなす
        val utf8Text = String(bytes, Charsets.UTF_8)
        if ('\uFFFD' !in utf8Text) return Charsets.UTF_8

        // Shift-JIS / Windows-31J にフォールバック
        return try {
            Charset.forName("Windows-31J")
        } catch (_: Exception) {
            Charset.forName("Shift_JIS")
        }
    }

    // -----------------------------------------------------------------------
    // ヘッダー行検出
    // -----------------------------------------------------------------------

    private fun findHeaderIndex(rows: List<List<String>>): Int {
        return rows.indexOfFirst { row ->
            val hasDate = row.any { col ->
                col.contains("利用日") ||
                col.contains("日付") ||
                col.contains("取引日") ||
                col.equals("date", ignoreCase = true)
            }
            val hasAmount = row.any { col ->
                col.contains("金額") ||
                col.equals("amount", ignoreCase = true)
            }
            hasDate && hasAmount
        }
    }

    // -----------------------------------------------------------------------
    // フォーマット判別
    // -----------------------------------------------------------------------

    private fun detectFormat(headers: List<String>): CsvFormat {
        // "ご利用〇〇" という敬語列名はイオンカード特有
        return if (headers.any { it.startsWith("ご利用") }) CsvFormat.AEON_CARD
        else CsvFormat.GENERIC
    }

    // -----------------------------------------------------------------------
    // イオンカード CSV パーサー
    // -----------------------------------------------------------------------

    private fun parseAeon(rows: List<List<String>>, headers: List<String>): List<ParsedTransaction> {
        val dateIdx   = headers.indexOfFirst { it.contains("利用日") || it.contains("日付") }
        val nameIdx   = headers.indexOfFirst { it.contains("店名") || it.contains("商品名") || it.contains("利用先") }
        val amountIdx = headers.indexOfFirst { it.contains("金額") }
        if (dateIdx < 0 || nameIdx < 0 || amountIdx < 0) return emptyList()

        return rows.mapNotNull { row ->
            val maxIdx = maxOf(dateIdx, nameIdx, amountIdx)
            if (row.size <= maxIdx) return@mapNotNull null

            val date   = toDate(row[dateIdx])             ?: return@mapNotNull null
            val name   = row[nameIdx].trim().ifEmpty { return@mapNotNull null }
            val amount = toAmount(row[amountIdx])          ?: return@mapNotNull null

            ParsedTransaction(date, name, amount)
        }
    }

    // -----------------------------------------------------------------------
    // 汎用 CSV パーサー
    // -----------------------------------------------------------------------

    private val dateCandidates   = listOf("date", "日付", "利用日", "取引日", "購入日", "ご利用日", "お取引日")
    private val amountCandidates = listOf("amount", "金額", "利用金額", "取引金額", "ご利用金額", "支払金額", "出金額", "出金")
    private val nameCandidates   = listOf("name", "店名", "利用先", "ご利用店名", "摘要", "内容", "取引先", "商品名")
    private val memoCandidates   = listOf("memo", "メモ", "備考", "摘要")

    private fun parseGeneric(rows: List<List<String>>, headers: List<String>): List<ParsedTransaction> {
        fun findIdx(candidates: List<String>) = headers.indexOfFirst { h ->
            candidates.any { h.contains(it, ignoreCase = true) }
        }

        val dateIdx   = findIdx(dateCandidates)
        val amountIdx = findIdx(amountCandidates)
        val nameIdx   = findIdx(nameCandidates)
        if (dateIdx < 0 || amountIdx < 0 || nameIdx < 0) return emptyList()

        val memoIdx = findIdx(memoCandidates).takeIf { it >= 0 && it != nameIdx }

        return rows.mapNotNull { row ->
            val maxIdx = maxOf(dateIdx, nameIdx, amountIdx)
            if (row.size <= maxIdx) return@mapNotNull null

            val date   = toDate(row[dateIdx])             ?: return@mapNotNull null
            val name   = row[nameIdx].trim().ifEmpty { return@mapNotNull null }
            val amount = toAmount(row[amountIdx])          ?: return@mapNotNull null
            val memo   = memoIdx?.let { row.getOrNull(it)?.trim()?.ifEmpty { null } }

            ParsedTransaction(date, name, amount, memo)
        }
    }

    // -----------------------------------------------------------------------
    // 日付・金額パーサー
    // -----------------------------------------------------------------------

    /**
     * 各種日付文字列を "YYYY-MM-DD" に変換する。
     *   "2026/01/15"  → "2026-01-15"
     *   "2026-01-15"  → "2026-01-15"
     *   "2026年1月15日" → "2026-01-15"
     *   "01/15"       → 当年を補完
     */
    private fun toDate(raw: String): String? {
        val s = raw.trim()

        // YYYY/MM/DD または YYYY-MM-DD
        Regex("""(\d{4})[/\-](\d{1,2})[/\-](\d{1,2})""").find(s)?.let { m ->
            val (y, mo, d) = m.destructured
            return "%04d-%02d-%02d".format(y.toInt(), mo.toInt(), d.toInt())
        }

        // YYYY年MM月DD日
        Regex("""(\d{4})年(\d{1,2})月(\d{1,2})日""").find(s)?.let { m ->
            val (y, mo, d) = m.destructured
            return "%04d-%02d-%02d".format(y.toInt(), mo.toInt(), d.toInt())
        }

        // MM/DD（年なし → 当年を補完）
        Regex("""^(\d{1,2})/(\d{1,2})$""").find(s)?.let { m ->
            val (mo, d) = m.destructured
            val year = Calendar.getInstance().get(Calendar.YEAR)
            return "%04d-%02d-%02d".format(year, mo.toInt(), d.toInt())
        }

        return null
    }

    /**
     * 金額文字列を Int に変換する。
     *   "3,500"  → 3500
     *   "¥3,500" → 3500
     *   "-3,500" → -3500
     */
    private fun toAmount(raw: String): Int? {
        val s = raw.trim()
            .replace(",", "")
            .replace("¥", "")
            .replace("￥", "")
            .replace("円", "")
            .replace(" ", "")
        return s.toIntOrNull()
    }

    // -----------------------------------------------------------------------
    // RFC 4180 CSV パーサー
    // -----------------------------------------------------------------------

    /**
     * テキストを CSV としてパースし、行×列のリストを返す。
     * クォートされたフィールド・フィールド内改行・ダブルクォートエスケープに対応。
     */
    private fun parseCsvText(text: String): List<List<String>> {
        val result       = mutableListOf<List<String>>()
        val currentRow   = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes     = false
        var i            = 0

        while (i < text.length) {
            val ch = text[i]
            when {
                // "" → " (ダブルクォートエスケープ)
                ch == '"' && inQuotes && i + 1 < text.length && text[i + 1] == '"' -> {
                    currentField.append('"')
                    i += 2
                }
                ch == '"' -> {
                    inQuotes = !inQuotes
                    i++
                }
                ch == ',' && !inQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField.clear()
                    i++
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField.clear()
                    if (currentRow.any { it.isNotBlank() }) result.add(currentRow.toList())
                    currentRow.clear()
                    // CRLF を 1 行扱い
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    i++
                }
                else -> {
                    currentField.append(ch)
                    i++
                }
            }
        }

        // 末尾に改行がない場合の最終行処理
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotBlank() }) result.add(currentRow.toList())
        }

        return result
    }
}
