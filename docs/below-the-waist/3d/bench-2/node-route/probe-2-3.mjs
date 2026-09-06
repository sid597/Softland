// The composer's Node route for the fold of attacks 2 and 3 (2026-09-06): the new functions (sweep-and-sphere.js,
// pickup-replay.js) evaluated beside the committed bench's own functions, and every definer number checked before
// the bench takes the functions. Run from the repository root:
//   node docs/below-the-waist/3d/bench-2/node-route/probe-2-3.mjs [--bench <path-or-git-ref>]
// Default subject: the bench at commit 2737f41 (the bytes both attacks read, SHA-256 010c91d4…). Pass a path to run
// the same checks against the patched bench (the attacks as the test suite).
import crypto from 'node:crypto';
import assert from 'node:assert';
import {loadBench} from './load.mjs';

// Since the fold of attack 4 the loader is shared (load.mjs): a bench that embeds the sphere, pickup and executor block
// runs it as embedded, with every sidecar unit checked against the embed (attack 4 §2: this probe used to load the
// sidecars over the bench's functions, so a poisoned embedded gLocate passed 55/55). The default subject, commit
// 2737f41, has no block: the sidecars load, which is how the definers' numbers were checked before the bench took them.
const argBench = process.argv.indexOf('--bench') >= 0 ? process.argv[process.argv.indexOf('--bench') + 1] : null;
const loaded = loadBench(argBench || '2737f41');
const B = loaded.B, source = loaded.source, benchHash = loaded.benchSha256;
const hash = x => crypto.createHash('sha256').update(ArrayBuffer.isView(x) ? Buffer.from(x.buffer, x.byteOffset, x.byteLength) : JSON.stringify(x)).digest('hex');
const R = 200, C = 2 * Math.PI * R, out = {bench: benchHash, route: loaded.route, custody: loaded.custody};
const near = (a, b, eps = 1e-9) => Math.abs(a - b) <= eps;
const checks = [];
const check = (name, ok, detail) => { checks.push({name, ok: !!ok, detail}); if (!ok) console.error('FAIL', name, detail); };

// ---- A. the post's footprint (attack 3): the capsule vs the swept nib with w = 10 + 30p²
const con = {revision: 'P0', params: {r: .2, h: .8, at: [0, 0], chart: {scale: 1, offset: 0}}};
const p0 = B.evaluateP(con, null);
const p1 = B.evaluateP({...con, revision: 'P1', params: {...con.params, chart: {scale: 2, offset: 100}}}, p0);
const p2 = B.evaluateP({...con, revision: 'P2', params: {...con.params, chart: {scale: 2, offset: 100}}}, p1);
const knots = B.markKnots(p0.support, 'unwrapped');
const qA3 = [C - 40 - 7.4 / Math.sqrt(2), 360 + 7.4 / Math.sqrt(2)];
const legacyFn = B.classifyMarkCapsule || B.classifyMark; const legacy = legacyFn(knots, qA3);
const swept = B.classifyMarkSweep(knots, qA3, {});
out.pressure = {q: qA3, legacy, swept, definerMinimum: [0.5590205669041215, 0.5596267523290708]};
check('A3 legacy says inside', legacy.inside === true, legacy);
check('A3 sweep says outside', swept.status === 'outside' && swept.inside === false, swept);
check('A3 sweep minimum within the definer enclosure', swept.lower >= 0.5590205669041215 - 1e-9 && swept.sd <= 0.5596267523290708 + 1e-9, [swept.lower, swept.sd, swept.work]);
// the bench's own probes, before and after: the crossing, Pm (15 mm along u), pd 25
for (const [label, q] of [['crossing', [C, 400]], ['Pm15', [C + 15, 400]], ['Pm15neg', [C - 15, 400]], ['pd25', [C + 25, 400]], ['pd25neg', [C - 25, 400]]]) {
  out.pressure[label] = {legacy: legacyFn(knots, q), swept: B.classifyMarkSweep(knots, q, {})};
}
check('crossing: two contributors at t .5', out.pressure.crossing.swept.contributors.length === 2 && out.pressure.crossing.swept.contributors.every(c => near(c.t, .5, 1e-6)), out.pressure.crossing.swept.contributors);

