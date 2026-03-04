# 実装TODOリスト

> 作業開始時にこのファイルを参照すること。
> 完了したタスクは `[x]` に更新する。
> 詳細仕様は `CLAUDE.md` → `docs/` 配下の参照ファイルを確認。

---

## 現在の状態（2026-03-01時点）

- `app/build.gradle.kts` — 全依存関係設定済み・minSdk=26修正済み ✅
- `gradle/libs.versions.toml` — バージョン定義済み ✅
- `MainActivity.kt` — @AndroidEntryPoint 更新済み ✅
- `DashboardApplication.kt` — @HiltAndroidApp 実装済み ✅
- `ui/theme/` — Color.kt, Theme.kt, Type.kt 存在 ✅
- `data/local/entity/` — 全6エンティティ実装済み ✅
- `data/local/dao/` — 全6 DAO実装済み ✅
- `data/local/AppDatabase.kt` — デフォルトカテゴリコールバック付き実装済み ✅
- `domain/model/` — 全5ドメインモデル実装済み ✅
- `data/repository/` — 全6 interface + impl 実装済み ✅
- `di/DatabaseModule.kt` + `di/RepositoryModule.kt` — 実装済み ✅
- `util/TextNormalizer.kt` — 実装済み ✅
- `util/CsvParser.kt` — 実装済み ✅
- `domain/usecase/ClassifyTransactionUseCase.kt` — 実装済み ✅
- `domain/usecase/ImportCsvUseCase.kt` — 実装済み ✅
- `domain/usecase/GetDashboardSummaryUseCase.kt` — 実装済み ✅
- `ui/navigation/Screen.kt` — 実装済み ✅
- `ui/navigation/AppNavHost.kt` — 実装済み ✅
- `ui/components/BottomNavBar.kt` — 実装済み ✅
- `ui/components/TopBar.kt` — 実装済み ✅
- `ui/AppViewModel.kt` — 実装済み ✅
- `ui/kakeibo/dashboard/` — DashboardViewModel + DashboardScreen 実装済み ✅
- `ui/kakeibo/transactions/` — TransactionsViewModel + TransactionsScreen 実装済み ✅
- `ui/kakeibo/import/` — ImportViewModel + ImportScreen 実装済み ✅
- `ui/kakeibo/add/` — AddTransactionViewModel + AddTransactionScreen 実装済み ✅
- `ui/kakeibo/budget/` — BudgetViewModel + BudgetScreen 実装済み ✅
- `ui/kakeibo/settings/` — SettingsViewModel + SettingsScreen 実装済み ✅
- `MainActivity.kt` — AppNavHost 接続済み ✅
- **Steps 1〜16 完了。Phase 1 MVP 実装完了。**

---

## Phase 1（MVP）実装タスク

### Step 1: Applicationクラス / エントリーポイント ✅

- [x] `DashboardApplication.kt` 作成（`@HiltAndroidApp`）
- [x] `AndroidManifest.xml` に `android:name=".DashboardApplication"` 追加
- [x] `MainActivity.kt` 更新（`@AndroidEntryPoint` + 暫定Scaffold）

### Step 2: Data層 — Room Entities ✅

- [x] `data/local/entity/TransactionEntity.kt`
- [x] `data/local/entity/CategoryEntity.kt`
- [x] `data/local/entity/CreditCardEntity.kt`
- [x] `data/local/entity/BudgetEntity.kt`
- [x] `data/local/entity/CategoryRuleEntity.kt`
- [x] `data/local/entity/ImportHistoryEntity.kt`

### Step 3: Data層 — DAOs ✅

- [x] `data/local/dao/TransactionDao.kt`
- [x] `data/local/dao/CategoryDao.kt`
- [x] `data/local/dao/CreditCardDao.kt`
- [x] `data/local/dao/BudgetDao.kt`
- [x] `data/local/dao/CategoryRuleDao.kt`
- [x] `data/local/dao/ImportHistoryDao.kt`

### Step 4: Data層 — AppDatabase ✅

- [x] `data/local/AppDatabase.kt`（全Entity登録・バージョン1）
- [x] データベースコールバック（初期カテゴリ7件を`onCreate`でINSERT）

### Step 5: Domain層 — ドメインモデル ✅

- [x] `domain/model/Transaction.kt`
- [x] `domain/model/Category.kt`
- [x] `domain/model/CreditCard.kt`
- [x] `domain/model/Budget.kt`
- [x] `domain/model/CategoryRule.kt`

