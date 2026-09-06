const { B, dabsOf, declOf, scopeOf, strictHost } = require('./harness.js');
const pickup = { tool: { name: 'pickup brush', size: 16, streamline: 0, fit: 'polyline', width: 'size * p', pickup: 0.5 }, source: { kind: 'pen', samples: [[26, 28, 0.65, 0], [102, 100, 0.9, 90], [28, 100, 1, 180], [102, 28, 0.7, 270]] },
  paint: { fill: null, stroke: { overlap: 'accumulate', spacing: 12, tip: 'nib', width: 'knot', unit: 'local', cap: 'round', join: 'round', align: 'center', color: [1, 0, 0, 0.62] } },
  surface: { id: 'paint', revision: 0, width: 128, height: 128, localToTexel: [1, 0, 0, 1, 0, 0], color: 'linear-premultiplied-rgba', initial: { clear: [0, 0, 1, 1] } },
  program: { each: 'dabs', state: { carry: [1, 0, 0, 1], surface: 'surface.initial' }, steps: [{ out: 'sample', op: 'sample', surface: 'state.surface', point: 'dab.xy', filter: 'nearest' }, { out: 'carry', op: 'mix', a: 'state.carry', b: 'sample', amount: 'tool.pickup' }, { out: 'surface', op: 'paint', surface: 'state.surface', region: 'dab.path', rgba: 'carry', opacity: 'paint.stroke.color.3', blend: 'source-over' }], next: { carry: 'carry', surface: 'surface' }, return: ['state.surface', 'path', 'dabs'] },
  snap: false, identity: { id: 'surface-read', revision: 1 } };
const proofs = { tool: { name: 'two paint proofs', size: 8, streamline: 0, fit: 'polyline', width: 'size*p' }, source: { kind: 'pen', samples: [[8, 8, 1, 0]] },
  paint: { fill: null, stroke: { overlap: 'accumulate', spacing: 12, tip: 'nib', width: 'knot', unit: 'local', cap: 'round', join: 'round', color: [1, 0, 0, 0.5] } },
  surface: { id: 'proof', revision: 0, width: 16, height: 16, localToTexel: [1, 0, 0, 1, 0, 0], color: 'linear-premultiplied-rgba', initial: { clear: [0, 0, 0, 0] } },
  program: { each: 'dabs', state: { base: 'surface.initial' },
    steps: [{ out: 'red', op: 'paint', surface: 'state.base', region: 'dab.path', rgba: [1, 0, 0, 1], opacity: 0.5, blend: 'source-over' }, { out: 'before', op: 'sample', surface: 'red', point: 'dab.xy', filter: 'nearest' }, { out: 'blue', op: 'paint', surface: 'state.base', region: 'dab.path', rgba: [0, 0, 1, 1], opacity: 0.5, blend: 'source-over' }, { out: 'after', op: 'sample', surface: 'red', point: 'dab.xy', filter: 'nearest' }, { out: 'other', op: 'sample', surface: 'blue', point: 'dab.xy', filter: 'nearest' }],
    next: { surface: 'red', red: 'red', blue: 'blue', before: 'before', after: 'after', other: 'other' }, return: ['state.surface', 'state.red', 'state.blue', 'state.before', 'state.after', 'state.other'] },
  snap: false, identity: { id: 'branch-proof', revision: 1 } };
function run(rec, label) { const { path, dabs } = dabsOf(rec); const decl = declOf(rec.surface); const host = strictHost(decl); const r = B.runProgram(rec.program, scopeOf(rec, host, path, dabs, decl), host);
  const consumed = r.log.filter(l => l.consumed).length; console.log(label, 'ok', r.ok, 'dabs', dabs.length, 'steps', r.steps, 'consumed paints', consumed, 'released', host.released, 'final', r.results['state.surface'] && r.results['state.surface'].key); return { r, host, dabs, decl, path }; }
const P = run(pickup, 'pickup'); console.log('  texel (64,64)', P.host.texel(P.r.results['state.surface'], 64, 64).map(v => +v.toFixed(4)));
const Q = run(proofs, 'proofs'); console.log('  before', Q.r.results['state.before'], 'after', Q.r.results['state.after'], 'other', Q.r.results['state.other']);
module.exports = { pickup, proofs, run };
