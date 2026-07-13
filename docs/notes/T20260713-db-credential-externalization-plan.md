# Task T20260713: 本番DB資格情報の環境変数外部化 — 準備ノート（提案のみ・未適用）

- 種別: 設計準備（application-prod.yml・サーバ設定は本ノートでは変更しない）
- 対象: `spring.datasource.username` / `spring.datasource.password`（および Flyway 側の同等設定）の直書き解消

---

## 1. 現状

### 1.1 直書き箇所

`src/main/resources/application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fundanalyzer?serverTimezone=Asia/Tokyo   # 9行目
    username: fundanalyzer                                                    # 10行目
    password: fundanalyzer                                                    # 11行目
    driverClassName: com.mysql.cj.jdbc.Driver
    ...
  flyway:
    url: jdbc:mysql://localhost:3306/fundanalyzer?serverTimezone=Asia/Tokyo   # 21行目
    schemas: fundanalyzer
    user: fundanalyzer                                                       # 23行目
    password: fundanalyzer                                                   # 24行目
```

`spring.datasource.*` と `spring.flyway.*` は接続先・資格情報が重複定義されている（Flyway は独自の
migration 接続を張るため、`spring.datasource.*` を自動では継承しない）。両方を同時に外部化対象とする。

`url` はホスト・DB名を含み `localhost:3306/fundanalyzer` で機密性は低いが、資格情報と対になっているため
本ノートでは `username`/`password` を主対象、`url` は任意（後述）とする。

### 1.2 本番での読み込み・env 供給経路の実態

- `application-prod.yml` は jar 内 (`src/main/resources/`) にパッケージされ、`--spring.profiles.active=prod`
  （`release/start.bat` 末尾の起動引数）で有効化される。`release/config/application-prod.yml` という
  外部差分ファイルは**リポジトリ・本番サーバいずれにも存在しない**（コード上の想定と異なる。本番はソース同梱の
  1ファイルのみで運用されている）。
- 本番サービスは **NSSM が `Application=C:\fundanalyzer\bin\start.bat` を起動する構成**（`iorid/ssh-log.md`
  116行目）。NSSM の `AppEnvironmentExtra` は使われていない。
- `release/start.bat` は起動時にカレントディレクトリの `env` ファイル（本番実体: `C:\fundanalyzer\bin\env`、
  `release/tool/release-for-jenkins.bat` の `set ENV=%FUNDANALYZER_DIR%\env` から特定）を1行ずつ読み、
  `set %%a=%%b` で **cmd.exe プロセスの環境変数として展開してから `java -jar` を起動**する。
  ```bat
  for /f "usebackq tokens=1,* delims==" %%a in ("env") do (
      set %%a=%%b
  )
  ```
  この for ループはキー名を一切限定していないため、`SECURITY_USER`/`SECURITY_PASSWORD`/`SLACK_WEBHOOK_*` と
  同じ仕組みで **`env` ファイルに行を追加するだけ**で任意の環境変数を java プロセスへ渡せる。
  → nssm の再設定（`AppEnvironmentExtra` 追加）は不要。既存の `release/env`（ローカルは `.gitignore` 済み・
  サーバ実体 `C:\fundanalyzer\bin\env`）にキーを追記するだけで供給経路が成立する。
- 対比: `iorid/ssh-log.md` 484行目・507行目の `orbit` は NSSM の `Application=java.exe -jar ...` 直起動構成
  のため `AppEnvironmentExtra` に `SPRING_DATASOURCE_URL`/`SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD`
  を設定している。fundanalyzer は起動シェル（start.bat）が env ファイルを仲介する構成のため、同じ目的でも
  **NSSM 設定ではなく `env` ファイル追記で足りる**という違いがある。
- 既存の外部化パターン（`application.yml` 151-152行目 / `application-prod.yml` 96-97行目）:
  ```yaml
  security:
    user: ${SECURITY_USER}          # prod: デフォルトなし → 未設定なら起動失敗
    password: ${SECURITY_PASSWORD}
  ```
  dev 側 (`application.yml`) はデフォルト値付き `${SECURITY_USER:admin}`、prod 側は
  デフォルトなし `${SECURITY_USER}` という書式で「prod は必須・未設定なら fail-fast」を実現している。
  `SECURITY.md` にも「prod: `release/env` に必須設定（未設定の場合は起動失敗）」と明記されている。

