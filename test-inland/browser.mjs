import puppeteer from 'puppeteer';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';

// Every product mutation below is a canvas gesture followed by native keyboard
// input. Test HTTP endpoints only read Rama, hold admission or inject a labelled
// provider fault. They do not implement the product's transport.
const root = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const out = path.join(root, 'target/inland/receipts');
const base = 'http://localhost:8127';
const workspace = `receipt-${Date.now()}`;
const url = `${base}/?workspace=${workspace}`;
const receipt = {workspace, url, checks:[], failures:[], browserErrors:[], success:false};
await fs.mkdir(out, {recursive:true});
let browser;
const check = (name, detail={}) => {receipt.checks.push({name,...detail}); console.log(`PASS ${name}`);};
const pause = ms => new Promise(resolve=>setTimeout(resolve,ms));
const wait = (p,fn,...args)=>p.waitForFunction(fn,{timeout:30000},...args);
const cell = (p,key)=>p.evaluate(key=>window.__inlandCell(key),key);
const control = async (action, body={}) => {
  const r = await fetch(`${base}/__test/${action}`, {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  assert.equal(r.status,200,`Test control ${action}: launch with --test-controls`);
  return r.json();
};
const row = (name,layer='base')=>control('read',{kind:'rows',path:[workspace,`${layer}/${name}`]});
const metrics = async ()=>{
  const r = await fetch(`${base}/__test/metrics`); assert.equal(r.status,200); return r.json();
};
const launch = ()=>puppeteer.launch({executablePath:'/usr/bin/google-chrome',headless:false,
  args:['--no-sandbox','--enable-unsafe-webgpu','--use-angle=vulkan','--enable-features=Vulkan,WebGPU,UnsafeWebGPU','--ignore-gpu-blocklist','--disable-dev-shm-usage'],
  defaultViewport:{width:1440,height:1000,deviceScaleFactor:1}});
const client = async ()=>{
  const context=await browser.createIncognitoBrowserContext(), p=await context.newPage();
  p.on('pageerror',e=>receipt.browserErrors.push(e.message));
  p.on('console',m=>{if(m.type()==='error') receipt.browserErrors.push(m.text());});
  await p.goto(url,{waitUntil:'networkidle0'});
  await wait(p,()=>window.__inlandGPU?.draws>0 && window.__inlandScenePartCount===2 && Boolean(window.__inlandTargetBox?.('instrument-view/active-instrument')));
  return p;
};
const click = async (p,id)=>{
  await wait(p,id=>Boolean(window.__inlandTargetBox?.(id)),id);
  const b=await p.evaluate(id=>window.__inlandTargetBox(id),id);
  await p.mouse.click(b.x+b.w/2,b.y+b.h/2);
};
const fill = async(p,id,text)=>{
  await click(p,id); await p.keyboard.down('Control'); await p.keyboard.press('A'); await p.keyboard.up('Control');
  await p.keyboard.sendCharacter(text);
  await wait(p,({id,text})=>document.getElementById(id)?.value===text,{id,text});
};
const tab = (p,name)=>click(p,`frame-view/tab-${name}`);
const point = async(p,name,select=true)=>{
  const pos=await p.evaluate(name=>{
    const r=document.querySelector('#softland').getBoundingClientRect();
    for(let y=r.top+340;y<Math.min(r.bottom,r.top+655);y+=6)
      for(let x=r.left+200;x<Math.min(r.right,r.left+850);x+=6)
        if(window.__inlandPickAt(x,y)===name) return {x,y};
    return null;
  },name);
  assert.ok(pos,`The real Region3D picker finds ${name}`);
  if(select) await p.mouse.click(pos.x,pos.y); else await p.mouse.move(pos.x,pos.y);
};
const selection = (p,name)=>wait(p,name=>window.__inlandCell('selection')===name,name);
const inspect = async(p,name)=>{
  await tab(p,'definitions');
  let found=false;
  for(let n=0;n<4;n++){
    await pause(150);
    found=await p.evaluate(id=>Boolean(window.__inlandTargetBox?.(id)),`library-view/rows/${name}/inspect`);
    if(found) break;
    await click(p,'library-view/page');
  }
  assert.ok(found,`Addressable library contains ${name}`);
  await click(p,`library-view/rows/${name}/inspect`);
  await wait(p,name=>window.__inlandCell('inspecting')===name && Boolean(window.__inlandTargetBox?.('editor-view/record')),name);
};
const admission = (p,status)=>wait(p,status=>window.__inlandCell('admission')?.status===status,status);
const apply = async(p,source,button='apply',status='accepted')=>{
  await fill(p,'editor-view/record',source);
  const previous=(await cell(p,'admission'))?.['request-id'];
  await click(p,`editor-view/${button}`);
  await wait(p,previous=>window.__inlandCell('admission')?.['request-id']!==previous,previous);
  await admission(p,status);
};
const identity = name=>`{:name "${name}" :label "Direct subject" :body {:steps [] :return [:get :subject]}}`;
const containing = name=>`{:name "${name}" :label "Containing object or self" :body {:steps [{:out :result :op :call :args {:name "containing-object-or-self" :bindings {:subject [:get :subject]}}}] :return [:get :result]}}`;
const supportText = p=>p.evaluate(()=>window.__inland['support-view']?.value?.find(x=>x.id==='supports')?.text);
const work = p=>p.evaluate(()=>({headline:window.__inlandWork['frame-view/headline'],shape:window.__inlandGPU['scene-preparations']}));
const liveParentReads = owner=>owner.paths.filter(p=>p.path.endsWith(':parent]]')).reduce((n,p)=>n+(p.kind==='opened'?p.count:p.kind==='closed'?-p.count:0),0);

try {
  const bundle=await fetch(`${base}/js/main.js`);
  assert.equal(bundle.status,200,'Electric browser compilation is required before browser verification. Run bin/inland build and complete its normal activation.');
  await control('release'); await control('fault',{mode:null});
  browser=await launch();
  const a=await client(), b=await client();
  receipt.adapter=await a.evaluate(()=>window.__inlandAdapter);
  const owners=await Promise.all([a,b].map(p=>p.evaluate(()=>window.__inland.owner)));
  receipt.owners=owners;
  await a.screenshot({path:path.join(out,'01-instrument.png'),fullPage:true});

  // 1. Open the very instrument doing the pointing; admission controls behavior.
  await point(a,'orb'); await selection(a,'orb'); await point(a,'ring'); await selection(a,'ring');
  await click(a,'instrument-view/active-instrument'); await selection(a,'pointer');
  await wait(a,()=>window.__inlandCell('inspecting')==='targeting');
  const original=await row('targeting');
  await apply(a,'{:name "targeting" :body {:steps [{:out :bad :op :eval}] :return nil}}','apply','rejected');
  assert.deepEqual(await row('targeting'),original);
  await point(a,'orb'); await selection(a,'orb');
  await click(a,'editor-view/example');
  assert.match((await cell(a,'draft')).value,/containing-object-or-self/);
  await control('hold'); await click(a,'editor-view/apply'); await admission(a,'pending');
  await point(b,'ring'); await selection(b,'ring'); assert.deepEqual(await row('targeting'),original);
  await control('release'); await admission(a,'accepted');
  await point(a,'orb'); await selection(a,'assembly'); await point(a,'ring'); await selection(a,'assembly');
  await click(a,'instrument-view/active-instrument'); await selection(a,'pointer');
  check('1: rejection and pending preserve the accepted rule; acceptance changes both parts while the instrument remains addressable');
  await click(a,'editor-view/appearance');
  const oldAppearance=(await cell(a,'draft')).value;
  const appearance=oldAppearance.replace(':width 2',':width 3.5');
  assert.notEqual(appearance,oldAppearance); await apply(a,appearance);
  await a.screenshot({path:path.join(out,'02-opened.png'),fullPage:true});
  check('1: authored instrument presentation edited through the same record editor');

  // 2. Keep a second tool, give it an independent named rule, pin and promote.
  await fill(a,'editor-view/variation','group-pointer'); await click(a,'editor-view/keep'); await admission(a,'accepted');
  await inspect(a,'targeting'); await fill(a,'editor-view/variation','second-targeting');
  await click(a,'editor-view/clone-record'); await admission(a,'accepted');
  await inspect(a,'group-pointer');
  await apply(a,'{:name "group-pointer" :label "Group pointer" :catalog "instruments" :targeting "second-targeting" :presentation "instrument-appearance"}');
  await click(a,'tools-view/tools/group-pointer/activate');
  await point(a,'orb'); await selection(a,'assembly');
  await inspect(a,'targeting'); await apply(a,identity('targeting'));
  await point(a,'orb'); await selection(a,'assembly');
  await click(a,'tools-view/tools/pointer/activate'); await point(a,'orb'); await selection(a,'orb');
  await inspect(a,'targeting'); await apply(a,containing('targeting'),'candidate');
  await point(a,'orb'); await selection(a,'orb');
  await click(a,'editor-view/candidate-context'); await point(a,'orb'); await selection(a,'assembly');
  await click(a,'editor-view/base-context'); await click(a,'editor-view/reload'); await click(a,'editor-view/pin');
  await click(a,'editor-view/promote'); await admission(a,'accepted');
  await point(a,'orb'); await selection(a,'orb');
  await click(a,'editor-view/live'); await point(a,'orb'); await selection(a,'assembly');
  assert.equal((await row('targeting')).body.steps[0].args.name,'containing-object-or-self');
  await a.screenshot({path:path.join(out,'03-kept.png'),fullPage:true});
  check('2: independently referenced tool, candidate/base contexts, exact pin, live follow and revision-checked promotion');

  // 3. Narrow maintained work, multiple supports, removal and independent owners.
  await point(b,'orb',false); await pause(250);
  const unchanged=await work(b), beforeReads=liveParentReads((await metrics())[owners[1]]);
  await inspect(a,'targeting'); await apply(a,identity('targeting'));
  await wait(b,()=>window.__inland['preview-view']?.value?.[0]?.text==='Pointing would select orb');
  assert.equal(await cell(b,'selection'),'ring','A completed pointing command is not replayed by a rule edit');
  const afterReads=liveParentReads((await metrics())[owners[1]]);
  assert.ok(afterReads<beforeReads,'Removing the authored parent read disposes the dependent proxy');
  assert.deepEqual(await work(b),unchanged,'Unrelated headline and Region3D preparation stay unchanged');
  await wait(b,()=>window.__inland['support-view']?.value?.[0]?.text.includes('2 supports'));
  await inspect(a,'support-orb'); await apply(a,'{:name "support-orb" :body {:steps [] :return nil}}');
  await wait(b,()=>window.__inland['support-view']?.value?.[0]?.text.includes('1 supports'));
  await inspect(a,'support-ring'); await apply(a,'{:name "support-ring" :body {:steps [] :return nil}}');
  await wait(b,()=>window.__inland['support-view']?.value?.[0]?.text.includes('0 conclusion'));
  assert.match(await supportText(b),/Root absence complete: 1/);
  await inspect(a,'orb'); await apply(a,(await cell(a,'draft')).value.replace(':parent "assembly"',':parent nil'));
  assert.equal((await row('orb')).parent,null);
  const beforeClose=(await metrics())[owners[0]], gpuClosed=await a.evaluate(()=>window.__inlandGPU.closed||0);
  await click(a,'frame-view/close'); await wait(a,n=>window.__inlandGPU.closed>n && Boolean(window.__inlandTargetBox?.('closed/reopen')),gpuClosed);
  const closed=(await metrics())[owners[0]];
  assert.ok(closed.closed>beforeClose.closed,'Closing releases source subscriptions');
  assert.equal(await a.evaluate(()=>document.querySelectorAll('.native-input').length),0);
  const retained=closed.paths.filter(p=>p.kind==='opened' && !p.path.includes('base/world') && !p.path.includes('closed-view') && !p.path.includes('event/reopen') && !p.path.includes('reopen-rule'));
  for(const p of retained){
    const ended=closed.paths.find(q=>q.kind==='closed'&&q.path===p.path)?.count||0;
    assert.equal(ended,p.count,`Closed view released ${p.path}`);
  }
  await point(b,'ring'); await selection(b,'ring'); assert.ok(await row('group-pointer'));
  await click(a,'closed/reopen'); await wait(a,()=>Boolean(window.__inlandTargetBox?.('frame-view/tab-walk')));
  check('3: tracked read removed, unrelated work unchanged, alternative support retained then withdrawn, complete absence and independent view disposal', {beforeReads,afterReads,closed,completedSelection:'ring'});

  // 4. The authored caller invokes a total frontier/visited step; owner repeats it.
  await tab(a,'walk'); await click(a,'walk-view/run');
  await wait(a,()=>window.__inlandCell('walk-result')?.status==='complete');
  assert.deepEqual((await cell(a,'walk-result')).value,['assembly','orb','ring']);
  await fill(a,'walk-view/budget','1'); await click(a,'walk-view/run');
  await wait(a,()=>window.__inlandCell('walk-result')?.status==='exhausted');
  await fill(a,'walk-view/budget','128'); await click(a,'walk-view/run'); await click(a,'walk-view/cancel');
  await wait(a,()=>window.__inlandCell('walk-result')?.status==='cancelled');
  const cancelled=await cell(a,'walk-result'); await pause(300); assert.deepEqual(await cell(a,'walk-result'),cancelled);
  await click(a,'walk-view/open-caller'); assert.match((await cell(a,'draft')).value,/walk-step/);
  check('4: ordinary authored caller/step crosses a branch and cycle, with visible exhaustion and cancellation');

  // 5. A real bounded call, followed by explicitly controlled non-success paths.
  await tab(a,'resident');
  await fill(a,'resident-view/prompt','Describe a tool that remains usable while its targeting rule changes, in two short sentences.');
  await click(a,'resident-view/ask');
  const ask=await cell(a,'observing'); assert.ok(ask);
  await a.waitForFunction(()=>window.__inland['resident-view']?.value?.find(x=>x.id==='status')?.text==='Status: :complete',{timeout:100000});
  const acceptedReply=await row(ask);
  assert.equal(acceptedReply.status,'complete'); assert.equal(acceptedReply.provenance,'machine'); assert.ok(acceptedReply.reply?.length);
  receipt.provider={activity:ask,model:acceptedReply.activity.model,status:acceptedReply.status,reply:acceptedReply.reply};
  for(const [mode,status] of [['failure','failed'],['uncertain','unconfirmed']]){
    await control('fault',{mode}); await click(a,'resident-view/ask');
    await wait(a,s=>window.__inland['resident-view']?.value?.find(x=>x.id==='status')?.text===`Status: :${s}`,status);
    const name=await cell(a,'observing'), current=await row(name);
    assert.equal(current.status,status); await pause(500); assert.deepEqual(await row(name),current,'No blind retry');
  }
  await control('fault',{mode:null});
  await click(a,'frame-view/close'); await click(a,'closed/reopen'); await tab(a,'resident');
  await click(a,`resident-view/history/${ask}/observe`);
  await wait(a,()=>window.__inland['resident-view']?.value?.find(x=>x.id==='machine-mark')?.text.includes('machine-authored'));
  check('5: real Claude reply admitted with provenance; controlled failure/uncertainty retained without retry; reopened view retrieves reply');

  // Recovery uses a new browser process and new app JVM against the same disk.
  await browser.close(); browser=null;
  const oldPid=await fs.readFile(path.join(root,'.inland-runtime/app.pid'),'utf8');
  execFileSync(path.join(root,'bin/inland'),['stop-app'],{cwd:root});
  execFileSync(path.join(root,'bin/inland'),['serve','--test-controls'],{cwd:root,timeout:120000,stdio:'pipe'});
  const newPid=await fs.readFile(path.join(root,'.inland-runtime/app.pid'),'utf8'); assert.notEqual(oldPid,newPid);
  browser=await launch(); const c=await client();
  await click(c,'tools-view/tools/group-pointer/activate'); await point(c,'ring'); await selection(c,'assembly');
  await inspect(c,'second-targeting'); assert.match((await cell(c,'draft')).value,/containing-object-or-self/);
  assert.deepEqual(await row(ask),acceptedReply); assert.equal(await c.evaluate(()=>localStorage.length),0);
  check('Recovery: replaced browser and app JVM retrieve both instruments and the accepted provider reply from real isolated Rama storage',{oldPid:oldPid.trim(),newPid:newPid.trim()});
  assert.equal(await c.evaluate(()=>window.__inlandGPU.errors||0),0);
  assert.equal(await c.evaluate(()=>window.__inlandRenderError||null),null);
  assert.deepEqual(receipt.browserErrors,[]);
  receipt.success=true;
} catch(error){
  receipt.failures.push(error.stack); console.error(error.message); process.exitCode=1;
  if(browser){
    receipt.states=await Promise.all((await browser.pages()).map(p=>p.evaluate(()=>({state:window.__inland,gpu:window.__inlandGPU,error:window.__inlandRenderError})).catch(()=>null)));
    for(const [i,p] of (await browser.pages()).entries()) await p.screenshot({path:path.join(out,`failure-${i}.png`),fullPage:true}).catch(()=>{});
  }
} finally {
  await control('release').catch(()=>{}); await control('fault',{mode:null}).catch(()=>{});
  await fs.writeFile(path.join(out,'browser.json'),JSON.stringify(receipt,null,2));
  if(browser) await browser.close();
}
