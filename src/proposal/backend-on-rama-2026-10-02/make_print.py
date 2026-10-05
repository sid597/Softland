# Builds the three-level print from mapping.json. Four A4 landscape sheets, static HTML.
#   Level 1: the ladder alone.            Level 2: how much code sits on each row.
#   Level 3: which files (sheet 3) and what each build declares below the depots (sheet 4).
# Run:  python3 make_print.py <output folder>   then print the HTML to PDF with a browser.
# The output folder is required, so that a run never leaves the print inside the repo.
import html, io, json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
if len(sys.argv) != 2:
    sys.exit("usage: python3 make_print.py <output folder>")
D = json.load(io.open(os.path.join(HERE, "mapping.json"), encoding="utf-8"))
OUT = os.path.join(os.path.abspath(sys.argv[1]), "architecture-three-levels.html")
def e(s): return html.escape(s)
def wb(s): return e(s).replace("-", "-<wbr>").replace("/", "/<wbr>")
def n(x): return format(x, ",")

# ---------------------------------------------------------------- totals from the file data
ROWS = D["rows"]
def totals(build):
    t = {}
    for f in D["builds"][build]["files"]:
        for k, v in f["split"].items(): t[k] = t.get(k, 0) + v
    return t
TOT = {b: totals(b) for b in ("server", "instance")}
LINES = {b: sum(f["lines"] for f in D["builds"][b]["files"]) for b in TOT}
def val(build, rid): return TOT[build].get(rid, 0) + (TOT[build].get("gates_parked", 0) if rid == "gates" else 0)
def parked(build, rid): return TOT[build].get("gates_parked", 0) if rid == "gates" else 0
MAX = max(val(b, r["id"]) for b in TOT for r in ROWS)
TRACK = 96.0

