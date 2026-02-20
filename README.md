# SES AI ASSISTANT Core Library

## 1. 概要 (Overview)
本プロジェクトは、SES AI ASSISTANT システムのバックエンドで共通的に利用されるJavaライブラリです。
データベースエンティティ（RDB/DynamoDB）、ドメインロジック（Value Object）、および外部API（AWS, OpenAI, Gemini, Slack等）との通信基盤を提供します。

## 2. プロジェクト構造 (Project Structure)
- `documents`
    - 各種設計書・規約・AIエージェント利用ファイル
- `src/main/java/copel/sesproductpackage/core/`
    - `api/`: 外部サービス（AWS, GPT, Slack, LINE）との連携クラス。
    - `database/`: Entity/Lotパターンに基づいたDB操作クラス。
    - `unit/`: ドメイン知識をカプセル化した Value Object 群。
    - `util/`: 共通ユーティリティ（DB接続、文字列操作、プロパティ管理）。

## 3. 開発ガイドライン (Development Guidelines)
本プロジェクトのメンテナンスには Gemini CLI を使用し、以下の「掟」に従います。
詳細は `GEMINI.md` を参照してください。

### 3.1 AI Agent 開発手順 (AI Agent Development Procedure)
本プロジェクトでは、Gemini CLI を「自律型エンジニア」として扱い、以下の手順で開発を進めます。

1. **指示の作成 (`INSTRUCTION.md`)**:
   - `documents/INSTRUCTION.md` のテンプレートに従い、実装したい内容（機能追加、バグ修正、リサーチ等）を具体的に記載します。
   - 過去の指示は消さずに追記しても構いませんが、AIは常に「最新の未完了指示」を優先します。

2. **AI の起動**:
   - ターミナルで Gemini CLI を起動し、以下のプロンプトを入力して指示を読み込ませます。
   - 例: `documents/INSTRUCTION.md と documents/GEMINI.md を読み取って、自律的に開発を開始してください。`

3. **自律フェーズ (AI による自動実行)**:
   - **調査**: AI が既存の `unit` や `api` クラスを調査し、プロジェクトの設計思想に沿った最適な実装方針を提示します。
   - **計画**: AI が `TODO.md` を作成・更新し、具体的な作業ステップ（プラン）を管理します。
   - **実行・検証**: AI が `replace` 等でコードを修正し、`mvn test` 等で自律的に動作確認とリファクタリングを行います。

4. **完了と整理 (Auto-Archive)**:
   - タスクが成功（検証パス）すると、AI は実行した指示を `documents/ARCHIVE_INSTRUCTIONS.md` に自動的に移動します。
   - `INSTRUCTION.md` は再び次の指示を受け取れるようテンプレートの状態に初期化されます。

### 3.2 設計思想
- **Entity/Lot パターン**: 単一レコード（Entity）とリスト操作（Lot）の分離。
- **Domain Primitive**: プリミティブ型を避け、`unit` パッケージの型を積極的に利用。
- **抽象化レイヤー**: ビジネスロジックから外部APIの具体的実装を隠蔽。

## 4. ビルドとテスト (Build & Test)
Mavenを使用してビルドおよびテストを行います。
```bash
mvn clean install
mvn test
```

## 5. 関連ファイル
- `documents/GEMINI.md`: プロジェクト固有の設計ルールとAI運用規約。
- `documents/INSTRUCTION.md`: AIへの開発指示用ファイル（入力用）。
- `documents/ARCHIVE_INSTRUCTIONS.md`: 完了した指示の履歴。
- `TODO.md`: 現在進行中のタスクログ（AI管理）。
