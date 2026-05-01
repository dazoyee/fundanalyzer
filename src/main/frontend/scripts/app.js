// 画面刷新タスクの共通クライアントエントリ
// htmx / Alpine.js / Lucide / Litepicker / Chart.js を 1 ファイルにバンドルする

import htmx from 'htmx.org';
import Alpine from 'alpinejs';
import { createIcons, icons } from 'lucide';
import Litepicker from 'litepicker';
import Chart from 'chart.js/auto';

// htmx 2.x の ESM bundle はサイドエフェクトで window 登録しないため明示する
window.htmx = htmx;
window.Alpine = Alpine;
window.Litepicker = Litepicker;
window.Chart = Chart;

// htmx 補正: hx-get などの leading slash 相対パスに Spring Boot の context-path を自動付加する。
// layout-v2 が <body data-context-path="@{/}"> を出力する前提（例: "/fundanalyzer/"）。
// 同一オリジン内の絶対パスにのみ補正を適用し、外部 URL や既に補正済の URL は触らない。
document.body.addEventListener('htmx:configRequest', (event) => {
  const raw = document.body.dataset.contextPath;
  if (!raw) return;
  // root context ("/") の場合は補正不要。それ以外は trailing slash を除去
  const contextPath = (raw.endsWith('/') && raw.length > 1) ? raw.slice(0, -1) : raw;
  if (!contextPath || contextPath === '/') return;
  const path = event.detail.path;
  if (typeof path !== 'string') return;
  // 外部 URL（http://・https://・//）は対象外
  if (/^https?:\/\//.test(path) || path.startsWith('//')) return;
  // すでに context-path で始まっていれば二重補正しない
  if (path === contextPath || path.startsWith(`${contextPath}/`)) return;
  // leading slash の相対パスのみ補正する（相対パスや query-only は対象外）
  if (!path.startsWith('/')) return;
  event.detail.path = `${contextPath}${path}`;
});

document.addEventListener('DOMContentLoaded', () => {
  createIcons({ icons });
  Alpine.start();
  htmx.process(document.body);
});

// htmx の swap 後にも Lucide アイコンを再描画する
document.body.addEventListener('htmx:afterSwap', () => {
  createIcons({ icons });
});