// ---- B. the sphere reach tool (attack 2 §2, at ×2)
const reach = B.reachRegion({seed: [Math.PI, Math.PI / 3], radius: 150, distance: 'surface'});
const seed = reach.seed;
const q1 = B.sphUnit(3 * Math.PI / 2, Math.PI / 3), q2 = B.sphUnit(Math.PI, Math.PI / 3 - 0.76), qPole = [0, 1, 0], qAnti = seed.map(v => -v);
out.reach = {
  seed, rho: reach.rho, area: reach.area, bounds: reach.bounds,
  row1: {intrinsic: reach.distance(q1), frozen: B.sphFrozenDist(seed, q1), chord: B.sphChord(seed, q1), member: reach.member(q1)},
  row2: {intrinsic: reach.distance(q2), chord: B.sphChord(seed, q2), member: reach.member(q2)},
  pole: {intrinsic: reach.distance(qPole), member: reach.member(qPole), piece: B.sphPieceOf(qPole), alphaOnce: B.over([0, 0, 0, .5], [0, 0, 0, 0])[3], alphaTwice: B.over([0, 0, 0, .5], B.over([0, 0, 0, .5], [0, 0, 0, 0]))[3]},
  antipode: {intrinsic: reach.distance(qAnti), r628: reach.distance(qAnti) <= 628, r629: reach.distance(qAnti) <= 629},
  radius140: {member: B.reachRegion({seed: [Math.PI, Math.PI / 3], radius: 140}).member(q1), throughOldClip: reach.member(q1) ? [0, .4, 0, .4] : [0, 0, 0, 0]},
  pieces: {seed: B.sphPieceOf(seed), q1: B.sphPieceOf(q1), east: B.sphPieceOf(B.sphUnit(0.5, 0.3))},
  band: {underlying: B.sphBandDistance(B.sphPoint0(Math.PI * R, 20), B.sphPoint0(Math.PI * R, -20), [-10, 10], 'underlying'), face: B.sphBandDistance(B.sphPoint0(Math.PI * R, 20), B.sphPoint0(Math.PI * R, -20), [-10, 10], 'face')}
};
check('reach row 1 intrinsic = 2 × 72.273424781342', near(out.reach.row1.intrinsic, 2 * 72.273424781342, 1e-9), out.reach.row1);
check('reach row 1 frozen = 2 × 78.539816339745', near(out.reach.row1.frozen, 2 * 78.539816339745, 1e-9), out.reach.row1);
check('reach row 1 inside, frozen outside', out.reach.row1.member && out.reach.row1.frozen > 150, out.reach.row1);
check('reach row 2 intrinsic 152 outside, chord = 2 × 74.184093882597 inside', near(out.reach.row2.intrinsic, 152, 1e-9) && !out.reach.row2.member && near(out.reach.row2.chord, 2 * 74.184093882597, 1e-9) && out.reach.row2.chord < 150, out.reach.row2);
check('pole = 2 × 52.359877559830, north piece', near(out.reach.pole.intrinsic, 2 * 52.359877559830, 1e-9) && out.reach.pole.piece.piece === 'north', out.reach.pole);
check('pole alpha .5 once, .75 twice', near(out.reach.pole.alphaOnce, .5) && near(out.reach.pole.alphaTwice, .75), out.reach.pole);
check('area = 4 × 16858.48556844744', near(out.reach.area, 4 * 16858.48556844744, 1e-6), out.reach.area);
const defBounds = [[-95.616091682125, 22.447204794743], [29.284176809936, 100], [-68.163876002334, 68.163876002334]];
check('bounds = 2 × the definer', reach.bounds.every((b, i) => near(b[0], 2 * defBounds[i][0], 1e-8) && near(b[1], 2 * defBounds[i][1], 1e-8)), reach.bounds);
check('antipode πR: 628 outside, 629 inside', near(out.reach.antipode.intrinsic, Math.PI * R, 1e-9) && !out.reach.antipode.r628 && out.reach.antipode.r629, out.reach.antipode);
check('radius 140 excludes row 1; the retained clip keeps it', !out.reach.radius140.member && out.reach.radius140.throughOldClip[3] === .4, out.reach.radius140);
check('band: underlying 40 mm; face ∞', near(out.reach.band.underlying.d, 40, 1e-9) && out.reach.band.face.d === Infinity, out.reach.band);
// bounds sampled: 4,320 locations on the cap against the bounds
let violations = 0;
for (let i = 0; i < 60; i++) for (let j = 0; j < 72; j++) {
  const ang = reach.rho * (i / 59), az = 2 * Math.PI * j / 72;
  // a point at angular distance ang from the seed, azimuth az: rotate in the tangent basis
  const t1 = B.v3norm(B.v3cross(seed, Math.abs(seed[1]) < .9 ? [0, 1, 0] : [1, 0, 0])), t2 = B.v3cross(seed, t1);
  const p = B.v3add(B.v3scale(seed, Math.cos(ang)), B.v3add(B.v3scale(t1, Math.sin(ang) * Math.cos(az)), B.v3scale(t2, Math.sin(ang) * Math.sin(az))));
  for (let k = 0; k < 3; k++) if (R * p[k] < reach.bounds[k][0] - 1e-9 || R * p[k] > reach.bounds[k][1] + 1e-9) violations++;
}
check('bounds hold on 4,320 sampled locations', violations === 0, violations);

