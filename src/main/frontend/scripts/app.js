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

// 用語ツールチップ (fragments/tooltip.html 対応)。
// open boolean のみを持つ最小コンポーネント。hover / focus / click で開閉する。
Alpine.data('tooltip', () => ({
  open: false,
}));

document.addEventListener('DOMContentLoaded', () => {
  createIcons({ icons });
  Alpine.start();
  htmx.process(document.body);
});

// htmx の swap 後にも Lucide アイコンを再描画する
document.body.addEventListener('htmx:afterSwap', () => {
  createIcons({ icons });
});

// htmx:load: outerHTML swap で挿入された新要素に対して発生する。
// iOS Safari では innerHTML 経由の <script> が実行されないため、
// チャートデータを data-* 属性に埋め込み、ここで描画する。
document.body.addEventListener('htmx:load', (evt) => {
  const canvas = evt.detail.elt.querySelector
    ? evt.detail.elt.querySelector('canvas[data-summary-chart]')
    : null;
  if (!canvas) return;
  const labels = JSON.parse(canvas.dataset.labels || '[]');
  const cvPoints = JSON.parse(canvas.dataset.cv || '[]');
  const stPoints = JSON.parse(canvas.dataset.st || '[]');
  if (!labels.length) return;
  // x-show が display:none を外した直後はブラウザのレイアウトが未確定で
  // canvas の clientHeight が 0 になる。double rAF でレイアウト確定後に初期化。
  requestAnimationFrame(() => { requestAnimationFrame(() => {
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels,
      datasets: [
        { label: '企業価値', data: cvPoints, borderColor: 'rgb(16,185,129)', backgroundColor: 'rgba(16,185,129,0.1)', spanGaps: true, tension: 0.1, pointRadius: 3, borderWidth: 2 },
        { label: '株価', data: stPoints, borderColor: 'rgb(100,116,139)', backgroundColor: 'transparent', spanGaps: true, tension: 0.1, pointRadius: 3, borderWidth: 1.5 }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: true, position: 'top', labels: { boxWidth: 12, font: { size: 11 } } } },
      scales: { x: { ticks: { maxTicksLimit: 6, font: { size: 10 } } } }
    }
    });
  }); }); // close double rAF
});
