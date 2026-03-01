# 実装TODOリスト

> 作業開始時にこのファイルを参照すること。
> 完了したタスクは `[x]` に更新する。
> 詳細仕様は `CLAUDE.md` → `docs/` 配下の参照ファイルを確認。

---

## 現在の状態（2026-03-01時点）

- `app/build.gradle.kts` — 全依存関係設定済み ✅
- `gradle/libs.versions.toml` — バージョン定義済み ✅
- `MainActivity.kt` — 空のスキャフォールド（更新必要）
- `ui/theme/` — Color.kt, Theme.kt, Type.kt 存在
- **それ以外はすべて未実装**

---

## Phase 1（MVP）実装タスク

### Step 1: Applicationクラス / エントリーポイント

- [ ] `DashboardApplication.kt` 作成（`@HiltAndroidApp`）
- [ ] `AndroidManifest.xml` に `android:name=".DashboardApplication"` 追加
- [ ] `MainActivity.kt` 更新（`@AndroidEntryPoint` + `NavHost` 呼び出し）

### Step 2: Data層 — Room Entities

> 詳細スキーマ: `docs/db_schema.md`

- [ ] `data/local/entity/TransactionEntity.kt`
- [ ] `data/local/entity/CategoryEntity.kt`
- [ ] `data/local/entity/CreditCardEntity.kt`
- [ ] `data/local/entity/BudgetEntity.kt`
- [ ] `data/local/entity/CategoryRuleEntity.kt`
- [ ] `data/local/entity/ImportHistoryEntity.kt`

### Step 3: Data層 — DAOs

- [ ] `data/local/dao/TransactionDao.kt`
  - `insert`, `delete`, `deleteByImportId`, `getAll`, `getByMonth`, `getUnclassified`, `search`
- [ ] `data/local/dao/CategoryDao.kt`
  - `insert`, `update`, `delete`, `getAll`
- [ ] `data/local/dao/CreditCardDao.kt`
  - `insert`, `update`, `delete`, `getAll`
- [ ] `data/local/dao/BudgetDao.kt`
  - `insert`, `update`, `delete`, `getByMonth`
- [ ] `data/local/dao/CategoryRuleDao.kt`
  - `insert`, `delete`, `getAll`（exact→prefix→contains順）
- [ ] `data/local/dao/ImportHistoryDao.kt`
  - `insert`, `delete`, `getAll`, `findByHash`

### Step 4: Data層 — AppDatabase

- [ ] `data/local/AppDatabase.kt`（全Entity登録・バージョン1）
- [ ] データベースコールバック（初期カテゴリ7件を`prepopulate`）

### Step 5: Domain層 — ドメインモデル

- [ ] `domain/model/Transaction.kt`
- [ ] `domain/model/Category.kt`
- [ ] `domain/model/CreditCard.kt`
- [ ] `domain/model/Budget.kt`
- [ ] `domain/model/CategoryRule.kt`

### Step 6: Data層 — Repositoryインターフェース & 実装

- [ ] `data/repository/TransactionRepository.kt`（interface）
- [ ] `data/repository/TransactionRepositoryImpl.kt`（Room実装）
- [ ] `data/repository/CategoryRepository.kt` + `Impl`
- [ ] `data/repository/BudgetRepository.kt` + `Impl`
- [ ] `data/repository/CardRepository.kt` + `Impl`
- [ ] `data/repository/RuleRepository.kt` + `Impl`
- [ ] `data/repository/ImportHistoryRepository.kt` + `Impl`

### Step 7: DI — Hiltモジュール

- [ ] `di/DatabaseModule.kt`（AppDatabase, 各DAO提供）
- [ ] `di/RepositoryModule.kt`（各Repository実装をinterfaceにバインド）

### Step 8: Util層

- [ ] `util/TextNormalizer.kt`（全角→半角変換・正規化）
- [ ] `util/CsvParser.kt`
  - イオンカードCSVフォーマット対応
  - 汎用CSV（列名自動判別）
  - 文字コード自動判別（UTF-8 / Shift-JIS）
  - SHA-256ハッシュ生成

### Step 9: Domain層 — UseCases

