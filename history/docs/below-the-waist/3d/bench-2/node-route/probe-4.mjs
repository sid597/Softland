// The composer's Node route for the fold of the definer's attack 4 (2026-09-06): the attack's numbers through the
// bench's own functions and the executor over the bench's records. Run from the repository root:
//   node history/docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs                       # the committed bench's functions + the sidecars (the route before the bench takes the change)
//   node history/docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs --bench <path|ref>    # the bench's embedded block executed as embedded, with custody of every sidecar unit
//   node history/docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs --cold < continuation.json   # resume a suspended brush in a new process, print the summary
//   node history/docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs --record <file.json> [--budget <n>]   # run a pasted record (a cold author's, say) and print its summary
//   --dump prints every number; --write lands receipts-4.json and continuation-4.json beside this file.
// Exit 0 with "failed": 0 is the fold's check; every fix keeps probe-2-3.mjs passing too.
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {loadBench, here, sha256} from './load.mjs';

const arg = name => process.argv.includes(name) ? process.argv[process.argv.indexOf(name) + 1] : null;
const subject = arg('--bench');
const {B, benchSha256, route, custody} = loadBench(subject || 'HEAD', {sidecars: !subject});
const records = JSON.parse(JSON.stringify(B.X_RECORDS));   // records.js, embedded in the bench and loaded here
const copy = v => JSON.parse(JSON.stringify(v));
const near = (a, b, eps = 1e-9) => Math.abs(a - b) <= eps;
const nearAll = (a, b, eps = 1e-9) => a.length === b.length && a.every((v, i) => near(v, b[i], eps));
const checks = [], out = {bench: benchSha256, route, custody};
const check = (name, ok, detail) => { checks.push({name, ok: !!ok, detail}); if (!ok) console.error('FAIL', name, JSON.stringify(detail)); };
const paintingHash = p => B.xPaintingSha256(p);
const summary = r => ({status: r.status, at: r.at, carry: r.state.carry, painting: paintingHash(r.state.painting), colors: r.history.map(h => h.read.color), carries: r.history.map(h => h.carry), changed: r.history.map(h => h.painting.changed)});

if (arg('--record')) {
  // one record, printed as its summary; --then <file> runs a second record with the first's returned region and painting
  // handed in as the inputs it declares (by kind); --budget <n> is the work granted to reads (default every binding)
  const rec = JSON.parse(fs.readFileSync(arg('--record'), 'utf8')), budget = arg('--budget') != null ? Number(arg('--budget')) : Infinity;
  const r = B.xRun(rec, {budget});
  const print = (label, run) => { console.log(label); console.log(JSON.stringify(B.xSummary(run), (k, v) => v === Infinity ? 'Infinity' : v, 1)); if (run.log && run.log.some(l => l.note || l.error)) console.log(JSON.stringify({log: run.log.filter(l => l.note || l.error)}, null, 1)); };
  print('record ' + rec.id, r);
  let status = r.status;
  if (arg('--then')) {
    const rec2 = JSON.parse(fs.readFileSync(arg('--then'), 'utf8')), kept = {};
    for (const k in r.results || {}) { const v = r.results[k]; if (B.xIsRegion(v)) kept['surface-region'] = v; if (B.xIsPainting(v)) kept.painting = v; }
    const inputs = {}, bound = [];
    for (const k in rec2.inputs || {}) {
      const want = rec2.inputs[k], v = want && kept[want.kind]; if (!v) continue;
      inputs[k] = v;
      // a declared subject left open (no id, or a placeholder in angle brackets) is bound to what the first record returned, and said
      if (want.kind === 'surface-region' && (!want.id || /^</.test(want.id))) { want.id = v.id; bound.push(k + ' ← the region the first record returned, id ' + v.id.slice(0, 8) + '…'); }
      if (want.kind === 'painting' && (!want.sha256 || /^</.test(want.sha256))) { want.sha256 = B.xPaintingSha256(v); bound.push(k + ' ← the painting the first record returned, ' + want.sha256.slice(0, 8) + '…'); }
    }
    if (bound.length) console.log('bound: ' + bound.join('; '));
    const r2 = B.xRun(rec2, {budget, inputs});
    print('then ' + rec2.id + ' with inputs ' + JSON.stringify(Object.keys(inputs)), r2);
    status = r2.status;
  }
  process.exit(status === 'complete' ? 0 : 2);
}
if (process.argv.includes('--cold')) {
  const c = JSON.parse(fs.readFileSync(0, 'utf8'));
  const r = B.xResume(c, 1);
  console.log(JSON.stringify(r.status === 'complete' ? summary(r) : {status: r.status, reason: r.reason}));
  process.exit(0);
}

