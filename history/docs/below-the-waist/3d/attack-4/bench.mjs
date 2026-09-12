// Read the shipped bench, never overlay its functions with the adjacent Node modules.
import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';

export const defaultBench = 'history/docs/below-the-waist/3d/bench-2/seam-bench.html';
// Recorded Git revisions predate the archive; local files use defaultBench.
const recordedBench = 'docs/below-the-waist/3d/bench-2/seam-bench.html';
export const hash = value => crypto.createHash('sha256').update(
  typeof value === 'string' || Buffer.isBuffer(value) ? value : JSON.stringify(value)).digest('hex');
export const copy = value => JSON.parse(JSON.stringify(value));
export function benchSource(subject = defaultBench) {
  return fs.existsSync(subject) ? fs.readFileSync(subject, 'utf8') :
    execFileSync('git', ['show', subject + ':' + recordedBench], {encoding: 'utf8'});
}
export function between(source, start, end) {
  const a = source.indexOf(start), b = source.indexOf(end, a + start.length);
  assert(a >= 0 && b > a, 'missing or reversed bench anchors: ' + start + ' / ' + end);
  return source.slice(a, b);
}
export function declaration(source, name) {
  const a = source.indexOf('  function ' + name + '(');
  assert(a >= 0, 'missing bench function: ' + name);
  const nl = source.indexOf('\n', a), first = source.slice(a, nl);
  // These named bench functions close either on their first line or at the same two-space indent.
  const b = /}\s*(\/\/.*)?$/.test(first) ? nl : source.indexOf('\n  }', nl) + 4;
  assert(b > a, 'missing bench function end: ' + name);
  const text = source.slice(a, b);
  new vm.Script(text); // fail at the extraction boundary, before any claimed receipt
  return text;
}
export function loadBench(source) {
  const names = ['v3dot', 'v3cross', 'v3len', 'v3norm', 'v3add', 'v3scale',
    'over', 'markWidth', 'markAt', 'markSegments', 'sweepMin', 'postSegmentObjective', 'classifyMarkSweep'];
  const pieces = names.map(n => declaration(source, n));
  pieces.push(between(source, '  var G_R =', '  // ───── the bench\'s own pieces around them:'));
  const B = vm.createContext({Math, TAU: 2 * Math.PI, console});
  vm.runInContext(pieces.join('\n'), B);
  return {B, custody: {benchSha256: hash(source), declarationsSha256: hash(pieces.join('\n')),
    functions: names, sphereAndPickup: 'embedded G_R through pickupConsume; no sidecar overrides'}};
}
export function shaders(source) {
  const C = vm.createContext({});
  vm.runInContext(between(source, '  var FS =', '  // ── the field F:') + '\n' +
    between(source, '  var FSF =', '  var prog, locs'), C);
  return {main: C.FS, field: C.FSF};
}
