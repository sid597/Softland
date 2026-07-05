#!/usr/bin/env python3
"""Parse giants-research-deep.md into structured data and inject into template.html -> index.html."""
import re, json, sys, os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC  = os.path.join(HERE, "..", "giants-research-deep.md")
TPL  = os.path.join(HERE, "template.html")
OUT  = os.path.join(HERE, "index.html")

SCENTS = {
 1:"Bush → Engelbart → Nelson. Xanadu's quartet (transclusion, bidirectional links, permanent versioning, provenance) is your ontology — but it never shipped; the Web won by deliberately keeping less.",
 2:"Semantic zoom was born here (Pad, 1993) and killed by 'desert fog' — sparse scale-bands with no scent. Forty years of beautiful demos; navigation never solved.",
 3:"The sensemaking dual-loop + 'information scent'; Klein's rival frame model; and the empirical sting — absorbing the effortful loop deskills the user.",
 4:"Explorable explanations / reactive documents genuinely work — and die of 100-hour authoring cost (Distill's hiatus). Generation must collapse that cost.",
 5:"HyperCard → spreadsheet → local-first. The 'abstraction barrier' is why end-user programming never spread; the CRDT log = deterministic projection = your ontology, proven elsewhere.",
 6:"Lynch's imageability + Alexander's pattern language — and Alexander's own warning: a catalogue without a generative grammar (and maintenance) dies.",
 7:"Toulmin → IBIS → discourse graphs is the literal ancestry of Q→C→E→D→R→F — and the 70-year-unsolved wall of premature formalization & value-lag.",
 8:"Playfair → Bertin → Tufte; 'insight, not pictures.' t-SNE/UMAP distances lie: a projection must never assert structure the truth doesn't contain.",
 9:"Applied category theory: functorial migration = 'one ontology, many projections,' rigorous but tool-starved for 12 years. Its lesson: pick a benchmark.",
 10:"Cartographic generalization is the formal model for honest semantic zoom — simplify without lying. Google Earth proved one-continuous-zoom is achievable.",
}
FAMILY = {1:"media",4:"media",5:"media", 3:"sense",7:"sense", 2:"space",6:"space",10:"space", 8:"rep",9:"rep"}
SUPP = [("kittur","Kittur","Kittur — sensemaking at scale"),
        ("wonderos","WonderOS","WonderOS — itemized OS"),
        ("folk","Folk Computer","Folk Computer — provenance stance"),
        ("apparatus","Apparatus","Apparatus — using = authoring")]

def read(p):
    with open(p, encoding="utf-8") as f: return f.read()

def split_closer(content):
    """Return (body, steal, avoid, open) from a thread/node block."""
    lines = content.split("\n")
    cut = None; skip = False
    for i,l in enumerate(lines):
        if re.sub(r'[#*:_\s]', '', l).lower() == 'forsoftland': cut = i; skip = True; break
    if cut is None:  # supplementary nodes: no 'For Softland' header — cut at first STEAL/AVOID/OPEN bullet
        for i,l in enumerate(lines):
            if re.match(r'\s*[-*]\s*\*\*\s*(STEAL|AVOID|OPEN)', l, re.I): cut = i; break
    if cut is None:
        body = content; closer_lines = []
    else:
        body = "\n".join(lines[:cut]); closer_lines = lines[cut+(1 if skip else 0):]
    # strip stray progress / leftover notes from body
    body = re.sub(r'(?m)^_\(threads.*$', '', body).strip()
    vals = {"steal":"","avoid":"","open":""}
    cur = None
    for l in closer_lines:
        m = re.match(r'\s*[-*]?\s*\*\*\s*(STEAL|AVOID|OPEN)[^*]*\*\*\s*[:—\-]*\s*(.*)$', l, re.I)
        if m:
            cur = m.group(1).lower(); vals[cur] = m.group(2).strip()
        elif re.match(r'\s*[-*]?\s*_?\*{0,2}\s*sources', l, re.I):
            cur = None
        elif cur and l.strip():
            vals[cur] += "\n" + l.strip()
    return body.strip(), vals["steal"].strip(), vals["avoid"].strip(), vals["open"].strip()

def nums_in(text):
    out=set()
    for m in re.finditer(r'threads?\s+([0-9][0-9,\s and]*)', text, re.I):
        for n in re.findall(r'\d+', m.group(1)):
            if 1<=int(n)<=10: out.add(int(n))
    for m in re.finditer(r'\[([0-9][0-9,\s]*)\]', text):
        for n in re.findall(r'\d+', m.group(1)):
            if 1<=int(n)<=10: out.add(int(n))
    return out

