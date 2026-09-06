// Attack 4 reference construction. Node executes the shipped G bindings/composition and arithmetic.
// This file adds the surface-read adapter, transaction/continuation and spherical painting operations.
// It does NOT claim that the bench already has these operations wired into its record interpreter.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
import assert from 'node:assert/strict';
import {defaultBench, benchSource, loadBench, hash, copy} from './bench.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const arg = name => process.argv.includes(name) ? process.argv[process.argv.indexOf(name) + 1] : null;
const subject = arg('--bench') || defaultBench;
const source = benchSource(subject), {B, custody} = loadBench(source);
const records = JSON.parse(fs.readFileSync(path.join(here, 'records.json'), 'utf8'));
assert.equal(B.G_R, records.subject.radiusMM, 'this fixture is the bench sphere at 200 mm');

function encode(a) {
  const b = Buffer.alloc(a.length * 4);
  a.forEach((v, i) => b.writeFloatLE(v, i * 4));
  return b.toString('base64');
}
function decode(bytes) {
  const b = Buffer.from(bytes, 'base64');
  return Float32Array.from({length: b.length / 4}, (_, i) => b.readFloatLE(i * 4));
}
const paintingHash = a => hash(Buffer.from(encode(a), 'base64'));
const summary = s => ({next: s.next, carry: s.carry, paintingSha256: paintingHash(s.painting), history: s.history});
const stateHash = s => hash(summary(s));
const recipeHash = f => hash({id: f.brush.id, program: f.brush.program,
  pickup: f.brush.pickup, opacity: f.brush.opacity, radiusMM: f.brush.radiusMM});
const initial = f => ({next: 0, carry: f.brush.initialCarry.slice(),
  painting: new Float32Array(f.painting.grid[0] * f.painting.grid[1] * 4), history: []});
