# パーソナルダッシュボード（Android版）要件定義書

| 項目 | 内容 |
|---|---|
| 文書番号 | DASHBOARD-ANDROID-REQ-001 |
| バージョン | v1.0 |
| 作成日 | 2026-02-23 |
| ステータス | ドラフト |
| 対応プラットフォーム | Android |

## 改訂履歴

| Ver | 日付 | 変更内容 |
|---|---|---|
| v1.0 | 2026-02-23 | Web版要件定義書(v7.0)をAndroid / Kotlin向けに書き直し |

---

## 1. プロジェクト概要

### 1.1 コンセプト

Web版パーソナルダッシュボードのAndroidネイティブアプリ版。自分のスマートフォン1台にのみインストールして使う個人用ツール。Phase 1 では家計簿モジュールを実装する。

**Web版との違い：**

| 項目 | Web版 | Android版 |
|---|---|---|
| 認証 | JWT + メール/パスワード | **不要**（自分のスマホのみ） |
| DB | Cloudflare D1（クラウド） | Room / SQLite（ローカル） |
| 配信 | Cloudflare Pages | **サイドロード**（APK直インストール） |
| ログイン画面 | あり | **なし**（起動即ホーム） |

### 1.2 スコープ

| 項目 | 内容 |
|---|---|
| 対象デバイス | 自分のAndroidスマートフォン（API 26 / Android 8.0 以上） |
| 配信方法 | Google Play非公開・APKをUSBまたはADBでサイドロード |
| データ保存 | ローカルのみ（Room / SQLite） |
| 将来の拡張 | Web版Cloudflare D1との同期APIを後から追加できる設計 |

---

## 2. アーキテクチャ方針

### 2.1 全体構成

**MVVM + Repository パターン**を採用する。将来のWeb連携時にRepositoryの実装を差し替えるだけで対応できる設計にする。

```
UI Layer        ViewModel           Domain/Data Layer
─────────────── ─────────────────── ──────────────────────────────
Composable  ←→  ViewModel       ←→  Repository（interface）
Screen              ↓                   ├─ LocalRepository（Room）  ← Phase 1
                 UiState                └─ RemoteRepository（API）  ← 将来
```

### 2.2 技術スタック

| カテゴリ | 技術 | バージョン目安 | 備考 |
|---|---|---|---|
| 言語 | Kotlin | 2.0系 | |
| UIフレームワーク | Jetpack Compose | BOM最新 | Material Design 3 |
| アーキテクチャ | MVVM + Repository | — | |
| DI | Hilt | 最新 | ViewModelへの注入 |
| 非同期 | Kotlin Coroutines + Flow | 最新 | |
| ローカルDB | Room | 最新 | SQLiteラッパー |
| ORM的操作 | Room DAO | — | Drizzle相当 |
| CSVパース | opencsv または kotlin-csv | 最新 | クライアントサイドで完結 |
| グラフ | Vico | 最新 | Compose対応・Androidグラフライブラリ |
| ナビゲーション | Navigation Compose | 最新 | |
| AIによる自動仕分け | Anthropic Claude API | 最新 | Retrofit経由・Phase 2以降 |
| HTTP通信 | Retrofit + OkHttp | 最新 | 将来のWeb連携用 |
| ビルド | Gradle (KTS) | 最新 | |
| 最小SDK | API 26（Android 8.0） | — | |
| ターゲットSDK | API 35（Android 15） | — | |

### 2.3 モジュール構成（パッケージ構成）