// ---- A. the coating-reading brush on G through the executor (attack 4 §1)
const D = { // the definer's receipts (attack-4/receipts.json)
  colors: [[0.25, 0, 0.5, 0.75], [0.5390625, 0, 0.328125, 0.8671875], [0.5, 0, 0, 0.5], [0.6631851196289062, 0, 0.2650909423828125, 0.9282760620117188]],
  carries: [[0.8125, 0, 0.125, 0.9375], [0.744140625, 0, 0.17578125, 0.919921875], [0.68310546875, 0, 0.1318359375, 0.81494140625], [0.6781253814697266, 0, 0.16514968872070312, 0.8432750701904297]],
  changed: [401, 321, 405, 401], painting: '30cb13f2147784bdae44b4aa303d412c8980f00c09435c0983521cd291d75579',
  guessCarries: [[0.875, 0, 0, 0.875], [0.80078125, 0, 0.0703125, 0.87109375], [0.7255859375, 0, 0.052734375, 0.7783203125], [0.7258682250976562, 0, 0.0880279541015625, 0.8138961791992188]],
  guessPainting: 'c1cf81702b98fb287cbbb1432166d81e7a55750d3306c3934cd09528960d2c3d',
  changedPickup: {carry: [0.522857666015625, 0, 0.27911376953125, 0.801971435546875], painting: '4df8a532ec4048f5489927daff4e7dd3635853c66c8d6bb09ebe04a18fc5f0ce'},
  reused: {carry: [0.778701844625175, 0, 0.1365891443565488, 0.9152909861877561], painting: 'a5aba6ed27c234df463b322995e052bba3e3c4fa7871e453d4a0400270cd887a'},
  reordered: {carry: [0.7607002258300781, 0, 0.08257484436035156, 0.8432750701904297], painting: 'ae8c231cbfe1ddb4c568913cced70bd8ce8988a25af93d66958c6c14bbb6963a', first: [0.5, 0, 0.25, 0.75]}
};
const pickup = records.pickup;
const eager = B.xRun(pickup);                                           // every binding granted: the ordinary run
out.eager = eager.status === 'complete' ? summary(eager) : {status: eager.status, reason: eager.reason, log: eager.log};
check('eager: complete after four dabs', eager.status === 'complete' && eager.at === 4, out.eager);
check('eager: the four resolved samples', eager.status === 'complete' && D.colors.every((c, i) => nearAll(eager.history[i].read.color, c, 1e-12)), out.eager.colors);
check('eager: the four carries, (0.8125, 0, 0.125, 0.9375) first', eager.status === 'complete' && D.carries.every((c, i) => nearAll(eager.history[i].carry, c, 1e-12)), out.eager.carries);
check('eager: changed texels 401, 321, 405, 401', eager.status === 'complete' && D.changed.every((n, i) => eager.history[i].painting.changed === n), out.eager.changed);
check('eager: painting 30cb13f2… (the definer\'s bytes)', eager.status === 'complete' && paintingHash(eager.state.painting) === D.painting, out.eager.painting);
check('eager: contributors kA, kB and the brush\'s own painting at every dab; at .41 B is excluded by its root restriction', eager.status === 'complete' && eager.history.map(h => h.read.contributors.join()).join(' | ') === 'kA,kB,pickup-G@0 | kA,kB,pickup-G@1 | kA,pickup-G@2 | kA,kB,pickup-G@3', eager.history.map(h => h.read.contributors));
// the delayed schedule: demand with B's merge step withheld → pending, no next state, the transaction retained
const held = B.xRun(pickup, {budget: 0});
const held2 = B.xRun(pickup, {budget: 0});
out.pending = {status: held.status, reason: held.reason, at: held.at, carry: held.state.carry, paintingRevision: held.state.painting.revision, request: held.request && held.request.id, queued: held.continuation && held.continuation.queued.map(e => e.id), missing: held.continuation && held.continuation.missing, repeatIdentical: sha256(held.continuation) === sha256(held2.continuation)};
check('pending: no next state (at 0, the initial carry, painting revision 0)', held.status === 'pending' && held.at === 0 && nearAll(held.state.carry, [1, 0, 0, 1]) && held.state.painting.revision === 0 && held.history.length === 0, out.pending);
check('pending: the dependency named (χ: B@0 → M@2), three events queued', held.continuation && /B@0 → M@2/.test(JSON.stringify(held.continuation.missing)) && held.continuation.queued.length === 3, out.pending);
check('pending: repeating the demand gives the identical transaction', out.pending.repeatIdentical, out.pending);
check('pending: kA known, kB may contribute (no colour returned)', held.read.known.join() === 'kA' && held.read.color === undefined, held.read);
const delayed = B.xResume(held.continuation, 1);
out.delayed = delayed.status === 'complete' ? summary(delayed) : {status: delayed.status, reason: delayed.reason};
check('delayed = eager (carry, painting bytes, every sample and carry)', delayed.status === 'complete' && JSON.stringify(summary(delayed)) === JSON.stringify(summary(eager)), out.delayed);
// cold: the continuation as bytes through a new process
const coldFile = path.join(here, 'continuation-4.json');
const coldText = JSON.stringify(held.continuation, null, 1) + '\n';
if (process.argv.includes('--write')) fs.writeFileSync(coldFile, coldText);
const cold = JSON.parse(execFileSync(process.execPath, [fileURLToPath(import.meta.url)].concat(subject ? ['--bench', subject] : [], ['--cold']), {input: coldText, encoding: 'utf8'}));
out.cold = cold;
check('cold = eager (a new process, the continuation from stdin)', JSON.stringify(cold) === JSON.stringify(summary(eager)), cold);
// the prohibited ordinary behaviour, run as a declared construction: the first read consumes A with B guessed transparent
const guessing = copy(pickup); guessing.program.steps[1].pending = 'provisional';
const guess0 = B.xRun(guessing, {budget: 0, until: 1});
const guess = guess0.status === 'complete' ? B.xRun(guessing, {from: B.xCheckpoint(guess0, guessing), budget: 1}) : guess0;
out.transparentGuess = guess.status === 'complete' ? {...summary(guess), provisional: guess.history[0].read.provisional} : {status: guess.status, reason: guess.reason};
check('guess: first carry (0.875, 0, 0, 0.875), retained as a provisional read in the history', guess.status === 'complete' && nearAll(guess.history[0].carry, [0.875, 0, 0, 0.875]) && guess.history[0].read.provisional && /transparent/.test(guess.history[0].read.provisional.interpretation), out.transparentGuess);
check('guess: the four carries and the painting c1cf8170… differ from eager at every dab', guess.status === 'complete' && D.guessCarries.every((c, i) => nearAll(guess.history[i].carry, c, 1e-12)) && paintingHash(guess.state.painting) === D.guessPainting && guess.history.every((h, i) => !nearAll(h.carry, eager.history[i].carry)), out.transparentGuess);
// an order edit while the request waits cannot relabel its eventual answer; the reordered run has its own result
const answered = B.xDemand(pickup, 'picked', {budget: 1});
check('demand: the read answered outside a commit, resolved (0.25, 0, 0.5, 0.75)', answered.status === 'answered' && nearAll(answered.value.color, [0.25, 0, 0.5, 0.75]), answered.value);
const reorderedRecord = copy(pickup); reorderedRecord.coating.order = ['kB', 'kA'];
const stale = B.xRun(reorderedRecord, {answer: {request: answered.request, value: answered.value}});
out.stale = {status: stale.status, reason: stale.reason, at: stale.at, history: stale.history.length};
check('stale: the old answer under the new order commits nothing', stale.status === 'stale' && stale.history.length === 0, out.stale);
const reordered = B.xRun(reorderedRecord);
out.reordered = reordered.status === 'complete' ? summary(reordered) : {status: reordered.status, reason: reordered.reason};
check('reordered: (0.5, 0, 0.25, 0.75) first, painting ae8c231c…', reordered.status === 'complete' && nearAll(reordered.history[0].read.color, D.reordered.first) && paintingHash(reordered.state.painting) === D.reordered.painting && nearAll(reordered.state.carry, D.reordered.carry, 1e-12), out.reordered);
// no saved order → needs-policy, not a transparent sample and not a plain pending
const unordered = copy(pickup); unordered.coating.order = null;
const policy = B.xRun(unordered);
out.needsPolicy = {status: policy.status, reason: policy.reason, candidates: policy.read && policy.read.candidates, at: policy.at};
check('needs-policy: candidates kA, kB; no next state', policy.status === 'needs-policy' && policy.read.candidates.join() === 'kA,kB' && policy.history.length === 0, out.needsPolicy);
// delivering an already consumed answer again commits nothing
const duplicate = B.xRun(pickup, {from: B.xCheckpoint(eager, pickup), answer: {request: answered.request, value: answered.value}});
out.duplicate = {status: duplicate.status, reason: duplicate.reason};
check('duplicate: a consumed answer delivered into the final state is stale', duplicate.status === 'stale', out.duplicate);
// the recipe changed as data: the old answer is stale; the run has its own result
const variant = copy(pickup); variant.id = 'coating-pickup@1'; variant.tool.pickup = 0.5;
const changedResponse = B.xRun(variant, {answer: {request: answered.request, value: answered.value}});
const changed = B.xRun(variant);
out.changedRecipe = {response: {status: changedResponse.status, reason: changedResponse.reason}, run: changed.status === 'complete' ? summary(changed) : {status: changed.status}};
check('changed recipe (pickup .25 → .5): the old answer is stale; painting 4df8a532…', changedResponse.status === 'stale' && changed.status === 'complete' && paintingHash(changed.state.painting) === D.changedPickup.painting && nearAll(changed.state.carry, D.changedPickup.carry, 1e-12), out.changedRecipe);
// reuse: the first run's painting as the second recipe's start, the first result unchanged
const reusing = copy(variant); reusing.program.state.painting = 'inputs.start'; reusing.inputs = {start: {kind: 'painting', id: 'pickup-G', sha256: D.painting}};
const reused = B.xRun(reusing, {inputs: {start: eager.state.painting}});
out.reused = reused.status === 'complete' ? summary(reused) : {status: reused.status, reason: reused.reason};
check('reuse: the first painting as input → a5aba6ed…; the retained first result unchanged', reused.status === 'complete' && paintingHash(reused.state.painting) === D.reused.painting && nearAll(reused.state.carry, D.reused.carry, 1e-12) && paintingHash(eager.state.painting) === D.painting, out.reused);
const wrongInput = B.xRun(reusing, {inputs: {start: changed.state.painting}});
check('reuse: another painting under the declared subject is refused as stale', wrongInput.status === 'stale', {status: wrongInput.status, reason: wrongInput.reason});

