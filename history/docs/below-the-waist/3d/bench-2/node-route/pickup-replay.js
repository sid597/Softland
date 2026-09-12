// The pickup brush's replay on the post (the definer's attack 3, Appendix A, carried onto the bench).
// Plain script, no exports; the bench embeds it beside sweep-and-sphere.js. Uses the bench's own
// postWorld, postChart, postPreimages, hitPost, markWidth, over (they must be in scope) and a `PICKUP_N` texel count.
//
// A painting is a logical 96 × 96 Float32 RGBA value over the authored patch [C−120, C+120] × [280, 520] mm
// (premultiplied, linear light), independent of the renderer's chart. The program is saved data: read the previous
// painting at the dab centre; mix that colour into the carried pigment (amount .25); construct the dab's disc;
// source-over the carried colour at deposition opacity .5; return the new painting and the carry. The order and
// wiring live in the record; the interpreter below executes named operations over references.
var PICKUP_N = 96;
var PICKUP_PROGRAM = [
  {id: 'picked', op: 'sample', args: [{ref: 'surface'}, {ref: 'dab.center'}, {ref: 'domain'}]},
  {id: 'carryOut', op: 'mix', args: [{ref: 'carry'}, {ref: 'picked'}, 0.25]},
  {id: 'footprint', op: 'disk', args: [{ref: 'dab.center'}, {ref: 'dab.radius'}]},
  {id: 'surfaceOut', op: 'over-region', args: [{ref: 'surface'}, {ref: 'footprint'}, {ref: 'carryOut'}, 0.5, {ref: 'domain'}]}
];
function pickupMix(a, b, t) { return a.map(function (v, i) { return v * (1 - t) + b[i] * t; }); }
function pickupSample(a, q, d) { // nearest texel of the logical painting a over domain d = [u0, v0, w, h]
  var N = PICKUP_N;
  var x = Math.max(0, Math.min(N - 1, Math.floor((q[0] - d[0]) / d[2] * N))), y = Math.max(0, Math.min(N - 1, Math.floor((q[1] - d[1]) / d[3] * N)));
  return Array.from(a.slice(4 * (y * N + x), 4 * (y * N + x) + 4));
}
var PICKUP_OPS = {
  sample: pickupSample,
  mix: pickupMix,
  disk: function (center, radius) { return {center: center, radius: radius}; },
  'over-region': function (a, f, color, alpha, d) {
    var N = PICKUP_N, out = a.slice(), top = color.map(function (x) { return x * alpha; }), x, y, q, i;
    for (y = 0; y < N; y++) for (x = 0; x < N; x++) {
      q = [d[0] + (x + 0.5) * d[2] / N, d[1] + (y + 0.5) * d[3] / N];
      if (Math.hypot(q[0] - f.center[0], q[1] - f.center[1]) <= f.radius) { i = 4 * (y * N + x); out.set(over(top, Array.from(a.slice(i, i + 4))), i); }
    }
    return out;
  }
};
function pickupStep(surface, carry, dab, domain, prog) {
  var e = {surface: surface, carry: carry, domain: domain, 'dab.center': dab.q, 'dab.radius': markWidth(dab.p) / 2}, n, k;
  for (k = 0; k < prog.length; k++) { n = prog[k]; e[n.id] = PICKUP_OPS[n.op].apply(null, n.args.map(function (a) { return a && a.ref ? e[a.ref] : a; })); }
  return {surface: e.surfaceOut, carry: e.carryOut};
}
// Divide each straight segment into intervals no longer than 4 mm; retain the source segment and parameter on each
// emitted dab; shared knots emitted once (155 dabs for the bench's knots).
function pickupDabs(ks) {
  var out = [], i, a, b, n, j, t;
  for (i = 0; i + 1 < ks.length; i++) {
    a = ks[i]; b = ks[i + 1]; n = Math.ceil(Math.hypot(b.u - a.u, b.v - a.v) / 4);
    for (j = i ? 1 : 0; j <= n; j++) { t = j / n; out.push({q: [a.u + (b.u - a.u) * t, a.v + (b.v - a.v) * t], p: a.p + (b.p - a.p) * t, source: [a.id, b.id, t]}); }
  }
  return out;
}
// A fresh ray at an authored point: hit the wall from just outside along the inward normal, read the current chart,
// invert to the authored domain (one preimage for a domain narrower than C).
function pickupFreshLocation(ev, q, domain) {
  var P = ev.support, R = P.R, w = postWorld(P, q[0], q[1]);
  var normal = [Math.cos(q[0] / R), 0, Math.sin(q[0] / R)];
  var hit = hitPost({origin: w.map(function (v, i) { return v + 0.03 * normal[i]; }), dir: normal.map(function (v) { return -v; })}, P);
  var ch = postChart(P, hit.point);
  var pre = postPreimages(P, {u0: domain[0], u1: domain[0] + domain[2], v0: domain[1], v1: domain[1] + domain[3]}, ch.a, ch.b);
  if (pre.length !== 1) throw new Error('pickup: expected one preimage, got ' + pre.length);
  return [pre[0].u, pre[0].v];
}
// The bindings of the two paintings. A is authored on the whole wall (P0); the wall is then shown trimmed into two
// bands (v ≤ 390 and v ≥ 410) and A shown by restriction; B is authored on the upper band; the strip is restored at
// the merge. A's rule permits restoring its older whole-wall attachment through the unchanged generator, which is
// why A can appear in the strip; B retains its band restriction and gains no history there.
function pickupBindings(recordA, recordB) {
  return [
    {id: 'binding-A', record: recordA, from: 'P0.whole-wall', to: 'P2.wall', rule: 'retain-source; restore ancestor', before: [[280, 390], [410, 520]], after: [[280, 520]]},
    {id: 'binding-B', record: recordB, from: 'P1-cut.upper', to: 'P2.wall', rule: 'retain restriction', before: [[410, 520]], after: [[410, 520]]}
  ];
}
function pickupResolve(bindings, ev, q, merged, domainA) {
  if (!merged && q[1] > 390 && q[1] < 410) return {status: 'absent-support', revision: ev.revision, ids: []};
  var location = pickupFreshLocation(ev, q, domainA);
  var selected = bindings.filter(function (b) { return b[merged ? 'after' : 'before'].some(function (iv) { return q[1] >= iv[0] && q[1] <= iv[1]; }); });
  return {status: 'resolved', revision: ev.revision, location: location, ids: selected.map(function (b) { return b.record.id; })};
}
// pickupReplay(mode, ctx): mode 'baseline' (no rechart, no merge), 'sequence' (rechart before dab 20, restore the
// strip before dab 80), 'reset' (the negative control: the carry reset at the rechart). ctx = {dabs, knotsRecord,
// evals: [p0, p1, p2], bindings, seed, stopAt?}. Returns {surface, carry, prefix (the painting before dab 20),
// merged, locationError, trace}.
function pickupReplay(mode, ctx) {
  var N = PICKUP_N, surface = ctx.seed.slice(), carry = [1, 0, 0, 1], ev = ctx.evals[0], merged = false, prefix = null, locationError = 0, trace = [], i, q, r, ds = ctx.dabs;
  var stopAt = ctx.stopAt != null ? ctx.stopAt : ds.length;
  for (i = 0; i < ds.length && i < stopAt; i++) {
    if (i === 20) { prefix = surface.slice(); if (mode !== 'baseline') ev = ctx.evals[1]; if (mode === 'reset') carry = [1, 0, 0, 1]; }
    if (i === 80 && mode !== 'baseline') { merged = true; ev = ctx.evals[2]; }
    if (i === 20 || i === 80) trace.push({beforeDab: i, strip: pickupResolve(ctx.bindings, ev, [ctx.C, 400], merged, ctx.recordA.domain), overlap: pickupResolve(ctx.bindings, ev, [ctx.C + 40, 440], merged, ctx.recordA.domain)});
    q = pickupFreshLocation(ev, ds[i].q, ctx.recordA.domain);
    locationError = Math.max(locationError, Math.hypot(q[0] - ds[i].q[0], q[1] - ds[i].q[1]));
    r = pickupStep(surface, carry, ds[i], ctx.recordA.domain, PICKUP_PROGRAM); surface = r.surface; carry = r.carry;
  }
  return {surface: surface, carry: carry, prefix: prefix, merged: merged, locationError: locationError, trace: trace, ev: ev, dabsDone: i};
}
// The composed reading at an authored point after the sequence: which records bind there (by band), each sampled in
// its own painting; the saved order composes, no order → needs-policy.
function pickupAt(bindings, ev, q, merged, order, paintings, domainA) {
  var resolved = pickupResolve(bindings, ev, q, merged, domainA);
  if (resolved.status !== 'resolved') return resolved;
  var loc = resolved.location;
  var layers = bindings.filter(function (b) { return b[merged ? 'after' : 'before'].some(function (iv) { return q[1] >= iv[0] && q[1] <= iv[1]; }); })
    .map(function (b) { return {id: b.record.id, binding: b.id, color: pickupSample(paintings[b.record.id], loc, b.record.domain)}; });
  if (layers.length > 1 && !order) return {status: 'needs-policy', ids: layers.map(function (x) { return x.id; }), location: loc};
  var color = [0, 0, 0, 0], k, l;
  for (k = 0; k < (order || ['paint-A']).length; k++) { l = layers.filter(function (x) { return x.id === (order || ['paint-A'])[k]; })[0]; if (l) color = over(l.color, color); }
  return {status: 'resolved', ids: layers.map(function (x) { return x.id; }), location: loc, color: color};
}
// The stateful read (attack 3): a brush that reads the composed coating cannot take a pending binding as
// transparent; pending returns no next carry.
function pickupConsume(read, oldCarry) {
  return read.status === 'resolved' ? {status: 'resolved', nextCarry: pickupMix(oldCarry, read.color, 0.25)} : {status: 'pending', dependency: read.dependency};
}
