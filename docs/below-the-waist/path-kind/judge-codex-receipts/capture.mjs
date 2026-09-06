// Judge-owned driver. Executes each freshly built client, without editing it.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';
import puppeteer from 'puppeteer';

const [root, out, buildOverride] = process.argv.slice(2);
if (!root || !out) throw new Error('Usage: node capture.mjs <worktree> <output>');
fs.mkdirSync(path.join(out, 'png'), {recursive: true});
const started = new Date().toISOString();
const t0 = performance.now();
const build = buildOverride || path.join(root, 'target/render-verifier/js/main.js');
const errors = [];
const args = ['--no-sandbox', '--enable-unsafe-webgpu', '--use-angle=swiftshader',
  '--enable-features=WebGPU,UnsafeWebGPU'];
const browser = await puppeteer.launch({executablePath: '/usr/bin/google-chrome', headless: 'chrome', args});
try {
  const page = await browser.newPage();
  page.on('pageerror', error => errors.push(String(error)));
  page.on('console', message => {
    if (message.type() === 'error' && !message.text().includes('404')) errors.push(message.text());
  });
  await page.setRequestInterception(true);
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.origin !== 'http://localhost') return request.abort();
    if (url.pathname === '/') return request.respond({status: 200, contentType: 'text/html',
      body: '<!doctype html><meta charset="utf-8"><script src="/js/main.js"></script>'});
    if (url.pathname === '/js/main.js') return request.respond({status: 200, contentType: 'text/javascript', body: fs.readFileSync(build)});
    const route = [['/js/', 'target/render-verifier/js'], ['/fonts/', 'resources/public/fonts'],
      ['/images/', 'test/app/fixtures/render_engine/images']].find(([prefix]) => url.pathname.startsWith(prefix));
    if (!route) return request.respond({status: 404, body: ''});
    const directory = path.resolve(root, route[1]);
    const file = path.resolve(directory, decodeURIComponent(url.pathname.slice(route[0].length)));
    if (!file.startsWith(directory + path.sep) || !fs.existsSync(file)) return request.abort();
    return request.respond({status: 200, contentType: file.endsWith('.js') ? 'text/javascript' :
      file.endsWith('.json') ? 'application/json' : 'application/octet-stream', body: fs.readFileSync(file)});
  });
  await page.goto('http://localhost', {waitUntil: 'load'});
  await page.waitForFunction(() => window.__renderVerifierDone === true, {timeout: 600000});
  const result = await page.evaluate(() => window.__renderVerifierResult);
  const extract = value => {
    if (Array.isArray(value)) return value.map(extract);
    if (!value || typeof value !== 'object') return value;
    return Object.fromEntries(Object.entries(value).map(([key, child]) => {
      if (key === 'png-data-url' && value.file) {
        const relative = 'png/' + path.basename(value.file);
        fs.writeFileSync(path.join(out, relative), Buffer.from(child.split(',')[1], 'base64'));
        return ['png', relative];
      }
      return [key, extract(child)];
    }));
  };
  const cleaned = extract(result);
  const metadata = {started, finished: new Date().toISOString(), elapsedSeconds: (performance.now()-t0)/1000,
    root, head: execFileSync('git', ['rev-parse', 'HEAD'], {cwd: root, encoding: 'utf8'}).trim(),
    buildSha256: crypto.createHash('sha256').update(fs.readFileSync(build)).digest('hex'),
    browser: await browser.version(), node: process.version, args, adapter: result.adapter,
    judge: {family: 'Codex', model: 'GPT-6 (system identity)', version: 'exact serving build unavailable',
      effort: 'not exposed by session metadata available within fence', tools: ['exec_command', 'Puppeteer', 'Chrome WebGPU SwiftShader']},
    errors};
  fs.writeFileSync(path.join(out, 'result.json'), JSON.stringify(cleaned, null, 2)+'\n');
  fs.writeFileSync(path.join(out, 'run.json'), JSON.stringify(metadata, null, 2)+'\n');
  console.log(JSON.stringify({metadata, keys: Object.keys(result), fatal: result.fatal,
    path: result['path-step']?.['pass?'], region: result['region3d-floor']?.['pass?']}, null, 2));
  if (result.fatal || errors.length) process.exitCode = 1;
} finally { await browser.close(); }
