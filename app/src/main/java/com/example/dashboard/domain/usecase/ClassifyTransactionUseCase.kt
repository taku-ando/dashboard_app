package com.example.dashboard.domain.usecase

import com.example.dashboard.data.repository.RuleRepository
import com.example.dashboard.domain.model.CategoryRule
import com.example.dashboard.util.TextNormalizer
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class ClassifyResult(
    val categoryId: String?,
    val classifiedBy: String   // "rule" | "keyword" | "unclassified"
)

/**
 * 店名を受け取り、3段階パイプラインで自動仕分けする UseCase。
 *
 * ① CategoryRule 照合（exact → prefix → contains の優先順）
 * ② キーワード辞書照合（TextNormalizer で正規化後）
 * ③ AI（Phase 2以降）→ 現時点は "unclassified" を返す
 *
 * 単発呼び出し:  invoke(shopName)          ← ルールを都度 DB から取得
 * バッチ呼び出し: invoke(shopName, rules)   ← 呼び出し元がルールを渡す（インポート時）
 */
@Singleton
class ClassifyTransactionUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    /** 単発用（ViewModel からカテゴリ再分類する際など）。 */
    suspend operator fun invoke(shopName: String): ClassifyResult {
        val rules = ruleRepository.getAll().first()
        return invoke(shopName, rules)
    }

    /** バッチ用（ImportCsvUseCase がルールを事前取得して渡す）。 */
    operator fun invoke(shopName: String, rules: List<CategoryRule>): ClassifyResult {
        val normalized = TextNormalizer.normalize(shopName)

        // ① CategoryRule 照合
        classifyWithRules(normalized, rules)?.let { return it }

        // ② キーワード辞書照合
        matchKeyword(normalized)?.let { return ClassifyResult(it, "keyword") }

        // ③ AI は Phase 2 以降
        return ClassifyResult(null, "unclassified")
    }

    // -----------------------------------------------------------------------
    // ① CategoryRule 照合
    // -----------------------------------------------------------------------

    private fun classifyWithRules(
        normalized: String,
        rules: List<CategoryRule>
    ): ClassifyResult? {
        // exact → prefix → contains の優先順で最初のマッチを返す
        for (matchType in listOf("exact", "prefix", "contains")) {
            val match = rules.firstOrNull { rule ->
                rule.matchType == matchType &&
                    matchRule(normalized, TextNormalizer.normalize(rule.shopName), matchType)
            }
            if (match != null) return ClassifyResult(match.categoryId, "rule")
        }
        return null
    }

    private fun matchRule(normalized: String, ruleNorm: String, matchType: String): Boolean =
        when (matchType) {
            "exact"    -> normalized.equals(ruleNorm, ignoreCase = true)
            "prefix"   -> normalized.startsWith(ruleNorm, ignoreCase = true)
            "contains" -> normalized.contains(ruleNorm, ignoreCase = true)
            else       -> false
        }

    // -----------------------------------------------------------------------
    // ② キーワード辞書照合
    // -----------------------------------------------------------------------

    private fun matchKeyword(normalized: String): String? {
        for ((keyword, categoryId) in KEYWORD_MAP) {
            if (normalized.contains(keyword, ignoreCase = true)) return categoryId
        }
        return null
    }

    companion object {
        /**
         * キーワード → カテゴリID のマッピング。
         * 先に定義したキーワードが優先される（exact-like な順序）。
         * キーワードは半角で記述（TextNormalizer 適用後の店名と照合）。
         */
        private val KEYWORD_MAP: Map<String, String> = linkedMapOf(
            // ── 食費 ────────────────────────────────────────────────────
            "マクドナルド"       to "cat_food",
            "モスバーガー"       to "cat_food",
            "ケンタッキー"       to "cat_food",
            "KFC"               to "cat_food",
            "スターバックス"     to "cat_food",
            "ドトール"           to "cat_food",
            "コメダ"             to "cat_food",
            "タリーズ"           to "cat_food",
            "サイゼリヤ"         to "cat_food",
            "ガスト"             to "cat_food",
            "デニーズ"           to "cat_food",
            "バーミヤン"         to "cat_food",
            "すき家"             to "cat_food",
            "松屋"               to "cat_food",
            "吉野家"             to "cat_food",
            "なか卯"             to "cat_food",
            "マルエツ"           to "cat_food",
            "マックスバリュ"     to "cat_food",
            "ヨークマート"       to "cat_food",
            "ライフ"             to "cat_food",
            "セブン-イレブン"    to "cat_food",
            "セブンイレブン"     to "cat_food",
            "ファミリーマート"   to "cat_food",
            "ファミマ"           to "cat_food",
            "ローソン"           to "cat_food",
            "ミニストップ"       to "cat_food",
            "スーパー"           to "cat_food",
            "コンビニ"           to "cat_food",
            "レストラン"         to "cat_food",
            "カフェ"             to "cat_food",
            "弁当"               to "cat_food",
            "うどん"             to "cat_food",
            "そば"               to "cat_food",
            "ラーメン"           to "cat_food",
            "焼肉"               to "cat_food",
            "寿司"               to "cat_food",
            "食堂"               to "cat_food",
            "餃子"               to "cat_food",

            // ── 交通費 ──────────────────────────────────────────────────
            "Suica"             to "cat_transport",
            "PASMO"             to "cat_transport",
            "ICOCA"             to "cat_transport",
            "manaca"            to "cat_transport",
            "Kitaca"            to "cat_transport",
            "東京メトロ"         to "cat_transport",
            "都営"               to "cat_transport",
            "近鉄"               to "cat_transport",
            "阪急"               to "cat_transport",
            "阪神"               to "cat_transport",
            "南海"               to "cat_transport",
            "京阪"               to "cat_transport",
            "JR"                to "cat_transport",
            "新幹線"             to "cat_transport",
            "タクシー"           to "cat_transport",
            "ウーバー"           to "cat_transport",
            "Uber"              to "cat_transport",
            "電車"               to "cat_transport",
            "バス"               to "cat_transport",
            "高速道路"           to "cat_transport",
            "ETC"               to "cat_transport",
            "駐車場"             to "cat_transport",
            "パーキング"         to "cat_transport",
            "JAL"               to "cat_transport",
            "ANA"               to "cat_transport",

            // ── 娯楽 ────────────────────────────────────────────────────
            "Netflix"           to "cat_entertainment",
            "Spotify"           to "cat_entertainment",
            "Amazon Prime"      to "cat_entertainment",
            "Disney"            to "cat_entertainment",
            "Hulu"              to "cat_entertainment",
            "U-NEXT"            to "cat_entertainment",
            "YouTube Premium"   to "cat_entertainment",
            "Apple Music"       to "cat_entertainment",
            "PlayStation"       to "cat_entertainment",
            "Nintendo"          to "cat_entertainment",
            "Steam"             to "cat_entertainment",
            "カラオケ"           to "cat_entertainment",
            "ジョイサウンド"     to "cat_entertainment",
            "ビッグエコー"       to "cat_entertainment",
            "ボウリング"         to "cat_entertainment",
            "映画"               to "cat_entertainment",
            "コンサート"         to "cat_entertainment",
            "ライブ"             to "cat_entertainment",
            "ゲームセンター"     to "cat_entertainment",
            "ゲーム"             to "cat_entertainment",

            // ── ショッピング ─────────────────────────────────────────────
            "Amazon"            to "cat_shopping",
            "楽天"               to "cat_shopping",
            "ZOZOTOWN"          to "cat_shopping",
            "メルカリ"           to "cat_shopping",
            "ラクマ"             to "cat_shopping",
            "ユニクロ"           to "cat_shopping",
            "GU"                to "cat_shopping",
            "ZARA"              to "cat_shopping",
            "H&M"               to "cat_shopping",
            "無印良品"           to "cat_shopping",
            "MUJI"              to "cat_shopping",
            "ニトリ"             to "cat_shopping",
            "ヤマダ電機"         to "cat_shopping",
            "ヨドバシ"           to "cat_shopping",
            "ビックカメラ"       to "cat_shopping",
            "ケーズデンキ"       to "cat_shopping",

            // ── 医療・健康 ───────────────────────────────────────────────
            "マツモトキヨシ"     to "cat_medical",
            "マツキヨ"           to "cat_medical",
            "ツルハ"             to "cat_medical",
            "ウエルシア"         to "cat_medical",
            "スギ薬局"           to "cat_medical",
            "コスモス薬品"       to "cat_medical",
            "ドラッグストア"     to "cat_medical",
            "薬局"               to "cat_medical",
            "クリニック"         to "cat_medical",
            "病院"               to "cat_medical",
            "歯科"               to "cat_medical",
            "眼科"               to "cat_medical",
            "内科"               to "cat_medical",
            "皮膚科"             to "cat_medical",
            "整形外科"           to "cat_medical",
            "フィットネス"       to "cat_medical",
            "スポーツクラブ"     to "cat_medical",
            "ジム"               to "cat_medical",

            // ── 光熱費・通信 ─────────────────────────────────────────────
            "東京電力"           to "cat_utilities",
            "関西電力"           to "cat_utilities",
            "東京ガス"           to "cat_utilities",
            "大阪ガス"           to "cat_utilities",
            "docomo"            to "cat_utilities",
            "NTT"               to "cat_utilities",
            "au"                to "cat_utilities",
            "SoftBank"          to "cat_utilities",
            "ソフトバンク"       to "cat_utilities",
            "楽天モバイル"       to "cat_utilities",
            "UQモバイル"         to "cat_utilities",
            "ワイモバイル"       to "cat_utilities",
            "NHK"               to "cat_utilities",
            "電力"               to "cat_utilities",
            "ガス"               to "cat_utilities",
            "水道"               to "cat_utilities",
            "携帯"               to "cat_utilities",
            "光回線"             to "cat_utilities",
            "インターネット"     to "cat_utilities"
        )
    }
}