# ---------------------------------------------------------------- sheet 1: the ladder, as a drawing
def svg_ladder():
    o = []
    def rect(x, y, w, h, kind):
        if kind == "none":
            o.append('<rect x="%s" y="%s" width="%s" height="%s" rx="1.2" class="b-none"/>' % (x, y, w, h))
        else:
            o.append('<rect x="%s" y="%s" width="%s" height="%s" rx="1.2" class="b-%s"/>' % (x, y, w, h, kind))
    def box(x, y, w, h, kind, title, sub="", sub2=""):
        rect(x, y, w, h, kind)
        o.append('<text x="%s" y="%s" class="t">%s</text>' % (x + 3, y + 5.0, e(title)))
        if sub:  o.append('<text x="%s" y="%s" class="s">%s</text>' % (x + 3, y + 9.3, e(sub)))
        if sub2: o.append('<text x="%s" y="%s" class="s">%s</text>' % (x + 3, y + 12.9, e(sub2)))
        if kind == "data":
            o.append('<rect x="%s" y="%s" width="9.5" height="3.6" class="tag"/><text x="%s" y="%s" class="tagt">DATA</text>' % (x + w - 9.5, y, x + w - 4.75, y + 2.7))
        elif kind == "code":
            o.append('<text x="%s" y="%s" class="codet">code</text>' % (x + w - 2, y + 3.4))
    def line(pts, arrow=True, cls="ln"):
        d = "M" + " L".join("%s %s" % p for p in pts)
        o.append('<path d="%s" class="%s"%s/>' % (d, cls, ' marker-end="url(#ah)"' if arrow else ""))
    def lab(x, y, s, anchor="start", cls="l"):
        o.append('<text x="%s" y="%s" class="%s" text-anchor="%s">%s</text>' % (x, y, cls, anchor, e(s)))
    def rowlab(y, name, gloss):
        o.append('<text x="0" y="%s" class="rn">%s</text><text x="0" y="%s" class="rg">%s</text>' % (y, e(name), y + 4.0, e(gloss)))
    o.append('<defs><marker id="ah" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="3.2" markerHeight="3.2" orient="auto-start-reverse"><path d="M0 0 L10 5 L0 10 z" fill="#0b0b0b"/></marker></defs>')
    XO, XI, W = 44, 120, 62           # outside column, inside column, box width
    cxo, cxi, cxm = XO + W / 2, XI + W / 2, (XO + XI + W) / 2
    # rows
    rowlab(7, "DATA SOURCES", "nobody writes them")
    box(XO, 1, W, 15.5, "none", "outside", "not Softland's")
    box(XI, 1, W, 15.5, "none", "inside", "Softland's own: a fact that landed,", "or a signal on its own screen")
    rowlab(33, "INLETS (g)", "code · us")
    box(XO, 28, W, 13, "code", "outside inlet", "moves it in · does not know what it is")
    box(XI, 28, W, 13, "code", "inside inlet", "notices it")
    rowlab(59, "ADAPTERS", "data · whoever brings the source")
    box(XO, 54, W, 13, "data", "adapters", "give it a shape (g)")
    rowlab(86, "CONVERTERS", "data · whoever makes the tool")
    box(XO, 80, XI + W - XO, 14, "data", "converters", "take a shape, run a function, give an act or a series of acts")
    rowlab(111, "ACT", "one form · code checks it")
    box(cxm - W / 2, 106, W, 11, "code", "act", "one form, whatever made it")
    rowlab(134, "DEPOTS", "code · us")
    box(XO, 129, W, 11, "code", "streaming depot")
    box(XI, 129, W, 11, "code", "microbatch depot")
    o.append('<text x="0" y="155" class="rg2">below the depots</text>')
    box(XO, 150, XI + W - XO, 11, "code", "gates and the record", "decide each act · keep what is accepted")
    # flows
    line([(cxo, 16.5), (cxo, 28)]); lab(cxo + 2, 23.2, "a thing, in the source's form")
    line([(cxi, 16.5), (cxi, 28)]); lab(cxi + 2, 23.2, "a thing, in Softland's form")
    line([(cxo, 41), (cxo, 54)]); lab(cxo + 2, 48.6, "what arrived")
    line([(cxo, 67), (cxo, 80)]); lab(cxo + 2, 74.6, "a thing with a shape")
    line([(cxi, 41), (cxi, 80)]); lab(cxi + 2, 61.5, "it has a shape already (g)")
    line([(cxm, 94), (cxm, 106)]); lab(cxm + 2, 101, "an act, or a series of acts")
    line([(cxm, 117), (cxm, 123)], arrow=False)
    line([(cxo, 123), (cxi, 123)], arrow=False)
    line([(cxo, 123), (cxo, 129)]); line([(cxi, 123), (cxi, 129)])
    line([(cxo, 140), (cxo, 150)]); line([(cxi, 140), (cxi, 150)])
    # the loop
    XR = XI + W
    line([(XR, 153), (XR + 9, 153), (XR + 9, 8.5), (XR, 8.5)])
    o.append('<text transform="translate(%s 118) rotate(-90)" class="l">what is accepted lands as a fact · a fact is an inside source</text>' % (XR + 12.2))
    # beside the ladder
    SX, SW = 208, 69
    o.append('<text x="%s" y="50.5" class="rg2">beside the ladder</text>' % SX)
    box(SX, 54, SW, 40, "code", "RUN BY")
    for i, s in enumerate(["what runs the adapters and the converters", "", "match: finds the ones whose patterns match", "the evaluator: runs a body under a budget,", "and records each read", "leaves: the small steps a body is written in"]):
        if s: lab(SX + 3, 63.3 + i * 3.9, s, cls="s")
    box(SX, 102, SW, 20, "code", "AN ASK · what leaves", "paint · prompt · enact · send", "what comes back arrives at the outside inlet")
    box(SX, 139, SW, 22, "code", "READS AND SHOWN", "resolve: the one exit for reads", "what a screen shows, what a prompt is given")
    line([(XR, 158), (SX, 158)])
    return '<svg class="ladder" viewBox="0 0 277 163" width="277mm" height="163mm" xmlns="http://www.w3.org/2000/svg">' + "".join(o) + "</svg>"

