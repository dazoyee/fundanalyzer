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

document.addEventListener('DOMContentLoaded', () => {
  createIcons({ icons });
  Alpine.start();
  htmx.process(document.body);
});

// htmx の swap 後にも Lucide アイコンを再描画する
document.body.addEventListener('htmx:afterSwap', () => {
  createIcons({ icons });
});
