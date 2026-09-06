// The loader the probes share (the fold of attack 4, 2026-09-06): the bench's pure declarations evaluated in Node,
// with custody of the embedded source. Given a bench that embeds the sphere, pickup and executor block, that block is
// executed as embedded, never overlaid by the sidecar files, and every function in it is compared with its sidecar
// text (attack 4 §2: the earlier probe loaded the sidecars over the bench's functions, so a bench whose embedded
// gLocate returned opaque green still passed 55/55). Given a bench from before the block existed (the default of
// probe-2-3.mjs, commit 2737f41), the sidecars are loaded, which is how the definers' numbers were checked before
// the bench took the functions.
import fs from 'node:fs';
import vm from 'node:vm';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

export const here = path.dirname(fileURLToPath(import.meta.url));
export const defaultBench = 'docs/below-the-waist/3d/bench-2/seam-bench.html';
export const sidecars = ['sweep-and-sphere.js', 'pickup-replay.js', 'executor.js', 'records.js'];
export const BLOCK_START = '  var G_R =', BLOCK_END = '  // ───── the bench\'s own pieces around them';
export const sha256 = value => crypto.createHash('sha256').update(typeof value === 'string' || Buffer.isBuffer(value) ? value : JSON.stringify(value)).digest('hex');

export function benchSource(subject) {
  if (!subject) subject = defaultBench;
  return fs.existsSync(subject) ? fs.readFileSync(subject, 'utf8') : execFileSync('git', ['show', subject + ':' + defaultBench], {encoding: 'utf8'});
}
// one named top-level bench function, at the bench's two-space indent: from its declaration to the next one
export function declaration(source, name, optional) {
  const a = source.indexOf('  function ' + name + '(');
  if (a < 0 && optional) return '';
  assert(a >= 0, 'missing bench function: ' + name);
  const b = source.indexOf('\n  function ', a + 1);
  assert(b > a, 'missing bench function end: ' + name);
  return source.slice(a, b);
}
export function embeddedBlock(source) {
  const a = source.indexOf(BLOCK_START), b = source.indexOf(BLOCK_END);
  return a >= 0 && b > a ? source.slice(a, b) : null;
}
const normalize = s => s.replace(/^\/\/.*$/gm, '').replace(/\s+/g, ' ').trim();
// A sidecar's declaration units: each top-level `var` or `function` with the lines up to the next one, the
// column-0 comment lines dropped (the bench carries them at its own indent, or not at all).
export function unitsOf(script) {
  const out = []; let cur = [], name = null;
  for (const line of script.split('\n')) {
    const m = line.match(/^(?:var|function) (\w+)/);
    if (m && name) { out.push({name, text: cur.join('\n')}); cur = []; }
    if (m) name = m[1];
    if (name) cur.push(line);
  }
  if (name) out.push({name, text: cur.join('\n')});
  return out;
}
// The custody comparison: every declaration unit of every sidecar must appear in the bench, whitespace ignored, so
// a bench whose embedded gLocate (or X_OPS, or G_RECORDS) diverges from the sidecar fails here before any number
// is trusted. The bench keeps its own six v3 helpers (declared once, above the block, v3norm with `|| 1`): skipped.
export function custody(source) {
  const block = embeddedBlock(source);
  if (!block) return {embedded: false, compared: 0, mismatches: []};
  const N = normalize(source), mismatches = []; let compared = 0;
  for (const file of sidecars) for (const u of unitsOf(fs.readFileSync(path.join(here, file), 'utf8'))) {
    if (/^v3/.test(u.name)) continue;
    compared++;
    if (!N.includes(normalize(u.text))) mismatches.push(file + ': ' + u.name);
  }
  return {embedded: true, compared, mismatches};
}
// The bench's own functions the sidecars lean on (in scope when the bench runs; extracted here by name).
export const OWN = ['sweepMin', 'postSegmentObjective', 'classifyMarkSweep', 'over', 'markWidth', 'markAt', 'markSegments', 'capsuleSD', 'classifyMark', 'classifyMarkCapsule', 'postOf', 'postPeriod', 'postWorld', 'postChart', 'postPreimages', 'hitPost', 'markKnots', 'markDomain', 'evaluateP', 'composeAt',
  'fieldPoly', 'polyEval', 'polyDeriv', 'isolateRealRoots', 'rayBoxT', 'fieldGrad', 'fieldValue', 'hitField'];
// loadBench(subject, {sidecars: true}) forces the sidecar route over the bench's own functions: the composer's route
// before the bench takes a change (the numbers checked first, then the bench patched, then --bench checks the embed).
export function loadBench(subject, opts = {}) {
  const source = benchSource(subject), block = opts.sidecars ? null : embeddedBlock(source);
  const own = OWN.map(n => declaration(source, n, ['classifyMarkCapsule', 'sweepMin', 'postSegmentObjective', 'classifyMarkSweep'].includes(n))).join('\n');
  const v3 = ['v3dot', 'v3cross', 'v3len', 'v3norm', 'v3add', 'v3scale', 'v3sub'].map(n => declaration(source, n)).join('\n');
  const B = vm.createContext({Math, TAU: 2 * Math.PI, console, Buffer, performance});
  vm.runInContext(v3 + '\n' + own, B);
  let route;
  if (block) { vm.runInContext(block, B); route = 'embedded (executed from the bench\'s own bytes)'; }
  else {
    // a bench from before the block: the pure sidecars (sweepMin and the post's sweep sit in sweep-and-sphere.js too)
    for (const file of sidecars) vm.runInContext(fs.readFileSync(path.join(here, file), 'utf8'), B);
    route = opts.sidecars ? 'sidecars over the bench\'s own functions (forced)' : 'sidecars (the bench does not embed the block)';
  }
  return {B, source, benchSha256: sha256(source), route, custody: custody(source)};
}