// ---- C. the sequence on G (attack 2 §4, at ×2)
const ray = B.gFreshRay(.65);
out.sequence = {ray, worldMM: ray.worldMM, defWorld: [-49.910026996760, 86.446702565523, -5.996400647944].map(v => 2 * v)};
check('fresh ray world point = 2 × the definer', ray.worldMM.every((v, i) => near(v, out.sequence.defWorld[i], 1e-8)), ray.worldMM);
check('|world| = R, ray distance 2R', near(B.v3len(ray.worldMM), R, 1e-9) && ray.hitT === 400, ray);
out.sequence.evals = [0, 1, 2].map(rev => B.gLocate(rev, ray.point, {order: ['kA', 'kB']}));
const defChart = [[326.116373773273, 104.408915755942], [113.058186886636, 104.408915755942], [169.160415825622, 104.408915755942]];
out.sequence.evals.forEach((ev, rev) => {
  check(`eval ${rev} chart location = 2 × the definer`, near(ev.here.a, 2 * defChart[rev][0], 1e-8) && near(ev.here.b, 2 * defChart[rev][1], 1e-8), ev.here);
  const a = ev.bindings.find(b => b.record === 'A@0'), b = ev.bindings.find(b => b.record === 'B@0');
  check(`eval ${rev} A resolved at t .65, inside kA`, a && a.status === 'resolved' && a.inside && near(a.t, .65, 1e-9), a);
  check(`eval ${rev} B resolved at (ξ, η) = 2 × (11.957108414293, 4.408915755942), inside kB`, b && b.status === 'resolved' && b.inside && near(b.root.xi, 2 * 11.957108414293, 1e-8) && near(b.root.eta, 2 * 4.408915755942, 1e-8), b);
  check(`eval ${rev} composition blue over red (0.25, 0, 0.5, 0.75)`, ev.composition.rgba && [0.25, 0, 0.5, 0.75].every((v, i) => near(ev.composition.rgba[i], v)), ev.composition);
});
out.sequence.reversed = B.gLocate(2, ray.point, {order: ['kB', 'kA']}).composition;
out.sequence.noOrder = B.gLocate(2, ray.point, {order: null}).composition;
check('reversed (0.5, 0, 0.25, 0.75)', [0.5, 0, 0.25, 0.75].every((v, i) => near(out.sequence.reversed.rgba[i], v)), out.sequence.reversed);
check('no order → needs-policy with both', out.sequence.noOrder.status === 'needs-policy' && out.sequence.noOrder.contributors.length === 2, out.sequence.noOrder);
// the metric in chart 2 at latitude 60° (v = πR/3): [[1, −.25], [−.25, 1.0625]]
out.sequence.metric2 = B.gChartMetric(B.G_CHARTS[2], Math.PI * R / 3);
check('metric of chart 2 at latitude 60°', near(out.sequence.metric2[0][0], 1, 1e-9) && near(out.sequence.metric2[0][1], -.25, 1e-9) && near(out.sequence.metric2[1][1], 1.0625, 1e-9), out.sequence.metric2);
// S2∘χ∘φ = S0 on sampled source parameters
let maxDiff = 0;
for (const t of [0, .2, .41, .5, .65, 1]) {
  const p = B.gArcCentre(t), u = B.gUnwrapU(p), v = R * B.sphLat(p);
  for (const ch of B.G_CHARTS) { const c = ch.fromRoot(u, v), r = ch.toRoot(c.a, c.b), w = B.v3scale(B.sphPoint0(r.u, r.v), R); maxDiff = Math.max(maxDiff, B.v3len(w.map((x, i) => x - R * p[i]))); }
}
out.sequence.maxReconstructionMM = maxDiff;
check('S2∘χ∘φ = S0 to 1e-12 mm on six parameters', maxDiff < 1e-12, maxDiff);
// the .41 restriction query: kB's centre only 19.2 mm away, but ξ = 2 × −7.190692912533 is outside B's root domain
const r41 = B.gLocate(2, B.gArcCentre(.41), {order: ['kA', 'kB']});
out.sequence.q41 = {locate: r41, distToTap: B.sphDist(B.gArcCentre(.41), B.gArcCentre(.65))};
check('.41: only kA; B outside its root restriction at ξ = 2 × −7.190692912533', r41.composition.contributors.length === 1 && r41.composition.contributors[0] === 'kA' && near(r41.bindings.find(b => b.record === 'B@0').root.xi, 2 * -7.190692912533, 1e-8) && out.sequence.q41.distToTap < 24, r41);
// the on-demand budget: 0 withholds B's merge step
out.sequence.budget0 = B.gLocate(2, ray.point, {order: ['kA', 'kB'], budget: 0});
check('budget 0 → composition unresolved, kA known, kB may contribute', out.sequence.budget0.composition.status === 'unresolved' && out.sequence.budget0.composition.rgba === null && out.sequence.budget0.composition.known[0] === 'kA', out.sequence.budget0.composition);
out.sequence.budget1 = B.gLocate(2, ray.point, {order: ['kA', 'kB'], budget: 1});
check('budget 1 → resolved (0.25, 0, 0.5, 0.75)', out.sequence.budget1.composition.status === 'resolved' && near(out.sequence.budget1.composition.rgba[3], .75), out.sequence.budget1.composition);
// negative controls
const pA = B.gArcCentre(.65), uA = B.gUnwrapU(pA), vA = R * B.sphLat(pA);
const chiOnly = B.G_CHARTS[2].toRoot(uA + vA / 4 + 60, vA);            // χ applied to A's ORIGINAL coordinates, omitting φ
out.sequence.chiOnlyDisplacementMM = B.v3len(B.v3scale(B.sphPoint0(chiOnly.u, chiOnly.v), R).map((x, i) => x - R * pA[i]));
check('omit φ: 2 × 85.192705396047 mm displacement', near(out.sequence.chiOnlyDisplacementMM, 2 * 85.192705396047, 1e-6), out.sequence.chiOnlyDisplacementMM);
out.sequence.dropRetained = B.gLocate(2, ray.point, {order: ['kA', 'kB'], dropRetained: true});
check('retained φ removed → unresolved: missing retained dependency', out.sequence.dropRetained.composition.status === 'unresolved' && /retained dependency/.test(out.sequence.dropRetained.unresolved[0].missing), out.sequence.dropRetained.unresolved);
// kA by the general sweep (constant radius) against the closed form
const oA = {f: t => B.sphDist(ray.point, B.sphArc(B.gArcCentre(0), B.gArcCentre(1), t)) - 16, L: B.sphDist(B.gArcCentre(0), B.gArcCentre(1))};
const swA = B.sweepMin(oA.f, oA.L, {tol: 1e-9, budget: 20000}), closedA = B.sphArcDistClosed(ray.point, B.G_N, B.G_E, -.2, .2);
out.sequence.kAsweepVsClosed = {sweep: swA, closed: closedA};
check('kA: the general sweep agrees with the closed form', near(swA.upper, closedA.d - 16, 1e-6) && near(swA.t, closedA.t, 1e-4), out.sequence.kAsweepVsClosed);
// the source records' bytes through the sequence
out.sequence.recordHash = hash(B.G_RECORDS);

