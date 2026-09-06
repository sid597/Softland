// Bounded numerical diagnostics on the shipped shaders. No committed bench bytes are changed.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import assert from 'node:assert/strict';
import {benchSource, shaders, hash} from './bench.mjs';
const require = createRequire(import.meta.url), puppeteer = require('puppeteer');
const here = path.dirname(fileURLToPath(import.meta.url));
const arg = name => process.argv.includes(name) ? process.argv[process.argv.indexOf(name) + 1] : null;
const source = benchSource(arg('--bench') || undefined), shader = shaders(source);
const fixture = JSON.parse(fs.readFileSync(path.join(here, 'records.json'), 'utf8'));
function replaceOne(s, a, b) { assert.equal(s.split(a).length, 2, 'instrument anchor: ' + a); return s.replace(a, b); }
const expose = '\nwindow.__attack4={renderView,lease,cameraOf,rayFromRegionPoint,hitField,projectPoint,m4point,fieldValue,fieldPoly,isolateRealRoots,state,S,views,gl,shaderLog,readFieldDepth};\n})();\n</script>';
const browser = await puppeteer.launch({executablePath: '/usr/bin/google-chrome', headless: true,
  args: ['--no-sandbox', '--use-angle=swiftshader', '--use-gl=angle', '--enable-unsafe-swiftshader']});
