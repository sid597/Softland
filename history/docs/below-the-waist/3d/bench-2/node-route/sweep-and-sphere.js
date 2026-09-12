// The fold of the definer's attacks 2 and 3 (2026-09-06): the pure functions the Seam Bench embeds verbatim.
// Plain script, no exports: the bench pastes it into its <script>; probe-2-3.mjs evaluates it in Node beside the
// bench's own functions (markAt, markWidth, over, composeAt, postWorld, postChart, postPreimages, hitPost) and checks
// every number against the two attacks before the bench changes. Style follows the bench (ES5, `var`).
//
// Contents
//   1. sweepMin            the swept nib as an objective f(t) = d(q, c(t)) - w(p(t))/2, minimised per segment by
//                          branch-and-bound under a Lipschitz bound (the definer's attack 3); inside / outside /
//                          boundary / pending, with the enclosure [lower, upper] and the work spent
//   2. the post            postSegmentObjective + classifyMarkSweep: the mark on the post's wall with the width
//                          10 + 30p² carried into the footprint (attack 3's correction of the bench's capsule)
//   3. the sphere G        R = 200 mm (the post's radius; the definer's attack 2 at ×2 in every length, attack 3
//                          at ×1): unit vectors, surface distance, the chart S0 (u = R·lon, v = R·lat) and its
//                          unwrapped reading, the chart pieces, the reach tool (a surface-distance region with
//                          bounds, membership by intrinsic distance, the frozen-metric and chord readings for
//                          contrast), the band continuation (where distance may run), the antipode
//   4. the sequence        A and B on G: kA (an 80 mm great-circle arc, nib 16 mm) and kB (a tap, 24 mm) through
//                          G@0 → G@1 (φ, a chart change) → G@2 (χ∘φ, a merge into M); bindings retained per
//                          record; a fresh ray inverted through each; composition; the on-demand budget
//   5. the sphere sweep    attack 3's bow-tie on G with great-circle arcs and w = 10 + 30p²: the same sweepMin
//                          over a surface-distance objective

// ---------------------------------------------------------------------------------------------------------- 1
// sweepMin(f, L, opts): minimise f on [0, 1] where |f(s) - f(t)| <= L |s - t|.
//   opts.budget  evaluations allowed (the work demand); exhausted → status 'pending' with the enclosure so far
//   opts.tol     stop when upper - lower <= tol (the error demand; default 1e-4); with opts.decide, stop as soon as the sign
//                of the minimum is proved (upper < 0 → inside, lower > 0 → outside)
// Returns {status: 'inside'|'outside'|'boundary'|'pending', lower, upper, t, work}: lower <= min f <= upper,
// t the best sample seen. 'boundary' means the minimum lies within tol of zero (the query's declared tolerance
// decides). Each cell's lower bound is f(mid) - L·(hi - lo)/2; cells whose lower bound exceeds the best value are
// dropped; the cell with the lowest bound is split next.
function sweepMin(f, L, opts) {
  var budget = opts.budget != null ? opts.budget : 4000, tol = opts.tol != null ? opts.tol : 1e-4, decide = !!opts.decide;
  var work = 0, upper = Infinity, tBest = NaN, cells, i, c, m, lower;
  function cell(lo, hi) {
    var t = (lo + hi) / 2, v = f(t); work++;
    if (v < upper) { upper = v; tBest = t; }
    return {lo: lo, hi: hi, lower: v - L * (hi - lo) / 2};
  }
  cells = [cell(0, 1)];
  for (;;) {
    cells.sort(function (x, y) { return x.lower - y.lower; });   // the definer's order: the lowest bound splits next
    lower = cells[0].lower;
    if (decide && (upper < 0 || lower > 0)) break;
    if (upper - lower <= tol) break;
    if (work + 2 > budget) return {status: 'pending', lower: lower, upper: upper, t: tBest, work: work};
    c = cells.shift(); m = (c.lo + c.hi) / 2;
    cells.push(cell(c.lo, m)); cells.push(cell(m, c.hi));
    for (i = cells.length - 1; i >= 0; i--) if (cells[i].lower > upper) cells.splice(i, 1);
  }
  return {status: upper < 0 ? 'inside' : (lower > 0 ? 'outside' : 'boundary'), lower: lower, upper: upper, t: tBest, work: work};
}