# ---------------------------------------------------------------- sheet 2: bars
def bar(series, value, pk=0, title=""):
    if value == 0: return '<div class="bar-line"><span class="val none">none</span></div>'
    seg = '<span class="bar %s%s" style="width:%.2fmm" title="%s"></span>' % (series, "" if pk else " last", (value - pk) / MAX * TRACK, e(title))
    if pk: seg += '<span class="bar %s tex last" style="width:%.2fmm" title="parked or retired: %s lines"></span>' % (series, pk / MAX * TRACK, n(pk))
    return '<div class="bar-line">%s<span class="val">%s</span></div>' % (seg, n(value))
def bars_row(r):
    rid = r["id"]; d = D["descriptions"][rid]
    def ln(b): return ('<div class="ln"><div class="series">%s</div><div class="bars">%s</div><div class="desc">%s</div></div>'
                       % (b, bar(b, val(b, rid), parked(b, rid), "%s · %s · %s lines" % (b, r["name"], n(val(b, rid)))), e(d[b])))
    chip = ""
    if rid == "converters":
        chip = ('<div class="ln"><div></div><div class="chipcell"><div class="datachip"><span class="tag">DATA</span>%s'
                '<span class="note"> · counted in records, so not on the bar scale</span></div></div></div>') % e(D["data_today"])
    return '<div class="row"><div class="rname"><b>%s</b><span>%s</span></div><div class="lines">%s%s%s</div></div>' % (e(r["name"]), e(r["gloss"]), ln("server"), ln("instance"), chip)
def flow(label): return '<div class="flow">▼ <span>%s</span></div>' % e(label)
def sheet2():
    by = {r["id"]: r for r in ROWS}
    p = ['<h3 class="first">On the ladder</h3>']
    p.append('<div class="row"><div class="rname"><b>DATA SOURCES</b><span>nobody writes them</span></div><div class="lines">'
             '<div class="ln src"><div class="series">server</div><div class="srcs">markdown and other files · git · Claude Code transcripts · HTTP posts from a client · a CLI model\'s reply</div></div>'
             '<div class="ln src"><div class="series">instance</div><div class="srcs">a hand on a screen · a login · the repo\'s files at a commit · a resident\'s reply</div></div></div></div>')
    p += [flow("a thing"), bars_row(by["inlets"]), flow("what arrived"), bars_row(by["adapters"]), flow("a thing with a shape"),
          bars_row(by["converters"]), flow("an act, or a series of acts"), bars_row(by["act"]), flow("append to a depot")]
    p.append('<h3>Beside the ladder</h3>')
    p += [bars_row(by["runby"]), bars_row(by["ask"]), bars_row(by["datacode"])]
    p.append('<h3>Below the depots</h3>')
    p += [bars_row(by["gates"]), bars_row(by["reads"]), bars_row(by["probes"])]
    return "".join(p)

# ---------------------------------------------------------------- sheet 3: files
def short(build, f):
    if build == "server": return f.split("/")[-1] if not f.startswith("worn/8") else "8 material specs"
    return f.replace("rig/store/", "rig/").replace("rig/bench/", "rig/")
def chips(build, rid):
    items = []
    for f in D["builds"][build]["files"]:
        for k, v in f["split"].items():
            if k == rid or (rid == "gates" and k == "gates_parked"):
                items.append((v, short(build, f["file"]), k == "gates_parked", v != f["lines"]))
    items.sort(key=lambda t: -t[0])
    out = ""
    for v, name, pk, part in items:
        out += ('<span class="chip"><span class="cl">%s%s <b>%s</b></span><span class="cb %s%s" style="width:%.2fmm"></span></span>'
                % (e(name), "*" if part else "", n(v), build, " tex" if pk else "", max(v / 100.0, 0.6)))
    return out or '<span class="nil">none</span>'
def sheet3():
    p = []
    last = None
    names = {"ladder": "On the ladder", "beside": "Beside the ladder", "below": "Below the depots"}
    for r in ROWS:
        if r["group"] != last:
            p.append('<h3 class="%s">%s</h3>' % ("first" if last is None else "", names[r["group"]])); last = r["group"]
        p.append('<div class="frow"><div class="rname"><b>%s</b><span>%s · %s</span></div><div class="fcell">%s</div><div class="fcell">%s</div></div>'
                 % (e(r["name"]), n(val("server", r["id"])), n(val("instance", r["id"])), chips("server", r["id"]), chips("instance", r["id"])))
    return "".join(p)