// ---- B. the retained region obeys its declared radius and keeps a truthful subject (attack 4 §2)
const R = B.G_R, seed = [Math.PI, Math.PI / 3];
const anti = B.sphUnit(seed[0], seed[1]).map(v => -v), q1 = B.sphUnit(3 * Math.PI / 2, Math.PI / 3);
out.reach = {};
for (const radius of [628, 629, 2 * Math.PI * R]) {
  const region = B.reachRegion({seed, radius, distance: 'surface'});
  out.reach[radius] = {distance: region.distance(anti), byDistance: region.distance(anti) <= radius, member: region.member(anti), area: region.area, bounds: region.bounds, saturated: region.saturated};
}
check('reach 628: the antipode (πR = 628.3185307179587) outside by both, area 502654.50582273136', !out.reach[628].member && !out.reach[628].byDistance && near(out.reach[628].area, 502654.50582273136, 1e-6), out.reach[628]);
check('reach 629: the antipode inside by member as by distance; the whole sphere, area 4πR², bounds [−200, 200]³', out.reach[629].member && out.reach[629].byDistance && near(out.reach[629].area, 4 * Math.PI * R * R, 1e-6) && out.reach[629].bounds.every(b => b[0] === -R && b[1] === R), out.reach[629]);
check('reach 2πR: the whole sphere, not an empty region with reversed bounds', out.reach[2 * Math.PI * R].member && near(out.reach[2 * Math.PI * R].area, 4 * Math.PI * R * R, 1e-6) && out.reach[2 * Math.PI * R].bounds.every(b => b[0] < b[1]), out.reach[2 * Math.PI * R]);
const rr = {seed: seed.slice(), radius: 150, distance: 'surface'}, retained = B.reachRegion(rr); rr.radius = 140;
out.reach.alias = {recordRadius: retained.record.radius, member: retained.member(q1), fromReportedRecord: B.reachRegion(retained.record).member(q1), distance: retained.distance(q1), callerRadiusNow: rr.radius};
check('alias: the result reports the radius it was built from (150) after the caller\'s edit to 140, and reconstructs to the same membership', retained.record.radius === 150 && retained.member(q1) && out.reach.alias.fromReportedRecord, out.reach.alias);
const refused = B.reachRegion({seed, radius: -10, distance: 'surface'});
check('admission: a negative radius is refused (unsupported), zero is the seed alone', refused.status === 'unsupported' && B.reachRegion({seed, radius: 0}).member(B.sphUnit(seed[0], seed[1])) && !B.reachRegion({seed, radius: 0}).member(q1), refused);
check('the earlier numbers hold: 150 → q1 inside at 144.546849562683, area 67433.94227378976', retained.member(q1) && near(retained.distance(q1), 144.54684956268315, 1e-9) && near(retained.area, 67433.94227378976, 1e-6), retained.area);
// the reach tool as a record through the executor, its result kept as a clip by another record
const reach150 = B.xRun(records.reach);
const reachRecord140 = copy(records.reach); reachRecord140.tool.radius = 140;
const reach140 = B.xRun(reachRecord140);
out.reachRecords = {r150: B.xDescribe(reach150.results.region), r140: B.xDescribe(reach140.results.region)};
check('reach record: the region with its subject, area and bounds; the tool\'s radius change is a new result, the retained one unchanged', reach150.status === 'complete' && reach150.results.region.record.radius === 150 && reach140.results.region.record.radius === 140 && reach150.results.region.member(q1) && !reach140.results.region.member(q1) && reach150.results.region.id !== reach140.results.region.id, out.reachRecords);
const clipRecord = copy(records.clip); clipRecord.inputs.clip.id = reach150.results.region.id;
const throughOld = B.xRun(clipRecord, {inputs: {clip: reach150.results.region}});
const clipRecord140 = copy(records.clip); clipRecord140.inputs.clip.id = reach140.results.region.id;
const throughNew = B.xRun(clipRecord140, {inputs: {clip: reach140.results.region}});
const wrongClip = B.xRun(clipRecord, {inputs: {clip: reach140.results.region}});
out.clip = {throughRetained: throughOld.status === 'complete' ? B.xDescribe(throughOld.results['state.painting']) : throughOld, throughNew: throughNew.status === 'complete' ? B.xDescribe(throughNew.results['state.painting']) : throughNew, wrong: {status: wrongClip.status, reason: wrongClip.reason}};
check('clip: a dab at q1 through the retained region@150 deposits; through the result@140 it deposits nothing; another region under the declared subject is refused', throughOld.status === 'complete' && throughOld.results['state.painting'].changed > 0 && throughNew.status === 'complete' && throughNew.results['state.painting'].changed === 0 && wrongClip.status === 'stale', out.clip);

