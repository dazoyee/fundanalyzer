# T20260502 — /v3/edinet-list 機能復元タスク（優先度 5）

## 課題（1 行）

画面刷新 Phase 5（commit efcaaf62）で `/v3/edinet-list` を移植した際、旧 `/edinet` にあった **「会社情報更新」ボタン（GET `/v1/company`）** と **「処理状況更新」ボタン（GET `/v1/update/edinet/view`）**、および **「会社リスト更新日」表示** が消失している。`edinet-list-v2.html` を旧版相当に拡張する。

## 関連タスク

- 親: Phase 5（commit efcaaf62）
- 並走優先度: 1=edinet-detail / 2=corporate / 3=index / 4=valuation / **5=本タスク**

---

## ステップ 1：把握・整理

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | ① 「会社情報更新」ボタン（GET `/v1/company`）／② 「処理状況更新」ボタン（GET `/v1/update/edinet/view`）／③ 「会社リスト更新日：${companyUpdated}」表示 |
| **後回し** | DataTables export 機能 |
| **対象外** | バックエンド変更／カラム数変更（既に 9 列で旧版と同等） |

### バックエンド endpoint

- `GET /v1/company` — 会社マスタ更新（CSV 取り込み）
- `GET /v1/update/edinet/view` — EDINET 処理状況更新

### 現状の v3 テンプレート確認

`edinet-list-v2.html` 12-14 行目に既に `companyUpdated` 表示が残っているが、視認性の悪い場所。フォーム 2 種は完全消失。

---

## ステップ 3：影響設計（★Gate 1）

### 3.2 影響範囲分析

| 層 | 影響 |
|---|---|
| 参照層 | `edinet-list-v2.html`（50 → 約 80 行） |
| 状態層 | 該当なし |
| データ層 | 該当なし |

### Gate 1：人間の承認

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02

---

## ステップ 4：テスト設計

### テストケース

| # | ケース | 期待結果 |
|---|---|---|
| 1 | `/v3/edinet-list` 上部に 2 アクションボタン表示 | 会社情報更新 + 処理状況更新 |
| 2 | 「会社情報更新」クリック | GET `/v1/company` 実行 |
| 3 | 「処理状況更新」クリック | GET `/v1/update/edinet/view` 実行 |
| 4 | 「会社リスト更新日」が判別可能な位置に表示 | callout / inline で `${companyUpdated}` 表示 |

### Gate 2

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02

---

## ステップ 5：実行サイクル

1. edinet-list-v2.html 上部に 2 ボタンと更新日 callout を追加

---

## ステップ 6：多軸検証（★Gate 3）

- [ ] **承認者**: iori-oiso
- [ ] **承認日**: ____

---

## §6 検証結果（実装後に追記）
