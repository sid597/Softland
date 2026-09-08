// drive_view.cjs — drive the compiled view page in headless Chromium and record
// the checkpoints: the picture, the pointer's readout, typing, an edit in place.
//   node probes/text-tool-on-the-waist/view/drive_view.cjs [served-dir] [receipts-dir]
// The served dir holds index.html and main.js (see README).
const { chromium } = require('/opt/node22/lib/node_modules/playwright');
const http = require('http');
const fs = require('fs');
const path = require('path');

const root = process.argv[2] || 'target/view';
const outDir = process.argv[3] || 'probes/text-tool-on-the-waist/view/receipts';
fs.mkdirSync(outDir, { recursive: true });

const server = http.createServer((req, res) => {
  let p = req.url.split('?')[0];
  if (p === '/') p = '/index.html';
  const f = path.join(root, p);
  if (!fs.existsSync(f)) { res.writeHead(404); res.end(); return; }
  res.writeHead(200, { 'content-type': f.endsWith('.js') ? 'text/javascript' : 'text/html; charset=utf-8' });
  fs.createReadStream(f).pipe(res);
});

(async () => {
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;
  const browser = await chromium.launch({ headless: true, args: ['--enable-unsafe-webgpu', '--use-angle=swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 1180, height: 640 } });
  const log = [];
  page.on('console', (m) => log.push('[page] ' + m.text()));
  page.on('pageerror', (e) => log.push('[pageerror] ' + e.message));
  await page.goto(`http://127.0.0.1:${port}/`);
  await page.waitForFunction(() => window.softland && window.softland.ready, null, { timeout: 60000 });

  const metrics = () => page.evaluate(() => window.softland.metrics());
  const shot = (name) => page.screenshot({ path: path.join(outDir, name) });
  const receipts = {};

  receipts['1-initial'] = await metrics();
  await shot('01-initial.png');
  receipts['1b-bench'] = await page.evaluate(() => window.softland.bench());

  const box = await (await page.$('#painting')).boundingBox();
  // run-1 starts at local [4 4]; zoom 4; "t" is box [1 0 2 8] → local x 5..7, y 4..12 → texel (24, 32)
  await page.mouse.move(box.x + 24, box.y + 32);
  await page.waitForTimeout(150);
  receipts['2-pointer-on-t'] = await metrics();
  await shot('02-pointer-on-t.png');
  // run-2 (foreign, half size) starts at local [4 40]; "a" box [1 3 4 5] × 0.5 → x 4.5..6.5, y 41.5..44 → texel (22, 170)
  await page.mouse.move(box.x + 22, box.y + 170);
  await page.waitForTimeout(150);
  receipts['3-pointer-on-foreign-a'] = await metrics();
  await page.mouse.move(box.x + 400, box.y + 250);
  await page.waitForTimeout(150);
  receipts['4-pointer-on-nothing'] = await metrics();

  await page.keyboard.type(' and more keys', { delay: 15 });
  await page.waitForTimeout(150);
  receipts['5-typed'] = await metrics();
  await shot('03-typed.png');
  await page.keyboard.press('Backspace');
  await page.keyboard.press('Backspace');
  await page.waitForTimeout(100);
  receipts['6-backspace-twice'] = await metrics();
  await page.keyboard.press('Enter');
  await page.keyboard.type('a new line', { delay: 15 });
  await page.waitForTimeout(150);
  receipts['7-enter-and-line'] = await metrics();
  await shot('04-enter-line.png');

  // point at the tool and change one field through the page's own editor
  await page.click('#records span:text-is("layout@1")');
  const editor = await page.$('#editor');
  const shown = await editor.inputValue();
  receipts['8-editor-shows-layout'] = shown.includes('"layout@1"') && shown.includes(':width 100');
  await editor.fill(shown.replace(':width 100', ':width 60'));
  await page.click('#apply');
  await page.waitForTimeout(200);
  receipts['9-width-60'] = await metrics();
  receipts['9-status'] = await page.evaluate(() => document.getElementById('status').textContent);
  await shot('05-width-60.png');

  // the foreign rule: foreign-scale 0.5 → 1
  await page.click('#records span:text-is("layout@1")');
  const shown2 = await editor.inputValue();
  await editor.fill(shown2.replace(':foreign-scale 0.5', ':foreign-scale 1'));
  await page.click('#apply');
  await page.waitForTimeout(200);
  receipts['10-foreign-scale-1'] = await metrics();
  await shot('06-foreign-full-size.png');

  // the cursor record: move it back into the text
  const cursor = await page.evaluate(() => window.softland.record('cursor-1'));
  receipts['11-cursor-record'] = cursor;
  await page.evaluate((edn) => window.softland.put(edn), cursor.replace(/:offset \d+/, ':offset 3'));
  await page.waitForTimeout(150);
  receipts['12-cursor-offset-3'] = await metrics();
  await shot('07-cursor-at-3.png');

  receipts['13-store-log'] = await page.evaluate(() => window.softland.log());
  // the store survives a reload of the page in this browser
  await page.reload();
  await page.waitForFunction(() => window.softland && window.softland.ready, null, { timeout: 60000 });
  receipts['14-after-reload'] = await metrics();
  receipts['14-cursor-after-reload'] = await page.evaluate(() => window.softland.record('cursor-1'));
  await shot('08-after-reload.png');
  receipts['console'] = log;
  fs.writeFileSync(path.join(outDir, 'receipts.edn'),
    Object.entries(receipts).map(([k, v]) => `;; ${k}\n${typeof v === 'string' ? v : JSON.stringify(v)}\n`).join('\n'));
  console.log(Object.entries(receipts).map(([k, v]) => `${k}: ${typeof v === 'string' ? v.slice(0, 600) : JSON.stringify(v).slice(0, 600)}`).join('\n\n'));
  await browser.close();
  server.close();
})().catch((e) => { console.error(e); process.exit(1); });
