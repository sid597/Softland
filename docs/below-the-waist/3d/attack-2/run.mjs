/** Attack-2 numerical bench, not client implementation. Node built-ins only.
 * Inputs: records.json and the pinned committed bench's over/composeAt bytes.
 * Output: deterministic receipts on stdout; --write also retains receipts.json.
 * New reference capabilities: exact spherical regions, bounded ellipsoid
 * distance, and affine domain-relation composition with explicit query demand.
 * No browser, shader, kernel, or production executor is exercised here.
 */
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {readFileSync, writeFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import vm from 'node:vm';

const here = new URL('.', import.meta.url);
const repo = fileURLToPath(new URL('../../../../', here));
const records = JSON.parse(readFileSync(new URL('records.json', here), 'utf8'));
const sha = x => createHash('sha256').update(typeof x === 'string' ? x : JSON.stringify(x)).digest('hex');
const clone = x => JSON.parse(JSON.stringify(x));
const benchPath = 'docs/below-the-waist/3d/bench-2/seam-bench.html';
const bench = execFileSync('git', ['show', `${records.composerCommit}:${benchPath}`], {cwd: repo, encoding: 'utf8'});
// Both pinned functions are single lines. Fail on a changed extraction shape.
const borrowed = ['over', 'composeAt'].map(name => {
  const line = bench.split('\n').find(x => x.startsWith(`  function ${name}(`));
  assert.ok(line?.trimEnd().endsWith('}'), `missing complete bench function ${name}`);
  return line;
}).join('\n');
const benchFns = vm.runInNewContext(`${borrowed}\n({over, composeAt})`);
const composeAt = (order, layers) => clone(benchFns.composeAt(order, layers));
const PI = Math.PI, TAU = 2 * PI, R = records.sphere.radiusMm;
const rad = deg => deg * PI / 180;
const dot = (a, b) => a.reduce((s, x, i) => s + x * b[i], 0);
const scale = (a, k) => a.map(x => x * k);
const add = (a, b) => a.map((x, i) => x + b[i]);
const norm = a => Math.hypot(...a);
const unit = a => scale(a, 1 / norm(a));
const cross = (a,b) => [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]];
const distance = (a,b) => norm(a.map((x,i) => x-b[i]));
const near = (a,b,eps=1e-9) => assert.ok(Math.abs(a-b) <= eps, `${a} != ${b} within ${eps}`);
const vectorNear = (a,b,eps=1e-9) => a.forEach((x,i) => near(x,b[i],eps));
const identity = [[1,0,0],[0,1,0]];
const apply = (m,p) => m.map(row => row[0]*p[0]+row[1]*p[1]+row[2]);
function inverse(m) {
  const [[a,b,c],[d,e,f]] = m, det = a*e-b*d;
  assert.ok(Math.abs(det)>1e-15);
  return [[e/det,-b/det,(b*f-e*c)/det],[-d/det,a/det,(d*c-a*f)/det]];
}
// after(before(p)); explicit coordinate maps, not a production graph language.
function combine(after,before) {
  return after.map(row => [row[0]*before[0][0]+row[1]*before[1][0],
    row[0]*before[0][1]+row[1]*before[1][1],
    row[0]*before[0][2]+row[1]*before[1][2]+row[2]]);
}
function sphereUV([u,v],radius=R) {
  const lon=u/radius, lat=v/radius;
  return [radius*Math.cos(lat)*Math.cos(lon),radius*Math.sin(lat),radius*Math.cos(lat)*Math.sin(lon)];
}
const sphereDegrees = p => sphereUV(p.map(x=>R*rad(x)));
function baseUV(p,radius=R) {
  let lon=Math.atan2(p[2],p[0]);
  if(lon<0) lon+=TAU; // the fixtures' authored domains straddle +pi
  return [radius*lon,radius*Math.atan2(p[1],Math.hypot(p[0],p[2]))];
}
// atan2 remains useful near coincident/antipodal locations. Mathematical sphere
// distance is exact; computed receipts are binary64, not interval arithmetic.
function sphereDistance(a,b,radius=R) {
  const x=unit(a),y=unit(b);
  return radius*Math.atan2(norm(cross(x,y)),dot(x,y));
}
function capBounds(n,radius) {
  if(radius>=PI*R) return [[-R,R],[-R,R],[-R,R]];
  const c=Math.cos(radius/R),s=Math.sin(radius/R);
  return n.map(x => {
    const b=Math.sqrt(Math.max(0,1-x*x))*s;
    return [R*(-x>=c ? -1 : x*c-b),R*(x>=c ? 1 : x*c+b)];
  });
}
function capMember(n,radius,p) { return sphereDistance(n,p)<=radius; }
function clippedPaint(region,p,paint) {
  assert.equal(region.operation,'intrinsic-sphere-ball');
  assert.equal(region.support.radiusMm,R);
  return capMember(region.seed,region.radiusMm,p) ? paint : [0,0,0,0];
}
function chartPieces(p) {
  const lat=Math.atan2(p[1],Math.hypot(p[0],p[2]));
  const cut=rad(records.reachTool.polarChartAboveDegrees);
  let lon=Math.atan2(p[2],p[0]);
  if(lon>=PI) lon-=TAU;
  return [lat>cut?'north-xz':null,lat<=cut&&lon<0?'longitude-negative':null,
    lat<=cut&&lon>=0?'longitude-nonnegative':null].filter(Boolean);
}
function metricInChart(matrix,latitude) {
  const g=Math.cos(latitude)**2,[[a,b],[c,d]]=matrix;
  return [[g*a*a+c*c,g*a*b+c*d],[g*a*b+c*d,g*b*b+d*d]];
}
function capReceipt() {
  const f=records.reachTool,n=unit(sphereDegrees(f.seedDegrees));
  const q=sphereDegrees(f.membershipProbeDegrees);
  const intrinsic=sphereDistance(n,q);
  const frozen=R*Math.cos(rad(f.seedDegrees[1]))*rad(f.membershipProbeDegrees[0]-f.seedDegrees[0]);
  assert.ok(intrinsic<f.radiusMm && frozen>f.radiusMm);
  assert.ok(intrinsic>f.changedRadiusMm);
  const meridian=sphereUV([R*rad(f.seedDegrees[0]),R*(rad(f.seedDegrees[1])+f.meridianProbeOffsetRadians)]);
  const meridianIntrinsic=sphereDistance(n,meridian), chord=distance(scale(n,R),meridian);
  assert.ok(chord<f.radiusMm && meridianIntrinsic>f.radiusMm);
  const bounds=capBounds(n,f.radiusMm), chartCounts={'north-xz':0,'longitude-negative':0,'longitude-nonnegative':0};
  // Membership and bounds exercised at interior samples and 1440 exact cap
  // boundary samples. The all-point bound is the analytic extremum above.
  const e=unit(cross(n,[0,0,1])), f2=cross(n,e);
  let outsideBounds=0;
  for(let j=0;j<1440;j++) for(const rho of [0,f.radiusMm/(2*R),f.radiusMm/R]) {
    const a=TAU*j/1440;
    const p=scale(add(scale(n,Math.cos(rho)),scale(add(scale(e,Math.cos(a)),scale(f2,Math.sin(a))),Math.sin(rho))),R);
    for(let k=0;k<3;k++) if(p[k]<bounds[k][0]-1e-9 || p[k]>bounds[k][1]+1e-9) outsideBounds++;
    const pieces=chartPieces(p);assert.equal(pieces.length,1);chartCounts[pieces[0]]++;
  }
  assert.equal(outsideBounds,0);
  assert.ok(Object.values(chartCounts).every(x=>x>0));
  const pole=[0,R,0];
  assert.ok(capMember(n,f.radiusMm,pole));
  const polePieces=chartPieces(pole);assert.deepEqual(polePieces,['north-xz']);
  const polePaint=composeAt(polePieces,Object.fromEntries(polePieces.map(id=>[id,[.5,0,0,.5]])));
  const antipode=scale(n,-R), antipodalDistance=sphereDistance(n,antipode);
  near(antipodalDistance,PI*R);
  const retainedRegion={id:'reach-result@0',operation:'intrinsic-sphere-ball',support:clone(records.sphere),seed:n,radiusMm:f.radiusMm,pathDomain:'whole sphere'};
  const changedRegion={...clone(retainedRegion),id:'reach-result@1',radiusMm:f.changedRadiusMm};
  const reused={clip:clone(retainedRegion),paint:[0,0.4,0,0.4]};
  const retainedPaint=clippedPaint(reused.clip,q,reused.paint),changedPaint=clippedPaint(changedRegion,q,reused.paint);
  assert.deepEqual(retainedPaint,[0,0.4,0,0.4]);assert.deepEqual(changedPaint,[0,0,0,0]);
  const trim=f.trimProbe;
  const trimmedDirect=Math.abs(trim.seedLatitudeMm-trim.queryLatitudeMm);
  assert.ok(trim.seedLatitudeMm>trim.removedOpenLatitudeBandMm[1]);
  assert.ok(trim.queryLatitudeMm<trim.removedOpenLatitudeBandMm[0]);
  return {
    support:'G@0', radiusMm:f.radiusMm, metricAtSeed:metricInChart(identity,rad(f.seedDegrees[1])),
    intrinsicVsFrozen:{intrinsicMm:intrinsic,frozenMetricMm:frozen,inside:true,frozenInside:false,afterRadiusChange:{radiusMm:f.changedRadiusMm,inside:false}},
    intrinsicVsChord:{intrinsicMm:meridianIntrinsic,chordMm:chord,inside:false,chordInside:true},
    areaMm2:2*PI*R*R*(1-Math.cos(f.radiusMm/R)),worldBoundsMm:bounds,
    atlas:{pieces:['longitude-negative','longitude-nonnegative','north-xz'],counts:chartCounts,sampledPoints:4320,outsideBounds,
      pole:{chart:polePieces[0],coordinates:[pole[0],pole[2]],distanceMm:sphereDistance(n,pole),inside:capMember(n,f.radiusMm,pole),logicalDeposits:polePieces.length,alpha:polePaint.rgba[3],duplicateDepositAlpha:composeAt(['a','b'],{a:[0.5,0,0,0.5],b:[0.5,0,0,0.5]}).rgba[3]}},
    cutLocus:{distanceMm:antipodalDistance,pathWitnesses:'all great semicircles; no unique initial direction',membership:f.antipodeRadiiMm.map(radiusMm=>({radiusMm,inside:capMember(n,radiusMm,antipode),status:'complete'}))},
    trim:{untrimmedDistanceThenClipMm:trimmedDirect,untrimmedThenClipInside:trimmedDirect<=trim.radiusMm,stayWithinTrimmedDomain:{distance:'Infinity',inside:false,evidence:'the removed full latitude band disconnects the two components'}},
    reusableClip:{retainedRegion,queryPoint:q,retainedPaint,changedRadius70Paint:changedPaint}
  };
}
function ellipsoidReceipt() {
  const e=records.ellipsoid,[a,b,c]=e.semiaxesMm,N=e.rectanglePanels,dt=PI/(2*N);
  assert.ok(a>b && b>c);
  vectorNear(e.seedUnit,[1,0,0]); vectorNear(e.queryUnit,[0,1,0]);
  const speed=t=>Math.hypot(a*Math.sin(t),b*Math.cos(t));
  let pathLo=0,pathHi=0;
  for(let j=0;j<N;j++){pathLo+=speed(j*dt)*dt;pathHi+=speed((j+1)*dt)*dt;}
  // Speed is monotone on this quarter ellipse, so these are left/right
  // rectangle bounds in real arithmetic. Round OUT by much more than the
  // binary64 sum discrepancy; this is not a formally verified FP library.
  pathLo=Math.floor(pathLo*1e6)/1e6-1e-6;
  pathHi=Math.ceil(pathHi*1e6)/1e6+1e-6;
  const chord=Math.hypot(a,b), globalLower=Math.max(chord,c*PI/2);
  const lower=Math.floor(globalLower*1e6)/1e6-1e-6,upper=pathHi;
  const answers=e.radiusQueriesMm.map(radiusMm=>({radiusMm,lowerMm:lower,upperMm:upper,
    status:upper<=radiusMm?'inside':lower>radiusMm?'outside':'unresolved',
    unresolvedPortion:lower<=radiusMm && upper>radiusMm ? {queryPointMm:[0,b,0]} : null}));
  assert.deepEqual(answers.map(x=>x.status),['outside','unresolved','inside']);
  return {support:'E@0',worldBoundsMm:[[-a,a],[-b,b],[-c,c]],
    distanceLowerMm:lower,distanceUpperMm:upper,feasiblePathLengthBoundsMm:[pathLo,pathHi],
    panels:N,answers,limit:'The path upper bound is not a claim that this curve is globally shortest. Refining this quadrature alone cannot certify the unresolved 160 mm query.'};
}
function arcFrame(degrees) {
  const lon=rad(degrees[0]);
  return {n:unit(sphereDegrees(degrees)),e:[-Math.sin(lon),0,Math.cos(lon)]};
}
function arcPoint(mark,t) {
  const theta=(2*t-1)*mark.halfLengthMm/R;
  return scale(add(scale(mark.frame.n,Math.cos(theta)),scale(mark.frame.e,Math.sin(theta))),R);
}
// Closest point on a minor great-circle arc: maximize q dot c(theta) over
// the closed angular interval. Endpoints and stationary maxima are exhaustive.
function arcHit(mark,p,radius=R) {
  const q=unit(p),a=dot(q,mark.frame.n),b=dot(q,mark.frame.e),h=mark.halfLengthMm/radius;
  let angles=[-h,h];
  if(Math.hypot(a,b)<1e-14) return {inside:PI*radius/2<=mark.radiusMm,distanceMm:PI*radius/2,sourceParameters:'all',status:'complete'};
  const base=Math.atan2(b,a);
  for(let k=-1;k<=1;k++){const x=base+k*TAU;if(x>=-h&&x<=h) angles.push(x);}
  const found=angles.map(theta=>({t:(theta+h)/(2*h),point:scale(add(scale(mark.frame.n,Math.cos(theta)),scale(mark.frame.e,Math.sin(theta))),radius)}))
    .map(x=>({...x,distanceMm:sphereDistance(x.point,p,radius)})).sort((x,y)=>x.distanceMm-y.distanceMm);
  return {inside:found[0].distanceMm<=mark.radiusMm,distanceMm:found[0].distanceMm,sourceParameter:found[0].t,status:'complete'};
}
function makePackage() {
  const s=records.sequence,lon0=R*rad(s.seedDegrees[0]);
  const a={...clone(s.markA),frame:arcFrame(s.seedDegrees),support:'G@0',domain:'A@0'};
  const b={...clone(s.markB),point:arcPoint(a,s.markB.seedFromMarkAParameter),support:'G@0',domain:'B@0'};
  const bOrigin=[R*rad(s.rootB.originDegreesAndLatitudeMm[0]),s.rootB.originDegreesAndLatitudeMm[1]];
  b.authoredPoint=baseUV(b.point).map((x,i)=>x-bOrigin[i]);
  const aBase=[s.rootA.longitudeOffsetBoundsMm.map(x=>x+lon0),s.rootA.latitudeBoundsMm];
  const bBase=s.rootB.bounds.map((range,i)=>range.map(x=>x+bOrigin[i]));
  const stage0={revision:0,chartToBase:identity,faces:['A','B'],baseDomains:[aBase,bBase],bindings:[{mark:'kA',root:'A@0',chain:[]},{mark:'kB',root:'B@0',chain:[]}]};
  const stage1={...clone(stage0),revision:1,chartToBase:s.chart1ToBaseUV,
    bindings:stage0.bindings.map(b=>({...clone(b),chain:[...b.chain,'phi']}))};
  // This fixture's union is exactly one rectangle: equal latitude bounds and
  // overlapping longitude ranges. No general CAD Boolean is being supplied.
  assert.deepEqual(aBase[1],bBase[1]);assert.ok(aBase[0][1]>=bBase[0][0]);
  const union=[[Math.min(aBase[0][0],bBase[0][0]),Math.max(aBase[0][1],bBase[0][1])],aBase[1]];
  const stage2={revision:2,chartToBase:s.chart2ToBaseUV,faces:['M'],baseDomains:[union],
    bindings:stage1.bindings.map(b=>({...clone(b),chain:[...b.chain,b.mark==='kA'?'A-merge':'B-merge']}))};
  return {sphere:clone(records.sphere),order:clone(s.order),marks:{kA:a,kB:b},
    roots:{'A@0':{matrix:identity,bounds:aBase},
      'B@0':{matrix:[[1,0,bOrigin[0]],[0,1,bOrigin[1]]],bounds:s.rootB.bounds}},
    relations:{phi:{matrix:s.chart0ToChart1,cost:0,from:'G@0/S0',to:'G@1/S1',evidence:'same points',errorMm:0},
      'A-merge':{matrix:s.chart1ToChart2,cost:0,from:'G@1/A',to:'G@2/M',evidence:'restricted old support',errorMm:0},
      'B-merge':{matrix:s.chart1ToChart2,cost:1,from:'G@1/B',to:'G@2/M',evidence:'restricted old support',errorMm:0}},
    stages:[stage0,stage1,stage2]};
}
function relationMatrix(pkg,binding,work) {
  if(binding.materialized) return {status:'complete',matrix:binding.materialized,workUsed:0};
  let m=pkg.roots[binding.root].matrix,used=0;
  for(const id of binding.chain){
    const r=pkg.relations[id];
    if(!r) return {status:'unresolved',relation:id,reason:'missing retained dependency',workUsed:used};
    if(used+r.cost>work) return {status:'unresolved',relation:id,reason:'work limit',workUsed:used};
    used+=r.cost; m=combine(r.matrix,m);
  }
  return {status:'complete',matrix:m,workUsed:used};
}
function raySphere(ray,radius=R) {
  const b=dot(ray.origin,ray.direction),c=dot(ray.origin,ray.origin)-radius*radius;
  const t=-b-Math.sqrt(b*b-c); // fixtures: normalized inward ray, outside sphere
  return {tMm:t,point:add(ray.origin,scale(ray.direction,t))};
}
function query(pkg,stage,ray,work=1,order=pkg.order) {
  const radius=pkg.sphere.radiusMm;
  const h=raySphere(ray,radius),uv=baseUV(h.point,radius),current=apply(inverse(stage.chartToBase),uv);
  if(!stage.baseDomains.some(bounds=>uv.every((x,i)=>x>=bounds[i][0]&&x<=bounds[i][1])))
    return {support:{id:pkg.sphere.id,revision:stage.revision,status:'miss'},contributors:[],unresolved:[],composition:composeAt(order,{})};
  const contributors=[],unresolved=[]; const layers={};
  for(const binding of stage.bindings){
    const resolved=relationMatrix(pkg,binding,work);
    if(resolved.status!=='complete') {unresolved.push({mark:binding.mark,root:binding.root,targetRevision:stage.revision,queryChartPoint:current,...resolved});continue;}
    const authored=apply(inverse(resolved.matrix),current),root=pkg.roots[binding.root];
    if(!authored.every((x,i)=>x>=root.bounds[i][0]&&x<=root.bounds[i][1])) continue;
    const sourcePoint=sphereUV(apply(root.matrix,authored),radius),mark=pkg.marks[binding.mark];
    const hit=mark.curve==='tap' ? {inside:sphereDistance(mark.point,sourcePoint,radius)<=mark.radiusMm,distanceMm:sphereDistance(mark.point,sourcePoint,radius),sourceParameter:0,status:'complete'} : arcHit(mark,sourcePoint,radius);
    if(hit.inside){layers[mark.id]=mark.paint;contributors.push({mark:mark.id,sourceRevision:mark.revision,root:binding.root,authored,segment:mark.segmentId??'tap',...hit});}
  }
  return {support:{id:pkg.sphere.id,revision:stage.revision,face:stage.revision===2?'M':'A+B',status:'complete',...h},chartPoint:current,
    contributors,unresolved,composition:unresolved.length ? {status:'unresolved',rgba:null,knownContributors:contributors.map(x=>x.mark),possibleContributors:unresolved.map(x=>x.mark)} : composeAt(order,layers)};
}
function rayFor(mark,t) { const p=arcPoint(mark,t),n=unit(p);return {origin:scale(n,3*R),direction:scale(n,-1)}; }
function sequenceReceipt() {
  const pkg=makePackage(),s=records.sequence,bytes=JSON.stringify(pkg.marks),hash=sha(bytes);
  const ray=rayFor(pkg.marks.kA,s.freshQuerySourceParameter);
  const stages=pkg.stages.map(stage=>{
    const q=query(pkg,stage,ray);
    assert.equal(q.composition.status,'composed');
    assert.deepEqual(q.contributors.map(x=>x.mark),['kA','kB']);
    near(q.contributors[0].sourceParameter,s.freshQuerySourceParameter);
    assert.equal(JSON.stringify(pkg.marks),bytes);
    let maxWorldErrorMm=0,maxInverseErrorMm=0;
    for(const t of s.worldChecksAtParameters){
      const point=arcPoint(pkg.marks.kA,t),rootUV=baseUV(point),binding=stage.bindings[0];
      const m=relationMatrix(pkg,binding,1).matrix,current=apply(m,rootUV);
      maxWorldErrorMm=Math.max(maxWorldErrorMm,distance(point,sphereUV(apply(stage.chartToBase,current))));
      maxInverseErrorMm=Math.max(maxInverseErrorMm,distance(rootUV,apply(inverse(m),current)));
    }
    assert.ok(maxWorldErrorMm<1e-10);assert.ok(maxInverseErrorMm<1e-10);
    return {revision:stage.revision,faces:stage.faces,sourceBytesSha256:hash,maxWorldErrorMm,maxInverseErrorMm,freshQuery:q};
  });
  const current=pkg.stages[2],reverse=query(pkg,current,ray,1,[...s.order].reverse()),noOrder=query(pkg,current,ray,1,null);
  assert.deepEqual(stages[2].freshQuery.composition.rgba,[0.25,0,0.5,0.75]);
  assert.deepEqual(reverse.composition.rgba,[0.5,0,0.25,0.75]);
  assert.equal(noOrder.composition.status,'needs-policy');
  const pending=query(pkg,current,ray,0);
  assert.equal(pending.support.status,'complete');
  assert.equal(pending.composition.status,'unresolved');assert.equal(pending.composition.rgba,null);
  assert.deepEqual(pending.contributors.map(x=>x.mark),['kA']);
  // Missing the earlier map is a deliberately wrong implementation, not a
  // reported defect in bench 2. It must be observable away from old knots.
  const q0=baseUV(arcPoint(pkg.marks.kA,s.freshQuerySourceParameter));
  const wrong=apply(s.chart1ToChart2,q0);
  const wrongWorld=sphereUV(apply(current.chartToBase,wrong));
  const wrongShift=distance(wrongWorld,arcPoint(pkg.marks.kA,s.freshQuerySourceParameter));
  assert.ok(wrongShift>80);
  const materialized=clone(pkg);
  for(const binding of materialized.stages[2].bindings) binding.materialized=relationMatrix(materialized,binding,1).matrix;
  materialized.relations={}; // direct maps plus root restrictions remain sufficient
  const materializedQuery=query(materialized,materialized.stages[2],ray,0);
  assert.deepEqual(materializedQuery.composition,stages[2].freshQuery.composition);
  vectorNear(materializedQuery.contributors[0].authored,stages[2].freshQuery.contributors[0].authored);
  // Cold process: only bytes plus capability code enter; no previous JS object,
  // completed point-query memo, or live evaluation handle is available.
  const newRay=rayFor(pkg.marks.kA,s.secondQueryAfterReloadParameter);
  const retained=clone(pkg);retained.stages=[null,null,retained.stages[2]];
  const continuationBytes=JSON.stringify({pkg:retained,ray:newRay},null,2)+'\n';
  const cold=JSON.parse(execFileSync(process.execPath,[fileURLToPath(import.meta.url),'--cold'],{
    cwd:repo,input:continuationBytes,encoding:'utf8'}));
  assert.deepEqual(cold.composition,query(pkg,current,newRay).composition);
  near(cold.contributors[0].sourceParameter,s.secondQueryAfterReloadParameter);
  const missing=clone(pkg);delete missing.relations.phi;
  const missingAnswer=query(missing,missing.stages[2],ray);
  assert.equal(missingAnswer.composition.status,'unresolved');
  assert.ok(missingAnswer.unresolved.every(x=>x.reason==='missing retained dependency'));
  const seamQueries=[.49,.5,.51].map(t=>query(pkg,current,rayFor(pkg.marks.kA,t)));
  seamQueries.forEach(q=>assert.deepEqual(q.contributors.map(x=>x.mark),['kA','kB']));
  const outsideOldB=rayFor(pkg.marks.kA,s.secondQueryAfterReloadParameter);
  assert.ok(sphereDistance(pkg.marks.kB.point,raySphere(outsideOldB).point)<pkg.marks.kB.radiusMm);
  assert.deepEqual(query(pkg,current,outsideOldB).contributors.map(x=>x.mark),['kA']);
  if(process.argv.includes('--write')) writeFileSync(new URL('continuation.json',here),continuationBytes);
  return {stages,reverseOrder:reverse.composition,noOrder:noOrder.composition,
    pending,afterDemandResolved:query(pkg,current,ray,1).composition,
    materialized:materializedQuery.composition,coldReload:{newParameter:s.secondQueryAfterReloadParameter,answer:cold},
    missingDependency:missingAnswer.composition,negativeControls:{lastMapOnlyWorldShiftMm:wrongShift,
      pretendingPendingIsAbsent:composeAt(s.order,{kA:pkg.marks.kA.paint})},
    seamParameters:[.49,.5,.51],seamContributors:seamQueries.map(q=>q.contributors.map(x=>x.mark)),
    outsideOldB:{parameter:s.secondQueryAfterReloadParameter,distanceToUnclippedBlueMm:sphereDistance(pkg.marks.kB.point,raySphere(outsideOldB).point),
      blueRootCoordinates:apply(inverse(pkg.roots['B@0'].matrix),baseUV(raySphere(outsideOldB).point)),
      contributors:query(pkg,current,outsideOldB).contributors.map(x=>x.mark)},
    metricAtLatitude60InChart2:metricInChart(s.chart2ToBaseUV,rad(s.seedDegrees[1])),
    packageBytesSha256:sha(pkg),continuationBytesSha256:sha(continuationBytes),sourceBytesSha256:hash,
    recordHashes:Object.fromEntries(Object.entries(pkg.marks).map(([id,mark])=>[id,sha(mark)]))};
}
if(process.argv.includes('--cold')) {
  const {pkg,ray}=JSON.parse(readFileSync(0,'utf8'));
  process.stdout.write(JSON.stringify(query(pkg,pkg.stages[2],ray))+'\n');
} else {
  const receipt={schema:'attack-2-receipts/v1',node:process.version,
    inputs:{composerCommit:records.composerCommit,benchPath,benchSha256:sha(bench),borrowedFunctions:['over','composeAt'],borrowedFunctionsSha256:sha(borrowed),recordsSha256:sha(readFileSync(new URL('records.json',here),'utf8')),runnerSha256:sha(readFileSync(fileURLToPath(import.meta.url),'utf8'))},
    sphericalRegion:capReceipt(),ellipsoid:ellipsoidReceipt(),sequence:sequenceReceipt(),
    scope:'New Node reference capabilities plus two exact committed bench functions. No client, GPU, browser, arbitrary-surface distance solver, or painting replay claim.'};
  const output=JSON.stringify(receipt,null,2)+'\n';
  if(process.argv.includes('--write')) writeFileSync(new URL('receipts.json',here),output);
  process.stdout.write(output);
}
