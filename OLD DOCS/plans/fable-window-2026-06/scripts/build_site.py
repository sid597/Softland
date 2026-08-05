#!/usr/bin/env python3
"""Build a standalone navigable index.html from the fable-window artifacts:
plan.md, open-threads.md, 12 chunk summaries, and the full message corpus.
Self-contained (no network), dark-mode default, message IDs cross-linked.
"""
import json, os, re, html

BASE = '/mnt/data/projects/Softland/docs/plans/fable-window-2026-06'
OUT = os.path.join(BASE, 'index.html')
MSG_TRUNCATE = 3000  # chars embedded per message; full text stays in messages.jsonl


def slugify(s):
    return re.sub(r'[^a-z0-9]+', '-', s.lower()).strip('-')


def md_to_html(md, doc_id):
    """Pragmatic markdown -> HTML for the constructs used in these docs."""
    lines = md.split('\n')
    out, toc = [], []
    in_code = False
    in_table = False
    list_stack = []  # 'ul' | 'ol'
    para = []

    def close_para():
        if para:
            out.append('<p>' + inline(' '.join(para)) + '</p>')
            para.clear()

    def close_lists(to_depth=0):
        while len(list_stack) > to_depth:
            out.append(f'</{list_stack.pop()}>')

    def close_table():
        nonlocal in_table
        if in_table:
            out.append('</tbody></table></div>')
            in_table = False

    def inline(s):
        s = html.escape(s, quote=False)
        s = re.sub(r'`([^`]+)`', r'<code>\1</code>', s)
        s = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', s)
        s = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', s)
        return s

    i = 0
    header_seen = False
    while i < len(lines):
        line = lines[i]
        if line.startswith('```'):
            close_para(); close_lists(); close_table()
            if not in_code:
                out.append('<pre><code>')
            else:
                out.append('</code></pre>')
            in_code = not in_code
            i += 1; continue
        if in_code:
            out.append(html.escape(line))
            i += 1; continue

        # table
        if line.lstrip().startswith('|') and line.rstrip().endswith('|'):
            cells = [c.strip() for c in line.strip().strip('|').split('|')]
            nxt = lines[i+1] if i + 1 < len(lines) else ''
            is_sep = bool(re.fullmatch(r'\s*\|?[\s:|-]+\|?\s*', nxt)) and '-' in nxt
            if not in_table and is_sep:
                close_para(); close_lists()
                out.append('<div class="tblwrap"><table><thead><tr>'
                           + ''.join(f'<th>{inline(c)}</th>' for c in cells)
                           + '</tr></thead><tbody>')
                in_table = True
                i += 2; continue
            if in_table:
                out.append('<tr>' + ''.join(f'<td>{inline(c)}</td>' for c in cells) + '</tr>')
                i += 1; continue
        else:
            close_table()

        m = re.match(r'^(#{1,4})\s+(.*)', line)
        if m:
            close_para(); close_lists()
            lvl = len(m.group(1))
            text = m.group(2)
            if lvl == 1 and not header_seen:
                header_seen = True
                out.append(f'<h1>{inline(text)}</h1>')
            else:
                hid = f'{doc_id}-{slugify(text)[:60]}'
                out.append(f'<h{lvl} id="{hid}">{inline(text)}</h{lvl}>')
                if lvl == 2:
                    toc.append((hid, text))
            i += 1; continue

        if re.fullmatch(r'\s*(-{3,}|\*{3,})\s*', line):
            close_para(); close_lists()
            out.append('<hr>')
            i += 1; continue

        m = re.match(r'^(\s*)([-*]|\d+\.)\s+(.*)', line)
        if m:
            close_para()
            depth = 1 + (1 if len(m.group(1)) >= 2 else 0)
            kind = 'ol' if m.group(2)[0].isdigit() else 'ul'
            while len(list_stack) > depth:
                out.append(f'</{list_stack.pop()}>')
            while len(list_stack) < depth:
                list_stack.append(kind)
                out.append(f'<{kind}>')
            out.append(f'<li>{inline(m.group(3))}</li>')
            i += 1; continue

        if line.startswith('>'):
            close_para(); close_lists()
            out.append(f'<blockquote>{inline(line.lstrip("> "))}</blockquote>')
            i += 1; continue

        if not line.strip():
            close_para(); close_lists()
            i += 1; continue

        para.append(line.strip())
        i += 1

    close_para(); close_lists(); close_table()
    body = '\n'.join(x for x in out if x is not None)
    if toc:
        toc_html = '<nav class="toc"><b>On this page</b><ul>' + ''.join(
            f'<li><a href="#{h}" onclick="document.getElementById(\'{h}\').scrollIntoView();return false;">{html.escape(t)}</a></li>'
            for h, t in toc) + '</ul></nav>'
        # insert toc after h1
        body = re.sub(r'(</h1>)', r'\1' + toc_html, body, count=1)
    return body


