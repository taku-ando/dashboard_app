# CLAUDE.md — パーソナルダッシュボード Android版 開発ガイド

このファイルはClaude Codeが自動的に読み込む設計サマリー。
詳細は `docs/` 配下の参照ファイルを必要に応じて読むこと。

---

## プロジェクト概要

自分のAndroidスマートフォン専用の家計簿アプリ。
**ログイン不要・Google Play非公開・APKサイドロード**で運用。
Phase 1 は家計簿モジュールのみ。データはローカルのRoomDBに保存。

---

## 技術スタック

| 役割 | 技術 | バージョン |
|---|---|---|
| 言語 | Kotlin | 2.0系 |
| UI | Jetpack Compose + Material3 | BOM 2024.12.01 |
| アーキテクチャ | MVVM + Repository パターン | — |
| DI | Hilt | 2.52 |
| 非同期 | Kotlin Coroutines + Flow | — |
| ローカルDB | Room | 2.6.1 |
| グラフ | Vico | 2.0.0 |
| ナビゲーション | Navigation Compose | 2.8.5 |
| CSVパース | kotlin-csv または opencsv | — |
| HTTP通信 | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| AI仕分け | Claude API（Retrofit経由） | Phase 2以降 |
| ビルド | Gradle KTS | — |
| 最小SDK | API 26（Android 8.0） | — |
| ターゲットSDK | API 35（Android 15） | — |

---

## 重要な実装方針

### 認証
**一切不要。** 起動後すぐに `DashboardScreen` を表示する。

### CSVインポート
- ファイル選択は `ActivityResultContracts.GetContent("text/*")` を使用
- `fileHash`（SHA-256）で重複ファイルを検知 → `AlertDialog` で警告
- 同一日・同名・同額の取引が複数あっても**両方取り込む**（取りこぼしなし優先）
- 文字コード自動判別（UTF-8 / Shift-JIS）

### 削除ポリシー
- 取引1件：スワイプまたはメニューから物理削除（`AlertDialog` で確認）
- インポート履歴単位：該当 `importId` の全取引を物理削除 → `import_history` も削除

### APIキー管理
```
# local.properties（gitignore対象）
ANTHROPIC_API_KEY=sk-ant-xxxxx

# build.gradle.kts
buildConfigField("String", "ANTHROPIC_API_KEY",
    "\"${localProperties["ANTHROPIC_API_KEY"]}\"")
```

### DBスキーマ設計方針
- カラム名・型はWeb版（Cloudflare D1）スキーマと合わせる
- `userId` は Phase 1 では常に `"local"` 固定
- 日付は ISO 8601 文字列、金額は Int（円単位）
- 削除はすべて物理削除

---

## 開発フェーズ

| フェーズ | スコープ |
|---|---|
| **Phase 1（MVP）** | Roomセットアップ・CSVインポート・手動入力・キーワード自動仕分け・カテゴリ別集計・月別推移・明細一覧 |
| **Phase 2** | 学習DB仕分け・Claude API仕分け・未分類管理・複数カード・予算管理 |
| **Phase 3** | Web版（Cloudflare D1）との同期API連携 |
| **Phase 4** | タスク管理モジュール・ホームウィジェット |

---

## ビルド・インストール方法

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 参照ドキュメント

| ファイル | 内容 |
|---|---|
| `todo.md` | **実装TODOリスト（作業開始時に必ず確認）** |
| `docs/package_structure.md` | パッケージ構成・画面ルート・ナビゲーション詳細 |
| `docs/db_schema.md` | Room Entityコード・DBスキーマ・デフォルトカテゴリ・自動仕分けパイプライン |
| `claude_requirements.md` | Android版要件定義書（詳細） |
| `docs/api_design_v2.md` | Web版API設計書（Phase 3連携時に参照） |
