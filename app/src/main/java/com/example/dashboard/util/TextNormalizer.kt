package com.example.dashboard.util

/**
 * 店名テキストの正規化ユーティリティ。
 * 自動仕分けのキーワード照合前に適用し、全角/半角の揺らぎを吸収する。
 */
object TextNormalizer {

    /**
     * 全角英数字・記号を半角に変換し、前後の空白をトリムする。
     *
     * 変換対象:
     *   - 全角ASCII可視文字 (U+FF01〜U+FF5E) → 半角 (U+0021〜U+007E)
     *   - 全角スペース (U+3000) → 半角スペース (U+0020)
     */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(
                when {
                    ch in '\uFF01'..'\uFF5E' -> (ch.code - 0xFEE0).toChar()
                    ch == '\u3000' -> ' '
                    else -> ch
                }
            )
        }
        return sb.toString().trim()
    }
}
