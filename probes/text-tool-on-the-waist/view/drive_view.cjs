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

const assets = 'resources/public'; // the repository's font files, served beside the build
const server = http.createServer((req, res) => {
  let p = req.url.split('?')[0];
  if (p === '/') p = '/index.html';
  let f = path.join(root, p);
  if (!fs.existsSync(f)) f = path.join(assets, p);
  if (!fs.existsSync(f)) { res.writeHead(404); res.end(); return; }
  const type = f.endsWith('.js') ? 'text/javascript' : f.endsWith('.ttf') ? 'font/ttf' : 'text/html; charset=utf-8';
  res.writeHead(200, { 'content-type': type });
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
  const centre = async (run, i) => (await page.evaluate(([r, k]) => window.softland.glyphCentre(r, k), [run, i])).split(' ').map(Number);
  // the first glyph of run-1, "t", by its own placement
  const t = await centre('run-1', 0);
  await page.mouse.move(box.x + t[0], box.y + t[1]);
  await page.waitForTimeout(150);
  receipts['2-pointer-on-t'] = await metrics();
  await shot('02-pointer-on-t.png');
  // the first glyph of run-2 (foreign, half size), "a"
  const a = await centre('run-2', 0);
  await page.mouse.move(box.x + a[0], box.y + a[1]);
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

  // click places the cursor; a drag selects; the selection is the cursor's anchor
  const g4 = await centre('run-1', 4);
  await page.mouse.move(box.x + g4[0] - 3, box.y + g4[1]);
  await page.mouse.down();
  await page.waitForTimeout(120);
  const g7 = await centre('run-1', 7);
  await page.mouse.move(box.x + g7[0] + 3, box.y + g7[1], { steps: 4 });
  await page.waitForTimeout(200);
  await page.mouse.up();
  await page.waitForTimeout(200);
  receipts['19-selected'] = await page.evaluate(() => window.softland.cursor());
  receipts['19-selected-metrics'] = await metrics();
  await shot('12-selection.png');
  await page.keyboard.press('Control+c');
  await page.waitForTimeout(100);
  receipts['20-copied'] = await metrics();
  await page.keyboard.type('X');
  await page.waitForTimeout(200);
  receipts['21-typed-over-selection'] = await page.evaluate(() => window.softland.text('run-1'));
  receipts['21-cursor'] = await page.evaluate(() => window.softland.cursor());
  await shot('13-typed-over-selection.png');
  await page.keyboard.press('ArrowLeft');
  await page.keyboard.press('Shift+ArrowLeft');
  await page.keyboard.press('Shift+ArrowLeft');
  await page.waitForTimeout(200);
  receipts['22-shift-arrows'] = await page.evaluate(() => window.softland.cursor());
  await page.keyboard.press('Backspace');
  await page.waitForTimeout(200);
  receipts['23-backspaced-selection'] = await page.evaluate(() => window.softland.text('run-1'));
  receipts['23-last-keys'] = await page.evaluate(() => window.softland.record('run-1').slice(-260));

  // stand in the agent's view: Sid's runs only, its own cursor, zoom 3, from view-1
  await page.selectOption('#views', 'view-2');
  await page.waitForTimeout(300);
  receipts['15-view-2'] = await metrics();
  await shot('09-view-2.png');
  const t2 = await centre('run-1', 0);
  await page.mouse.move(box.x + t2[0], box.y + t2[1]);
  await page.waitForTimeout(150);
  receipts['16-view-2-pointer'] = await metrics();
  await page.mouse.move(box.x + 400, box.y + 250);
  await page.keyboard.type(' by the agent', { delay: 15 });
  await page.waitForTimeout(200);
  receipts['17-view-2-typed'] = await metrics();
  receipts['17-run-1-last-keys'] = await page.evaluate(() => window.softland.record('run-1').slice(-220));
  await shot('10-view-2-typed.png');
  await page.selectOption('#views', 'view-1');
  await page.waitForTimeout(300);
  receipts['18-back-in-view-1'] = await metrics();
  await shot('11-back-in-view-1.png');

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