def linkify_ids(h):
    return re.sub(r'\b([CX]-\d{4})\b', r'<a class="mid" href="#msg-\1">\1</a>', h)


def read(p):
    with open(p) as f:
        return f.read()


def main():
    plan_html = linkify_ids(md_to_html(read(os.path.join(BASE, 'plan.md')), 'plan'))
    threads_html = linkify_ids(md_to_html(read(os.path.join(BASE, 'open-threads.md')), 'threads'))

    chunks = []
    for n in range(1, 13):
        p = os.path.join(BASE, 'data', 'summaries', f'chunk-{n:02d}-summary.md')
        raw = read(p)
        m = re.search(r'#\s*Chunk\s*\d+\s*summary\s*\(([^)]*)\)', raw)
        label = m.group(1) if m else f'chunk {n:02d}'
        label = label.replace('2026-', '').replace(' → ', '→').replace(' -> ', '→')
        chunks.append({'n': n, 'label': label,
                       'html': linkify_ids(md_to_html(raw, f'chunk-{n:02d}'))})

    msgs = []
    with open(os.path.join(BASE, 'data', 'messages.jsonl')) as f:
        for line in f:
            d = json.loads(line)
            t = d['text']
            if len(t) > MSG_TRUNCATE:
                t = t[:MSG_TRUNCATE] + f'\n… [truncated, {d["n_chars"]} chars total — full text in data/messages.jsonl]'
            msgs.append([d['id'], d['ts'], d['source'], d['session'], d['kind'], t])

    stats = html.escape(read(os.path.join(BASE, 'data', 'stats.txt')))

    msg_json = json.dumps(msgs, ensure_ascii=False).replace('</', '<\\/')

    chunk_nav = ''.join(
        f'<a class="navlink sub" href="#chunk-{c["n"]:02d}" data-view="chunk-{c["n"]:02d}">'
        f'{c["n"]:02d} · {html.escape(c["label"])}</a>' for c in chunks)
    chunk_sections = ''.join(
        f'<section class="view doc" id="view-chunk-{c["n"]:02d}">{c["html"]}</section>' for c in chunks)

    page = TEMPLATE
    page = page.replace('__PLAN__', plan_html)
    page = page.replace('__THREADS__', threads_html)
    page = page.replace('__CHUNK_NAV__', chunk_nav)
    page = page.replace('__CHUNK_SECTIONS__', chunk_sections)
    page = page.replace('__STATS__', stats)
    page = page.replace('__MSGS__', msg_json)

    with open(OUT, 'w') as f:
        f.write(page)
    print(f'wrote {OUT}  ({os.path.getsize(OUT)/1e6:.1f} MB, {len(msgs)} messages embedded)')