### Step 6: Data層 — Repositoryインターフェース & 実装 ✅

- [x] `data/repository/TransactionRepository.kt` + `Impl`
- [x] `data/repository/CategoryRepository.kt` + `Impl`
- [x] `data/repository/BudgetRepository.kt` + `Impl`
- [x] `data/repository/CardRepository.kt` + `Impl`
- [x] `data/repository/RuleRepository.kt` + `Impl`
- [x] `data/repository/ImportHistoryRepository.kt` + `Impl`

### Step 7: DI — Hiltモジュール ✅

- [x] `di/DatabaseModule.kt`（AppDatabase, 各DAO提供）
- [x] `di/RepositoryModule.kt`（各Repository実装をinterfaceにバインド）

### Step 8: Util層 ✅

- [x] `util/TextNormalizer.kt`（全角→半角変換・正規化）
- [x] `util/CsvParser.kt`
  - イオンカードCSVフォーマット対応
  - 汎用CSV（列名自動判別）
  - 文字コード自動判別（UTF-8 / Shift-JIS / Windows-31J）
  - SHA-256ハッシュ生成

### Step 9: Domain層 — UseCases ✅

- [x] `domain/usecase/ClassifyTransactionUseCase.kt`
  - ① CategoryRule照合（exact → prefix → contains）
  - ② キーワード辞書照合（TextNormalizerで正規化後）
  - ③ AI（Phase 2以降・未実装のまま残す）
- [x] `domain/usecase/ImportCsvUseCase.kt`
  - CSV解析 → SHA-256重複チェック → 自動仕分け → DB保存
- [x] `domain/usecase/GetDashboardSummaryUseCase.kt`
  - 当月合計・カテゴリ別集計・予算消化率・未分類件数

### Step 10: UI層 — ナビゲーション ✅

- [x] `ui/components/BottomNavBar.kt`（5タブ）
- [x] `ui/components/TopBar.kt`（共通タイトルバー）
- [x] ナビゲーションルート定義（`ui/navigation/Screen.kt`）
- [x] `AppNavHost.kt`（NavGraph全画面登録）

### Step 11: UI層 — ダッシュボード画面 ✅

- [x] `ui/kakeibo/dashboard/DashboardViewModel.kt`
- [x] `ui/kakeibo/dashboard/DashboardScreen.kt`
  - 今月合計金額カード
  - カテゴリ別ドーナツグラフ（Canvas実装）
  - 月別推移棒グラフ（直近12ヶ月・Canvas実装）
  - 予算アラート（超過カテゴリ警告）
  - 未分類件数バッジ

### Step 12: UI層 — 取引一覧画面 ✅

- [x] `ui/kakeibo/transactions/TransactionsViewModel.kt`
- [x] `ui/kakeibo/transactions/TransactionsScreen.kt`
  - LazyColumn一覧（日付別グルーピング）
  - スワイプ削除（AlertDialog確認）
  - SearchBar・カテゴリフィルターチップ
  - タップ → カテゴリ変更ドロップダウン

### Step 13: UI層 — インポート画面 ✅

- [x] `ui/kakeibo/import/ImportViewModel.kt`
- [x] `ui/kakeibo/import/ImportScreen.kt`
  - SAFファイルピッカー（GetContent("text/*")）
  - 重複ファイル警告AlertDialog
  - インポート結果Snackbar
  - インポート履歴タブ（まとめて削除）

### Step 14: UI層 — 手動入力画面 ✅

- [x] `ui/kakeibo/add/AddTransactionViewModel.kt`
- [x] `ui/kakeibo/add/AddTransactionScreen.kt`
  - Material3 DatePickerDialog
  - 金額・店名・カテゴリ・メモ入力
  - バリデーション + Snackbar

### Step 15: UI層 — 予算管理画面 ✅

- [x] `ui/kakeibo/budget/BudgetViewModel.kt`
- [x] `ui/kakeibo/budget/BudgetScreen.kt`
  - カテゴリ別予算設定（AlertDialogで編集）
  - LinearProgressIndicatorで消化率表示
  - 超過アラート（errorカラー）

### Step 16: UI層 — 設定画面 ✅

- [x] `ui/kakeibo/settings/SettingsViewModel.kt`
- [x] `ui/kakeibo/settings/SettingsScreen.kt`
  - カード管理（登録・削除）
  - カテゴリ管理（一覧・追加・削除）
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
