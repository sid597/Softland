// Session 12 harness: the definer's attack-4 records on the bench's pure declarations (Node route).
const B = require('./bench.js');
const src = require('fs').readFileSync('/mnt/data/projects/Softland/docs/below-the-waist/path-kind/bench-9/waist-bench.html', 'utf8');
const normStrokeSrc = src.split('\n').find(l => l.trim().startsWith('function normStroke'));
const normStroke = new Function('blendIndex', normStrokeSrc + '; return normStroke;')(B.blendIndex);

function dabsOf(rec) { // the bench's rebuild route to dabs: buildPath → buildDabs with the stroke's spacing and width expression
  const path = B.buildPath(rec); const st = normStroke(rec.paint.stroke);
  let widthFn = null; if (st.widthExpr) { const f = B.compileExpr(st.widthExpr); const scope = {}; for (const k of f.names) if (typeof rec.tool[k] === 'number') scope[k] = rec.tool[k]; if (typeof scope.size !== 'number') scope.size = typeof rec.tool.size === 'number' ? rec.tool.size : 8; widthFn = (p, s) => { const w = f({ ...scope, p, s, v: 0 }); return isFinite(w) ? Math.max(0, w) : 0; }; }
  const D = B.buildDabs(path, { widthLocal: typeof st.width === 'number' ? st.width : null, knotScale: 1, fallbackW: 4, envTol: 0.1, widthFn, spacing: st.spacing });
  return { path, dabs: D.dabs };
}
function declOf(sd) { return { id: sd.id, revision: sd.revision, width: sd.width, height: sd.height, localToTexel: sd.localToTexel || [1, 0, 0, 1, 0, 0], color: sd.color, initial: { clear: (sd.initial && sd.initial.clear) || [0, 0, 0, 0] } }; }
function scopeOf(rec, host, path, dabs, decl) { return { tool: rec.tool, paint: rec.paint, source: rec.source, identity: rec.identity, path, dabs, surface: { id: decl.id, revision: decl.revision, width: decl.width, height: decl.height, initial: host.initial } }; }
// the reference adapter: the CPU host, but a released value can never be read again (what the GPU host does with its pool)
function strictHost(decl) { const h = B.cpuHost(decl); const dead = new Set(); const chk = s => { if (dead.has(s)) throw new Error('surface ' + s.key + ' was released: a stale read, refused'); };
  return { ...h, released: 0, release(s) { if (s.frozen) return; dead.add(s); this.released++; }, sample(s, p, f) { chk(s); return h.sample(s, p, f); }, paint(s, r, c, o, b, ctx) { chk(s); return h.paint(s, r, c, o, b, ctx); }, content(s) { chk(s); return h.content(s); } }; }
module.exports = { B, normStroke, dabsOf, declOf, scopeOf, strictHost };

if (require.main === module) {
  const nested = JSON.parse(require('fs').readFileSync(__dirname + '/nested.json', 'utf8'));
  const { path, dabs } = dabsOf(nested); const decl = declOf(nested.surface);
  const host = strictHost(decl); const run = B.runProgram(nested.program, scopeOf(nested, host, path, dabs, decl), host);
  console.log('ok', run.ok, 'steps', run.steps, 'released', host.released, 'log', run.log.map(l => l.step + (l.error ? ' ERR ' + l.error : '') + (l.consumed ? ' consumed' : '')).join(' | '));
  const proofs = run.results['state.proofs'], blue = run.results['state.surface'];
  const tryRead = (s, label) => { try { console.log(label, host.sample(s, [8, 8])); } catch (e) { console.log(label, 'REFUSED:', e.message); } };
  tryRead(proofs[0].surface, 'returned red proof'); tryRead(blue, 'selected blue');
}