# ---------------------------------------------------------------- sheet 4: declared modules
BADGE = {"deployed": "● deployed", "in-process": "◐ in-process only", "parked": "○ parked", "retired": "✕ retired"}
def dep_html(depots):
    if not depots: return "<div class='nil'>no depot</div>"
    out = ""
    for d in depots:
        name, _, note = d.partition("  ")
        out += "<div>%s%s</div>" % (wb(name), (" <span class='by'>%s</span>" % e(note)) if note else "")
    return out
def body(fl, heads, wide):
    top = "".join("<div>%s</div>" % wb(t) for t in fl["topology"]) or "<div class='nil'>no write topology</div>"
    ps = fl["pstates"]
    if wide: pst = "<b>%d PStates</b> <span class='by'>listed below</span>" % len(ps)
    else: pst = ("<b>%d PStates</b> " % len(ps) + " ".join('<span class="n">%s</span>' % e(x) for x in ps)) if ps else "<span class='nil'>no PStates</span>"
    h = (lambda t: "<i>%s</i>" % t) if heads else (lambda t: "")
    return ('<div class="cbody%s"><div class="col dep">%s%s</div><div class="arr">→</div><div class="col top">%s%s</div><div class="arr">→</div><div class="col ps">%s<div class="names">%s</div></div></div>'
            % (("" if heads else " second") + (" wide" if wide else ""), h("depots"), dep_html(fl["depots"]), h("topology"), top, h("PStates"), pst))
def card(m):
    wide = m.get("wide", False)
    b = "".join(body(fl, k == 0, wide) for k, fl in enumerate(m["flows"]))
    if wide: b += '<div class="pslist">%s</div>' % "".join("<div>%s</div>" % wb(x) for fl in m["flows"] for x in fl["pstates"])
    q = m.get("queries")
    qh = ('<div class="queries"><b>%d query topologies</b> %s</div>' % (len(q), " · ".join('<span class="n">%s</span>' % e(x) for x in q))) if q else ""
    x = ('<div class="extra">%s</div>' % e(m["note"])) if m.get("note") else ""
    badge = "● " + m["badge"] if m.get("badge") else BADGE[m["status"]]
    return ('<div class="card %s %s"><div class="chead"><span class="mname">%s</span><span class="badge">%s</span></div>%s%s%s</div>'
            % (m["status"], "instance-card" if m["build"] == "instance" else "", wb(m["name"]), badge, b, qh, x))
def sheet4():
    ms = D["modules"]
    sv = [m for m in ms if m["build"] == "server"]; ins = [m for m in ms if m["build"] == "instance"]
    left = ['<h3>The server · 8 modules · 17 depots · 5 stream and 2 microbatch topologies · 12 queries · 84 PStates</h3>'] + [card(m) for m in sv if m["status"] == "deployed"]
    right = ['<h3>The server, continued · not on the cluster</h3>'] + [card(m) for m in sv if m["status"] != "deployed"]
    right.append('<h3 class="inst">The instance · 1 module in use · 5 depots · 1 stream and 1 microbatch topology · 21 queries · 6 PStates</h3>')
    right += [card(m) for m in ins]
    return '<div class="cols"><div>%s</div><div>%s</div></div>' % ("".join(left), "".join(right))