// ---- D. attack 3's sphere construction on G (×1)
const phi = Math.PI / 3, sk = [B.sphUnit(-.18, phi - .1), B.sphUnit(.18, phi + .1), B.sphUnit(-.18, phi + .1), B.sphUnit(.18, phi - .1)];
let crossing = B.v3norm(B.v3cross(B.v3cross(sk[0], sk[1]), B.v3cross(sk[2], sk[3]))); if (B.v3dot(crossing, B.sphUnit(0, phi)) < 0) crossing = crossing.map(v => -v);
const pressures = [.2, .8, .9, .5];
const sKnots = sk.map((p, i) => ({p, pressure: pressures[i]}));
out.sphereSweep = {
  intrinsic16: B.sphDist(B.sphUnit(0, phi), B.sphUnit(.08, phi)),
  candidates: B.classifySphereMark(sKnots, crossing, {decide: true, budget: 10001}),
  limited: B.sweepMin(B.sphSegmentObjective(B.sphUnit(-.2, 0), B.sphUnit(.2, 0), .5, .5, B.sphUnit(0, .045)).f, B.sphSegmentObjective(B.sphUnit(-.2, 0), B.sphUnit(.2, 0), .5, .5, B.sphUnit(0, .045)).L, {decide: true, budget: 1}),
  resolved: B.sweepMin(B.sphSegmentObjective(B.sphUnit(-.2, 0), B.sphUnit(.2, 0), .5, .5, B.sphUnit(0, .045)).f, B.sphSegmentObjective(B.sphUnit(-.2, 0), B.sphUnit(.2, 0), .5, .5, B.sphUnit(0, .045)).L, {decide: true, budget: 10001})
};
check('16 mm of chart at latitude 60° = 7.99839984003628 mm on the surface', near(out.sphereSweep.intrinsic16, 7.99839984003628, 1e-9), out.sphereSweep.intrinsic16);
const cands = out.sphereSweep.candidates;
check('crossing: segments 0 and 2 inside, witnesses −4.0906052517 / −7.6906052517', cands[0].status === 'inside' && cands[2].status === 'inside' && near(cands[0].upper, -4.0906052517, 1e-9) && near(cands[2].upper, -7.6906052517, 1e-9), cands);
check('segment 1 outside after 37 evaluations, enclosure [0.0448254734, 0.4524847667]', cands[1].status === 'outside' && cands[1].work === 37 && near(cands[1].lower, 0.0448254734, 1e-9) && near(cands[1].upper, 0.4524847667, 1e-9), cands[1]);
check('equatorial query: budget 1 → pending [−39.75, 0.25]', out.sphereSweep.limited.status === 'pending' && near(out.sphereSweep.limited.lower, -39.75, 1e-9) && near(out.sphereSweep.limited.upper, .25, 1e-9), out.sphereSweep.limited);
check('equatorial query: outside after 43, [0.0720365548, 0.25]', out.sphereSweep.resolved.status === 'outside' && out.sphereSweep.resolved.work === 43 && near(out.sphereSweep.resolved.lower, 0.0720365548, 1e-9), out.sphereSweep.resolved);