```
com.example.dashboard/
  ui/
    kakeibo/
      dashboard/
        DashboardScreen.kt
        DashboardViewModel.kt
      transactions/
        TransactionsScreen.kt
        TransactionsViewModel.kt
      import/
        ImportScreen.kt
        ImportViewModel.kt
      budget/
        BudgetScreen.kt
        BudgetViewModel.kt
      cards/
        CardsScreen.kt
        CardsViewModel.kt
      settings/
        SettingsScreen.kt        # 仕分けルール・カテゴリ管理
        SettingsViewModel.kt
    components/
      BottomNavBar.kt            # ボトムナビゲーション
      TopBar.kt
  data/
    local/
      AppDatabase.kt             # RoomDatabase
      dao/
        TransactionDao.kt
        CategoryDao.kt
        CreditCardDao.kt
        BudgetDao.kt
        CategoryRuleDao.kt
        ImportHistoryDao.kt
      entity/
        TransactionEntity.kt
        CategoryEntity.kt
        CreditCardEntity.kt
        BudgetEntity.kt
        CategoryRuleEntity.kt
        ImportHistoryEntity.kt
    repository/
      TransactionRepository.kt          # interface
      TransactionRepositoryImpl.kt      # Room実装
      CategoryRepository.kt
      BudgetRepository.kt
      CardRepository.kt
      RuleRepository.kt
    remote/                             # 将来のAPI連携用（Phase 3以降）
      ApiService.kt
      RemoteTransactionRepository.kt
  domain/
    model/                             # UIで使うドメインモデル（Entityとは分離）
      Transaction.kt
      Category.kt
      CreditCard.kt
      Budget.kt
      CategoryRule.kt
    usecase/
      ClassifyTransactionUseCase.kt    # 3段階自動仕分けロジック
      ImportCsvUseCase.kt
      GetDashboardSummaryUseCase.kt
  di/
    DatabaseModule.kt                  # Hiltモジュール
    RepositoryModule.kt
  util/
    CsvParser.kt
    TextNormalizer.kt                  # 全角/半角正規化
```

### 2.4 ナビゲーション

Web版のサイドバーに相当するナビゲーションは、**ボトムナビゲーションバー**で実装する。スマートフォンのUXに合わせた標準的なパターン。

| タブ | アイコン | 遷移先 |
|---|---|---|
| ホーム | 🏠 | ダッシュボード |
| 明細 | 📋 | 取引一覧 |
| 入力 | ➕ | CSVインポート / 手動入力 |
| 予算 | 💰 | 予算管理 |
| 設定 | ⚙️ | カード・カテゴリ・仕分けルール |

---

## 3. ユーザー要件

### 3.1 認証

**認証は不要。** 自分のスマートフォンにのみインストールするため、ログイン画面・パスワード・セッション管理は一切実装しない。アプリ起動後すぐにダッシュボードを表示する。

### 3.2 データ所有

すべてのデータはデバイスローカルのRoomデータベースに保存する。アプリをアンインストールするとデータが消える点を仕様として受け入れる。将来的なバックアップ・Web連携はPhase 3以降で検討。

---

## 4. 機能要件（家計簿モジュール）

Web版と機能仕様は共通。以下にAndroid固有の差異を記載する。

### 4.1 データ入力

#### 4.1.1 CSVインポート

| 優先度 | 要件 | 詳細 |
|---|---|---|
| 必須 | ファイルピッカーからCSV選択 | Androidのストレージアクセスフレームワーク（SAF）を使用 |
| 必須 | イオンカードCSV対応 | 公式フォーマット（利用日・利用先・金額等） |
| 必須 | 汎用CSV対応 | 列名を自動判別 |
| 必須 | 文字コード自動判別 | UTF-8 / Shift-JIS |
| 必須 | 重複チェック（ファイル単位） | SHA-256ハッシュで同一ファイルを検知・警告ダイアログ表示 |
| 必須 | 同一内容取引の扱い | 同一日・同名・同額でも両方取り込む（取りこぼしなし優先） |
| 推奨 | インポートプレビュー | 取り込み前に件数・仕分け結果をBottomSheetで確認 |

> **Web版との違い：** ドラッグ&ドロップ非対応。代わりにAndroidのファイルピッカー（`ActivityResultContracts.GetContent`）を使用する。

#### 4.1.2 手動入力

Web版と同仕様。日付ピッカーはMaterial3の`DatePicker`を使用。