// ---- C. the two-record sequence as a record through the executor (attack 2 §4 at ×2, the round's own test)
const seq = B.xRun(records.sequence);
out.sequence = seq.status === 'complete' ? seq.history.map(h => ({event: h.event, color: h.read.color, contributors: h.read.contributors})) : {status: seq.status, reason: seq.reason};
check('sequence: (0.25, 0, 0.5, 0.75) from kA and kB at G@0, G@1 and G@2', seq.status === 'complete' && seq.history.length === 3 && seq.history.every(h => nearAll(h.read.color, [0.25, 0, 0.5, 0.75]) && h.read.contributors.join() === 'kA,kB'), out.sequence);
const seqHeld = B.xRun(records.sequence, {budget: 0});
out.sequenceHeld = {status: seqHeld.status, at: seqHeld.at, missing: seqHeld.continuation && seqHeld.continuation.missing};
check('sequence: with no work granted, G@0 and G@1 resolve and G@2 is pending at B\'s merge step', seqHeld.status === 'pending' && seqHeld.at === 2 && seqHeld.history.length === 2, out.sequenceHeld);
const seqResumed = B.xResume(seqHeld.continuation, 1);
check('sequence: resumed under the grant, the same three compositions', seqResumed.status === 'complete' && seqResumed.history.every(h => nearAll(h.read.color, [0.25, 0, 0.5, 0.75])), seqResumed.status);
const seqUnordered = copy(records.sequence); seqUnordered.coating.order = null;
check('sequence: no order → needs-policy at G@0', B.xRun(seqUnordered).status === 'needs-policy', B.xRun(seqUnordered).reason);
// a new tool said as one record: an unknown operation is named, not guessed
const unknown = copy(records.reach); unknown.program.steps[0].op = 'geodesic-disc';
const u = B.xRun(unknown);
check('a step naming an operation the table lacks is refused with the table printed', u.status === 'unsupported' && /geodesic-disc/.test(u.reason) && /surface-region/.test(u.reason), u.reason);
const notDeclared = copy(records.reach); notDeclared.tool.distance = 'chord';
check('a distance the operation does not declare (chord) is refused', B.xRun(notDeclared).status === 'unsupported', B.xRun(notDeclared).reason);