try {
  const p = await browser.newPage(), errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.setViewport({width: 1500, height: 1100, deviceScaleFactor: 1});
  const parity = [];
  for (const mode of ['eye', 'shifted', 'shifted-newton-diagnostic']) {
    let html = replaceOne(source, 'antialias: true', 'antialias: false');
    html = replaceOne(html, '\n})();\n</script>', expose);
    if (mode === 'shifted-newton-diagnostic') {
      // Same returned root, refined with the unexpanded spatial field. Three steps only;
      // a diagnostic at the captured crossing rays, not a proposed global root solver.
      html = replaceOne(html,
        "'      leaf = innerWall ? 1 : 0; vec3 g = innerWall ? -fieldGrad(p, uFieldInner) : fieldGrad(p, uFieldOuter);',",
        "'      vec3 ext = innerWall ? uFieldInner : uFieldOuter; for (int j=0;j<3;j++) { float df = dot(fieldGrad(p, ext), d); if (abs(df)>1e-8) lo -= fieldValue(p, ext)/df; p = o + lo*d; }',\n" +
        "'      leaf = innerWall ? 1 : 0; vec3 g = innerWall ? -fieldGrad(p, uFieldInner) : fieldGrad(p, uFieldOuter);',");
    }
    for (const small of [false, true]) {
      await p.goto('about:blank#field=1&fx=0.5&aim=F' + (mode === 'eye' ? '&fshift=0' : '') + (small ? '&fa=0.1' : ''));
      await p.setContent(html, {waitUntil: 'domcontentloaded'});
      await p.waitForFunction(() => window.__attack4);
      parity.push({mode, small, ...await p.evaluate(() => {
        const a = window.__attack4, ls = a.lease(), cam = a.cameraOf(a.views.V, ls);
        const ray = a.rayFromRegionPoint(cam, 226.5, 127.5), hit = a.hitField(ray, a.S.things.F, 20000);
        if (!hit || !hit.point) throw Error('no reference crossing');
        return {...a.readFieldDepth(226, 127, hit.point), shaderLog: a.shaderLog,
          samples: a.gl.getParameter(a.gl.SAMPLES), glError: a.gl.getError()};
      })});
    }
  }
  // Direct GPU calls remove projection, depth encoding and rasterized proxy geometry from the question.
  const direct = await p.evaluate(({shader, fixture}) => {
    const c = document.createElement('canvas'); c.width = 1; c.height = 1;
    const gl = c.getContext('webgl2', {antialias: false});
    if (!gl || !gl.getExtension('EXT_color_buffer_float')) throw Error('RGBA32F diagnostic unavailable');
    function compile(type, src) {
      const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s);
      if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw Error(gl.getShaderInfoLog(s)); return s;
    }
    const vs = '#version 300 es\nvoid main(){vec2 p=gl_VertexID==0?vec2(-1,-1):gl_VertexID==1?vec2(3,-1):vec2(-1,3);gl_Position=vec4(p,0,1);}';
    function program(fs) {
      const p = gl.createProgram(); gl.attachShader(p, compile(gl.VERTEX_SHADER, vs));
      gl.attachShader(p, compile(gl.FRAGMENT_SHADER, fs)); gl.linkProgram(p);
      if (!gl.getProgramParameter(p, gl.LINK_STATUS)) throw Error(gl.getProgramInfoLog(p)); return p;
    }
    const tex = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.texStorage2D(gl.TEXTURE_2D, 1, gl.RGBA32F, 1, 1);
    const fb = gl.createFramebuffer(); gl.bindFramebuffer(gl.FRAMEBUFFER, fb);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, tex, 0);
    if (gl.checkFramebufferStatus(gl.FRAMEBUFFER) !== gl.FRAMEBUFFER_COMPLETE) throw Error('incomplete float framebuffer');
    const vao = gl.createVertexArray(); gl.bindVertexArray(vao); gl.viewport(0, 0, 1, 1);
    function read(prog) { gl.useProgram(prog); gl.drawArrays(gl.TRIANGLES, 0, 3); const out = new Float32Array(4);
      gl.readPixels(0, 0, 1, 1, gl.RGBA, gl.FLOAT, out); return Array.from(out); }
    const body = shader.field.slice(0, shader.field.lastIndexOf('void main()'));
    const pf = program(body + '\nuniform vec3 probeO, probeD;\nvoid main(){int leaf;vec3 n;float hit=fieldHit(probeO,probeD,leaf,n);float c0,c1,c2,c3,c4;quarticAlongRay(probeO,probeD,uFieldOuter,c0,c1,c2,c3,c4);int count=quarticRoots(c0,c1,c2,c3,c4,0.8,1.2);fragColor=vec4(hit,float(count),R_[0],R_[1]);}');
    const field = [];
    for (const shift of [0, 1]) for (const name of ['axis', 'contact']) {
      gl.useProgram(pf);
      gl.uniform3fv(gl.getUniformLocation(pf, 'uFieldOuter'), fixture.gpu.field.outer);
      gl.uniform1i(gl.getUniformLocation(pf, 'uFieldHollow'), 0);
      gl.uniform1i(gl.getUniformLocation(pf, 'uFieldShift'), shift);
      gl.uniform3fv(gl.getUniformLocation(pf, 'probeO'), fixture.gpu.field[name].origin);
      gl.uniform3fv(gl.getUniformLocation(pf, 'probeD'), fixture.gpu.field[name].direction);
      field.push({name, shift, expectedT: fixture.gpu.field[name].expectedT, channels: read(pf)});
    }
    const nibBody = shader.main.slice(shader.main.indexOf('float sweepF('), shader.main.indexOf('// the mark on the wall:'));
    const pn = program('#version 300 es\nprecision highp float;out vec4 outColor;uniform vec2 q;\n' + nibBody +
      '\nvoid main(){outColor=vec4(sweepMinGL(q,vec2(0),vec2(160),.2,.8),0,0,1);}');
    const n = fixture.gpu.nib, t = n.parameter, pressure = .2 + .6*t, r = 5 + 15*pressure**2;
    const len = Math.hypot(160, 160), dr = 30*.6*pressure, along = -dr/len;
    const normal = [(along-Math.sqrt(1-along**2))/Math.sqrt(2),(along+Math.sqrt(1-along**2))/Math.sqrt(2)];
    const q = normal.map(v => 160*t+(r+n.clearanceMM)*v);
    gl.useProgram(pn); gl.uniform2fv(gl.getUniformLocation(pn, 'q'), q);
    const sd = read(pn)[0];
    const cpuWitness = Math.hypot(q[0]-160*t,q[1]-160*t)-r;
    const uploadedQ = Array.from(new Float32Array(q));
    const uploadedWitness = Math.hypot(uploadedQ[0]-160*t,uploadedQ[1]-160*t)-r;
    const ext = gl.getExtension('WEBGL_debug_renderer_info');
    const result = {field, nib: {q, uploadedQ, t, pressure, radiusMM: r, witnessMM: cpuWitness,
      witnessOnUploadedQMM: uploadedWitness, shaderMinimumMM: sd,
      witnessInside: cpuWitness < 0, shaderInside: sd < 0, realArithmeticScanEnvelopeMM: (len+30*.8*.6)/64},
      renderer: ext ? gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) : gl.getParameter(gl.RENDERER), glError: gl.getError()};
    gl.deleteProgram(pf); gl.deleteProgram(pn); gl.deleteFramebuffer(fb); gl.deleteTexture(tex); gl.deleteVertexArray(vao);
    return result;
  }, {shader, fixture});
  // The composer's own CPU consumer of its contact root: the event exists, the material-interval hit may not.
  const cpu = await p.evaluate(f => {
    const a = window.__attack4, F = {on: true, at: [0, 0, 0], params: {a: .2, b: .1, c: .1}, hollow: false};
    return ['axis', 'contact'].map(name => { const ray = {origin: f.gpu.field[name].origin, dir: f.gpu.field[name].direction};
      return {name, roots: a.isolateRealRoots(a.fieldPoly(F.params, ray.origin, ray.dir), .8, 1.2, 1e-7, 20000),
        hit: a.hitField(ray, F, 20000)}; });
  }, fixture);
  assert.deepEqual(errors, []); assert.equal(direct.glError, 0);
  parity.forEach(r => { assert(r.ok && r.centre); assert.equal(r.glError, 0); assert.equal(r.samples, 0);
    assert.equal(r.shaderLog.main, ''); assert.equal(r.shaderLog.field, ''); });
  const result = {schema: 'attack-4-gpu-receipts/v1', benchSha256: hash(source),
    runnerSha256: hash(fs.readFileSync(fileURLToPath(import.meta.url))), browser: await browser.version(),
    scope: 'SwiftShader only; six captured pixel-centre crossing readings and one-pixel calls to actual shader functions; no whole-image or hardware claim',
    parity, direct, cpu, pageErrors: errors};
  if (process.argv.includes('--write')) fs.writeFileSync(path.join(here, 'gpu-receipts.json'), JSON.stringify(result, null, 2) + '\n');
  console.log(JSON.stringify(result, null, 2));
} finally { await browser.close(); }