CSS = """
@page { size: A4 landscape; margin: 9mm; }
* { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
:root { --surface:#fcfcfb; --ink:#0b0b0b; --ink2:#52514e; --rule:#dddcd6; --server:#2a78d6; --server-d:#184f95; --instance:#eb6834; --instance-d:#a9441b;
        --sans:"Inter","Noto Sans","DejaVu Sans",Arial,sans-serif; --mono:"JetBrains Mono","DejaVu Sans Mono","Liberation Mono",monospace; }
html, body { margin:0; background:var(--surface); color:var(--ink); font-family:var(--sans); font-size:7.4pt; line-height:1.25; }
.page { width:279mm; height:191mm; overflow:hidden; page-break-after:always; break-after:page; display:flex; flex-direction:column; }
.page:last-child { page-break-after:auto; break-after:auto; }
header { border-bottom:0.5pt solid var(--ink); padding-bottom:1.5mm; margin-bottom:1.8mm; }
.lvl { display:inline-block; background:var(--ink); color:var(--surface); font-weight:700; font-size:6.8pt; letter-spacing:0.08em; padding:0.3mm 1.6mm; margin-right:2mm; vertical-align:2px; }
h1 { font-size:13pt; margin:0 0 0.8mm; font-weight:700; letter-spacing:-0.01em; display:inline-block; }
.subline { display:flex; justify-content:space-between; align-items:center; gap:6mm; }
.sub { color:var(--ink2); font-size:7.2pt; }
.legend { display:flex; gap:5mm; align-items:center; color:var(--ink2); font-size:7.2pt; white-space:nowrap; }
.sw { display:inline-block; width:7mm; height:2.6mm; border-radius:0 2px 2px 0; vertical-align:middle; margin-right:1.2mm; }
.sw.server { background:var(--server); } .sw.instance { background:var(--instance); }
.sw.tex { background:repeating-linear-gradient(45deg, #8f8e88 0 1.1px, #d6d5cf 1.1px 2.6px); }
.sw.code { background:#e7e6e1; border:0.5pt solid var(--ink2); border-radius:1px; } .sw.data { background:#fff; border:0.5pt solid var(--ink); border-radius:1px; } .sw.none { background:none; border:0.6pt dashed var(--ink2); border-radius:1px; }
h3 { font-size:7.2pt; font-weight:700; text-transform:uppercase; letter-spacing:0.06em; color:var(--ink2); margin:1.2mm 0 0.6mm; border-top:0.4pt solid var(--rule); padding-top:1.1mm; }
h3.first { border-top:none; margin-top:0; padding-top:0; }
.pfoot { margin-top:auto; color:var(--ink2); font-size:6.6pt; border-top:0.4pt solid var(--rule); padding-top:1.2mm; display:flex; justify-content:space-between; gap:6mm; }
/* sheet 1 */
svg.ladder { display:block; margin-top:1mm; font-family:var(--sans); }
svg .b-none { fill:none; stroke:#52514e; stroke-width:0.3; stroke-dasharray:1.4 1; }
svg .b-code { fill:#e7e6e1; stroke:#52514e; stroke-width:0.3; }
svg .b-data { fill:#ffffff; stroke:#0b0b0b; stroke-width:0.45; }
svg .tag { fill:#0b0b0b; } svg .tagt { fill:#fcfcfb; font-size:2.2px; font-weight:700; letter-spacing:0.12px; text-anchor:middle; }
svg .codet { fill:#52514e; font-size:2.1px; text-anchor:end; letter-spacing:0.1px; }
svg .t { font-size:3px; font-weight:700; fill:#0b0b0b; } svg .s { font-size:2.35px; fill:#0b0b0b; }
svg .l { font-size:2.25px; fill:#52514e; } svg .rn { font-size:3px; font-weight:700; letter-spacing:0.06px; } svg .rg { font-size:2.3px; fill:#52514e; } svg .rg2 { font-size:2.4px; font-weight:700; fill:#52514e; letter-spacing:0.15px; text-transform:uppercase; }
svg .ln { fill:none; stroke:#0b0b0b; stroke-width:0.35; }
.notes { display:grid; grid-template-columns:1fr 1fr 1fr; column-gap:5mm; font-size:6.8pt; margin-top:0.5mm; } .notes b { font-weight:700; }
/* sheet 2 */
.row { display:grid; grid-template-columns:37mm 1fr; column-gap:2mm; align-items:start; padding:0.25mm 0; }
.rname b { display:block; font-size:8.4pt; letter-spacing:0.02em; line-height:1.3; } .rname span { color:var(--ink2); }
.ln { display:grid; grid-template-columns:13mm 113mm 1fr; column-gap:2mm; align-items:start; min-height:4.4mm; }
.ln.src { grid-template-columns:13mm 1fr; }
.series { color:var(--ink2); padding-top:0.7mm; } .srcs { padding-top:0.7mm; }
.bar-line { height:4.4mm; display:flex; align-items:center; }
.bar { display:inline-block; height:3.3mm; margin-right:2px; } .bar.last { border-radius:0 4px 4px 0; margin-right:0; }
.bar.server, .cb.server { background:var(--server); } .bar.instance, .cb.instance { background:var(--instance); }
.bar.server.tex, .cb.server.tex { background:repeating-linear-gradient(45deg, var(--server-d) 0 1.1px, #9ec5f4 1.1px 2.6px); }
.bar.instance.tex, .cb.instance.tex { background:repeating-linear-gradient(45deg, var(--instance-d) 0 1.1px, #f6c0a8 1.1px 2.6px); }
.val { margin-left:1.6mm; font-variant-numeric:tabular-nums; font-weight:600; } .val.none { margin-left:0; font-weight:400; color:var(--ink2); }
.desc { font-size:6.7pt; line-height:1.17; padding:0.6mm 0 0.3mm; }
.chipcell { grid-column:2 / span 2; padding:0.2mm 0 0.6mm; }
.datachip { display:inline-block; border:0.5pt solid var(--ink); border-radius:2px; padding:0.25mm 1.6mm 0.25mm 0; font-size:6.8pt; }
.datachip .tag { background:var(--ink); color:var(--surface); font-weight:700; padding:0.25mm 1.4mm; margin-right:1.5mm; letter-spacing:0.05em; } .datachip .note { color:var(--ink2); }
.flow { color:var(--ink2); font-size:6.5pt; padding-left:3mm; line-height:1; }
/* sheet 3 */
.frow { display:grid; grid-template-columns:33mm 1fr 1fr; column-gap:4mm; align-items:start; padding:0.4mm 0; border-bottom:0.3pt dotted #c9c8c2; }
.frow.fhead { border-bottom:none; color:var(--ink2); font-size:7.2pt; padding-bottom:0.2mm; }
.fcell { display:flex; flex-wrap:wrap; gap:0.55mm 2.3mm; align-items:flex-end; }
.chip { display:inline-flex; flex-direction:column; } .cl { font-family:var(--mono); font-size:5.9pt; white-space:nowrap; line-height:1.25; } .cl b { font-family:var(--sans); font-size:6.5pt; }
.cb { display:block; height:1.15mm; border-radius:0 2px 2px 0; }
/* sheet 4 */
.cols { display:grid; grid-template-columns:1fr 1fr; column-gap:5mm; flex:1; align-items:start; }
.cols h3 { border-top:none; margin-top:0; padding-top:0; } .cols h3.inst { margin-top:2.4mm; border-top:0.4pt solid var(--rule); padding-top:1.4mm; }
.card { border:0.5pt solid var(--ink2); border-radius:2px; margin-bottom:1.3mm; background:#fff; break-inside:avoid; }
.card.parked, .card.retired { border-style:dashed; background:repeating-linear-gradient(45deg, #fcfcfb 0 4px, #efeeea 4px 5px); }
.chead { display:flex; justify-content:space-between; align-items:center; padding:0.7mm 1.6mm; border-bottom:0.4pt solid var(--rule); border-left:2.2mm solid var(--server); }
.card.instance-card .chead { border-left-color:var(--instance); }
.mname { font-family:var(--mono); font-weight:700; font-size:7.3pt; } .badge { font-size:6.7pt; white-space:nowrap; }
.cbody { display:grid; grid-template-columns:1.25fr 4mm 0.95fr 4mm 2.7fr; padding:0.8mm 1.6mm 0.7mm; align-items:start; }
.cbody.second { border-top:0.4pt dotted #b9b8b2; } .cbody.wide { grid-template-columns:2.3fr 4mm 1.5fr 4mm 1.1fr; }
.pslist { border-top:0.4pt dotted #b9b8b2; padding:0.6mm 1.6mm; column-count:3; column-gap:3mm; font-family:var(--mono); font-size:5.7pt; line-height:1.3; } .pslist div { break-inside:avoid; }
.col { font-family:var(--mono); font-size:5.9pt; line-height:1.32; }
.col i { display:block; font-family:var(--sans); font-style:normal; font-size:5.6pt; text-transform:uppercase; letter-spacing:0.07em; color:var(--ink2); margin-bottom:0.2mm; }
.col b { font-family:var(--sans); font-size:6.5pt; } .by { font-family:var(--sans); color:var(--ink2); } .n { white-space:nowrap; }
.arr { color:var(--ink2); font-size:8pt; text-align:center; padding-top:1.7mm; } .cbody.second .arr { padding-top:0; }
.nil { color:var(--ink2); font-family:var(--sans); }
.queries, .extra { border-top:0.4pt dotted #b9b8b2; padding:0.6mm 1.6mm; font-family:var(--mono); font-size:5.9pt; line-height:1.32; }
.queries b { font-family:var(--sans); font-size:6.5pt; margin-right:1mm; } .extra { font-family:var(--sans); font-size:6.6pt; color:var(--ink2); }
.foot { color:var(--ink2); font-size:6.6pt; }
@media screen { body { background:#e9e8e4; } .page { margin:8mm auto; padding:9mm; width:297mm; height:210mm; background:var(--surface); box-shadow:0 1px 6px rgba(0,0,0,.18); } }
"""
def page(level, title, sub, legend, bodyhtml, foot, k):
    return ('<section class="page"><header><span class="lvl">%s</span><h1>%s</h1><div class="subline"><div class="sub">%s</div><div class="legend">%s</div></div></header>%s'
            '<div class="pfoot"><span>%s</span><span>sheet %d of 4 · %s</span></div></section>\n' % (level, e(title), e(sub), legend, bodyhtml, e(foot), k, e(D["written"])))
