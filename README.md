# SES AI Assistant Core Library

**SES AI ASSISTANT システム全体の共通基盤**を提供する Java ライブラリです。RDB エンティティ、Domain Primitive（Value Object）、外部サービス（AWS, LLM, Slack等）との統一的な通信基盤を提供します。

## 概要

**言語**: Java 17 | **ビルド**: Maven | **テスト**: ユニットテストのみ

他の 8 つのリポジトリ（Lambda 関数、Web API、Batch）が依存する中核ライブラリです。Entity/Lot パターン、Domain Primitive、API アダプタの設計思想を統一的に提供し、システム全体の整合性を保ちます。

## プロジェクト構造

| パッケージ | 内容 |
|----------|------|
| **`database/`** | Entity/Lot パターンに基づいた RDB 操作クラス（`SES_AI_T_*`, `SES_AI_*Lot`） |
| **`unit/`** | ドメイン知識をカプセル化した Value Object 群（Domain Primitive） |
| **`api/`** | 外部サービス（AWS, OpenAI, Gemini, Slack, LINE）との統一的なアダプタ層 |
| **`util/`** | 共通ユーティリティ（DB接続、文字列操作、プロパティ管理） |

## 設計原則

### Entity/Lot パターン

- 単一レコード = `SES_AI_...` クラス
- リスト操作 = `SES_AI_...Lot` クラス
- 新しいテーブル追加時は、必ず対になる Lot クラスを作成

### Domain Primitive（Value Object）

プリミティブ型（String, int等）ではなく、`unit` パッケージのドメイン型を優先：
- `Money`, `Currency`, `OriginalDateTime` など

### API 抽象化層

LLM や外部サービスは `api` パッケージのプロキシを経由。特定のベンダーに依存した実装を避け、入れ替え可能な設計を維持。

## ビルド・テスト

```bash
# コンパイル
mvn clean compile

# ユニットテスト実行（カバレッジ確認）
mvn test

# パッケージ（JAR 生成）
mvn clean package

# 静的解析
mvn rewrite:run checkstyle:check spotless:apply pmd:pmd pmd:cpd spotbugs:check
```

Core 本体はまだ親 pom（`nectar-parent`）に移行していないため、上の個別のコマンドを使います（Core の UT の作り直しの際に移行予定）。

## 各リポジトリ向けのビルドツール（`tools/`）

Lambda・Web API の各リポジトリが使う親 pom・UT 用部品・UT 仕様の検査プラグインを `tools/` に置いています。
各リポジトリでのコマンド（`mvn spotless:apply` と `mvn clean verify`）、検査の中身、導入手順は [`tools/README.md`](tools/README.md) を参照してください。

## ドキュメント

| ファイル | 用途 |
|---------|------|
| **AGENTS.md** | 開発ガイドライン・AI エージェント運用規約（必読） |
| **tools/README.md** | 各リポジトリのビルド・UT・静的チェックのコマンドと導入手順 |
| **docs/INSTRUCTION.md** | AI への開発指示（テンプレート） |
| **docs/ARCHIVE_INSTRUCTIONS.md** | 完了した指示の履歴 |

## 依存元リポジトリ（8個）

このライブラリは以下リポジトリの土台となっています：

1. **AwsLambdaEmailMssageReceiver** — メール受信
2. **AwsLambdaLineMssageReceiver** — LINE 受信
3. **AwsLambdaSesInfoRegister** — 情報登録パイプライン
4. **AwsLambdaSesInfoMatcher** — マッチングエンジン
5. **SesAiAssitantWebAppBackend** — REST API バックエンド
6. **SesAiAssitantWebAppBatch** — バッチ処理
7. **AwsModuleEnvironmentVariables** — 環境変数設定
8. **SesAiAssistantDesignSpecification** — 設計書・仕様

**注意**: このライブラリの変更は、上記 8 つのリポジトリに波及します。変更後は、依存元リポジトリの README を確認し、矛盾がないか検証してください。

## 関連リポジトリ

- `../SesAiAssistantDesignSpecification/` — マスター仕様・全体アーキテクチャ

---

© 2024 Copel Co., Ltd.
