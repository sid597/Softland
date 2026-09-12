// The composer's dump extractor (the fold of attack 4, 2026-09-06): given a --dump-dom file, prints the plain text
// of #addr, #diff, #derived, #run, #glstat (tags stripped, entities decoded), one JSON object per invocation.
// Usage: node dump.mjs <dump-dom-file.html>
import fs from 'node:fs';

const file = process.argv[2];
if (!file) { console.error('usage: node dump.mjs <dump-dom-file.html>'); process.exit(1); }
const html = fs.readFileSync(file, 'utf8');

function decode(s) {
  return s.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&nbsp;/g, ' ');
}
function stripTags(s) { return decode(s.replace(/<[^>]+>/g, '')); }

function extract(id) {
  const m = html.match(new RegExp('<([a-zA-Z0-9]+)[^>]*\\bid="' + id + '"[^>]*>'));
  if (!m) return null;
  const tag = m[1], start = m.index + m[0].length;
  const closeIdx = html.indexOf('</' + tag + '>', start);
  if (closeIdx < 0) return null;
  return stripTags(html.slice(start, closeIdx)).trim();
}

const out = {};
for (const id of ['addr', 'diff', 'derived', 'run', 'glstat']) out[id] = extract(id);
console.log(JSON.stringify(out, null, 1));
