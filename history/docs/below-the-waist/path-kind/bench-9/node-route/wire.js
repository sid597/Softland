// The checkpoint-from-bytes route on the Node CPU host: prefix → capture → wire → fresh host → resume → compare with the straight run.
const { B, dabsOf, declOf, scopeOf, strictHost } = require('./harness.js');
const { pickup } = require('./regress.js');
const { path, dabs } = dabsOf(pickup); const decl = declOf(pickup.surface);
const hostA = strictHost(decl); const straight = B.runProgram(pickup.program, scopeOf(pickup, hostA, path, dabs, decl), hostA);
const finalA = hostA.content(straight.results['state.surface']);
const key = B.checkpointKey(pickup.program, straight.reads, decl, dabs, straight.itemFields, 12);
const pre = B.runProgram(pickup.program, scopeOf(pickup, hostA, path, dabs, decl), hostA, { until: 12 });
const cap = B.captureState(pre.state, s => hostA.content(s));
console.log('prefix at', pre.at, 'steps', pre.steps, 'carry', pre.state.carry, 'resources', Object.keys(cap.resources), 'state', JSON.stringify(cap.state));
const res = cap.resources['paint:11/surface']; const bytes = B.rgba32fLE.encode(res.content);
console.log('content sha256', B.sha256(bytes), 'bytes', bytes.byteLength, '(definer: 9757e826f4594d5aa2c628302831ddee00dc57c8037340ee5bc14428d395aa82)');
const text = B.encodeCheckpoint({ at: pre.at, state: cap.state, resources: cap.resources }, key, { host: 'cpu', revision: 1 });
console.log('wire bytes', text.length, 'sha256', B.sha256(text));
// discard host A; a fresh host B
const hostB = strictHost(decl); const straightB = B.runProgram(pickup.program, scopeOf(pickup, hostB, path, dabs, decl), hostB);
const keyB = B.checkpointKey(pickup.program, straightB.reads, decl, dabs, straightB.itemFields, 12);
const wire = JSON.parse(text); const loaded = B.decodeCheckpoint(wire, keyB, decl);
const state = B.restoreState(loaded.state, k => hostB.resolve(loaded.resources[k]));
const restoredDiff = (() => { const A = loaded.resources['paint:11/surface'].content, Bc = hostB.content(state.surface); let n = 0; for (let i = 0; i < A.length; i++) if (A[i] !== Bc[i]) n++; return n + ' of ' + A.length; })();
const tail = B.runProgram(pickup.program, scopeOf(pickup, hostB, path, dabs, decl), hostB, { from: { state, at: loaded.at } });
const finalB = hostB.content(tail.results['state.surface']); let n = 0, mx = 0; for (let i = 0; i < finalA.length; i++) { const d = Math.abs(finalA[i] - finalB[i]); if (d > 0) { n++; mx = Math.max(mx, d); } }
console.log('restored vs saved', restoredDiff, '· resumed tail steps', tail.steps, '· resumed final vs straight run (host A):', n, 'of', finalA.length, 'differ, max', mx);
// the definer's four edits, against the stored wire's dependencies
const edit = (f, label) => { const r = JSON.parse(JSON.stringify(pickup)); f(r); const d = dabsOf(r); const h = strictHost(declOf(r.surface)); const run = B.runProgram(r.program, scopeOf(r, h, d.path, d.dabs, declOf(r.surface)), h); const k = B.checkpointKey(r.program, run.reads, declOf(r.surface), d.dabs, run.itemFields, 12); try { B.decodeCheckpoint(wire, k, declOf(r.surface)); console.log(' ', label, '→ loads'); } catch (e) { console.log(' ', label, '→ refused:', e.message); } };
edit(r => { r.source.samples[0][2] = 0.4; }, 'first pressure .65 → .4');
edit(r => { r.source.samples[3][0] = 110; }, 'last x 102 → 110');
edit(r => { r.tool.pickup = 0.25; }, 'tool.pickup .5 → .25');
edit(r => { r.surface.width = 64; r.surface.height = 64; }, 'surface 64 × 64');
{ const w2 = JSON.parse(text); w2.dependencies.fixedDeps = w2.dependencies.fixedDeps.replace('session 11', 'session 99'); try { B.decodeCheckpoint(w2, keyB, decl); console.log('  capabilities changed → loads'); } catch (e) { console.log('  capabilities changed in the key → refused:', e.message); } }
{ const w3 = JSON.parse(text); w3.resources['paint:11/surface'].data = w3.resources['paint:11/surface'].data.slice(0, -8); try { B.decodeCheckpoint(w3, keyB, decl); console.log('  truncated → loads'); } catch (e) { console.log('  truncated payload → refused:', e.message); } }
{ const naive = JSON.parse(JSON.stringify({ content: res.content })); console.log('  naive JSON of a Float32Array →', Array.isArray(naive.content) ? 'array' : typeof naive.content, 'with', Object.keys(naive.content).length, 'keys; resolve gives length', hostB.resolve({ ...res, content: naive.content }).data.length); }