function sample(a, p, f) {
  const [u0, v0, w, h] = f.painting.domain, [nx, ny] = f.painting.grid;
  const u = B.gUnwrapU(p), v = B.G_R * B.sphLat(p);
  assert(u >= u0 && u < u0 + w && v >= v0 && v < v0 + h, 'read within authored patch');
  const x = Math.floor((u - u0) / w * nx), y = Math.floor((v - v0) / h * ny);
  return Array.from(a.slice(4 * (y * nx + x), 4 * (y * nx + x) + 4));
}
function paint(a, region, pigment, opacity, f) {
  const out = a.slice(), [u0, v0, w, h] = f.painting.domain, [nx, ny] = f.painting.grid;
  const top = pigment.map(x => x * opacity), changed = [];
  for (let y = 0; y < ny; y++) for (let x = 0; x < nx; x++) {
    const p = B.sphPoint0(u0 + (x + .5) * w / nx, v0 + (y + .5) * h / ny);
    if (B.sphDist(region.center, p) <= region.radiusMM) {
      const i = y * nx + x;
      out.set(B.over(top, Array.from(a.slice(4 * i, 4 * i + 4))), 4 * i);
      changed.push(i);
    }
  }
  return {painting: out, changed};
}
function snapshot(f, s) {
  const value = {support: f.subject, coating: {...f.coating, records: copy(B.G_RECORDS)},
    previousPainting: {id: f.painting.id, revision: s.next, sha256: paintingHash(s.painting)},
    read: {quantity: 'coating rgba', filter: f.painting.sample, domain: f.painting.domain, grid: f.painting.grid},
    composition: 'previous logical pickup painting over ordered A and B'};
  return {...copy(value), id: hash(value)};
}
function request(f, s) {
  const event = {...f.brush.events[s.next], point: Array.from(B.gArcCentre(f.brush.events[s.next].t))};
  const snap = snapshot(f, s), inputState = stateHash(s), recipe = recipeHash(f);
  return {id: hash({event, snapshot: snap.id, inputState, recipe}), event, snapshot: snap, inputState, recipe};
}
function readSurface(req, s, f, grant) {
  // The pinned definitions supply gLocate's value references, never its latest mutable globals.
  const live = B.G_RECORDS;
  let located;
  try {
    B.G_RECORDS = copy(req.snapshot.coating.records);
    located = B.gLocate(req.snapshot.support.revision, req.event.point,
      {budget: grant ? 1 : 0, order: req.snapshot.coating.order});
  } finally { B.G_RECORDS = live; }
  const comp = located.composition, common = {request: req.id, snapshot: req.snapshot.id, located: copy(located)};
  if (comp.status === 'unresolved') return {...common, status: 'pending', unresolved: copy(located.unresolved)};
  if (comp.status === 'needs-policy') return {...common, status: 'needs-policy', candidates: copy(comp.contributors)};
  assert(['resolved', 'empty'].includes(comp.status));
  return {...common, status: 'resolved', color: Array.from(B.over(sample(s.painting, req.event.point, f), comp.rgba))};
}
function attempt(f, s, req, answer) {
  if (req.inputState !== stateHash(s) || req.recipe !== recipeHash(f) ||
      hash(f.brush.events[s.next]) !== hash(Object.fromEntries(Object.entries(req.event).filter(([k]) => k !== 'point'))) ||
      answer.request !== req.id || answer.snapshot !== req.snapshot.id)
    return {status: 'stale', reason: 'answer does not belong to this event, recipe, input state and read snapshot'};
  if (answer.status !== 'resolved') return {status: answer.status, continuation: {
    schema: 'attack-4-continuation/v1', implementation: custody, fixture: copy(f), request: copy(req), input: {...copy(summary(s)),
      painting: {encoding: 'Float32LE/base64', bytes: encode(s.painting)}},
    queuedEvents: copy(f.brush.events.slice(s.next + 1)), missing: copy(answer.unresolved || answer.candidates || [])}};
  const env = {snapshot: req.snapshot, 'event.point': req.event.point, carry: s.carry,
    painting: s.painting, pickup: f.brush.pickup, opacity: f.brush.opacity, radiusMM: f.brush.radiusMM};
  const ops = {
    'read-surface': (snap, point) => {
      assert.equal(snap.id, req.snapshot.id); assert.deepEqual(point, req.event.point);
      return answer.color;
    },
    mix: (a, b, t) => Array.from(B.pickupMix(a, b, t)),
    'surface-region': (center, radiusMM) => ({center, radiusMM}),
    paint: (a, region, color, alpha) => paint(a, region, color, alpha, f)
  };
  for (const node of f.brush.program) {
    assert(ops[node.op], 'unimplemented operation: ' + node.op);
    env[node.id] = ops[node.op](...node.args.map(a => a && a.ref ? env[a.ref] : a));
  }
  const read = {event: req.event.id, request: req.id, snapshot: req.snapshot.id,
    color: answer.color, contributors: answer.located.composition.contributors,
    interpretation: answer.control || 'resolved coating', carry: env.carryOut, changedTexels: env.surfaceOut.changed.length};
  return {status: 'committed', nextState: {next: s.next + 1, carry: env.carryOut,
    painting: env.surfaceOut.painting, history: [...s.history, read]}, read};
}
function rest(f, s) {
  while (s.next < f.brush.events.length) {
    const req = request(f, s), a = attempt(f, s, req, readSurface(req, s, f, true));
    assert.equal(a.status, 'committed'); s = a.nextState;
  }
  return s;
}
function resume(c) {
  assert.equal(c.implementation.benchSha256, custody.benchSha256, 'continuation must retain its bench implementation');
  assert.equal(c.implementation.declarationsSha256, custody.declarationsSha256);
  const f = c.fixture, s = {...c.input, painting: decode(c.input.painting.bytes)};
  assert.equal(paintingHash(s.painting), c.input.paintingSha256);
  assert.equal(stateHash(s), c.request.inputState);
  const a = attempt(f, s, c.request, readSurface(c.request, s, f, true));
  assert.equal(a.status, 'committed');
  return rest(f, a.nextState);
}
if (process.argv.includes('--cold')) {
  const c = JSON.parse(fs.readFileSync(0, 'utf8'));
  console.log(JSON.stringify(summary(resume(c))));
} else {
  const f = copy(records), s0 = initial(f), startHash = stateHash(s0), req = request(f, s0);
  const eager = rest(f, s0);
  const pendingRead = readSurface(req, s0, f, false), held = attempt(f, s0, req, pendingRead);
  const repeat = attempt(f, s0, req, readSurface(req, s0, f, false));
  assert.equal(held.status, 'pending'); assert(!('nextState' in held));
  assert.equal(stateHash(s0), startHash); assert.deepEqual(held, repeat);
  const delayed = resume(copy(held.continuation));
  assert.deepEqual(summary(eager), summary(delayed));
  const cold = JSON.parse(execFileSync(process.execPath, [fileURLToPath(import.meta.url), '--bench', subject, '--cold'],
    {input: JSON.stringify(held.continuation), encoding: 'utf8'}));
  assert.deepEqual(cold, summary(eager));
  // The prohibited ordinary behavior: consume the known red layer as though B were transparent.
  const guessed = {...pendingRead, status: 'resolved', color: [.5, 0, 0, .5], control: 'B guessed transparent'};
  const wrong0 = attempt(f, s0, req, guessed), wrong = rest(f, wrong0.nextState);
  assert.notEqual(paintingHash(wrong.painting), paintingHash(eager.painting));
  assert.notDeepEqual(wrong.carry, eager.carry);
  wrong.history.forEach((row, i) => assert.notDeepEqual(row.carry, eager.history[i].carry));
  // An order edit while the request is waiting cannot relabel its eventual answer.
  const edit = copy(f); edit.coating.order.reverse();
  const newReq = request(edit, s0), oldAnswer = readSurface(req, s0, f, true);
  const stale = attempt(edit, s0, newReq, oldAnswer);
  assert.equal(stale.status, 'stale'); assert(!('nextState' in stale));
  const reordered = rest(edit, initial(edit));
  assert.notEqual(paintingHash(reordered.painting), paintingHash(eager.painting));
  const unordered = copy(f); unordered.coating.order = null;
  const policyReq = request(unordered, s0), policy = attempt(unordered, s0, policyReq, readSurface(policyReq, s0, unordered, true));
  assert.equal(policy.status, 'needs-policy'); assert(!('nextState' in policy));
  const duplicate = attempt(f, eager, req, oldAnswer);
  assert.equal(duplicate.status, 'stale'); assert(!('nextState' in duplicate));
  // Changing the recipe as data, and retaining the first result while running another recipe.
  const variant = copy(f); variant.brush.id = 'coating-pickup@1'; variant.brush.pickup = .5;
  const changedRecipeResponse = attempt(variant, s0, req, oldAnswer);
  assert.equal(changedRecipeResponse.status, 'stale'); assert(!('nextState' in changedRecipeResponse));
  const changed = rest(variant, initial(variant));
  assert.notEqual(paintingHash(changed.painting), paintingHash(eager.painting));
  const frozen = paintingHash(eager.painting), reused = initial(variant);
  reused.painting = eager.painting.slice();
  const reuseResult = rest(variant, reused);
  assert.equal(paintingHash(eager.painting), frozen);
  assert.notEqual(paintingHash(reuseResult.painting), paintingHash(changed.painting));

  const reach = f.reach.radiiMM.map(radius => {
    const region = B.reachRegion({seed: f.reach.seed, distance: f.reach.distance, radius});
    const anti = region.seed.map(v => -v);
    return {radiusMM: radius, distanceMM: region.distance(anti), byDistance: region.distance(anti) <= radius,
      byRegion: region.member(anti), areaMM2: region.area, wholeSphereAreaMM2: 4 * Math.PI * B.G_R ** 2,
      bounds: copy(region.bounds)};
  });
  const rr = {seed: f.reach.seed.slice(), radius: 150, distance: 'surface'}, retained = B.reachRegion(rr);
  const q = B.sphUnit(3 * Math.PI / 2, Math.PI / 3);
  rr.radius = 140;
  const alias = {recordRadiusMM: retained.record.radius, actualMember: retained.member(q),
    memberFromReportedRecord: B.reachRegion(retained.record).member(q), distanceMM: retained.distance(q)};
  const output = {schema: 'attack-4-receipts/v1', node: process.version, custody,
    recordsSha256: hash(records), runnerSha256: hash(fs.readFileSync(fileURLToPath(import.meta.url))),
    scope: 'Node reference transaction and spherical painting over exact shipped bench functions; not a browser brush integration',
    pending: {status: held.status, hasNextState: 'nextState' in held, inputStateUnchanged: stateHash(s0) === startHash,
      repeatIdentical: hash(held) === hash(repeat), queuedEvents: held.continuation.queuedEvents.map(e => e.id),
      read: pendingRead, continuationSha256: hash(held.continuation)},
    eager: summary(eager), delayed: summary(delayed), cold, transparentGuess: summary(wrong),
    firstResolved: oldAnswer.color, firstCarry: attempt(f, s0, req, oldAnswer).nextState.carry,
    firstGuessCarry: wrong0.nextState.carry, stale, duplicate, needsPolicy: policy.status, reordered: summary(reordered),
    changedRecipeResponse, changedPickup: summary(changed), reusedPainting: summary(reuseResult), retainedResultUnchanged: true,
    reach: {antipode: reach, retainedRecordAlias: alias},
    assertions: 'pending preserves state and queued events; eager=delayed=cold byte-for-byte; guess/order/recipe/reuse discriminate; stale response commits nothing'};
  if (process.argv.includes('--write')) {
    fs.writeFileSync(path.join(here, 'receipts.json'), JSON.stringify(output, null, 2) + '\n');
    fs.writeFileSync(path.join(here, 'continuation.json'), JSON.stringify(held.continuation, null, 2) + '\n');
  }
  console.log(JSON.stringify(process.argv.includes('--dump') ? output : {
    custody, pending: output.pending.status, eager: output.eager, delayedEqualsEager: true, coldEqualsEager: true,
    firstCarry: output.firstCarry, firstGuessCarry: output.firstGuessCarry,
    guessFinalCarry: wrong.carry, guessPainting: paintingHash(wrong.painting), stale,
    reach: output.reach, assertions: output.assertions}, null, 2));
}
