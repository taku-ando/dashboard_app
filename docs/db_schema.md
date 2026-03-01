# DBスキーマ（Room Entities）

`CLAUDE.md` から切り出した詳細参照ファイル。

**重要：** カラム名・型はWeb版（Cloudflare D1）スキーマと合わせる。将来の同期を容易にするため。

## エンティティ一覧

| Roomエンティティ | Web版テーブル | 備考 |
|---|---|---|
| `TransactionEntity` | transactions | userId は "local" 固定 |
| `CreditCardEntity` | credit_cards | |
| `ImportHistoryEntity` | import_history | |
| `CategoryEntity` | categories | |
| `CategoryRuleEntity` | category_rules | |
| `BudgetEntity` | budgets | |

## TransactionEntity

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,           // UUID
    val userId: String = "local",
    val cardId: String?,
    val importId: String?,
    val date: String,                     // "YYYY-MM-DD"
    val name: String,
    val categoryId: String?,
    val amount: Int,                      // 円単位
    val memo: String?,
    val source: String,                   // "csv" or "manual"
    val classifiedBy: String,             // "rule" | "keyword" | "ai" | "manual" | "unclassified"
    val createdAt: String                 // ISO 8601
)
```

## CreditCardEntity

```kotlin
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val name: String,
    val issuer: String,
    val colorCode: String,
    val createdAt: String
)
```

## ImportHistoryEntity

```kotlin
@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val fileName: String,
    val fileHash: String,                 // SHA-256
    val cardId: String?,
    val count: Int,
    val importedAt: String
)
```

## CategoryEntity

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Int                    // 0 or 1
)
```

## CategoryRuleEntity

```kotlin
@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val shopName: String,
    val matchType: String,                // "exact" | "prefix" | "contains"
    val categoryId: String,
    val createdAt: String,
    val updatedAt: String
)
```

## BudgetEntity

```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val categoryId: String,
    val year: Int,
    val month: Int,
    val limitAmount: Int
)
```

## 設計方針

- **Phase 1 では `userId` は常に `"local"` 固定。** 将来のWeb連携時に実際のUUIDに移行する
- 日付は ISO 8601 文字列（`String`）で保存
- 金額は `Int`（円単位）
- 削除はすべて物理削除（論理削除なし）

## 削除ポリシー

| 操作 | 実行場所 | 内部動作 |
|---|---|---|
| 取引1件削除 | 明細一覧のスワイプ or メニュー | 物理削除（確認ダイアログあり） |
| インポート履歴単位で削除 | インポート履歴画面 | import_idが一致する全取引を物理削除 → import_historyも削除 |

## デフォルトカテゴリ（初期データ）

| ID | カテゴリ | アイコン | キーワード例 |
|---|---|---|---|
| cat_food | 食費 | 🛒 | スーパー、コンビニ、レストラン、カフェ |
| cat_transport | 交通費 | 🚃 | 電車、バス、タクシー、Suica、JR |
| cat_entertainment | 娯楽 | 🎮 | 映画、Netflix、Spotify、ゲーム |
| cat_shopping | ショッピング | 🛍 | Amazon、楽天、ユニクロ |
| cat_medical | 医療・健康 | 💊 | 病院、薬局、クリニック |
| cat_utilities | 光熱費・通信 | 💡 | 電力、ガス、携帯、docomo |
| cat_other | その他 | 📦 | （デフォルト・上記に一致しない場合） |

## 自動仕分けパイプライン（ClassifyTransactionUseCase）

```
店名入力
  ↓
① category_rules を照合（exact → prefix → contains の優先順）
    一致 → classifiedBy = "rule"
  ↓（不一致の場合）
② キーワード辞書と照合（TextNormalizerで全角/半角正規化後）
    一致 → classifiedBy = "keyword"
  ↓（不一致の場合）
③ Claude API にバッチ送信（Phase 2以降・AIがONの場合のみ）
    成功 → classifiedBy = "ai"
    失敗 or OFF → classifiedBy = "unclassified"
```