// ---- D. the contact ray on the field: the event preserved for a geometric-boundary answer (attack 4 §3)
const F = {on: true, at: [0, 0, 0], params: {a: .2, b: .1, c: .1}, hollow: false};
const contact = B.hitField({origin: [1, .1, 0], dir: [-1, 0, 0]}, F, 20000), axis = B.hitField({origin: [1, 0, 0], dir: [-1, 0, 0]}, F, 20000);
const roots = B.isolateRealRoots(B.fieldPoly(F.params, [1, .1, 0], [-1, 0, 0]), .8, 1.2, 1e-7, 20000);
out.contact = {isolator: roots.roots, hit: contact && {t: contact.t, kind: contact.kind, material: contact.material, zeroArea: contact.zeroArea}, axis: axis && {t: axis.t, kind: axis.kind, material: axis.material}};
check('contact ray: the isolator finds the tangency (kind contact at t ≈ 1)', roots.roots.length === 1 && roots.roots[0].kind === 'contact' && near(roots.roots[0].t, 1, 1e-6), roots.roots);
check('contact ray: hitField serves the contact as a boundary event with no material interval (the shipped bench returned null)', contact && contact.kind === 'contact' && near(contact.t, 1, 1e-6) && contact.material.length === 0 && contact.zeroArea === true, out.contact.hit);
check('axis control: crossing at t 0.8, material [0.8, 1.2]', axis && axis.kind === 'crossing' && near(axis.t, .8, 1e-9) && axis.material.length === 1 && near(axis.material[0][1], 1.2, 1e-9), out.contact.axis);