// ---------------------------------------------------------------------------------------------------------- 2
// The post's mark, authored in the unwrapped (u, v) mm plane of the wall (a developable host: Euclidean there).
// A segment runs from knot i to knot i + 1, the centre linear in t, the pressure linear in t, the width
// 10 + 30p² (markWidth). The union of the nib's discs along a segment is NOT the convex hull of the end discs
// when the radius is not linear in t (attack 3): the objective below is the membership the bench declares.
function postSegmentObjective(knots, seg, q) { // q = [u, v] mm, as the bench's classifyMark takes it
  var a = knots[seg], b = knots[seg + 1];
  var du = b.u - a.u, dv = b.v - a.v, len = Math.sqrt(du * du + dv * dv);
  var L = len + 30 * Math.max(Math.abs(a.p), Math.abs(b.p)) * Math.abs(b.p - a.p);
  return {
    f: function (t) {
      var u = a.u + du * t, v = a.v + dv * t, p = a.p + (b.p - a.p) * t;
      return Math.sqrt((q[0] - u) * (q[0] - u) + (q[1] - v) * (q[1] - v)) - (10 + 30 * p * p) / 2;
    },
    L: L, len: len
  };
}
// classifyMarkSweep(knots, q, opts) → {inside, contributors: [{seg, t, width (= w), p, sd (= signed), lower, upper, work}],
//   nearest (= best): the segment with the least upper bound, sd (the signed distance to the union's boundary: the min over
//   segments; inside distance is a lower bound, as before), status: 'inside'|'outside'|'boundary'|'pending', work}
// opts as sweepMin's (default tol 1e-4 mm, budget 4000 per segment: a global Lipschitz bound cannot certify a flat
// minimum to 1e-9 in any budget; the readout's tolerance is the query's declared one).
function classifyMarkSweep(knots, q, opts) {
  opts = opts || {};
  var budget = opts.budget != null ? opts.budget : 4000, tol = opts.tol != null ? opts.tol : 1e-4;
  var rows = [], contributors = [], nearest = null, work = 0, pending = false, seg, o, r, i;
  function row(seg, r) {
    var p = knots[seg].p + (knots[seg + 1].p - knots[seg].p) * r.t, w = 10 + 30 * p * p;
    return {seg: seg, t: r.t, width: w, w: w, p: p, sd: r.upper, signed: r.upper, lower: r.lower, upper: r.upper, work: r.work, status: r.status};
  }
  // first decide membership per segment (cheap: the sign of the minimum), then refine only what the readout quotes
  for (seg = 0; seg + 1 < knots.length; seg++) {
    o = postSegmentObjective(knots, seg, q);
    r = sweepMin(o.f, o.L, {decide: true, budget: budget});
    work += r.work;
    if (r.status === 'pending') pending = true;
    rows.push({seg: seg, o: o, r: r});
  }
  for (i = 0; i < rows.length; i++) {
    var isNearest = true, j;
    for (j = 0; j < rows.length; j++) if (rows[j].r.upper < rows[i].r.upper) isNearest = false;
    if (rows[i].r.status === 'inside' || isNearest) { r = sweepMin(rows[i].o.f, rows[i].o.L, {tol: tol, budget: budget}); work += r.work; if (r.status === 'pending') pending = true; rows[i].r = r; }
    var e = row(rows[i].seg, rows[i].r);
    if (rows[i].r.status === 'inside' || (rows[i].r.upper < 0)) contributors.push(e);
    if (!nearest || e.upper < nearest.upper) nearest = e;
  }
  var inside = contributors.length > 0;
  return {
    inside: inside,
    contributors: contributors,
    nearest: nearest, best: nearest,
    sd: nearest ? nearest.upper : Infinity,
    lower: nearest ? nearest.lower : Infinity,
    status: inside ? 'inside' : (pending ? 'pending' : (nearest && nearest.lower > 0 ? 'outside' : 'boundary')),
    tol: tol, work: work
  };
}

