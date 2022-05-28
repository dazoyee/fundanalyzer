const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({width: 1920, height: 1080});
  await page.goto('http://localhost:8890/fundanalyzer/v2/index');
  await page.screenshot({path: 'fundanalyzer_v2_index.png'});
  await page.screenshot({path: 'fundanalyzer_v2_index-full.png', fullPage: true});

  await browser.close();
})();