TEMPLATE = r'''<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Softland — Fable Window 2026-06</title>
<style>
:root{
  --bg:#0e1116; --panel:#151a22; --panel2:#1b2230; --text:#d7dde6; --dim:#8b95a5;
  --accent:#7ab4f5; --claude:#e8a866; --codex:#5ecfc0; --border:#2a3344;
  --hl:#3b2f1a; --mark:#665320;
}
body.light{
  --bg:#f7f7f5; --panel:#ffffff; --panel2:#eef0f3; --text:#23272e; --dim:#6b7280;
  --accent:#1f6fd6; --claude:#b05c10; --codex:#0e7d72; --border:#d9dde3;
  --hl:#fdf3d7; --mark:#ffe9a8;
}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--text);
  font:16px/1.65 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Ubuntu Sans",sans-serif;}
a{color:var(--accent);text-decoration:none}
a:hover{text-decoration:underline}
header{position:sticky;top:0;z-index:30;display:flex;align-items:center;gap:.8rem;
  background:var(--panel);border-bottom:1px solid var(--border);padding:.55rem .9rem;}
header h1{font-size:1rem;margin:0;font-weight:600;flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
header button{background:var(--panel2);color:var(--text);border:1px solid var(--border);
  border-radius:8px;padding:.3rem .7rem;cursor:pointer;font-size:.85rem}
#layout{display:flex;min-height:calc(100vh - 49px)}
nav#side{width:255px;flex:0 0 255px;border-right:1px solid var(--border);background:var(--panel);
  padding:.8rem .6rem 2rem;position:sticky;top:49px;height:calc(100vh - 49px);overflow-y:auto}
.navlink{display:block;padding:.34rem .6rem;border-radius:8px;color:var(--text);font-size:.92rem;margin:1px 0}
.navlink.sub{font-size:.8rem;color:var(--dim);padding-left:1.2rem}
.navlink:hover{background:var(--panel2);text-decoration:none}
.navlink.active{background:var(--panel2);color:var(--accent);font-weight:600}
.navgroup{margin:.9rem .5rem .25rem;font-size:.7rem;letter-spacing:.12em;text-transform:uppercase;color:var(--dim)}
main{flex:1;min-width:0;padding:1.4rem 2rem 5rem}
.view{display:none;max-width:52rem;margin:0 auto}
.view.visible{display:block}
.doc h1{font-size:1.55rem;line-height:1.3;margin:.2rem 0 1rem}
.doc h2{font-size:1.2rem;margin:2.2rem 0 .6rem;padding-bottom:.25rem;border-bottom:1px solid var(--border)}
.doc h3{font-size:1.02rem;margin:1.5rem 0 .4rem;color:var(--accent)}
.doc h4{font-size:.95rem;margin:1.2rem 0 .3rem}
.doc p{margin:.55rem 0}
.doc li{margin:.25rem 0}
.doc code{background:var(--panel2);border:1px solid var(--border);border-radius:5px;padding:.05rem .35rem;font-size:.85em}
.doc pre{background:var(--panel2);border:1px solid var(--border);border-radius:10px;padding:.8rem 1rem;overflow-x:auto;font-size:.82rem;line-height:1.5}
.doc pre code{background:none;border:none;padding:0}
.doc blockquote{border-left:3px solid var(--accent);margin:.6rem 0;padding:.1rem .9rem;color:var(--dim)}
.tblwrap{overflow-x:auto;margin:.8rem 0}
table{border-collapse:collapse;font-size:.86rem;width:100%}
th,td{border:1px solid var(--border);padding:.4rem .6rem;text-align:left;vertical-align:top}
th{background:var(--panel2)}
.toc{background:var(--panel);border:1px solid var(--border);border-radius:12px;padding:.7rem 1rem;margin:1rem 0;font-size:.86rem}
.toc ul{margin:.35rem 0 0;padding-left:1.2rem}
a.mid{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:.83em;background:var(--panel2);
  border:1px solid var(--border);border-radius:5px;padding:0 .28rem;white-space:nowrap}
/* messages view */
#msgbar{position:sticky;top:49px;z-index:20;background:var(--bg);padding:.6rem 0 .5rem;border-bottom:1px solid var(--border)}
#months{display:flex;flex-wrap:wrap;gap:.35rem;margin-bottom:.5rem}
#months button{background:var(--panel2);border:1px solid var(--border);color:var(--text);
  border-radius:999px;padding:.22rem .75rem;font-size:.8rem;cursor:pointer}
#months button.active{background:var(--accent);color:#0b0e13;border-color:var(--accent);font-weight:600}
#msgctl{display:flex;gap:.5rem;flex-wrap:wrap;align-items:center}
#q{flex:1;min-width:200px;background:var(--panel);border:1px solid var(--border);color:var(--text);
  border-radius:8px;padding:.42rem .7rem;font-size:.9rem}
#msgctl label{font-size:.8rem;color:var(--dim);display:flex;align-items:center;gap:.25rem}
.daysep{margin:1.6rem 0 .6rem;font-weight:700;color:var(--accent);font-size:1rem;
  border-bottom:1px solid var(--border);padding-bottom:.2rem;position:sticky;top:138px;background:var(--bg)}
.sesssep{margin:.9rem 0 .4rem;color:var(--dim);font-size:.74rem;letter-spacing:.06em;text-transform:uppercase}
.msg{background:var(--panel);border:1px solid var(--border);border-left:3px solid var(--claude);
  border-radius:10px;padding:.5rem .8rem;margin:.45rem 0}
.msg.codex{border-left-color:var(--codex)}
.msg .meta{font:600 .74rem ui-monospace,Menlo,Consolas,monospace;color:var(--dim);margin-bottom:.2rem;display:flex;gap:.6rem;flex-wrap:wrap}
.msg .meta .id{color:var(--text)}
.msg.claude .meta .src{color:var(--claude)} .msg.codex .meta .src{color:var(--codex)}
.msg .body{white-space:pre-wrap;word-wrap:break-word;font-size:.9rem}
.msg.flash{background:var(--hl);border-color:var(--mark)}
.kind-slash .body{color:var(--accent);font-family:ui-monospace,Menlo,Consolas,monospace;font-size:.84rem}
#results{margin:.6rem 0}
.res{display:block;background:var(--panel);border:1px solid var(--border);border-radius:8px;
  padding:.4rem .7rem;margin:.3rem 0;font-size:.83rem;color:var(--text)}
.res:hover{background:var(--panel2);text-decoration:none}
.res b{color:var(--accent)}
.res mark{background:var(--mark);color:var(--text);border-radius:3px;padding:0 .1rem}
.count{color:var(--dim);font-size:.8rem;margin:.4rem 0}
#sideToggle{display:none}
@media (max-width:900px){
  #sideToggle{display:inline-block}
  nav#side{position:fixed;left:0;top:49px;transform:translateX(-100%);transition:transform .18s;box-shadow:4px 0 20px rgba(0,0,0,.4)}
  nav#side.open{transform:none}
  main{padding:1rem .9rem 4rem}
  .daysep{top:auto;position:static}
}
</style>
</head>
<body>
<header>
  <button id="sideToggle" onclick="document.getElementById('side').classList.toggle('open')">☰</button>
  <h1>Softland · Fable Window <span style="color:var(--dim);font-weight:400">Jun 11 → 22</span></h1>
  <button onclick="toggleTheme()" id="themeBtn">light</button>
</header>
<div id="layout">
<nav id="side">
  <div class="navgroup">Decide &amp; act</div>
  <a class="navlink" href="#plan" data-view="plan">📋 The 11-day plan</a>
  <a class="navlink" href="#threads" data-view="threads">🧵 Open threads inventory</a>
  <div class="navgroup">Raw material</div>
  <a class="navlink" href="#messages" data-view="messages">💬 All 2,404 messages</a>
  <div class="navgroup">Chunk summaries</div>
  __CHUNK_NAV__
  <div class="navgroup">Method</div>
  <a class="navlink" href="#about" data-view="about">⚙ Data &amp; verification</a>
</nav>
<main>
  <section class="view doc" id="view-plan">__PLAN__</section>
  <section class="view doc" id="view-threads">__THREADS__</section>
  __CHUNK_SECTIONS__
  <section class="view" id="view-messages">
    <div id="msgbar">
      <div id="months"></div>
      <div id="msgctl">
        <input id="q" type="search" placeholder="search all messages… (or type an id like X-1336)">
        <label><input type="checkbox" id="fClaude" checked> claude</label>
        <label><input type="checkbox" id="fCodex" checked> codex</label>
      </div>
    </div>
    <div id="results"></div>
    <div id="msglist"></div>
  </section>
  <section class="view doc" id="view-about">
    <h1>Data &amp; verification</h1>
    <p>Built from every user message in 73 Claude Code session files and 99 Softland Codex rollouts.
    Deduplicated across resumed sessions (uuid + content-hash) and Codex's double representation
    (event_msg + response_item). Messages over 3,000 chars are truncated here; full text lives in
    <code>data/messages.jsonl</code>. Wrapper leakage (system reminders, environment context) was
    grepped to zero; 5 random messages traced back to raw transcripts; 5 Codex sessions sampled to
    confirm no day-job leakage.</p>
    <pre>__STATS__</pre>
  </section>
</main>
</div>
<script>
const MSGS = __MSGS__;
// ---- theme ----
function applyTheme(t){document.body.classList.toggle('light', t==='light');
  document.getElementById('themeBtn').textContent = t==='light' ? 'dark' : 'light';
  localStorage.setItem('fw-theme', t);}
function toggleTheme(){applyTheme(document.body.classList.contains('light') ? 'dark' : 'light');}
applyTheme(localStorage.getItem('fw-theme') || 'dark');

// ---- month index ----
const MONTH_NAMES={'01':'Jan','02':'Feb','03':'Mar','04':'Apr','05':'May','06':'Jun'};
const months=[]; const monthOf=m=>m[1].slice(0,7);
{let cur=null; MSGS.forEach((m,i)=>{const mo=monthOf(m);
  if(!cur||cur.key!==mo){cur={key:mo,start:i,end:i,c:0,x:0};months.push(cur);}
  cur.end=i; if(m[2]==='claude')cur.c++; else cur.x++;});}
const idIndex={}; MSGS.forEach((m,i)=>idIndex[m[0]]=i);
let activeMonth = months[months.length-1].key;

function buildMonthBar(){
  const el=document.getElementById('months'); el.innerHTML='';
  months.forEach(mo=>{
    const b=document.createElement('button');
    const[y,m]=mo.key.split('-');
    b.textContent=`${MONTH_NAMES[m]} ${y.slice(2)} · ${mo.end-mo.start+1}`;
    b.className= mo.key===activeMonth?'active':'';
    b.onclick=()=>{activeMonth=mo.key; renderMonth(); buildMonthBar();};
    el.appendChild(b);
  });
}
function msgNode(m, flash){
  const d=document.createElement('div');
  d.className=`msg ${m[2]} kind-${m[4]}`+(flash?' flash':'');
  d.id='m-'+m[0];
  const meta=document.createElement('div'); meta.className='meta';
  meta.innerHTML=`<span class="id">${m[0]}</span><span class="src">${m[2]}${m[4]==='slash'?' /slash':''}</span><span>${m[1].slice(0,10)} ${m[1].slice(11,16)}Z</span><span>sess ${m[3]}</span>`;
  const body=document.createElement('div'); body.className='body'; body.textContent=m[5];
  d.appendChild(meta); d.appendChild(body);
  return d;
}
function filters(){return {c:document.getElementById('fClaude').checked, x:document.getElementById('fCodex').checked};}
function renderMonth(flashId){
  const f=filters();
  const mo=months.find(m=>m.key===activeMonth); if(!mo)return;
  const list=document.getElementById('msglist'); list.innerHTML='';
  let day=null, sess=null;
  for(let i=mo.start;i<=mo.end;i++){
    const m=MSGS[i];
    if(m[2]==='claude'&&!f.c) continue; if(m[2]==='codex'&&!f.x) continue;
    const d=m[1].slice(0,10);
    if(d!==day){const h=document.createElement('div');h.className='daysep';
      h.textContent=new Date(m[1]).toUTCString().slice(0,16);list.appendChild(h);day=d;sess=null;}
    const sk=m[2]+m[3];
    if(sk!==sess){const s=document.createElement('div');s.className='sesssep';
      s.textContent=`session · ${m[2]} ${m[3]}`;list.appendChild(s);sess=sk;}
    list.appendChild(msgNode(m, flashId===m[0]));
  }
  if(flashId){const el=document.getElementById('m-'+flashId);
    if(el)setTimeout(()=>el.scrollIntoView({block:'center'}),30);}
  else window.scrollTo(0,0);
}
// ---- search ----
let qTimer=null;
document.getElementById('q').addEventListener('input', e=>{
  clearTimeout(qTimer); qTimer=setTimeout(()=>doSearch(e.target.value),250);});
['fClaude','fCodex'].forEach(id=>document.getElementById(id).addEventListener('change',()=>{
  const q=document.getElementById('q').value; q.length>1?doSearch(q):renderMonth();}));
function doSearch(q){
  const res=document.getElementById('results');
  q=q.trim();
  if(q.length<2){res.innerHTML=''; renderMonth(); return;}
  const idm=q.toUpperCase().match(/^([CX]-\d{1,4})$/);
  if(idm){let id=idm[1]; const[p,n]=id.split('-'); id=p+'-'+n.padStart(4,'0');
    if(idIndex[id]!==undefined){jumpTo(id); return;}}
  const f=filters(); const ql=q.toLowerCase(); const hits=[];
  for(const m of MSGS){
    if(m[2]==='claude'&&!f.c)continue; if(m[2]==='codex'&&!f.x)continue;
    const ix=m[5].toLowerCase().indexOf(ql);
    if(ix>=0){hits.push([m,ix]); if(hits.length>=300)break;}
  }
  document.getElementById('msglist').innerHTML='';
  res.innerHTML=`<div class="count">${hits.length}${hits.length>=300?'+':''} matches for “${q.replace(/</g,'&lt;')}”</div>`;
  hits.forEach(([m,ix])=>{
    const a=document.createElement('a'); a.className='res'; a.href='#msg-'+m[0];
    const snip=m[5].slice(Math.max(0,ix-60), ix+q.length+90).replace(/\s+/g,' ');
    const safe=snip.replace(/</g,'&lt;');
    const re=new RegExp(q.replace(/[.*+?^${}()|[\]\\]/g,'\\$&'),'i');
    a.innerHTML=`<b>${m[0]}</b> · ${m[1].slice(0,10)} · ${m[2]} — ${safe.replace(re, s=>'<mark>'+s+'</mark>')}`;
    res.appendChild(a);
  });
}
function jumpTo(id){
  const i=idIndex[id]; if(i===undefined)return;
  activeMonth=monthOf(MSGS[i]);
  showView('messages', false);
  buildMonthBar();
  document.getElementById('results').innerHTML='';
  renderMonth(id);
}
// ---- views / routing ----
function showView(v, scroll=true){
  document.querySelectorAll('.view').forEach(s=>s.classList.remove('visible'));
  const el=document.getElementById('view-'+v); (el||document.getElementById('view-plan')).classList.add('visible');
  document.querySelectorAll('.navlink').forEach(a=>a.classList.toggle('active', a.dataset.view===v));
  document.getElementById('side').classList.remove('open');
  if(v==='messages'&&!document.getElementById('msglist').hasChildNodes()){buildMonthBar();renderMonth();}
  if(scroll)window.scrollTo(0,0);
}
function route(){
  const h=location.hash.slice(1);
  if(h.startsWith('msg-')){jumpTo(h.slice(4)); return;}
  showView(h||'plan');
}
window.addEventListener('hashchange', route);
route();
</script>
</body>
</html>'''

if __name__ == '__main__':
    main()