### 4.2 カード管理

Web版と同仕様。

### 4.3 分析・可視化

| 機能 | 仕様 |
|---|---|
| ダッシュボードデフォルト期間 | 今月 |
| カテゴリ別グラフ | Vicoのドーナツグラフ + 棒グラフ |
| 月別推移グラフ | Vicoの棒グラフ（直近12ヶ月） |
| 明細検索・フィルター | 店名・カテゴリ・カード・期間（メモは検索対象外） |
| ソート | 日付・金額の昇降順 |

### 4.4 予算管理

Web版と同仕様。プログレスバーはLinearProgressIndicator（Material3）を使用。

### 4.5 自動仕分け機能

3段階パイプラインはWeb版と同仕様。実装場所が異なる。

| 段階 | Web版 | Android版 |
|---|---|---|
| ① 学習DB照合 | category_rulesテーブル（D1） | category_rulesテーブル（Room） |
| ② キーワードマッチング | サーバーサイドTS | `ClassifyTransactionUseCase.kt` |
| ③ AI推定（Claude API） | SvelteKit +server.ts | Retrofit経由（Phase 2以降） |

---

## 5. カテゴリ定義

Web版と同仕様（7カテゴリ + カスタム追加可）。

| カテゴリ | アイコン | キーワード例 |
|---|---|---|
| 食費 | 🛒 | スーパー、コンビニ、レストラン、カフェ 等 |
| 交通費 | 🚃 | 電車、バス、タクシー、Suica、JR 等 |
| 娯楽 | 🎮 | 映画、Netflix、Spotify、ゲーム 等 |
| ショッピング | 🛍 | Amazon、楽天、ユニクロ 等 |
| 医療・健康 | 💊 | 病院、薬局、クリニック 等 |
| 光熱費・通信 | 💡 | 電力、ガス、携帯、docomo 等 |
| その他 | 📦 | 上記に一致しないデフォルト |

---

## 6. データモデル（Room）

### 6.1 設計方針

- Web版のDBスキーマをRoomエンティティに対応させる（カラム名・型は合わせる）
- `user_id` カラムは将来のWeb連携・マルチアカウント対応のために残す（Phase 1 では固定値 `"local"`）
- 日付は Web版と同様に ISO 8601 文字列（`String`）で保存
- 金額は `Int`（円単位）
- 削除はすべて物理削除

### 6.2 エンティティ一覧

Web版テーブルとの対応：

| Roomエンティティ | Web版テーブル | 備考 |
|---|---|---|
| `TransactionEntity` | transactions | `user_id`は`"local"`固定 |
| `CreditCardEntity` | credit_cards | |
| `ImportHistoryEntity` | import_history | |
| `CategoryEntity` | categories | |
| `CategoryRuleEntity` | category_rules | |
| `BudgetEntity` | budgets | |

### 6.3 TransactionEntity

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

### 6.4 CategoryRuleEntity

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

### 6.5 ImportHistoryEntity

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

### 6.6 削除ポリシー

Web版と同仕様。

| 操作 | 実行場所 | 内部動作 |
|---|---|---|
| 取引1件削除 | 明細一覧のスワイプ or メニュー | 物理削除（確認ダイアログあり） |
| インポート履歴単位で削除 | インポート履歴画面 | import_idが一致する全取引を物理削除 → import_historyも削除 |

---

## 7. 画面構成

### 7.1 全体レイアウト

ボトムナビゲーションバーで5タブ切り替え。各タブ内はNavigation Composeでサブ画面に遷移。

### 7.2 画面一覧

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

---

## 8. 非機能要件

### 8.1 パフォーマンス

- アプリ起動からダッシュボード表示：2秒以内
- CSVインポート（1,000件）：3秒以内
- DBクエリ（明細一覧表示）：500ms以内
- グラフ描画：1秒以内

### 8.2 対応環境