// ---- E. the coverage lane's envelope: the CPU certifies the sign at the definer's witness; the shader's scan is an approximation with a stated envelope
const t0 = 0.0158, pressure = .2 + .6 * t0, radius = 5 + 15 * pressure * pressure, len = Math.hypot(160, 160), dr = 30 * .6 * pressure, along = -dr / len;
const normal = [(along - Math.sqrt(1 - along * along)) / Math.sqrt(2), (along + Math.sqrt(1 - along * along)) / Math.sqrt(2)];
const q = normal.map(v => 160 * t0 + (radius - 0.0001) * v);
const knots = [{u: 0, v: 0, p: .2}, {u: 160, v: 160, p: .8}];
const obj = B.postSegmentObjective(knots, 0, q);
const cpu = B.sweepMin(obj.f, obj.L, {decide: true, budget: 20000, tol: 1e-6});
out.envelope = {q, witness: Math.hypot(q[0] - 160 * t0, q[1] - 160 * t0) - radius, cpu, scanEnvelopeMM: (len + 30 * .8 * .6) / 64, shaderMinimumMM: 0.000125885009765625, shaderSource: 'attack-4/gpu-receipts.json (SwiftShader, RGBA32F, one fragment)'};
check('the CPU sweep certifies inside at the definer\'s witness (−0.0001 mm) where the shader\'s scan returns +0.000126', cpu.status === 'inside' && cpu.upper < 0 && cpu.lower <= -0.0001 - 1e-9, out.envelope);
check('the scan\'s envelope in exact arithmetic, L/64 = 3.760533905932738 mm on this segment', near(out.envelope.scanEnvelopeMM, 3.760533905932738, 1e-12), out.envelope.scanEnvelopeMM);

