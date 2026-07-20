# Contributing Guide

開発・ビルド・コミットに関する規約をまとめます。
詳細な手順は [`docs/guideline/`](docs/guideline/README.md) を一次情報源として参照してください。

---

## ブランチ戦略

[Git Flow](https://nvie.com/posts/a-successful-git-branching-model/) を採用しています。

| ブランチ | 役割 |
|---|---|
| `main` | 常時リリース可能。直接 push 禁止 |
| `develop` | 開発の統合ブランチ。作業ブランチのベース |
| `feature/*` `fix/*` 等 | 短命の作業ブランチ。`develop` から派生し `develop` にマージ |
| `release/vX.Y.Z` | リリース準備ブランチ。`develop` から作成し `main` にマージ |

### ブランチ命名規約

```
<種別>/<簡潔な内容>
```

| 種別 | 用途 |
|---|---|
| `feature` | 新機能追加 |
| `fix` | バグ修正 |
| `refactor` | 挙動を変えないリファクタリング |
| `docs` | ドキュメントのみの変更 |
| `test` | テスト追加・修正のみ |
| `chore` | ビルド・依存更新など |

---

## コミット規約

[Conventional Commits](https://www.conventionalcommits.org/ja/v1.0.0/) に従います。

```
<type>: <要約（50文字以内）>

<本文：なぜ変更したかを書く>

<フッタ：関連タスク番号・Co-Authored-By 等>
```

- 本文には **「なぜ」** を書く（「何を」は diff で読める）
- 要約の末尾にピリオドを付けない
- 1 タスク = 1 コミット（主ブランチへは Squash Merge で取り込む）

### 禁止事項

- `main` への直接 push・強制 push
- `--no-verify` によるフックのスキップ
- シークレット（API キー・パスワード）のコミット
- 公開済みコミットの `--amend`

---

## 開発フロー

タスクは以下の 6 ステップで進めます。詳細は [`docs/guideline/workflow.md`](docs/guideline/workflow.md) を参照。

1. **把握・整理** — 関連コード・ドキュメントを確認し、スコープを確定する
2. **プロトタイピング** — API・ER 図・シーケンス図などで外から見える形を作る
3. **影響設計** *(Gate 1: 人間承認必須)* — 影響範囲分析・設計ドキュメント更新
4. **テスト設計** *(Gate 2)* — テスト戦略選定・テストケース作成
5. **実装** — テストファーストで実装する
6. **多軸検証** *(Gate 3: 人間承認必須)* — CI 通過・コードレビュー・動作確認

Gate 1 / Gate 3 は人間レビュアの承認が必須です（スキップ不可）。

---

## リリースフロー

### 自動リリースパイプライン（推奨）

`.github/workflows/pipeline.yml` の `Pipeline` ワークフローが、リリースブランチ作成から
本番デプロイ・`develop` へのバックマージまでを自動実行する。トリガーは `workflow_dispatch` で、
`version` 入力を空にすると CI（テストのみ）、バージョンを指定するとリリース＋本番デプロイが走る。

```bash
# CIのみ実行（テスト）
gh workflow run pipeline.yml --repo dazoyee/fundanalyzer

# リリース＋本番デプロイを実行（例: 2.5.11）
gh workflow run pipeline.yml -f version=2.5.11 --repo dazoyee/fundanalyzer
```

リリースジョブ（`release`）は本番ホスト上の self-hosted runner（ラベル: `fundanalyzer`）で動作し、
以下を順に行う。

1. `origin/develop` から `release/vX.Y.Z` を作成し、SNAPSHOT を外してリリースコミット
2. `main` へ `--no-ff` マージし、タグ `vX.Y.Z` を付けて `origin` へ push
3. `mvnw.cmd clean package -DskipTests` でビルド
4. Windows サービス（`fundanalyzer`）を停止し、現行 JAR をバックアップした上で新 JAR に差し替えて起動
5. Actuator ヘルスチェック（`http://localhost:8990/actuator/health`）に成功するまでリトライし、
   失敗時はバックアップ JAR へ自動ロールバックしてジョブを失敗させる
6. ヘルスチェック成功時のみ、リリースブランチを `develop` へバックマージし、次期 `SNAPSHOT` バージョンを
   コミットして `origin/develop` へ push する

このため、手元での `git checkout`/`merge`/`tag`/SSH デプロイ操作は本来不要であり、
下記「手動リリース手順」は自動パイプラインが使えない場合の参考手順として残す。

### 手動リリース手順（参考）

Git Flow 標準の手順でリリースする場合の操作は以下の通り。

#### 1. リリースブランチを作成

```bash
git checkout develop
git checkout -b release/vX.Y.Z
```

#### 2. pom.xml のバージョンを更新（SNAPSHOT を外す）

```bash
./mvnw versions:set -DnewVersion=X.Y.Z
./mvnw versions:commit
```

#### 3. リリースコミット

```bash
git commit -am "release: vX.Y.Z"
```

#### 4. main にマージ＋タグ付け

```bash
git checkout main
git merge --no-ff release/vX.Y.Z
git tag vX.Y.Z
git push origin main --tags
```

#### 5. develop にバックマージ

```bash
git checkout develop
git merge --no-ff release/vX.Y.Z
```

#### 6. 次バージョンの SNAPSHOT を develop にコミット

```bash
./mvnw versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT
./mvnw versions:commit
git commit -am "chore: prepare next development version X.Y.(Z+1)-SNAPSHOT"
git push origin develop
```

#### 7. リリースブランチを削除

```bash
git branch -d release/vX.Y.Z
```

---

## ビルド・テスト

Maven Wrapper を使用します（`mvn` コマンドは使用しない）。

```bash
# クリーンビルド
./mvnw clean package

# テスト実行（Playwright 除外）
./mvnw test -DexcludedGroups=playwright

# 静的解析一式（CI と同等）
./mvnw test surefire-report:report pmd:pmd pmd:cpd jacoco:report spotbugs:spotbugs
./mvnw checkstyle:check
```

---

## コーディング規約

- Java 17 / Spring Boot 3.1.0
- Lombok 使用中（既存コードに準拠）
- Checkstyle: `sun_checks.xml`（`./mvnw checkstyle:check` で確認）
- パッケージ構成: 機能別（`user/`, `order/` 等）。タイプ別（`controller/`, `service/`）は禁止
- クリーンアーキテクチャの依存方向を崩さない（`web` → `usecase` → `interactor` → `service/dao`）
- 新しい処理を追加する場合は `XxxUseCase`（interface）と `XxxInteractor`（impl）の対で追加

詳細は [`CLAUDE.md`](CLAUDE.md) のコーディング規約を参照してください。
