// The executor over the bench's records (the fold of the definer's attack 4, 2026-09-06): the piece bench 9 ran in
// sixty lines, here over the sphere G's capabilities. Plain script, no exports; the bench embeds it verbatim after
// pickup-replay.js and probe-4.mjs evaluates it in Node beside the bench's functions. Style follows the bench (ES5).
//
// A record is data. Its program is named steps over bindings: each step is an operation from the capability table
// X_OPS with its arguments as references ('state.carry', 'event.t', 'tool.radius', a previous step's `out`) or
// literals; `each` iterates the record's authored events with a state carried through `next`; `return` names what
// the run hands back. The executor moves values and keeps the order; every heavy thing is a capability, and the
// capabilities are the bench's own functions (reachRegion, gArcCentre, gLocate, over, pickupMix, sphDist).
//
// What bench 9 did not have. A read can come back pending (a binding withheld: gLocate with the work budget 0) or
// needs-policy (no composition order saved). A step consumes only a resolved read. A pending read suspends the run
// into a continuation that retains the event, the state before it, the record's hash and the read's pinned
// snapshot; nothing else moves (no next painting-step state). Resuming re-demands that read and commits only an
// answer that belongs to that request: a changed dependency (the order, the recipe, the input state) starts a new
// computation and an old answer is stale. The continuation is bytes: it survives a new process (--cold).
//
// The roots a reference may start from: state · event (the item of `each`) · events · tool · support · coating ·
// painting · inputs (values another construction returned, handed in by the caller with their subject checked).
// Every value a capability returns keeps its subject (a region owns a copy of its record; a painting carries its
// declaration, revision and bytes; a read carries its snapshot's id).