// ---------------------------------------------------------------------------------------------------------- 3
var G_R = 200; // mm; the sphere's radius, the post's
var G_C = 2 * Math.PI * G_R;
function v3dot(a, b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
function v3cross(a, b) { return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]; }
function v3len(a) { return Math.sqrt(v3dot(a, a)); }
function v3norm(a) { var l = v3len(a); return [a[0] / l, a[1] / l, a[2] / l]; }
function v3add(a, b) { return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]; }
function v3scale(a, s) { return [a[0] * s, a[1] * s, a[2] * s]; }
// Locations on G are unit vectors; Y up; longitude from +x towards +z (the post's sense), latitude from the equator.
function sphUnit(lon, lat) { return [Math.cos(lat) * Math.cos(lon), Math.sin(lat), Math.cos(lat) * Math.sin(lon)]; }
function sphAngle(p, q) { return Math.atan2(v3len(v3cross(p, q)), v3dot(p, q)); }
function sphDist(p, q) { return G_R * sphAngle(p, q); }              // the shorter great-circle distance, mm
function sphChord(p, q) { return G_R * v3len([p[0] - q[0], p[1] - q[1], p[2] - q[2]]); }
function sphLon(p) { return Math.atan2(p[2], p[0]); }                 // in [-π, π): the chart S0's cut is at ±π
function sphLat(p) { return Math.asin(Math.max(-1, Math.min(1, p[1]))); }
// The chart S0: u = R·lon, v = R·lat. Reading the chart gives u in [-πR, πR); the authored domains here are
// unwrapped around u = πR (the mark crosses the cut), so a chart point has preimages u + kC in a domain.
function sphChart0(p) { return {u: G_R * sphLon(p), v: G_R * sphLat(p)}; }
function sphPoint0(u, v) { return sphUnit(u / G_R, v / G_R); }
function sphPreimages(a, domain) { // chart u (any branch) → the u + kC inside [domain.u0, domain.u1]
  var out = [], k;
  for (k = -2; k <= 2; k++) { var u = a + k * G_C; if (u >= domain.u0 && u <= domain.u1) out.push({u: u, k: k}); }
  return out;
}
// The great-circle arc from a to b (unit vectors), uniform in angle; t in [0, 1].
function sphArc(a, b, t) {
  var z = sphAngle(a, b);
  if (z < 1e-12) return a;
  var sa = Math.sin((1 - t) * z) / Math.sin(z), sb = Math.sin(t * z) / Math.sin(z);
  return v3norm(v3add(v3scale(a, sa), v3scale(b, sb)));
}
// Distance from q to the minor arc n·cos θ + e·sin θ, θ in [th0, th1] (attack 2's closed form for a
// constant-radius sweep on a great circle): the closest centre is at θ* = clamp(atan2(q·e, q·n)).
function sphArcDistClosed(q, n, e, th0, th1) {
  var th = Math.max(th0, Math.min(th1, Math.atan2(v3dot(q, e), v3dot(q, n))));
  var c = v3add(v3scale(n, Math.cos(th)), v3scale(e, Math.sin(th)));
  return {d: sphDist(q, c), theta: th, t: (th - th0) / (th1 - th0), centre: c};
}
// The chart pieces of a region on G (attack 2): longitude in [-π, 0) and [0, π) at or below latitude 70°, and a
// north chart (x, z) above 70°, regular at the pole. Every location has one owner.
var G_NORTH_LAT = 70 * Math.PI / 180;
function sphPieceOf(p) {
  var lat = sphLat(p), lon = sphLon(p);
  if (lat > G_NORTH_LAT) return {piece: 'north', coords: [G_R * p[0], G_R * p[2]], note: '(x, z), embedding (x, √(R² − x² − z²), z)'};
  return {piece: lon < 0 ? 'west' : 'east', coords: [G_R * lon, G_R * lat], note: lon < 0 ? 'longitude in [−π, 0)' : 'longitude in [0, π)'};
}
// The reach tool: a saved record {seed: [lon, lat] rad | point: unit vector, radius mm, distance: 'surface'} → the
// retained region. The result owns a copy of its record, the subject it was built from: a caller's later edit of its
// own record does not reach in (attack 4: the caller's radius field went 150 → 140 and the result reported 140
// while its membership was still the 150 cap). The angular radius saturates at π: a radius of πR or more is the whole
// sphere, area 4πR², bounds [−R, R] on each axis (attack 4: letting cos(radius/R) cycle made 629 mm exclude the
// antipode, which is 0.68 mm inside that radius, and 2πR an empty region with reversed bounds). Membership is the
// same computation the readout prints, d(seed, q) ≤ radius by sphDist, so `member` and `distance` cannot disagree.
// Admission: a negative radius names no region and is refused (status 'unsupported'); zero is the seed alone.
function reachRegion(record) {
  var rec = JSON.parse(JSON.stringify(record));
  if (!(rec.radius >= 0)) return {status: 'unsupported', record: rec, reason: 'a negative radius names no region: refused'};
  var n = rec.point ? rec.point.slice() : sphUnit(rec.seed[0], rec.seed[1]);
  var saturated = rec.radius >= Math.PI * G_R, rho = saturated ? Math.PI : rec.radius / G_R, cr = Math.cos(rho), sr = Math.sin(rho);
  var bounds = [], i;
  for (i = 0; i < 3; i++) {
    var ni = n[i], s = Math.sqrt(Math.max(0, 1 - ni * ni));
    bounds.push([saturated || -ni >= cr ? -G_R : G_R * (ni * cr - s * sr), saturated || ni >= cr ? G_R : G_R * (ni * cr + s * sr)]);
  }
  return {
    status: 'resolved', record: rec, seed: n, rho: rho, saturated: saturated,
    area: saturated ? 4 * Math.PI * G_R * G_R : 2 * Math.PI * G_R * G_R * (1 - cr),
    bounds: bounds,                                          // world mm about G's centre, [min, max] per axis
    member: function (q) { return saturated || sphDist(n, q) <= rec.radius; },   // d(seed, q) ≤ radius, the printed distance
    distance: function (q) { return sphDist(n, q); }
  };
}
// The two competing readings attack 2 puts beside the intrinsic distance: the metric frozen at the seed
// (an ellipse in the chart) and the ambient chord.
function sphFrozenDist(seed, q) {
  var cs = sphChart0(seed), cq = sphChart0(q), dlon = (cq.u - cs.u) / G_R;
  while (dlon >= Math.PI) dlon -= 2 * Math.PI; while (dlon < -Math.PI) dlon += 2 * Math.PI;
  var du = G_R * dlon, dv = cq.v - cs.v, c = Math.cos(sphLat(seed));
  return Math.sqrt(c * c * du * du + dv * dv);
}
// Where distance paths may run (attack 2's boundary continuation; attack 3's two policies): with the open
// latitude band (v0, v1) removed from the support, 'underlying' measures on the whole sphere and clips the answer
// afterwards; 'face' requires the path to stay in the retained support, and a full band disconnects the two
// sides, so the distance is infinite (a topological fact of this fixture, not a general obstacle solver).
function sphBandDistance(seed, q, band, policy) {
  var vs = G_R * sphLat(seed), vq = G_R * sphLat(q);
  var inBand = function (v) { return v > band[0] && v < band[1]; };
  if (inBand(vs) || inBand(vq)) return {d: NaN, status: 'not on the support'};
  if (policy === 'face' && ((vs <= band[0]) !== (vq <= band[0]))) return {d: Infinity, status: 'no connecting path in the retained support'};
  return {d: sphDist(seed, q), status: policy === 'face' ? 'path within the retained support' : 'measured on the underlying sphere, clipped after'};
}