---

## 2. 提案する変更（差分・未適用）

### 2.1 application-prod.yml への提案差分

```diff
 spring:
   datasource:
     url: jdbc:mysql://localhost:3306/fundanalyzer?serverTimezone=Asia/Tokyo
-    username: fundanalyzer
-    password: fundanalyzer
+    username: ${DB_USERNAME}
+    password: ${DB_PASSWORD}
     driverClassName: com.mysql.cj.jdbc.Driver
     ...
   flyway:
     url: jdbc:mysql://localhost:3306/fundanalyzer?serverTimezone=Asia/Tokyo
     schemas: fundanalyzer
-    user: fundanalyzer
-    password: fundanalyzer
+    user: ${DB_USERNAME}
+    password: ${DB_PASSWORD}
     baseline-on-migrate: true
```

`spring.datasource.*` と `spring.flyway.*` は同一DBユーザーを指しているため `DB_USERNAME`/`DB_PASSWORD` を
共通キーとして1組だけ追加する（`SECURITY_USER`/`SECURITY_PASSWORD` と同じ命名規則: 用途_役割）。

### 2.2 必須env（デフォルトなし） vs 移行期デフォルト付きの論点

**必須env（デフォルトなし）を推奨する。** 理由:

- `SECURITY_USER`/`SECURITY_PASSWORD` と同じ「prod はデフォルトなし → 未設定なら起動失敗で検知」という
  既存の設計方針に揃えることで、設定漏れを実行時エラーではなく起動時エラーとして検知できる
  （Spring Boot は `${DB_USERNAME}` のようなプレースホルダが解決できないと `PlaceholderResolutionException` 等で
  起動時に fail する）。
- デフォルト付き（`${DB_USERNAME:fundanalyzer}`）にすると、`env` ファイルへの追記を忘れても**現行の
  ハードコード値がそのまま生き残り**、直書き解消という本来の目的が骨抜きになる。移行完了の確認が
  「起動できたかどうか」では判定できなくなり、`env` ファイルの中身を毎回目視する必要が生じる。
- 唯一のトレードオフはロールアウト順序の厳守が必須になる点（§4）。これは `SECURITY_USER` 導入時も
  同じ制約を負っており、運用上の前例がある。

移行期を設けるとすれば「猶予期間つきデフォルト値」ではなく、**Gate 1/2 レビューと同一デプロイ内で
`env` 追記→デプロイを完結させる**運用で対応し、恒久的なデフォルト値は残さない。

### 2.3 url の扱い（任意・優先度低）

`localhost:3306/fundanalyzer` はホスト名がループバックでDB名も非機密のため、`username`/`password` ほど
優先度は高くない。外部化するなら同様に `${DB_URL:jdbc:mysql://localhost:3306/fundanalyzer?serverTimezone=Asia/Tokyo}`
のようにデフォルト値付きで用意し、資格情報とは異なり「未設定でも動く」形にするのが妥当（後述リスク評価参照）。

---

## 3. サーバ側準備（本番Windowsサービスへのenv設定・具体手順）

fundanalyzer は NSSM が `start.bat` を起動し、`start.bat` が `env` ファイルを読んでプロセス環境変数化する
構成のため、**NSSM の `AppEnvironmentExtra` 設定は不要**。`env` ファイルへの追記のみで完結する。

1. `release/env.example` に以下を追記（テンプレート更新・コミット対象）:
   ```
   :: DB接続資格情報（本番では必須設定。未設定だと起動失敗する）
   DB_USERNAME=
   DB_PASSWORD=
   ```
2. サーバ実体 `C:\fundanalyzer\bin\env`（`.gitignore` 対象・非コミット）に実値を追記:
   ```
   DB_USERNAME=fundanalyzer
   DB_PASSWORD=<実際のパスワード>
   ```
   - 編集は `iorid/ssh-log.md` の教訓（UTF-8全文書き戻しでYAML破損した前例）に倣い、
     対象行のみの追記（末尾追記 `echo`、または `[IO.File]::AppendAllText` 相当）で行い、
     全文の読み書きは避ける。`env` ファイル自体はASCIIのみのため直接リスクは低いが、
     手順として既存ファイルの上書きではなく追記を徹底する。
   - 追記前に `C:\fundanalyzer\bin\env` をバックアップ（`env.bak_dbcred` 等）してからにする。