// ---------------------------------------------------------------------------------------------------------- 0
// A synchronous SHA-256 (FIPS 180-4) over bytes, so request, snapshot and painting identities are the same hex in
// the browser and in Node; strings are UTF-8 encoded first. The bench's earlier receipts use crypto.subtle for the
// replay's paintings; those stay as they are.
var X_K = [];
(function () {
  var i, j, n = 2, isPrime;
  while (X_K.length < 64) {
    isPrime = true; for (j = 2; j * j <= n; j++) if (n % j === 0) { isPrime = false; break; }
    if (isPrime) X_K.push(Math.floor(Math.pow(n, 1 / 3) % 1 * 4294967296) >>> 0);
    n++;
  }
})();
function xUtf8(s) {
  var out = [], i, c;
  for (i = 0; i < s.length; i++) {
    c = s.charCodeAt(i);
    if (c < 128) out.push(c);
    else if (c < 2048) out.push(192 | (c >> 6), 128 | (c & 63));
    else if (c >= 0xd800 && c < 0xdc00 && i + 1 < s.length) { c = 0x10000 + ((c - 0xd800) << 10) + (s.charCodeAt(++i) - 0xdc00); out.push(240 | (c >> 18), 128 | ((c >> 12) & 63), 128 | ((c >> 6) & 63), 128 | (c & 63)); }
    else out.push(224 | (c >> 12), 128 | ((c >> 6) & 63), 128 | (c & 63));
  }
  return new Uint8Array(out);
}
function xSha256(input) {
  var bytes = typeof input === 'string' ? xUtf8(input) : input instanceof Uint8Array ? input : new Uint8Array(input.buffer, input.byteOffset, input.byteLength);
  var H = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19];
  var l = bytes.length, padded = new Uint8Array(((l + 9 + 63) >> 6) << 6), i, j, w = new Array(64), a, b, c, d, e, f, g, h, t1, t2, s0, s1;
  padded.set(bytes); padded[l] = 0x80;
  var bits = l * 8; padded[padded.length - 4] = (bits >>> 24) & 255; padded[padded.length - 3] = (bits >>> 16) & 255; padded[padded.length - 2] = (bits >>> 8) & 255; padded[padded.length - 1] = bits & 255;
  padded[padded.length - 5] = Math.floor(bits / 4294967296) & 255;
  var rotr = function (x, n) { return (x >>> n) | (x << (32 - n)); };
  for (i = 0; i < padded.length; i += 64) {
    for (j = 0; j < 16; j++) w[j] = (padded[i + 4 * j] << 24) | (padded[i + 4 * j + 1] << 16) | (padded[i + 4 * j + 2] << 8) | padded[i + 4 * j + 3];
    for (j = 16; j < 64; j++) { s0 = rotr(w[j - 15], 7) ^ rotr(w[j - 15], 18) ^ (w[j - 15] >>> 3); s1 = rotr(w[j - 2], 17) ^ rotr(w[j - 2], 19) ^ (w[j - 2] >>> 10); w[j] = (w[j - 16] + s0 + w[j - 7] + s1) >>> 0; }
    a = H[0]; b = H[1]; c = H[2]; d = H[3]; e = H[4]; f = H[5]; g = H[6]; h = H[7];
    for (j = 0; j < 64; j++) {
      s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25); t1 = (h + s1 + ((e & f) ^ (~e & g)) + X_K[j] + w[j]) >>> 0;
      s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22); t2 = (s0 + ((a & b) ^ (a & c) ^ (b & c))) >>> 0;
      h = g; g = f; f = e; e = (d + t1) >>> 0; d = c; c = b; b = a; a = (t1 + t2) >>> 0;
    }
    H[0] = (H[0] + a) >>> 0; H[1] = (H[1] + b) >>> 0; H[2] = (H[2] + c) >>> 0; H[3] = (H[3] + d) >>> 0; H[4] = (H[4] + e) >>> 0; H[5] = (H[5] + f) >>> 0; H[6] = (H[6] + g) >>> 0; H[7] = (H[7] + h) >>> 0;
  }
  var hex = '';
  for (i = 0; i < 8; i++) hex += ('00000000' + H[i].toString(16)).slice(-8);
  return hex;
}
function xHash(value) { return xSha256(typeof value === 'string' ? value : JSON.stringify(value)); }
function xCopy(value) { return JSON.parse(JSON.stringify(value)); }
// Float32 paintings as bytes: little-endian Float32 → base64 (the continuation's wire form) and back.
function xFloat32ToBase64(a) {
  var bytes = new Uint8Array(a.buffer, a.byteOffset, a.byteLength), s = '', i;
  for (i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
  return typeof btoa === 'function' ? btoa(s) : Buffer.from(s, 'binary').toString('base64');
}
function xBase64ToFloat32(text) {
  var s = typeof atob === 'function' ? atob(text) : Buffer.from(text, 'base64').toString('binary'), bytes = new Uint8Array(s.length), i;
  for (i = 0; i < s.length; i++) bytes[i] = s.charCodeAt(i);
  return new Float32Array(bytes.buffer);
}
function xPaintingSha256(p) { return xSha256(new Uint8Array(p.data.buffer, p.data.byteOffset, p.data.byteLength)); }

// ---------------------------------------------------------------------------------------------------------- 1
// Values. A painting is a logical Float32 RGBA grid over an authored patch of G's chart S0 (u = R·longitude on the
// authored branch, v = R·latitude), premultiplied, linear light; the same value the definer's attack 4 paints.
function xMakePainting(decl) {
  var n = decl.grid[0] * decl.grid[1], data = new Float32Array(4 * n), i, c = decl.initial || [0, 0, 0, 0];
  if (c[0] || c[1] || c[2] || c[3]) for (i = 0; i < n; i++) data.set(c, 4 * i);
  return {kind: 'painting', id: decl.id, revision: 0, grid: decl.grid.slice(), domain: decl.domain.slice(), chart: decl.chart || 'S0', filter: decl.filter || 'nearest', data: data};
}
function xPaintingRef(p) { return {painting: p.id, revision: p.revision, sha256: xPaintingSha256(p)}; }
function xIsPainting(v) { return !!(v && typeof v === 'object' && v.kind === 'painting'); }
function xIsRegion(v) { return !!(v && typeof v === 'object' && v.kind === 'surface-region'); }
// the nearest logical texel of painting p at the unit vector q (the declared filter; the definer's read)
function xSample(p, q, filter) {
  if ((filter || p.filter) !== 'nearest') return {status: 'unsupported', reason: 'filter ' + filter + ': only nearest is declared'};
  var d = p.domain, nx = p.grid[0], ny = p.grid[1], u = gUnwrapU(q), v = G_R * sphLat(q);
  if (!(u >= d[0] && u < d[0] + d[2] && v >= d[1] && v < d[1] + d[3])) return {status: 'unsupported', reason: 'read outside the authored patch (u ' + u.toFixed(2) + ', v ' + v.toFixed(2) + ')'};
  var x = Math.floor((u - d[0]) / d[2] * nx), y = Math.floor((v - d[1]) / d[3] * ny);
  return {status: 'resolved', color: Array.from(p.data.slice(4 * (y * nx + x), 4 * (y * nx + x) + 4)), texel: [x, y]};
}
// the texel centres of painting p, as unit vectors: y outer, x inner
function xTexelCentre(p, x, y) { var d = p.domain; return sphPoint0(d[0] + (x + 0.5) * d[2] / p.grid[0], d[1] + (y + 0.5) * d[3] / p.grid[1]); }

// ---------------------------------------------------------------------------------------------------------- 2
// The capability table. Each operation declares the argument names it consumes; a step's other keys are reported as
// not consumed. `run(args, ctx)` returns the value that binds to the step's `out`. A read also declares `snapshot`,
// the inputs it pins, computed before it runs: the request a suspended read leaves behind names that snapshot, and an
// answer delivered later is checked against it without running anything.
var X_OPS = {
  // the point a curve of the retained records names at parameter t (an authored address, not a pointer sample)
  'curve-point': {args: ['curve', 't'], run: function (a) {
    if (a.curve !== 'kA/arc-AB') return {status: 'unsupported', reason: 'curve ' + a.curve + ': only kA/arc-AB is declared'};
    return gArcCentre(Number(a.t));
  }},
  // a region of surface distance on the support: the reach tool's result and the brush's footprint are one operation
  'surface-region': {args: ['support', 'seed', 'point', 'radius', 'distance'], run: function (a) {
    if (a.support && a.support.id !== 'G') return {status: 'unsupported', reason: 'support ' + a.support.id + ': only G (the sphere, R 200 mm) is declared'};
    if ((a.distance || 'surface') !== 'surface') return {status: 'unsupported', reason: 'distance ' + a.distance + ': only surface (intrinsic, the great circle) is declared'};
    if (!a.point && !a.seed) return {status: 'unsupported', reason: 'surface-region: a seed [longitude, latitude] rad or a point (unit vector) is needed'};
    var rec = {support: a.support ? {id: a.support.id, revision: a.support.revision} : {id: 'G'}, radius: Number(a.radius), distance: 'surface'};
    if (a.point) rec.point = Array.from(a.point); else rec.seed = Array.from(a.seed);
    var r = reachRegion(rec);
    if (r.status !== 'resolved') return r;
    r.kind = 'surface-region'; r.id = xHash(rec); r.pieces = xRegionPieces(r);
    return r;
  }},
  // membership of a point in a retained region, by the region's own distance
  'member': {args: ['region', 'point'], run: function (a) {
    if (!xIsRegion(a.region)) return {status: 'unsupported', reason: 'member: not a surface region'};
    return {status: 'resolved', inside: a.region.member(a.point), distance: a.region.distance(a.point), radius: a.region.record.radius};
  }},
  // the compositor's sample through the bindings: the layers named bottom to top, each 'coating' (the retained
  // records on G at the support's revision, inverted through their bindings, composed by the saved order) or a
  // painting; a withheld binding returns pending with the dependency named, no order returns needs-policy, and
  // neither is a colour; pending: 'provisional' is the declared exception (the known layers consumed, the guess retained)
  'read-surface': {args: ['support', 'revision', 'point', 'layers', 'order', 'budget', 'filter', 'pending'],
    snapshot: function (a, ctx) {
      var rev = xRevision(a), order = xOrder(a, ctx), layers = a.layers || ['coating'];
      return {support: {id: 'G', revision: rev}, coating: {records: xHash(G_RECORDS), order: order, chains: gBindings(rev).map(function (b) { return {binding: b.id, chain: b.chain}; })},
        layers: layers.map(function (l) { return xIsPainting(l) ? xPaintingRef(l) : l; }), read: {quantity: 'coating rgba', filter: a.filter || 'nearest', point: Array.from(a.point)}};
    },
    run: function (a, ctx) {
      var rev = xRevision(a), order = xOrder(a, ctx), layers = a.layers || ['coating'], budget = a.budget != null ? Number(a.budget) : ctx.budget;
      var snapId = ctx.snapshot, acc = [0, 0, 0, 0], contributors = [], i, l, s;
      for (i = 0; i < layers.length; i++) {
        l = layers[i];
        if (l === 'coating') {
          var located = gLocate(rev, a.point, {budget: budget, order: order}), comp = located.composition;
          if (comp.status === 'unresolved') {
            if ((a.pending || 'wait') !== 'provisional') return {status: 'pending', snapshot: snapId, missing: xCopy(located.unresolved), known: comp.known.slice()};
            var guess = [0, 0, 0, 0], j, m;                      // the known layers in the saved order, the missing ones transparent
            for (j = 0; j < (order || ['kA']).length; j++) if (comp.known.indexOf(order[j]) >= 0) { m = G_RECORDS[order[j] === 'kA' ? 'A' : 'B'].mark.rgba; guess = over(m, guess); }
            acc = over(guess, acc); contributors = contributors.concat(comp.known);
            var provisional = {missing: xCopy(located.unresolved), interpretation: 'missing layers guessed transparent (a declared provisional read)'};
            continue;
          }
          if (comp.status === 'needs-policy') return {status: 'needs-policy', snapshot: snapId, candidates: comp.contributors.slice()};
          acc = over(comp.rgba, acc); contributors = contributors.concat(comp.contributors);
        } else if (xIsPainting(l)) {
          s = xSample(l, a.point, a.filter);
          if (s.status !== 'resolved') return {status: 'unsupported', snapshot: snapId, reason: s.reason};
          acc = over(s.color, acc); contributors.push(l.id + '@' + l.revision);
        } else return {status: 'unsupported', snapshot: snapId, reason: 'layer ' + JSON.stringify(l) + ': a layer is "coating" or a painting'};
      }
      var out = {status: 'resolved', snapshot: snapId, color: acc, contributors: contributors};
      if (typeof provisional !== 'undefined') out.provisional = provisional;
      return out;
    }},
  // linear mix of two colours (the bench's pickupMix, the definer's arithmetic)
  'mix': {args: ['a', 'b', 'amount'], run: function (a) { return pickupMix(a.a, a.b, Number(a.amount)); }},
  // source-over deposit of rgba × opacity on the painting where the region (and the clip, if given) holds the texel
  // centre; a new painting value with the next revision; the input painting is not changed
  'paint': {args: ['painting', 'region', 'rgba', 'opacity', 'clip'], run: function (a) {
    if (!xIsPainting(a.painting)) return {status: 'unsupported', reason: 'paint: not a painting'};
    if (!xIsRegion(a.region)) return {status: 'unsupported', reason: 'paint: the region is not a surface region'};
    if (a.clip && !xIsRegion(a.clip)) return {status: 'unsupported', reason: 'paint: the clip is not a surface region'};
    var p = a.painting, out = p.data.slice(), top = a.rgba.map(function (x) { return x * Number(a.opacity == null ? 1 : a.opacity); }), changed = 0, x, y, q, i;
    for (y = 0; y < p.grid[1]; y++) for (x = 0; x < p.grid[0]; x++) {
      q = xTexelCentre(p, x, y);
      if (a.region.member(q) && (!a.clip || a.clip.member(q))) { i = 4 * (y * p.grid[0] + x); out.set(over(top, Array.from(p.data.slice(i, i + 4))), i); changed++; }
    }
    return {kind: 'painting', id: p.id, revision: p.revision + 1, grid: p.grid, domain: p.domain, chart: p.chart, filter: p.filter, data: out, changed: changed, clip: a.clip ? a.clip.id : null};
  }},
  // the painting's texel at a point (the declared filter)
  'sample': {args: ['painting', 'point', 'filter'], run: function (a) { return xIsPainting(a.painting) ? xSample(a.painting, a.point, a.filter) : {status: 'unsupported', reason: 'sample: not a painting'}; }}
};
// The chart pieces a cap meets (the region's, not its seed's; the cold mind found the seed's piece said nothing about
// a region straddling the cut): north when the cap reaches above latitude 70°; west and east by the longitudes the cap
// spans, every longitude once a pole is inside it.
function xRegionPieces(r) {
  var lat = sphLat(r.seed), lon = sphLon(r.seed), rho = r.rho, out = [], west, east;
  if (lat + rho >= G_NORTH_LAT) out.push('north');
  if (rho >= Math.PI / 2 - Math.abs(lat) - 1e-12) { west = east = true; }
  else {
    var half = Math.asin(Math.min(1, Math.sin(rho) / Math.cos(lat))), lo = lon - half, hi = lon + half;   // the longitudes spanned, about lon
    var spans = function (a, b) { var x; for (x = lo - 2 * Math.PI; x <= hi + 2 * Math.PI; x += 2 * Math.PI) { if (Math.max(x, a) < Math.min(x + (hi - lo), b)) return true; } return false; };
    west = spans(-Math.PI, 0); east = spans(0, Math.PI);
  }
  if (lat - rho < G_NORTH_LAT) { if (west) out.push('west'); if (east) out.push('east'); }
  return out;
}
function xRevision(a) { return a.revision != null ? Number(a.revision) : a.support && a.support.revision != null ? Number(a.support.revision) : 0; }
function xOrder(a, ctx) { return a.order === undefined ? (ctx.record.coating ? ctx.record.coating.order : ['kA', 'kB']) : a.order; }
var X_ROOTS = ['state', 'event', 'events', 'tool', 'support', 'coating', 'painting', 'inputs', 'host'];
// The host root: what G already holds, so a record can refer to it instead of retyping the number (the cold mind's
// finding: the reach seed is A@0's own seed). Read-only facts of the retained records and the sphere.
function xHost() {
  var A = G_RECORDS.A.mark, B = G_RECORDS.B.mark;
  return {id: 'G', R: G_R, C: G_C, piR: G_PI_R, revisions: [0, 1, 2], charts: G_CHARTS.map(function (c) { return c.name; }),
    A: {id: G_RECORDS.A.id, mark: A.id, curve: 'kA/arc-AB', seed: [sphLon(G_N), sphLat(G_N)], radius: A.radius, rgba: A.rgba.slice(), lengthMM: 80, domain: JSON.parse(JSON.stringify(G_RECORDS.A.domain))},
    B: {id: G_RECORDS.B.id, mark: B.id, at: B.at, point: gArcCentre(B.at), radius: B.radius, rgba: B.rgba.slice(), domain: JSON.parse(JSON.stringify(G_RECORDS.B.domain))}};
}
var X_CAPABILITIES = 'seam bench, the fold of attack 4: curve-point kA/arc-AB · surface-region on G by intrinsic distance · member · read-surface = coating through the retained bindings (gLocate) and paintings, bottom to top, premultiplied source-over, nearest · mix linear · paint source-over × opacity with an optional clip · sample nearest';

// ---------------------------------------------------------------------------------------------------------- 3
// Running a record. xRun(record, opts) → {status: 'complete' | 'pending' | 'needs-policy' | 'stale' | 'unsupported'
// | 'error', at, state, history, results, log, continuation?}.
//   opts.budget   the work granted to reads (default Infinity: every binding resolved)
//   opts.inputs   values other constructions returned, by the names the record's `inputs` declares (subject checked)
//   opts.from     a continuation (or a checkpoint from xCheckpoint) to resume from
//   opts.answer   {request, value}: a read's answer delivered for the request the run reaches; it must belong to it
//   opts.until    stop before that event (a prefix)
function xRecipeHash(record) { var r = xCopy(record); delete r.events; delete r.id; return xHash(r); }
function xStateSnapshot(state) { var out = {}, k; for (k in state) out[k] = xIsPainting(state[k]) ? xPaintingRef(state[k]) : state[k]; return out; }
function xStateHash(at, state, history) { return xHash({at: at, state: xStateSnapshot(state), history: history}); }
function xResolve(ref, scope) {
  if (typeof ref !== 'string') return ref;
  var parts = ref.split('.'), root = parts[0], cur, i;
  if (root === 'painting' && parts[1] === 'initial' && scope.painting) return xMakePainting(scope.painting);
  if (!(root in scope)) return ref;                                    // not a binding: a literal word
  cur = scope[root];
  for (i = 1; i < parts.length; i++) { if (cur == null) return undefined; cur = cur[parts[i]]; }
  return cur;
}
function xArgs(st, op, scope) {
  var args = {}, i, name;
  for (i = 0; i < op.args.length; i++) { name = op.args[i]; if (name in st) args[name] = name === 'layers' && Array.isArray(st[name]) ? st[name].map(function (r) { return r === 'coating' ? r : xResolve(r, scope); }) : xResolve(st[name], scope); }   // in `layers` the word coating names the coating itself, not the record's declaration of it
  return args;
}
function xSerializeState(state) {
  var out = {}, k;
  for (k in state) out[k] = xIsPainting(state[k]) ? {painting: state[k].id, revision: state[k].revision, grid: state[k].grid, domain: state[k].domain, chart: state[k].chart, filter: state[k].filter, sha256: xPaintingSha256(state[k]), encoding: 'Float32LE/base64', bytes: xFloat32ToBase64(state[k].data)} : state[k];
  return out;
}
function xRestoreState(saved) {
  var out = {}, k, v;
  for (k in saved) { v = saved[k]; out[k] = v && typeof v === 'object' && v.encoding === 'Float32LE/base64' ? {kind: 'painting', id: v.painting, revision: v.revision, grid: v.grid, domain: v.domain, chart: v.chart, filter: v.filter, data: xBase64ToFloat32(v.bytes)} : v; }
  return out;
}
// A checkpoint of a run (complete or suspended): what a later run resumes from, as bytes.
function xCheckpoint(run, record) {
  return {schema: 'seam-bench-executor-checkpoint/v1', recipe: xRecipeHash(record), input: {at: run.at, state: xSerializeState(run.state), history: xCopy(run.history)}, inputState: xStateHash(run.at, run.state, run.history)};
}
function xScope(base, state, item, outputs) {
  var s = {}, k; for (k in base) s[k] = base[k]; s.state = state; if (item !== null) s.event = item; for (k in outputs || {}) s[k] = outputs[k]; return s;
}
function xRun(record, opts) {
  opts = opts || {};
  var budget = opts.budget != null ? opts.budget : Infinity, program = record.program || {}, steps = program.steps || [], log = [], history = [], results = {};
  var recipe = xRecipeHash(record), inputs = opts.inputs || {}, answer = opts.answer || null, k, i;
  for (k in record.inputs || {}) {                                     // a declared input must be the subject the record names
    var want = record.inputs[k], got = inputs[k];
    if (got === undefined) return {status: 'unsupported', reason: 'input ' + k + ' not supplied'};
    if (want && want.sha256 && (!xIsPainting(got) || xPaintingSha256(got) !== want.sha256)) return {status: 'stale', reason: 'input ' + k + ': not the painting the record names (' + String(want.sha256).slice(0, 8) + '…)'};
    if (want && want.id && !want.sha256 && (!got || got.id !== want.id)) return {status: 'stale', reason: 'input ' + k + ': not the declared subject (' + String(want.id).slice(0, 8) + '…, got ' + String(got && got.id).slice(0, 8) + '…)'};
  }
  var base = {tool: record.tool || {}, support: record.support || {id: 'G', revision: 0}, coating: record.coating || null, painting: record.painting || null, events: record.events || [], inputs: inputs, host: xHost()};
  var state = {}, at = 0;
  if (opts.from) {
    var c = opts.from;
    if (c.recipe !== recipe) return {status: 'stale', reason: 'suspended under another recipe (' + c.recipe.slice(0, 8) + '…, now ' + recipe.slice(0, 8) + '…)'};
    state = xRestoreState(c.input.state); at = c.input.at; history = xCopy(c.input.history || []);
    var expect = c.request ? c.request.inputState : c.inputState;
    if (xStateHash(at, state, history) !== expect) return {status: 'stale', reason: 'the restored input state does not hash to the one retained'};
  } else for (k in program.state || {}) state[k] = xResolve(program.state[k], base);
  var items = program.each ? (xResolve(program.each, base) || []) : [null], end = typeof opts.until === 'number' ? Math.min(items.length, opts.until) : items.length, single = !program.each;
  for (; at < end; at++) {
    var item = items[at], outputs = {}, inputState = xStateHash(at, state, history), s, st, op, args, value, extra;
    for (s = 0; s < steps.length; s++) {
      st = steps[s]; op = X_OPS[st.op];
      if (!op) { log.push({at: at, step: st.out, op: st.op, error: 'unknown operation'}); return xFail('unsupported', 'unknown operation ' + st.op + ' (the table: ' + Object.keys(X_OPS).join(', ') + ')', at, state, history, log); }
      extra = Object.keys(st).filter(function (key) { return key !== 'out' && key !== 'op' && op.args.indexOf(key) < 0; });
      if (extra.length) log.push({at: at, step: st.out, op: st.op, note: 'not consumed: ' + extra.join(', ')});
      args = xArgs(st, op, xScope(base, state, item, outputs));
      var ctx = {record: record, budget: budget, at: at, step: st.out}, request = null;
      if (op.snapshot) {                                                 // a read: pin what it depends on, name the request
        ctx.snapshot = xHash(op.snapshot(args, ctx));
        request = {event: item, at: at, step: st.out, snapshot: ctx.snapshot, inputState: inputState, recipe: recipe}; request.id = xHash(request);
      }
      if (answer && request && answer.request.at === at && answer.request.step === st.out) {   // an answer delivered for this request
        var verdict = xBelongs(answer, request);
        if (verdict !== 'belongs') { log.push({at: at, step: st.out, op: st.op, stale: verdict}); return xFail('stale', verdict, at, state, history, log); }
        value = answer.value; answer = null; log.push({at: at, step: st.out, op: st.op, delivered: request.id.slice(0, 16) + '…'});
      } else {
        try { value = op.run(args, ctx); } catch (e) { log.push({at: at, step: st.out, op: st.op, error: e.message}); return xFail('error', st.out + ': ' + e.message, at, state, history, log); }
        if (opts.stopAt === st.out && request) return {status: 'answered', request: request, value: value, at: at};   // the read demanded outside a commit
      }
      if (value && typeof value === 'object' && (value.status === 'pending' || value.status === 'needs-policy')) {
        // the barrier: no next painting-step state; the transaction is retained as bytes with what it depends on
        log.push({at: at, step: st.out, op: st.op, status: value.status, missing: value.missing || value.candidates});
        var r = xFail(value.status, value.status === 'pending' ? 'dependency withheld: ' + JSON.stringify(value.missing) : 'no composition order saved; candidates ' + JSON.stringify(value.candidates), at, state, history, log);
        r.request = request; r.read = value;
        r.continuation = {schema: 'seam-bench-executor-continuation/v1', capabilities: X_CAPABILITIES, record: xCopy(record), recipe: recipe, request: request,
          input: {at: at, state: xSerializeState(state), history: xCopy(history)}, queued: (record.events || []).slice(at + 1), missing: xCopy(value.missing || value.candidates || [])};
        return r;
      }
      if (value && typeof value === 'object' && (value.status === 'unsupported' || value.status === 'error')) { log.push({at: at, step: st.out, op: st.op, error: value.reason}); return xFail('unsupported', st.out + ': ' + value.reason, at, state, history, log); }
      outputs[st.out] = value;
      log.push({at: at, step: st.out, op: st.op, value: xDescribe(value)});
    }
    if (single) { for (k in outputs) results[k] = outputs[k]; at++; break; }
    var next = {}, row = {event: item && item.id, at: at, inputState: inputState}, sc = xScope(base, state, item, outputs);
    for (k in program.next || {}) next[k] = xResolve(program.next[k], sc);
    for (k in outputs) if (outputs[k] && typeof outputs[k] === 'object' && outputs[k].snapshot) row.read = {step: k, request: xHash({event: item, at: at, step: k, snapshot: outputs[k].snapshot, inputState: inputState, recipe: recipe}), snapshot: outputs[k].snapshot, color: outputs[k].color, contributors: outputs[k].contributors, provisional: outputs[k].provisional || null};
    for (k in next) row[k] = xIsPainting(next[k]) ? {revision: next[k].revision, changed: next[k].changed, sha256: xPaintingSha256(next[k])} : next[k];
    history.push(row);
    for (k in next) state[k] = next[k];
  }
  if (answer) return xFail('stale', 'the answer was not consumed: no request pending at this state', at, state, history, log);
  var all = xScope(base, state, null, results);
  (program['return'] || []).forEach(function (r) { results[r] = xResolve(r, all); });
  return {status: 'complete', at: at, state: state, history: history, results: results, log: log, capabilities: X_CAPABILITIES};
}
function xFail(status, reason, at, state, history, log) { return {status: status, reason: reason, at: at, state: state, history: history, log: log}; }
// Does a delivered answer belong to the request the run is suspended at? Every difference has its own word.
function xBelongs(answer, request) {
  if (!answer || !answer.request) return 'the answer names no request';
  var a = answer.request;
  if (a.recipe !== request.recipe) return 'stale: the recipe changed since the read was demanded';
  if (a.inputState !== request.inputState) return 'stale: the input state is not the one the read was demanded in';
  if (a.at !== request.at || a.step !== request.step || xHash(a.event) !== xHash(request.event)) return 'stale: the answer belongs to another event or step';
  if (a.snapshot !== request.snapshot) return 'stale: the read\'s snapshot changed (order, bindings, painting or point)';
  if (!answer.value || answer.value.status !== 'resolved') return 'the answer is not a resolved read';
  return 'belongs';
}
// Demand a read without committing: the request and its answer under a grant, for delivering later (the delayed and
// cold schedules, and the controls: an answer under a changed order or recipe, an answer delivered twice).
function xDemand(record, step, opts) {
  opts = opts || {};
  return xRun(record, {budget: opts.budget != null ? opts.budget : Infinity, from: opts.from, inputs: opts.inputs, stopAt: step});
}
// Resume a continuation: the suspended read is re-demanded under the grant (or the delivered answer is checked), then the run continues.
function xResume(continuation, grant, opts) {
  opts = opts || {};
  return xRun(continuation.record, {budget: grant, from: continuation, inputs: opts.inputs, answer: opts.answer, until: opts.until});
}
// A short description of a value for a log line or a readout.
function xDescribe(v) {
  if (v == null) return v;
  if (xIsPainting(v)) return {painting: v.id, revision: v.revision, sha256: xPaintingSha256(v), changed: v.changed == null ? undefined : v.changed, clip: v.clip == null ? undefined : v.clip};
  if (xIsRegion(v)) return {region: v.id, radius: v.record.radius, area: v.area, bounds: v.bounds, saturated: v.saturated, pieces: v.pieces};   // ids and hashes in full: a record names a subject by them (the cold mind could not read a truncated one)
  if (Array.isArray(v)) return v.map(function (x) { return typeof x === 'number' ? +x.toFixed(6) : x; });
  if (typeof v === 'object' && v.status) { var o = {status: v.status}; if (v.color) o.color = v.color; if (v.contributors) o.contributors = v.contributors; if (v.snapshot) o.snapshot = String(v.snapshot).slice(0, 16) + '…'; if (v.missing) o.missing = v.missing; if (v.candidates) o.candidates = v.candidates; if (v.inside != null) o.inside = v.inside; if (v.distance != null) o.distance = v.distance; if (v.provisional) o.provisional = v.provisional; if (v.reason) o.reason = v.reason; if (v.texel) o.texel = v.texel; return o; }
  return v;
}
function xSummary(r) {
  var s = {status: r.status, at: r.at}, k;
  if (r.reason) s.reason = r.reason;
  if (r.state) { s.state = {}; for (k in r.state) s.state[k] = xIsPainting(r.state[k]) ? {painting: r.state[k].id, revision: r.state[k].revision, sha256: xPaintingSha256(r.state[k])} : r.state[k]; }
  if (r.history) s.history = r.history;
  if (r.results) { s.results = {}; for (k in r.results) s.results[k] = xDescribe(r.results[k]); }
  if (r.continuation) s.continuation = {request: r.continuation.request.id, missing: r.continuation.missing, queued: r.continuation.queued.map(function (e) { return e.id; })};
  return s;
}
