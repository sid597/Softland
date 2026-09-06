// The old probe's --bench argument is not by itself custody of the embedded sphere/pickup functions.
// The temporary poisoned bench is owned by this diagnostic and removed in finally.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
import assert from 'node:assert/strict';
import {benchSource, defaultBench, declaration, loadBench, hash} from './bench.mjs';
const here = path.dirname(fileURLToPath(import.meta.url));
const arg = process.argv.indexOf('--bench');
const subject = arg >= 0 ? process.argv[arg + 1] : defaultBench;
const source = benchSource(subject), {B} = loadBench(source);
const route = path.resolve(here, '../bench-2/node-route');
const probe = path.join(route, 'probe-2-3.mjs');
const compare = [];
for (const name of ['sweep-and-sphere.js', 'pickup-replay.js']) {
  const script = fs.readFileSync(path.join(route, name), 'utf8');
  const indented = script.split('\n').map(l => '  ' + l).join('\n');
  for (const match of script.matchAll(/^function (\w+)\(/gm)) {
    if (match[1].startsWith('v3')) continue; // the scribe explicitly retained the bench's vector helpers
    const embedded = declaration(source, match[1]), adjacent = declaration(indented, match[1]);
    const normalize = s => s.replace(/\s+/g, ' ').trim();
    compare.push({function: match[1], equalIgnoringWhitespace: normalize(embedded) === normalize(adjacent),
      embeddedSha256: hash(embedded), adjacentSha256: hash(adjacent)});
  }
}
const dir = fs.mkdtempSync(path.join(here, '.custody-'));
try {
  const poisoned = source.replace('rgba: rgba, contributors: layers.map', 'rgba: [0, 1, 0, 1], contributors: layers.map');
  assert.notEqual(poisoned, source);
  const target = path.join(dir, 'poisoned-bench.html'); fs.writeFileSync(target, poisoned);
  const originalProbe = JSON.parse(execFileSync(process.execPath, [probe, '--bench', subject], {encoding: 'utf8'}));
  const poisonedProbe = JSON.parse(execFileSync(process.execPath, [probe, '--bench', target], {encoding: 'utf8'}));
  const direct = loadBench(poisoned).B;
  const actual = Array.from(direct.gLocate(2, direct.gArcCentre(.65), {order: ['kA', 'kB']}).composition.rgba);
  assert.deepEqual(actual, [0, 1, 0, 1]);
  const q = B.sphUnit(3*Math.PI/2, Math.PI/3), rr = B.reachRegion({seed: [Math.PI, Math.PI/3], radius: 150});
  const r = {schema: 'attack-4-custody/v1', benchSha256: hash(source),
    runnerSha256: hash(fs.readFileSync(fileURLToPath(import.meta.url))), comparison: compare,
    originalProbe: {checks: originalProbe.checks.length, failed: originalProbe.failed},
    poisonedProbe: {checks: poisonedProbe.checks.length, failed: poisonedProbe.failed, actualEmbeddedRGBA: actual},
    scale: {sphereRadiusMM: B.G_R, attack2Factor: 2, attack3Factor: 1,
      attack2ReachMM: rr.distance(q), dividedBy2: rr.distance(q)/2, areaDividedBy4: rr.area/4,
      attack3SurfaceMM: B.sphDist(B.sphUnit(0, Math.PI/3), B.sphUnit(.08, Math.PI/3)),
      radiusOfArcA: B.G_RECORDS.A.mark.radius, radiusOfTapB: B.G_RECORDS.B.mark.radius},
    conclusion: 'Current non-vector functions match the sidecars. The deliberate green-paint mutation passes the old probe because it overlays those functions. Add source equality or execute the embedded definitions directly.'};
  if (process.argv.includes('--write')) fs.writeFileSync(path.join(here, 'custody-receipts.json'), JSON.stringify(r, null, 2) + '\n');
  console.log(JSON.stringify({...r, comparison: {functions: compare.length, mismatches: compare.filter(c => !c.equalIgnoringWhitespace)}}, null, 2));
} finally { fs.rmSync(dir, {recursive: true}); }