// ---- F. the cold mind's records (cold/), as fixtures: the definer's bytes from a record a fresh session wrote from the contract alone
const coldDir = path.join(here, 'cold');
if (fs.existsSync(path.join(coldDir, 'pickup-brush.json'))) {
  const cb = B.xRun(JSON.parse(fs.readFileSync(path.join(coldDir, 'pickup-brush.json'), 'utf8')));
  check('cold: the cold mind\'s pickup brush gives the definer\'s bytes and carry', cb.status === 'complete' && paintingHash(cb.state.painting) === D.painting && nearAll(cb.state.carry, D.carries[3], 1e-12), cb.status === 'complete' ? summary(cb) : cb.reason);
  const cr = B.xRun(JSON.parse(fs.readFileSync(path.join(coldDir, 'reach.json'), 'utf8')));
  const crRegion = cr.results && cr.results.reach;
  check('cold: the cold mind\'s reach tool is the 150 mm region, pieces north, west and east', cr.status === 'complete' && crRegion && crRegion.record.radius === 150 && near(crRegion.area, 67433.94227378976, 1e-6) && crRegion.pieces.join() === 'north,west,east', cr.status === 'complete' ? B.xDescribe(crRegion) : cr.reason);
  const cdRec = JSON.parse(fs.readFileSync(path.join(coldDir, 'reach-dab.json'), 'utf8')); cdRec.inputs.clip.id = crRegion.id;
  const cd = B.xRun(cdRec, {inputs: {clip: crRegion}});
  const cdPainting = cd.status === 'complete' ? Object.values(cd.results).filter(v => B.xIsPainting(v))[0] : null;
  check('cold: its dab through the reach as a clip changes 6096 texels', cdPainting && cdPainting.changed === 6096, cdPainting ? cdPainting.changed : cd.reason);
  const hostSeed = B.xRun({id: 'host-seed@0', support: {id: 'G'}, program: {steps: [{out: 'r', op: 'surface-region', support: 'support', seed: 'host.A.seed', radius: 'host.A.radius'}], return: ['r']}});
  check('host root: a region seeded at host.A.seed with host.A.radius is the arc\'s seed disc (16 mm)', hostSeed.status === 'complete' && hostSeed.results.r.record.radius === 16 && near(hostSeed.results.r.distance(B.G_N), 0, 1e-9), hostSeed.status === 'complete' ? B.xDescribe(hostSeed.results.r) : hostSeed.reason);
}

// ---- G. custody: the embedded block, unit by unit against the sidecars (only when the bench embeds it)
if (custody.embedded) check('custody: every sidecar unit is embedded verbatim (' + custody.compared + ' units)', custody.mismatches.length === 0, custody.mismatches);
else checks.push({name: 'custody: not applicable (' + route + ')', ok: true});

out.checks = checks;
const failed = checks.filter(c => !c.ok).length;
if (process.argv.includes('--write')) fs.writeFileSync(path.join(here, 'receipts-4.json'), JSON.stringify({...out, checks: undefined, failed, node: process.version}, (k, v) => typeof v === 'function' ? undefined : v === Infinity ? 'Infinity' : v, 1) + '\n');
if (process.argv.includes('--dump')) console.log(JSON.stringify(out, (k, v) => typeof v === 'function' ? undefined : v === Infinity ? 'Infinity' : v, 1));
else console.log(JSON.stringify({bench: benchSha256, route, custody, checks: checks.map(c => (c.ok ? 'ok   ' : 'FAIL ') + c.name), failed}, null, 1));
process.exit(failed ? 1 : 0);