3. NSSM 設定変更・サービス定義変更は**不要**（`orbit` のような `AppEnvironmentExtra` 追加は、
   fundanalyzer が既に env ファイル経由の仕組みを持つため対象外）。
4. 反映確認は `Restart-Service fundanalyzer` 後、Actuator `health` の DB コンポーネントと
   アプリログ（起動時の Flyway/Hikari 初期化ログ）で疎通を確認する。

---

## 4. ロールアウト順序（厳守）とロールバック

### 4.1 順序

**「① サーバ env 設定 → ② コード変更（application-prod.yml の `${DB_USERNAME}`/`${DB_PASSWORD}` 化）デプロイ」
の順序を厳守する。**

理由: `${DB_USERNAME}` はデフォルトなしのプレースホルダのため、env未設定のままデプロイすると
Spring Boot が起動時にプレースホルダを解決できず**起動失敗**する（`SECURITY_USER` と同じ fail-fast 特性）。
逆順（コード先行）にすると、次回の `sc start` / `Restart-Service` の瞬間にサービスがクラッシュループする。

具体手順:

1. `C:\fundanalyzer\bin\env` に `DB_USERNAME`/`DB_PASSWORD` を追記（§3-2）。**この時点ではコード未変更なので
   現行 jar は yml のハードコード値を使い続け、無害に追記できる**。
2. env 追記後、単独で `Restart-Service fundanalyzer` を行い、現行jar（DB_USERNAME等を読まない版）が
   問題なく起動することを確認（env追記自体が悪影響を与えないことの確認）。
3. `application-prod.yml` を `${DB_USERNAME}`/`${DB_PASSWORD}` に変更したコードをレビュー・マージし、
   通常のリリースフロー（jar ビルド→配置→`sc stop`→jar入替→`sc start`）でデプロイ。
4. デプロイ後、Actuator health・ログでDB接続成功を確認。

### 4.2 ロールバック手順

- **コード側**: 直前バージョンの jar（`fundanalyzer.jar.bak` 等、既存デプロイ手順で退避される）に戻す
  = 従来通りハードコード値を使う版に戻るため、env の有無に関わらず即座に復旧する。
- **env側**: `DB_USERNAME`/`DB_PASSWORD` の値が誤っていた場合は `env` ファイルを修正して
  `Restart-Service fundanalyzer` のみで復旧可能（jar の再配置不要）。
- 万一 ①②の順序を誤り起動失敗した場合も、env追記自体は無害な変更なので、**まず env ファイルの中身を
  確認・修正**し、それでも失敗するなら旧jarへのロールバックで復旧する。

---

## 5. リスクと重要度評価

| 観点 | 評価 |
|---|---|
| 露出範囲 | `localhost:3306` 接続かつ LAN 内（Tailscale/nginx経由の外部公開はアプリ層のみで、MySQLポートは外部非公開と推定される）。DBサーバ自体への外部からの直接到達性は本ノートの調査対象外だが、少なくともソースリポジトリ（GitHub, `dazoyee/fundanalyzer`）に資格情報が平文でコミットされている点が最大のリスク |
| 悪用可能性 | リポジトリへのアクセス権を持つ者（開発者・CI）は資格情報を閲覧可能。DBへのネットワーク到達性が別途必要なため、単独では致命的ではないが「多層防御の一角が欠けている」状態 |
| 対応優先度 | **中**。認証情報のクレデンシャルスタッフィングやリポジトリ流出時の被害拡大を防ぐ観点で対応価値は高いが、`SECURITY_USER`（外部公開Basic認証）ほどの緊急性はない（DBはlocalhost/LAN限定でインターネット直接露出はしていない前提） |
| 実施コストとのバランス | 変更自体は小さく（yml 4行 + env 2行追加）、既存の `SECURITY_USER` パターンを踏襲するだけで実装できるためコストは低い。次回のGate 1/2サイクルで着手可能 |

---

## 6. 境界・未実施事項

- 本ノートでは `application-prod.yml` / サーバ設定 / DB接続 / デプロイのいずれも実施していない（提案のみ）。
- `AnalysisResult`/`IndicatorValue` 等の算出ロジック（#13 担当範囲）には触れていない。
- 実装フェーズでは Gate 1（計画承認）→ 実装 → Gate 2（レビュー承認）の通常サイクルに乗せる。
