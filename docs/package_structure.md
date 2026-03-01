# パッケージ構成（詳細）

## ディレクトリツリー

```
com.example.dashboard/
  ui/
    kakeibo/
      dashboard/        DashboardScreen.kt + DashboardViewModel.kt
      transactions/     TransactionsScreen.kt + TransactionsViewModel.kt
      import/           ImportScreen.kt + ImportViewModel.kt
      budget/           BudgetScreen.kt + BudgetViewModel.kt
      cards/            CardsScreen.kt + CardsViewModel.kt
      settings/         SettingsScreen.kt + SettingsViewModel.kt
    components/         BottomNavBar.kt, TopBar.kt, 共通コンポーネント
  data/
    local/
      AppDatabase.kt
      dao/              TransactionDao, CategoryDao, CreditCardDao,
                        BudgetDao, CategoryRuleDao, ImportHistoryDao
      entity/           各エンティティ（Entityサフィックス）
    repository/
      *Repository.kt        interface定義
      *RepositoryImpl.kt    Room実装（Phase 1はこれのみ）
    remote/               将来のAPI連携用（Phase 3以降）
  domain/
    model/              UIで使うドメインモデル（Entityとは分離）
    usecase/            ClassifyTransactionUseCase, ImportCsvUseCase,
                        GetDashboardSummaryUseCase
  di/                   DatabaseModule.kt, RepositoryModule.kt（Hilt）
  util/                 CsvParser.kt, TextNormalizer.kt
```

## ナビゲーション構成

ボトムナビゲーションバー（Web版サイドバー相当）:

| タブ | アイコン | 遷移先 |
|---|---|---|
| ホーム | Home | DashboardScreen |
| 明細 | List | TransactionsScreen（未分類バッジ） |
| 入力 | Add | ImportScreen / AddTransactionScreen |
| 予算 | Wallet | BudgetScreen |
| 設定 | Settings | SettingsScreen（カード・カテゴリ・仕分けルール） |

## 画面ルート一覧

| 画面 | ルート | 主な内容 |
|---|---|---|
| ダッシュボード | `kakeibo/dashboard` | 今月サマリー・グラフ・予算アラート・未分類バッジ |
| 取引一覧 | `kakeibo/transactions` | 検索・フィルター・1件削除・カテゴリ修正 |
| CSVインポート | `kakeibo/import` | ファイルピッカー→プレビュー→確定 |
| インポート履歴 | `kakeibo/imports` | 履歴一覧・まとめて削除 |
| 手動入力 | `kakeibo/add` | フォーム入力 |
| 予算管理 | `kakeibo/budget` | カテゴリ別予算設定・消化率 |
| カード管理 | `settings/cards` | カード登録・編集・削除 |
| 仕分けルール | `settings/rules` | ルール一覧・追加・削除 |
| カテゴリ管理 | `settings/categories` | カテゴリ一覧・追加 |

## Repository パターン（Hilt注入）

```kotlin
// Phase 1: Hiltでこれを注入
@Module @InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides fun provideTransactionRepository(
        dao: TransactionDao
    ): TransactionRepository = LocalTransactionRepositoryImpl(dao)
}

// Phase 3: 差し替えるだけでWeb連携に対応
// @Provides fun provideTransactionRepository(
//     api: ApiService
// ): TransactionRepository = RemoteTransactionRepositoryImpl(api)
```