// ---- E. the pickup replay on the post (attack 3, Appendix A) through the bench's functions
const recordA = {id: 'paint-A', path: 'path-A', domain: [C - 120, 280, 240, 240], knots};
const recordB = {id: 'paint-B', path: 'path-B', domain: [C - 120, 410, 240, 110], knots: [{id: 'b0', u: C - 60, v: 440, p: .65}, {id: 'b1', u: C + 60, v: 440, p: .65}]};
const inputHash = hash({recordA, program: B.PICKUP_PROGRAM});
const ds = B.pickupDabs(knots), seedPaint = new Float32Array(96 * 96 * 4);
for (let i = 0; i < 96 * 96; i++) seedPaint.set([0, 0, 1, 1], 4 * i);
const bindings = B.pickupBindings(recordA, recordB);
const ctx = {dabs: ds, evals: [p0, p1, p2], bindings, seed: seedPaint, C, recordA};
const baseline = B.pickupReplay('baseline', ctx), sequence = B.pickupReplay('sequence', ctx), wrong = B.pickupReplay('reset', ctx);
let sb = new Float32Array(96 * 96 * 4), cb = [0, 1, 0, 1]; const programB = JSON.parse(JSON.stringify(B.PICKUP_PROGRAM)); programB[1].args[2] = 0;
for (const dab of B.pickupDabs(recordB.knots)) { const r = B.pickupStep(sb, cb, dab, recordB.domain, programB); sb = r.surface; cb = r.carry; }
const paintings = {'paint-A': sequence.surface, 'paint-B': sb};
out.pickup = {
  dabs: ds.length, source: inputHash, sourceAfter: hash({recordA, program: B.PICKUP_PROGRAM}),
  prefix: hash(sequence.prefix), baseline: hash(baseline.surface), sequence: hash(sequence.surface), reset: hash(wrong.surface),
  locationErrorMM: sequence.locationError, trace: sequence.trace,
  stripBefore: B.pickupAt(bindings, p1, [C, 400], false, null, paintings, recordA.domain),
  stripAfter: B.pickupAt(bindings, p2, [C, 400], true, ['paint-A', 'paint-B'], paintings, recordA.domain),
  overlap: B.pickupAt(bindings, p2, [C + 40, 440], true, ['paint-A', 'paint-B'], paintings, recordA.domain),
  unordered: B.pickupAt(bindings, p2, [C + 40, 440], true, null, paintings, recordA.domain),
  statefulRead: {
    held: B.pickupConsume({status: 'pending', dependency: ['support-P', 'P2', 'paint-B', 'binding-B']}, [1, 0, 0, 1]),
    complete: B.pickupConsume({status: 'resolved', color: B.over([0, 0, .5, .5], [.5, 0, 0, .5])}, [1, 0, 0, 1]),
    missing: B.pickupConsume({status: 'resolved', color: [.5, 0, 0, .5]}, [1, 0, 0, 1])
  }
};
check('155 dabs', ds.length === 155, ds.length);
check('source hash 86f34ffa… before and after', inputHash === '86f34ffa3b37d71c64cc758344974fafc18c66d64f60d04935a72a95f52c3337' && out.pickup.sourceAfter === inputHash, inputHash);
check('prefix 444d4449…', out.pickup.prefix === '444d444956c226030d2aface3e7caadf8f1200b54a1c9c7a0d8c08a0c1c5f36e', out.pickup.prefix);
check('final painting 1b7dce5c…, baseline = sequence', out.pickup.sequence === '1b7dce5c1ef1d5f70d13f30a4a9c863b9c08150ce89f8cdb5df8c553191533f1' && out.pickup.baseline === out.pickup.sequence, [out.pickup.baseline, out.pickup.sequence]);
check('reset control 3eed724f…', out.pickup.reset === '3eed724f663b0187598d10471e1c161d82bfe348ce0fae6f14449e243ab20108', out.pickup.reset);
check('fresh-hit location error 0', sequence.locationError === 0, sequence.locationError);
check('strip: absent before, A after with (0.003862565, 0, 0.996137440, 1)', out.pickup.stripBefore.status === 'absent-support' && out.pickup.stripAfter.ids.length === 1 && near(out.pickup.stripAfter.color[0], 0.003862565, 1e-8) && near(out.pickup.stripAfter.color[2], 0.996137440, 1e-8), [out.pickup.stripBefore, out.pickup.stripAfter]);
check('overlap B over A (0.000136339, 0.984375, 0.015488661, 1)', out.pickup.overlap.ids.length === 2 && near(out.pickup.overlap.color[0], 0.000136339, 1e-8) && near(out.pickup.overlap.color[1], 0.984375, 1e-8) && near(out.pickup.overlap.color[2], 0.015488661, 1e-8), out.pickup.overlap);
check('no order → needs-policy', out.pickup.unordered.status === 'needs-policy', out.pickup.unordered);
check('stateful read: held has no next carry; complete (0.8125, 0, 0.125, 0.9375); missing (0.875, 0, 0, 0.875)', !('nextCarry' in out.pickup.statefulRead.held) && near(out.pickup.statefulRead.complete.nextCarry[0], .8125) && near(out.pickup.statefulRead.complete.nextCarry[2], .125) && near(out.pickup.statefulRead.missing.nextCarry[0], .875), out.pickup.statefulRead);

// ---- F. custody of the embedded source (attack 4 §2): only when the bench embeds the block
if (loaded.custody.embedded) check('custody: every sidecar unit is embedded verbatim (' + loaded.custody.compared + ' units)', loaded.custody.mismatches.length === 0, loaded.custody.mismatches);

out.checks = checks;
const failed = checks.filter(c => !c.ok).length;
const dump = process.argv.includes('--dump');
if (dump) console.log(JSON.stringify(out, (k, v) => typeof v === 'function' ? undefined : (v === Infinity ? 'Infinity' : v), 1));
else console.log(JSON.stringify({bench: benchHash, route: loaded.route, custody: loaded.custody, checks: checks.map(c => (c.ok ? 'ok   ' : 'FAIL ') + c.name), failed}, null, 1));
process.exitCode = failed ? 1 : 0;