// ---------------------------------------------------------------------------------------------------------- 4
// The retained records on G and their bindings through three evaluations (attack 2 §4, at ×2).
var G_N = sphUnit(Math.PI, Math.PI / 3);      // the seed (longitude 180°, latitude 60°) = (−1/2, √3/2, 0)
var G_E = [0, 0, -1];                          // the eastward tangent there
var G_PI_R = Math.PI * G_R;
function gArcCentre(t) { var th = (80 * t - 40) / G_R; return v3add(v3scale(G_N, Math.cos(th)), v3scale(G_E, Math.sin(th))); }
function gUnwrapU(p) { var lon = sphLon(p); if (lon < 0) lon += 2 * Math.PI; return G_R * lon; } // the authored branch: u around πR
var G_RECORDS = {
  A: {id: 'A@0', root: 'S0 (u, v)', domain: {u0: G_PI_R - 160, u1: G_PI_R + 160, v0: 120, v1: 290},
      mark: {id: 'kA', kind: 'arc', segment: 'arc-AB', radius: 16, rgba: [0.5, 0, 0, 0.5], th0: -0.2, th1: 0.2}},
  B: {id: 'B@0', root: '(ξ, η), β(ξ, η) = (ξ + πR, η + 200)', domain: {x0: -10, x1: 200, y0: -80, y1: 90},
      mark: {id: 'kB', kind: 'tap', radius: 24, rgba: [0, 0, 0.5, 0.5], at: 0.65}}
};
function gBeta(xi, eta) { return {u: xi + G_PI_R, v: eta + 200}; }
function gBetaInv(u, v) { return {xi: u - G_PI_R, eta: v - 200}; }
// The charts: S0 (u, v); S1(a, b) = S0(2a + 200, b), φ(u, v) = ((u − 200)/2, v); S2(c, d) = S0(2c − d/2 + 80, d),
// χ(a, b) = (a + b/4 + 60, b). Each is a saved chart parameter of the same sphere: same points.
var G_CHARTS = [
  {rev: 0, name: 'S0', fromRoot: function (u, v) { return {a: u, b: v}; }, toRoot: function (a, b) { return {u: a, v: b}; },
   period: G_C, flat: [[1, 0], [0, 1]], cut: 'u = ±πR (longitude ±180°)'},
  {rev: 1, name: 'S1', fromRoot: function (u, v) { return {a: (u - 200) / 2, b: v}; }, toRoot: function (a, b) { return {u: 2 * a + 200, v: b}; },
   period: G_C / 2, flat: [[4, 0], [0, 1]], cut: 'a = ±πR/2'},
  {rev: 2, name: 'S2', fromRoot: function (u, v) { var a = (u - 200) / 2; return {a: a + v / 4 + 60, b: v}; },
   toRoot: function (c, d) { return {u: 2 * c - d / 2 + 80, v: d}; }, period: G_C / 2,
   flat: [[4, -1], [-1, 1.25]], cut: 'sheared'}
];
// The metric of chart k at (u, v) in chart units: Jᵀ diag(cos²(v/R), 1) J with J the Jacobian of toRoot
// (attack 2: at latitude 60° in chart 2, [[1, −.25], [−.25, 1.0625]] after the cos² factor).
function gChartMetric(chart, v) {
  var J = chart.rev === 0 ? [[1, 0], [0, 1]] : chart.rev === 1 ? [[2, 0], [0, 1]] : [[2, -0.5], [0, 1]];
  var c2 = Math.cos(v / G_R) * Math.cos(v / G_R);
  var g = [[c2, 0], [0, 1]];
  var JtG = [[J[0][0] * g[0][0] + J[1][0] * g[1][0], J[0][0] * g[0][1] + J[1][0] * g[1][1]], [J[0][1] * g[0][0] + J[1][1] * g[1][0], J[0][1] * g[0][1] + J[1][1] * g[1][1]]];
  return [[JtG[0][0] * J[0][0] + JtG[0][1] * J[1][0], JtG[0][0] * J[0][1] + JtG[0][1] * J[1][1]], [JtG[1][0] * J[0][0] + JtG[1][1] * J[1][0], JtG[1][0] * J[0][1] + JtG[1][1] * J[1][1]]];
}
// The bindings at revision k, each retained as a chain of relations from the record's root to the current chart,
// composable both ways; the valid subset is tested in the ROOT domain (attack 2: "they do not become M's whole
// domain merely because M has one face ID"). B's chain gains one unit of work at the merge (a deterministic demand
// test); a budget of 0 withholds that answer and returns the missing dependency instead of a guess.
function gBindings(rev, opts) {
  opts = opts || {};
  var chart = G_CHARTS[rev];
  var drop = opts.dropRetained; // negative control: remove retained φ without a composed replacement
  return [
    {record: G_RECORDS.A, id: 'binding-A', chain: rev === 0 ? ['identity'] : rev === 1 ? ['φ'] : ['φ', 'χ'], work: 0,
     toRoot: function (a, b) { if (drop && rev >= 1) return null; return chart.toRoot(a, b); },
     inRoot: function (r) { var d = G_RECORDS.A.domain; return r.u >= d.u0 && r.u <= d.u1 && r.v >= d.v0 && r.v <= d.v1; }},
    {record: G_RECORDS.B, id: 'binding-B', chain: rev === 0 ? ['β'] : rev === 1 ? ['β', 'φ'] : ['β', 'φ', 'χ'], work: rev === 2 ? 1 : 0,
     toRoot: function (a, b) { if (drop && rev >= 1) return null; var r = chart.toRoot(a, b); return gBetaInv(r.u, r.v); },
     inRoot: function (r) { var d = G_RECORDS.B.domain; return r.xi >= d.x0 && r.xi <= d.x1 && r.eta >= d.y0 && r.eta <= d.y1; }}
  ];
}
// Membership of the two marks, in world terms (the sphere's own distance), given the point q (unit vector).
function gMarkA(q) { var r = sphArcDistClosed(q, G_N, G_E, G_RECORDS.A.mark.th0, G_RECORDS.A.mark.th1); return {inside: r.d <= G_RECORDS.A.mark.radius, d: r.d, t: r.t, sd: r.d - G_RECORDS.A.mark.radius}; }
function gMarkB(q) { var d = sphDist(q, gArcCentre(G_RECORDS.B.mark.at)); return {inside: d <= G_RECORDS.B.mark.radius, d: d, sd: d - G_RECORDS.B.mark.radius}; }
// gLocate(rev, p, opts): p a unit vector on G hit by a fresh ray. Reads the current chart, inverts each retained
// binding, applies its root restriction, queries its retained mark, composes by the saved order.
//   opts.budget (default Infinity): work the demand grants to the correspondence; opts.order: ['kA','kB'] |
//   ['kB','kA'] | null (needs-policy); opts.dropRetained: the negative control.
function gLocate(rev, p, opts) {
  opts = opts || {};
  var chart = G_CHARTS[rev], budget = opts.budget != null ? opts.budget : Infinity;
  var uw = gUnwrapU(p), v = G_R * sphLat(p);
  var here = chart.fromRoot(uw, v);                              // the chart location on the authored branch
  var bindings = gBindings(rev, opts), out = [], unresolved = [], b, i, root, pre, k;
  for (i = 0; i < bindings.length; i++) {
    b = bindings[i];
    if (b.work > budget) { unresolved.push({binding: b.id, missing: b.chain[b.chain.length - 1] + ': ' + b.record.id + ' → M@' + rev + ' at the demanded chart point', work: b.work}); continue; }
    // the chart point has preimages on every branch; test each against the ROOT restriction
    var hits = [];
    for (k = -1; k <= 1; k++) {
      root = b.toRoot(here.a + k * chart.period, here.b);
      if (root === null) { unresolved.push({binding: b.id, missing: 'retained dependency φ removed without a composed replacement'}); hits = null; break; }
      if (b.inRoot(root)) hits.push({k: k, root: root});
    }
    if (hits === null) continue;
    if (hits.length === 0) { out.push({binding: b.id, record: b.record.id, status: 'outside the root restriction', root: b.toRoot(here.a, here.b)}); continue; }
    var m = b.record.mark.kind === 'arc' ? gMarkA(p) : gMarkB(p);
    out.push({binding: b.id, record: b.record.id, status: 'resolved', root: hits[0].root, k: hits[0].k, chain: b.chain, mark: b.record.mark.id, inside: m.inside, d: m.d, t: m.t, sd: m.sd});
  }
  var layers = out.filter(function (r) { return r.status === 'resolved' && r.inside; });
  var comp;
  if (unresolved.length) comp = {status: 'unresolved', rgba: null, known: layers.map(function (l) { return l.mark; }), may: unresolved.map(function (u) { return u.binding === 'binding-B' ? 'kB' : 'kA'; }), missing: unresolved};
  else if (layers.length > 1 && !opts.order) comp = {status: 'needs-policy', contributors: layers.map(function (l) { return l.mark; })};
  else {
    var rgba = [0, 0, 0, 0], order = opts.order || ['kA'], j, l;
    for (j = 0; j < order.length; j++) { l = layers.filter(function (x) { return x.mark === order[j]; })[0]; if (l) rgba = over(G_RECORDS[l.mark === 'kA' ? 'A' : 'B'].mark.rgba, rgba); }
    comp = {status: layers.length ? 'resolved' : 'empty', rgba: rgba, contributors: layers.map(function (l) { return l.mark; }), order: order};
  }
  return {rev: rev, chart: chart.name, here: here, unwrappedU: uw, v: v, bindings: out, unresolved: unresolved, composition: comp};
}
// The fresh ray of attack 2: from 3·c(.65) toward the centre; the hit is at ray distance 2R and world point c(.65)·R.
function gFreshRay(t) { var c = gArcCentre(t); return {origin: v3scale(c, 3 * G_R), dir: v3scale(c, -1), hitT: 2 * G_R, point: c, worldMM: v3scale(c, G_R)}; }