| 項目 | 仕様 |
|---|---|
| 最小SDK | API 26（Android 8.0） |
| ターゲットSDK | API 35（Android 15） |
| 画面サイズ | スマートフォン縦持ちを基本（タブレット対応は将来） |
| 配信方法 | APKサイドロード（Google Play非公開） |

### 8.3 セキュリティ

- ログイン不要のためJWT等の認証機構は不要
- RoomDBはアプリのプライベートストレージに保存（他アプリからアクセス不可）
- Claude APIキーは `BuildConfig` または `local.properties` で管理（ハードコード禁止）
- CSVファイルはメモリ上でパース・永続保存しない

### 8.4 将来のWeb連携に向けた設計指針

- `TransactionRepository` をinterfaceとして定義し、`LocalTransactionRepositoryImpl` を注入
- 将来 `RemoteTransactionRepositoryImpl`（Retrofit）を追加し、Hiltの注入先を差し替えるだけで連携できる構成
- `user_id` カラムをPhase 1から持っておくことで、マルチユーザー対応時のマイグレーションコストを削減
- エンティティのカラム名・型をWeb版DBスキーマと合わせておき、同期時のマッピングを単純化する

---

## 9. 開発フェーズ計画

| フェーズ | 主要スコープ |
|---|---|
| Phase 1（MVP） | DB設計・Roomセットアップ・CSVインポート・手動入力・キーワード自動仕分け・カテゴリ別集計・月別推移・明細一覧 |
| Phase 2 | 学習DB仕分け・Claude API仕分け・未分類管理・複数カード・予算管理 |
| Phase 3 | Web版（Cloudflare D1）との同期API連携 |
| Phase 4 | タスク管理モジュール追加・ウィジェット対応 |

---

## 10. 未決定事項

| 事項 | ステータス | 期限目安 |
|---|---|---|
| データバックアップ方法（Googleドライブ等） | 未検討 | Phase 2以降 |
| Web連携時の競合解決ポリシー（どちらを正とするか） | 未検討 | Phase 3着手前 |
| Claude APIキーの配布方法（APKに含める vs 起動時入力） | 要検討 | Phase 2着手前 |
| タスクモジュールの詳細仕様 | 未着手 | Phase 2完了後 |

---

## 11. 用語集

| 用語 | 説明 |
|---|---|
| Jetpack Compose | AndroidのモダンなUIツールキット。Svelteのコンポーネントに相当する宣言的UIを書ける。 |
| Room | AndroidのSQLiteラッパーライブラリ。DrizzleのAndroid版に相当。エンティティ・DAO・Databaseの3要素で構成。 |
| DAO（Data Access Object） | Roomのクエリ定義クラス。`@Query`アノテーションでSQLを書くか、`@Insert`等の便利メソッドを使う。 |
| ViewModel | UIの状態（UiState）を管理するクラス。画面回転等でも状態が破棄されない。SvelteのStoreに相当。 |
| Hilt | AndroidのDIフレームワーク。ViewModelやRepositoryをコンストラクタインジェクションで注入できる。 |
| Repository | データソースの抽象化レイヤー。ローカルDB・リモートAPIのどちらからデータを取得するかをViewModelから隠蔽する。 |
| Coroutines + Flow | Kotlinの非同期処理ライブラリ。`Flow`でDBの変更をリアクティブに受け取り、UIを自動更新できる。SvelteのStoreの変化検知に相当。 |
| Navigation Compose | Jetpack Composeのナビゲーションライブラリ。SvelteKitのファイルベースルーティングに相当。 |
| SAF | Storage Access Framework。AndroidでユーザーがファイルピッカーからCSVを選択するための仕組み。 |
| Vico | Jetpack Compose対応のグラフライブラリ。Chart.jsのAndroid版に相当。 |
| サイドロード | Google Playを経由せず、APKファイルを直接インストールすること。 |
| BuildConfig | Gradleビルド時に生成される定数クラス。APIキー等の秘密情報をコードに直書きせず管理するために使う。 |