def main():
    raw = read(SRC)
    # title
    mt = re.search(r'^#\s+(.*)$', raw, re.M)
    title = mt.group(1).strip() if mt else "Giants — Deep Research"
    # leading blockquote (status/method)
    head = raw.split("# PART I")[0]
    bq = "\n".join(re.sub(r'^>\s?','',l) for l in head.split("\n") if l.strip().startswith(">"))
    status = bq
    # trailing provenance blockquote
    prov = ""
    pm = re.findall(r'(?m)^>\s?(.*)$', raw.split("# PART III")[-1])
    if pm: prov = "\n".join(pm)

    # parts
    def part(name_a, name_b):
        a = raw.find(name_a); b = raw.find(name_b) if name_b else len(raw)
        return raw[a:b] if a>=0 else ""
    p1 = part("# PART I", "# PART II")
    p2 = part("# PART II", "# PART III")
    p3 = part("# PART III", None)

    # threads
    threads=[]
    tparts = re.split(r'(?m)^##\s+(\d+)\.\s+(.*)$', p1)
    # tparts: [pre, num, title, content, num, title, content, ...]
    for i in range(1, len(tparts), 3):
        num=int(tparts[i]); ttl=tparts[i+1].strip(); content=tparts[i+2]
        body,steal,avoid,opn = split_closer(content)
        threads.append({"num":num,"title":f"{num}. {ttl}","family":FAMILY.get(num,"media"),
                        "scent":SCENTS.get(num,""),"body":body,"steal":steal,"avoid":avoid,"open":opn})
    threads.sort(key=lambda t:t["num"])

    # supplementary
    supp=[]
    sparts = re.split(r'(?m)^##\s+(.*)$', p2)
    for i in range(1,len(sparts),2):
        name=sparts[i].strip(); content=sparts[i+1]
        sid="misc"; short=name
        for cid,key,lab in SUPP:
            if key.lower() in name.lower(): sid=cid; short=lab; break
        body,steal,avoid,opn = split_closer(content)
        supp.append({"id":sid,"name":name,"short":short,"body":body,"steal":steal,"avoid":avoid,"open":opn})

    # synthesis
    def section(p, hdr, nexthdrs):
        a=p.find(hdr)
        if a<0: return ""
        a+=len(hdr); end=len(p)
        for nh in nexthdrs:
            x=p.find(nh,a)
            if x>=0: end=min(end,x)
        return p[a:end].strip()
    exec_md = section(p3,"## Executive Synthesis",["## Cross-Thread","## The Big","## Consolidated"])
    conn_md = section(p3,"## Cross-Thread Connections",["## The Big","## Consolidated"])
    tens_md = section(p3,"## The Big Tensions Softland Inherits",["## Consolidated"])
    dir_md  = section(p3,"## Consolidated Softland Directives",["\n---","\n> "])
    # split lists
    connections=[c.strip() for c in re.split(r'(?m)^- ', conn_md) if c.strip()]
    tensions=[re.sub(r'^\d+\.\s*','',c.strip()) for c in re.split(r'(?m)^(?=\d+\.\s)', tens_md) if c.strip()]
    directives=[re.sub(r'^\d+\.\s*','',c.strip()) for c in re.split(r'(?m)^(?=\d+\.\s)', dir_md) if c.strip()]

    # edges
    edges=set()
    for c in connections:
        ns=sorted(nums_in(c))
        for a_ in range(len(ns)):
            for b_ in range(a_+1,len(ns)):
                edges.add((ns[a_],ns[b_]))
    for t in threads:
        for n in nums_in(t["body"]+" "+t["steal"]+" "+t["avoid"]+" "+t["open"]):
            if n!=t["num"]:
                edges.add(tuple(sorted((t["num"],n))))
    edges=[list(e) for e in sorted(edges)]

    data={"meta":{"title":title,"status":status,"method":"","provenance":prov,
                  "tagline":"Ten lineages of how people have tried to hold understanding in a form another mind can inhabit. Click any node to enter; the lines are the cross-thread connections — drawn, never inferred."},
          "threads":threads,"supplementary":supp,
          "synthesis":{"exec":exec_md,"connections":connections,"tensions":tensions,"directives":directives},
          "edges":edges}

    js=json.dumps(data,ensure_ascii=False).replace("</","<\\/")
    tpl=read(TPL)
    out=tpl.replace("/*__DATA__*/ {}", js)
    with open(OUT,"w",encoding="utf-8") as f: f.write(out)

    print(f"threads={len(threads)} supp={len(supp)} connections={len(connections)} tensions={len(tensions)} directives={len(directives)} edges={len(edges)}")
    print("missing-body:", [t["num"] for t in threads if len(t["body"])<200])
    print("missing-closer:", [t["num"] for t in threads if not (t["steal"] or t["avoid"] or t["open"])])
    print(f"wrote {OUT}  ({os.path.getsize(OUT)} bytes)")

main()