// ---------------------------------------------------------------------------------------------------------- 5
// Attack 3's construction on G: four knots joined by great-circle arcs, pressure linear in arc parameter,
// w = 10 + 30p²; the objective is the surface distance minus the half width; L = ℓ + 30·max|p|·|p1 − p0|.
function sphSegmentObjective(a, b, p0, p1, q) {
  var len = sphDist(a, b), L = len + 30 * Math.max(Math.abs(p0), Math.abs(p1)) * Math.abs(p1 - p0);
  return {f: function (t) { return sphDist(q, sphArc(a, b, t)) - (10 + 30 * Math.pow(p0 + (p1 - p0) * t, 2)) / 2; }, L: L, len: len};
}
function classifySphereMark(knots, q, opts) { // knots: [{p: unit vector, pressure}], q unit vector
  opts = opts || {};
  var rows = [], seg, o, r;
  for (seg = 0; seg + 1 < knots.length; seg++) {
    o = sphSegmentObjective(knots[seg].p, knots[seg + 1].p, knots[seg].pressure, knots[seg + 1].pressure, q);
    r = sweepMin(o.f, o.L, opts);
    rows.push({seg: seg, status: r.status, lower: r.lower, upper: r.upper, t: r.t, work: r.work});
  }
  return rows;
}