LEG_BUILD = '<span><span class="sw server"></span>server · src/app/server · %s lines</span><span><span class="sw instance"></span>instance · Inland on the rig · %s lines</span><span><span class="sw tex"></span>parked or retired</span>' % (n(LINES["server"]), n(LINES["instance"]))
g = D["ladder_guesses"]
notes = '<div class="notes"><div><b>(g) Inlets.</b> %s</div><div><b>(g) Adapters.</b> %s</div><div><b>(g) Inside.</b> %s</div></div>' % (e(g[0]), e(g[1]), e(g[2]))
doc = ('<!doctype html>\n<html lang="en"><head><meta charset="utf-8"><title>The architecture, in three levels</title><style>' + CSS + '</style></head><body>\n'
  + page("LEVEL 1", "The architecture: the ladder", "Sid's five things, and what goes in and out of each · no build is on this sheet · (g) marks a guess, not a ruling",
         '<span><span class="sw none"></span>nobody writes it</span><span><span class="sw code"></span>code · ours · a change is a rebuild</span><span><span class="sw data"></span>data · a change is an act</span>',
         svg_ladder() + notes, "Levels 2 and 3 use these same rows. Level 2 shows how much code sits on each. Level 3 shows which files, and what is declared below the depots.", 1)
  + page("LEVEL 2", "How much code sits on each row", "Lines of code · line counts are exact, the row a file sits on is an estimate", LEG_BUILD,
         sheet2(), "All bars share one scale. The longest is %s lines. The same rows as level 1; DATA AS CODE is grammars and defaults that the ladder treats as data but are written as code today." % n(MAX), 2)
  + page("LEVEL 3", "Which files sit on each row", "Each file with the lines it puts on that row · * a file split across rows · server on the left, instance on the right, rig files marked rig/", LEG_BUILD,
         sheet3(), "A bar under a name is that file's lines on this row, 1 mm for 100 lines. The split inside a starred file is an estimate. The full list is in mapping.json beside this file.", 3)
  + page("LEVEL 3", "Below the depots: what each build declares", "Every depot, topology, PState and query name is taken from the declarations in the code · no architecture is added here",
         '<span>● deployed</span><span>◐ in-process only</span><span>○ parked</span><span>✕ retired</span><span><span class="sw server"></span>server</span><span><span class="sw instance"></span>instance</span>',
         sheet4(), "Names from the code as it stood on 3 October 2026. Not the store: " + D["not_the_store"], 4)
  + '</body></html>')
io.open(OUT, "w", encoding="utf-8").write(doc)
print("wrote", OUT, len(doc), "bytes · max bar", MAX)