- [ ] `domain/usecase/ClassifyTransactionUseCase.kt`
  - ① CategoryRule照合（exact → prefix → contains）
  - ② キーワード辞書照合（TextNormalizerで正規化後）
  - ③ AI（Phase 2以降・未実装のまま残す）
- [ ] `domain/usecase/ImportCsvUseCase.kt`
  - CSV解析 → SHA-256重複チェック → 自動仕分け → DB保存
- [ ] `domain/usecase/GetDashboardSummaryUseCase.kt`
  - 当月合計・カテゴリ別集計・予算消化率・未分類件数

### Step 10: UI層 — ナビゲーション

> 詳細: `docs/package_structure.md`

- [ ] `ui/components/BottomNavBar.kt`（5タブ）
- [ ] `ui/components/TopBar.kt`（共通タイトルバー）
- [ ] ナビゲーションルート定義（sealed class or enum）
- [ ] `AppNavHost.kt`（NavGraph全画面登録）

### Step 11: UI層 — ダッシュボード画面

- [ ] `ui/kakeibo/dashboard/DashboardViewModel.kt`
  - `GetDashboardSummaryUseCase` 呼び出し
  - 月切り替えロジック
- [ ] `ui/kakeibo/dashboard/DashboardScreen.kt`
  - 今月合計金額カード
  - カテゴリ別ドーナツグラフ（Vico）
  - 月別推移棒グラフ（Vico・直近12ヶ月）
  - 予算アラート（超過カテゴリ警告）
  - 未分類件数バッジ

### Step 12: UI層 — 取引一覧画面

- [ ] `ui/kakeibo/transactions/TransactionsViewModel.kt`
  - 検索・フィルター（店名・カテゴリ・カード・期間）
  - ソート（日付・金額）
  - カテゴリ修正
  - 1件削除（確認ダイアログ）
- [ ] `ui/kakeibo/transactions/TransactionsScreen.kt`
  - LazyColumn一覧
  - スワイプ削除
  - 検索バー・フィルターチップ

### Step 13: UI層 — インポート画面

- [ ] `ui/kakeibo/import/ImportViewModel.kt`
  - SAFファイルピッカー連携
  - `ImportCsvUseCase` 呼び出し
  - 重複ファイル警告ダイアログ
  - インポートプレビュー（件数・仕分け結果）
- [ ] `ui/kakeibo/import/ImportScreen.kt`
  - ファイル選択ボタン
  - インポートプレビュー（BottomSheet）
  - 確定ボタン
  - インポート履歴タブ（まとめて削除）

### Step 14: UI層 — 手動入力画面

- [ ] 手動入力フォーム（日付ピッカー・金額・店名・カテゴリ選択・メモ）
  - Material3 `DatePicker` 使用

### Step 15: UI層 — 予算管理画面

- [ ] `ui/kakeibo/budget/BudgetViewModel.kt`
- [ ] `ui/kakeibo/budget/BudgetScreen.kt`
  - カテゴリ別予算設定
  - `LinearProgressIndicator` で消化率表示
  - 超過アラート

### Step 16: UI層 — 設定画面

- [ ] `ui/kakeibo/settings/SettingsViewModel.kt`
- [ ] `ui/kakeibo/settings/SettingsScreen.kt`
  - カード管理（登録・編集・削除）
  - カテゴリ管理（一覧・追加）
  - 仕分けルール管理（一覧・追加・削除）

---

## Phase 2（将来）

- [ ] Claude API連携（`ClassifyTransactionUseCase` ③ステップ実装）
- [ ] APIキー入力UI（設定画面）
- [ ] 未分類取引の一括仕分け画面
- [ ] 予算管理の強化

## Phase 3（将来）

- [ ] Retrofit経由でWeb版（Cloudflare D1）と同期
- [ ] `RemoteTransactionRepositoryImpl` 実装・Hilt差し替え

---

## 実装上の注意事項

- Entity作成時は必ず `docs/db_schema.md` のKotlinコードを参照
- パッケージ構成は `docs/package_structure.md` を参照
- ViewModel は `@HiltViewModel` + `@Inject constructor` で実装
- Flowは `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)` で変換
- Repository interfaceを必ず先に定義してから実装クラスを書く
- minSdk=26 対応（`java.nio.charset` 等でAPI level注意）
