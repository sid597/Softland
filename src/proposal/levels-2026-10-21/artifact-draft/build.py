#!/usr/bin/env python3
"""Builds levels.html, the explorable page for the levels exploration of
21 October 2026, from the round files in the folder above.

The page shows the rounds' own sentences. Each one is listed in content.py and
checked against its file by sources.check() before anything is written; the
build stops if any excerpt is missing from its file. The page's own guide text
(section questions, labels, one-line guides) is written here and marked on the
page as the page's own.

Run:    python3 src/proposal/levels-2026-10-21/artifact-draft/build.py
Writes: levels.html and img/notebook-p1.webp, img/notebook-p2.webp (this folder)
"""

import html
import json
import re
import subprocess
import sys

sys.dont_write_bytecode = True  # keep the artifact folder free of __pycache__

from markdown_it import MarkdownIt

import content as C
from sources import FILES, HERE, REPO, TEXT, check, name_of, norm, path_of

MD = MarkdownIt("commonmark", {"html": False}).enable("table")
ROUND_SRCS = ["01", "02", "02b", "03", "04", "05", "06", "07"]
LETTERS = list("ABCDEFGHIJKLM")
MX_COLS = ["02"] + LETTERS + ["06"]


# --------------------------------------------------------------------------
# Small renderers
# --------------------------------------------------------------------------

def esc(s):
    return html.escape(s, quote=True)


_A = re.compile(r'<a href="([^"]*)">(.*?)</a>', re.S)


def strip_links(h):
    """Links into the repo cannot work on the page: show them as paths."""
    def rep(m):
        href, text = m.group(1), m.group(2)
        if href.startswith(("http://", "https://")):
            return f'<a href="{href}" target="_blank" rel="noopener">{text}</a>'
        return f'<span class="path">{text}</span>'
    return _A.sub(rep, h)


def inl(x):
    """Inline HTML for an excerpt (or a markdown string)."""
    md = x.md if isinstance(x, C.Ex) else x
    return strip_links(MD.renderInline(norm(md)))


def cells(x):
    """The cells of a markdown table row excerpt."""
    return [c.strip() for c in x.md.strip().strip("|").split("|")]


def src_label(src, sec=None, cls="src"):
    text = name_of(src) + (f" § {sec}" if sec else "")
    return f'<span class="{cls}" title="{esc(path_of(src))}">{esc(text)}</span>'


BADGE_CLS = {"hand": "b-sid", "typed87": "b-sid", "typed9e": "b-sid", "long": "b-sidq",
             "go": "b-sidq", "older": "b-older", "pasted": "b-pasted", "noprov": "b-unknown",
             "typed55": "b-sid", "typed9a": "b-sid", "typed07": "b-sid", "typedc7": "b-sid", "typedb3": "b-sid"}
SID_KINDS = {"hand", "typed87", "typed9e", "long", "go", "older",
             "typed55", "typed9a", "typed07", "typedc7", "typedb3"}


def badge(kind):
    who, what, tip = C.BADGES[kind]
    return (f'<span class="badge {BADGE_CLS[kind]}" title="{esc(tip)}">'
            f'<span class="b-dot" aria-hidden="true"></span><span class="b-who">{esc(who)}</span>'
            f'<span class="b-what">{esc(what)}</span></span>')


def quote(x, kind, note=None, cls=""):
    qcls = "q-sid" if kind in SID_KINDS else ("q-pasted" if kind == "pasted" else "q-unknown")
    tail = note if note is not None else src_label(x.src)
    return (f'<figure class="quote {qcls} {cls}"><figcaption class="q-head">{badge(kind)}{tail}</figcaption>'
            f'<blockquote>{inl(x)}</blockquote></figure>')


KIND_TAGS = {
    "checked": ("✓", "checked in code", "Checked against code or pages by the session (MAP.md, What was checked)."),
    "outside": ("≈", "session's knowledge", "Outside references come from the session's knowledge and were not checked (MAP.md)."),
    "derived": ("→", "derived", "The session's own reasoning (MAP.md)."),
    "reading": ("◇", "a reading of the code", "The session's reading of the code, not run (MAP.md)."),
}


def kind_tag(k):
    sym, label, tip = KIND_TAGS[k]
    return f'<span class="ktag k-{k}" title="{esc(tip)}"><span aria-hidden="true">{sym}</span> {esc(label)}</span>'


def guide(text, cls="guide"):
    """The page's own words (not from any round)."""
    return f'<p class="{cls}">{text}</p>'


def wrap_section(sid, eyebrow, title, intro, body):
    return (f'<section class="sec" id="{sid}" data-nav="{sid}" aria-labelledby="h-{sid}">'
            f'<header class="sec-head"><p class="eyebrow">{eyebrow}</p>'
            f'<h2 id="h-{sid}">{title}</h2>{intro}</header>{body}</section>')


# --------------------------------------------------------------------------
# The round files, split at headings, rendered once
# --------------------------------------------------------------------------

def prep_insights(md):
    md = re.sub(r"^`★ Insight ─+`\s*$", "\n\n@@INSIGHT_OPEN@@\n\n", md, flags=re.M)
    md = re.sub(r"^`─+`\s*$", "\n\n@@INSIGHT_CLOSE@@\n\n", md, flags=re.M)
    return md


def render_md(md):
    h = MD.render(prep_insights(md))
    h = h.replace("<p>@@INSIGHT_OPEN@@</p>", '<aside class="insight"><p class="insight-h">★ Insight</p>')
    h = h.replace("<p>@@INSIGHT_CLOSE@@</p>", "</aside>")
    return strip_links(h)


def split_chunks(src):
    chunks, cur, fence = [], {"level": 0, "title": "", "lines": []}, False
    chunks.append(cur)
    for line in TEXT[src].split("\n"):
        if line.startswith("```"):
            fence = not fence
        m = None if fence else re.match(r"^(#{1,6}) (.+?)\s*$", line)
        if m:
            cur = {"level": len(m.group(1)), "title": m.group(2), "lines": [line]}
            chunks.append(cur)
        else:
            cur["lines"].append(line)
    return chunks


CHUNKS = {s: split_chunks(s) for s in ROUND_SRCS + ["13"]}

# The two chunks of 13-the-box.md that the page opens in place; the rest of that
# file is shown as quotes, never as a template.
REPLY_13 = "Reply («2cb1e5b4», 2026-10-21 18:15, session text, verbatim)"
RESTATE_13 = "The session's restatement («2b5185b2», 2026-10-23 08:54, session text, verbatim)"


def chunk_id(src, i):
    return f"{src}-{i}"


def section_ids(src, title):
    ch = CHUNKS[src]
    for i, c in enumerate(ch):
        if c["title"] == title:
            out = [chunk_id(src, i)]
            for j in range(i + 1, len(ch)):
                if ch[j]["level"] <= c["level"]:
                    break
                out.append(chunk_id(src, j))
            return out
    sys.exit(f"build: no heading {title!r} in {src}")


def reply_ids(src):
    """Every chunk from '## Reply (Verbatim)' to the end of the file."""
    ch = CHUNKS[src]
    start = next(i for i, c in enumerate(ch) if c["title"].startswith("Reply"))
    return [chunk_id(src, i) for i in range(start, len(ch))]


def prompt_text(src):
    t = TEXT[src]
    i = t.index("## Prompt") + len("## Prompt")
    j = t.index("\n---", i)
    return t[i:j].strip("\n")


def templates_html():
    out = []
    for s in ROUND_SRCS + ["13"]:
        parts = []
        for i, c in enumerate(CHUNKS[s]):
            if c["level"] == 0 or c["title"] == "Prompt" or c["title"].startswith("Round "):
                continue
            if s == "13" and c["title"] not in (REPLY_13, RESTATE_13):
                continue
            parts.append(f'<div class="mdc" data-chunk="{chunk_id(s, i)}">{render_md(chr(10).join(c["lines"]))}</div>')
        out.append(f'<template id="tpl-{s}">{"".join(parts)}</template>')
    return "\n".join(out)


def work_attr(work):
    ids = []
    for src, title in work:
        ids += section_ids(src, title)
    return " ".join(ids)


def work_label(work):
    return " · ".join(f"{name_of(s)} § {t}" for s, t in work)


def work_short(work):
    return " · ".join(f"{name_of(s)} § {re.split(r'[.:]', t)[0]}" for s, t in work)


# --------------------------------------------------------------------------
# MAP.md, parsed where it is a list or a table
# --------------------------------------------------------------------------

MAP = TEXT["MAP"]


def map_section(heading):
    i = MAP.index(heading)
    i = MAP.index("\n", i) + 1
    m = re.search(r"^#{2,3} ", MAP[i:], flags=re.M)
    return MAP[i:i + m.start()] if m else MAP[i:]


BREAKOUT_TITLES = dict(re.findall(r"^- \*\*([A-M])\*\* (.+)$", MAP, flags=re.M))


def parse_agreements():
    sec = map_section("### Agreements across breakouts (signals, not decisions)")
    items = re.split(r"\n(?=\d+\. \*\*)", "\n" + sec.strip())
    out = []
    for it in items:
        it = it.strip()
        if not it:
            continue
        m = re.match(r"(\d+)\. \*\*(.+?)\*\*\s*(.*)", it, flags=re.S)
        n, title, rest = int(m.group(1)), m.group(2), m.group(3)
        rest = re.sub(r"\n {3}", "\n", rest)
        out.append({"n": n, "title": title, "rest": rest.strip()})
    return out


AG_LETTERS = {1: "A D E", 2: "A B E G", 3: "A D E G J K L", 4: "A C D F", 5: "E H J 06",
              6: "B E J K M 06", 7: "C H J", 8: "06", 9: "E J 06", 10: "B H I K M",
              11: "G I L M", 12: "K 06"}


def parse_tensions():
    sec = map_section("### Tensions")
    out = []
    for line in sec.splitlines():
        m = re.match(r"^\| (\d+) \| (.+?) \| (.+?) \| (.+?) \|$", line)
        if m:
            frm = [f.strip() for f in m.group(3).split(",")]
            cols = ["02" if f == "round 2" else f for f in frm]
            st = m.group(4)
            cat = ("sharpened" if st.startswith("sharpened") else "reframed" if st.startswith("reframed")
                   else "resolved" if st.startswith("resolved") else "reading" if st.startswith("one,")
                   else "open")
            out.append({"n": int(m.group(1)), "text": m.group(2), "from": m.group(3), "cols": cols,
                        "status": st, "cat": cat})
    return out


def parse_rounds_table():
    sec = map_section("## The rounds")
    out = {}
    for line in sec.splitlines():
        m = re.match(r"^\| \[(.+?)\]\(.+?\) \| (.+) \|$", line)
        if m:
            out[m.group(1).split("-")[0]] = m.group(2)
    return out


def parse_notebook():
    sec = map_section("### The notebook pages")
    p1, p2 = sec.split("Page 2:")
    lines1 = [l[2:] for l in p1.splitlines() if l.startswith("> ")]
    page1 = [lines1[0]] + [l[2:] for l in lines1[1:] if l.startswith("- ")]
    page2 = [l[2:] for l in p2.splitlines() if l.startswith("> ") and l[2:].strip()]
    return {1: page1, 2: page2}


AGREEMENTS = parse_agreements()
TENSIONS = parse_tensions()
ROUNDS_TABLE = parse_rounds_table()
NOTEBOOK = parse_notebook()
assert len(AGREEMENTS) == 12 and len(TENSIONS) == 16, (len(AGREEMENTS), len(TENSIONS))
for a in AGREEMENTS:  # every letter credited to an agreement must appear in its text
    for L in AG_LETTERS[a["n"]].split():
        assert L in a["rest"] or L in a["title"], (a["n"], L)
assert len(NOTEBOOK[1]) == 8 and len(NOTEBOOK[2]) == 3, NOTEBOOK
MAP_Q87 = re.search(r"### The question that opened the many.*?\n> (.+?)\n", MAP, flags=re.S).group(1)

# Links each breakout feeds, from MAP's agreements and tensions.
FEEDS = {L: {"ag": [], "tn": []} for L in MX_COLS}
for a in AGREEMENTS:
    for L in AG_LETTERS[a["n"]].split():
        FEEDS[L]["ag"].append(a["n"])
for t in TENSIONS:
    for L in t["cols"]:
        FEEDS[L]["tn"].append(t["n"])

# Levels each breakout or bridge names, from content.TOUCH.
LEVELS_OF = {}
for lv, items in C.TOUCH.items():
    for key, _ in items:
        LEVELS_OF.setdefault(key, set()).add(lv)

# --------------------------------------------------------------------------
# Masthead and overview figure
# --------------------------------------------------------------------------

def nums(xs, prefix):
    return " ".join(f'<button class="chip-go" data-go="{prefix}-{n}">{n}</button>' for n in xs)


def hero_data():
    nodes, edges, dashed = [], [], []
    nodes.append({"id": "nb", "stage": 0, "label": "notebook", "sub": "2 pages", "v": "notebook", "go": "start",
                  "cap": (f'<p class="cap-t">Two notebook pages, 21 October</p>'
                          f'<p>Page 1 lists problems; page 2 says where things stand and asks for the resident builder. '
                          f'Round 4 reads them as three seeds.</p>')})
    for s in C.SEEDS:
        nodes.append({"id": s["id"], "stage": 1, "label": s["short"], "v": {"s1": "the box", "s2": "builder", "s3": "levels"}[s["id"]], "go": "seeds",
                      "cap": (f'<p class="cap-t">{inl(s["title"])}</p>{quote(s["quote"], s["badge"])}'
                              f'<p class="ex">{inl(s["packed"])}</p>'
                              f'<p class="cap-m">Round 4’s tree sends it to {", ".join(s["to"])}. {src_label("04")}</p>')})
        edges.append(["nb", s["id"]])
        for L in s["to"]:
            (dashed if (s["id"], L) == ("s2", "K") else edges).append([s["id"], L])
    short = {"A": "the computer’s levels", "B": "levels from the log up", "C": "one truth, many views",
             "D": "the narrow waist", "E": "the fractal layer", "F": "knobs across levels", "G": "pace",
             "H": "Plan 9", "I": "Conway’s law and Linux", "J": "translating vocabularies",
             "K": "capability security", "L": "biology", "M": "economics"}
    for b in C.BREAKOUTS:
        L = b["id"]
        f = FEEDS[L]
        feeds = []
        if f["ag"]:
            feeds.append("agreements " + ", ".join(map(str, f["ag"])))
        if f["tn"]:
            feeds.append(("tension " if len(f["tn"]) == 1 else "tensions ") + ", ".join(map(str, f["tn"])))
        nodes.append({"id": L, "stage": 2, "label": short[L], "mark": L, "go": f"bo-{L}",
                      "cap": (f'<p class="cap-t"><span class="cap-l">{L}</span> {esc(BREAKOUT_TITLES[L])}</p>'
                              f'<p class="ex">{inl(b["framing"])}</p>'
                              f'<p class="cap-m">Rounds {" and ".join(b["rounds"])}. In the map: {"; ".join(feeds) or "no item cites it"}.</p>')})
        edges.append([L, "map"])
    for br in C.BRIDGES:
        nodes.append({"id": br["id"], "stage": 3, "label": br["short"], "mark": str(br["n"]), "go": f'bo-{br["id"]}',
                      "cap": (f'<p class="cap-t">Bridge {inl(br["title"])}</p><p class="ex">{inl(br["answer"])}</p>'
                              f'<p class="cap-m">Its question names {", ".join(br["from"])}. {src_label("06")}</p>')})
        for L in br["from"]:
            edges.append([L, br["id"]])
        edges.append([br["id"], "map"])
    nodes.append({"id": "map", "stage": 4, "label": "the map", "sub": "12 agreements", "sub2": "16 tensions", "v": "the map",
                  "go": "map", "cap": (f'<p class="cap-t">The map</p><p class="ex">{inl(C.NOTHING_DECIDED)}</p>'
                                       f'<p class="cap-m">12 agreements, 16 tensions, 4 questions open to Sid, 6 branches not opened. {src_label("MAP")}</p>')})
    nodes.append({"id": "r7", "stage": 5, "label": "round 7", "mark": "7", "sub": "a fresh session’s attempt", "v": "7", "go": "few2",
                  "cap": (f'<p class="cap-t">Round 7: a fresh session’s attempt at the other two “few” parts</p>'
                          f'<p class="ex">{inl(C.R7["floors"])} {src_label("07")}</p>'
                          f'<p class="cap-m">21 October, 17:24 UTC. Seven minutes later Sid typed his answer; it is under “Two few parts”.</p>')})
    nodes.append({"id": "r13", "stage": 5, "label": "round 13", "mark": "13", "sub": "the box", "v": "13", "go": "box-hub",
                  "cap": (f'<p class="cap-t">Round 13: the box</p>'
                          f'<p class="ex">{inl(C.R13["other_half"])} {src_label("13")}</p>'
                          f'<p class="cap-m">21 October, 18:15 UTC, session text. On 23 October Sid named it as the way he agrees with to talk about the whole.</p>')})
    dashed.append(["map", "r7"]); dashed.append(["r7", "r13"])
    return {"nodes": nodes, "edges": edges, "dashed": dashed}


def masthead():
    legend_sid = "".join(f"<li>{badge(k)}</li>" for k in ["hand", "typed87", "long", "go", "older", "pasted", "noprov"])
    legend_kind = "".join(f"<li>{kind_tag(k)}</li>" for k in ["checked", "outside", "derived"])
    return f"""
<section class="mast" id="top" data-nav="top" aria-labelledby="h-top">
  <p class="eyebrow">Softland · 21 to 23 October 2026 · few, many, few, told from the box</p>
  <h1 id="h-top">The box and the act</h1>
  <p class="lede">On 21 October Sid wrote two notebook pages and opened a session with them. Page 2 says what he has: a box. The session fanned out into a stack of
  eleven levels, thirteen breakouts and three bridges: the many. That evening a fresh session tried the other two “few” parts (round 7); seven minutes later Sid typed that it feels off. The same evening, asked how Rama combines with this, the session answered in terms of the box (round 13); on 23 October Sid named that as the way he agrees with to talk about the whole. This page tells the whole from the box.</p>
  <p class="lede-strong">{inl(C.NOTHING_DECIDED)} {src_label("MAP")}</p>
  {quote(C.SID13["names"], "typedc7", note=src_label("13"), cls="q-method")}
  <figure class="fmf" aria-labelledby="fmf-cap-title">
    <div class="fmf-stage" id="fmf" role="img" aria-label="Few, many, few: one notebook, three seeds, thirteen breakouts and three bridges, one map, then two later readings: round 7, a fresh session’s attempt at the other two “few” parts, and round 13, the box."></div>
    <div class="fmf-cap" id="fmf-cap" aria-live="polite">
      <p class="cap-t" id="fmf-cap-title">Few, many, few</p>
      <p>Point at a node to see what it is; select it again to go there. Lines from the seeds follow round 4’s tree.
      Lines into the bridges follow what each bridge’s question names. Every breakout and bridge feeds the map. After the map, two dashed nodes: round 7, a fresh session’s attempt at the other two “few” parts, and round 13, the box.</p>
    </div>
  </figure>
  {quote(C.FEW_MANY_FEW, "long", note=src_label("MAP"), cls="q-method")}
  <div class="key" aria-label="How to read this page">
    <div class="key-col"><p class="key-h">Whose words</p><ul class="key-list">{legend_sid}</ul>
      <p class="key-note">Every quoted line carries a badge (Sid) or a file name (the session, from the replies in the round files).
      Text with neither is this page’s own guide.</p></div>
    <div class="key-col"><p class="key-h">What kind of statement</p><ul class="key-list">{legend_kind}</ul>
      <p class="key-note">MAP.md sorts every claim this way. Breakout cards carry the tags that apply.</p>
      <button class="btn" id="mark-sid-2" aria-pressed="false">Mark Sid’s words on the page</button></div>
  </div>
</section>"""


# --------------------------------------------------------------------------
# The box: Sid's page 2, and what each session said about each face of it
# --------------------------------------------------------------------------

FACES = [
    ("in", "Into the box", "What goes in: an act. Who may put one in, what it carries, and what the box checks."),
    ("inside", "Inside the box", "What the box does with it. It decides and keeps; what else happens inside is where the sessions differ."),
    ("out", "Out of the box", "What comes out, through which layers, in whose words, and along which directions."),
]
THREAD_CHIPS = {"start": "the builder’s first light", "bo-C": "breakout C", "tn-1": "tension 1"}
# An item that shares a target with another but has its own chip text, keyed by the bold lead of its line.
THREAD_CHIP_OWN = {"Proofs": "round 1, proof first"}


def ex_line(*xs, sec=None, tail=""):
    """One paragraph of one or more excerpts, then the file they come from."""
    return f'<p class="ex">{" ".join(inl(x) for x in xs)} {src_label(xs[0].src, sec)}{tail}</p>'


def split2(s):
    """Break a phrase over two lines, at the space nearest its middle."""
    i = min((m.start() for m in re.finditer(" ", s)), key=lambda k: abs(k - len(s) / 2))
    return s[:i], s[i + 1:]


def box_svg(horiz):
    """The box, drawn from his page-2 sentence. Every word on the figure is his (content.PAGE2_WORDS)."""
    W = {k: esc(norm(v.md)) for k, v in C.PAGE2_WORDS.items()}
    o1, o2 = (esc(t) for t in split2(norm(C.PAGE2_WORDS["out"].md)))
    mid = "h" if horiz else "v"
    arr = f'class="an-line" marker-end="url(#bx-arr-{mid})"'

    def hit(go, label, inner):
        return (f'<g class="bx-hit" data-go="{go}" role="button" tabindex="0" aria-label="{label}">'
                f'<title>{label}</title>{inner}</g>')

    def zone(x, y, w, h):
        return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" class="bx-zone"/>'

    if horiz:  # wide: left to right
        into = hit("box-in", "Into the box",
                   zone(4, 20, 238, 210) + f'<line x1="22" y1="125" x2="232" y2="125" {arr}/>'
                   f'<text x="127" y="106" text-anchor="middle" class="an-t3">{W["in"]}</text>'
                   f'<text x="127" y="152" text-anchor="middle" class="an-t3">{W["act"]}</text>')
        inside = hit("box-inside", "Inside the box",
                     '<rect x="250" y="30" width="260" height="190" rx="10" class="an-box"/>'
                     '<text x="380" y="82" text-anchor="middle" class="bx-title">the box</text>'
                     f'<text x="380" y="130" text-anchor="middle" class="an-t3">{W["do"]}</text>'
                     f'<text x="380" y="158" text-anchor="middle" class="an-t3">{W["version"]}</text>')
        out = hit("box-out", "Out of the box",
                  zone(518, 20, 238, 210) + f'<line x1="528" y1="125" x2="738" y2="125" {arr}/>'
                  f'<text x="633" y="92" text-anchor="middle" class="an-t3">{o1}</text>'
                  f'<text x="633" y="110" text-anchor="middle" class="an-t3">{o2}</text>')
        vb = "0 0 760 250"
    else:  # narrow: top to bottom, so the words stay a readable size on a phone
        into = hit("box-in", "Into the box",
                   zone(4, 4, 312, 130) + f'<line x1="160" y1="14" x2="160" y2="124" {arr}/>'
                   f'<text x="148" y="60" text-anchor="end" class="an-t3">{W["in"]}</text>'
                   f'<text x="172" y="92" class="an-t3">{W["act"]}</text>')
        inside = hit("box-inside", "Inside the box",
                     '<rect x="4" y="142" width="312" height="140" rx="10" class="an-box"/>'
                     '<text x="160" y="184" text-anchor="middle" class="bx-title">the box</text>'
                     f'<text x="160" y="226" text-anchor="middle" class="an-t3">{W["do"]}</text>'
                     f'<text x="160" y="252" text-anchor="middle" class="an-t3">{W["version"]}</text>')
        out = hit("box-out", "Out of the box",
                  zone(4, 290, 312, 146) + f'<line x1="160" y1="300" x2="160" y2="362" {arr}/>'
                  f'<text x="160" y="390" text-anchor="middle" class="an-t3">{o1}</text>'
                  f'<text x="160" y="408" text-anchor="middle" class="an-t3">{o2}</text>')
        vb = "0 0 320 440"
    return (f'<svg viewBox="{vb}" class="bx-svg bx-{mid}" role="group" aria-label="The box, drawn from his page-2 sentence: '
            f'into the box, inside the box, out of the box.">'
            f'<defs><marker id="bx-arr-{mid}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">'
            f'<path d="M0 0L10 5L0 10z" fill="currentColor"/></marker></defs>{into}{inside}{out}</svg>')


def r13_table():
    """The table in round 13's reply: three plain columns, no header."""
    rows = []
    for x in C.R13_TABLE:
        cs = re.split(r"\s{2,}", x.md.strip())
        assert len(cs) == 3, cs
        rows.append("<tr>" + "".join(f"<td>{esc(c)}</td>" for c in cs) + "</tr>")
    return f'<table class="kt"><tbody>{"".join(rows)}</tbody></table>'


def hub_card(who, when, body, foot):
    return (f'<article class="hub-card"><header class="hub-h"><p class="hub-who">{who}</p><p class="hub-when">{when}</p></header>'
            f'{body}<div class="hub-f">{foot}</div></article>')


def hub_card_r2(face):
    lines = []
    for x in C.HUB[face]["r2"]:
        depot = (' <button class="chip-go" data-go="depot">both sides of the depot question</button>'
                 if face == "inside" and "Its wall leaks" in x.md else "")
        lines.append(ex_line(x, tail=depot))
    return hub_card("Round 2", f"21 October · the many session · {name_of('02')}", "".join(lines),
                    '<button class="chip-go" data-go="box">Does the box hold?</button>')


def hub_card_many(face):
    lines = []
    for key, a, b in C.HUB[face]["many"]:
        label = "02b, thread 2" if key == "02b-2" else key
        both = inl(a) + (f" {inl(b)}" if b else "")
        lines.append(f'<p class="ex"><button class="chip-go" data-go="bo-{key}">{label}</button> {both} {src_label(a.src)}</p>')
    return hub_card("The many", "21 October · rounds 3, 5 and 6, and the 02b branch", "".join(lines),
                    '<button class="chip-go" data-go="breakouts">All the breakouts</button>')


def hub_card_r7(face):
    R = C.R7
    if face == "in":
        body = ex_line(R["in_atomic"]) + ex_line(R["in_act"]) + ex_line(R["in_user"])
    elif face == "inside":
        body = (ex_line(R["inside_do"], R["inside_do2"]) + ex_line(R["inside_version"], R["inside_version2"])
                + ex_line(R["inside_glass"]) + ex_line(R["inside_two_maps"]))
    else:
        body = (ex_line(R["out_effect"]) + ex_line(R["out_read"]) + ex_line(R["out_notknow"])
                + f'<ul class="ex-list">{more_list(R["out_notknow_list"])}</ul>')
    foot = ('<span class="guide-inline">Sid, seven minutes later:</span> '
            '<button class="chip-go" data-go="few2-answer">his answer</button> '
            '<button class="chip-go" data-go="few2">the two “few” parts</button>')
    return hub_card("Round 7", f"21 October, 17:24 UTC · a fresh session’s attempt at the two “few” parts · {name_of('07')}", body, foot)


def hub_card_r13(face):
    R, S = C.R13, C.RESTATE
    if face == "in":
        main = (ex_line(R["today"]) + ex_line(R["three_log"])
                + guide("That session’s check, 21 October; its text names no file.", "guide-inline"))
        sub = ex_line(S["into"])
    elif face == "inside":
        main = ex_line(R["what_rama"]) + r13_table() + ex_line(R["both_ends"]) + ex_line(R["open"], R["gap"])
        sub = ex_line(S["inside"]) + f'<ul class="ex-list">{more_list(S["inside_list"])}</ul>'
    else:
        main = ex_line(R["directions"]) + f'<ul class="ex-list">{more_list(R["by"])}</ul>' + ex_line(R["no_mech"])
        sub = ex_line(S["out"]) + f'<ul class="ex-list">{more_list(S["out_list"])}</ul>'
    sub = ('<div class="hub-sub"><p class="mini-h">The session’s restatement, 23 October 08:54 UTC. '
           'Its reply to his line; the record holds no line of his on it.</p>' + sub + "</div>")
    when = (f"21 October, 18:15 UTC · session text · Sid named it on 23 October as the way he agrees with to talk about the whole · {name_of('13')}")
    return hub_card("Round 13", when, main + sub, '<button class="chip-go" data-go="rd-13">Round 13 in full</button>')


def hub_band(face, title, line):
    cards = "".join(f(face) for f in (hub_card_r2, hub_card_many, hub_card_r7, hub_card_r13))
    return (f'<div class="hub-band" id="box-{face}"><h3 class="sub-h">{title}</h3>{guide(line)}'
            f'<div class="hub-cards">{cards}</div></div>')


def hub_section():
    btns = "".join(f'<button class="seg" data-go="box-{k}">{t}</button>' for k, t, _ in FACES)
    bands = "".join(hub_band(k, t, line) for k, t, line in FACES)
    threads = "".join(
        f'<li>{inl(x)} {src_label("13")}'
        + (f' <button class="chip-go" data-go="{t}">{next((c for k, c in THREAD_CHIP_OWN.items() if f"**{k}:**" in x.md), THREAD_CHIPS[t])}</button>' if t else "") + "</li>"
        for t, x in C.RESTATE["threads"])
    body = f"""
{quote(C.PAGE2_SENTENCE, "hand", note=src_label("LOG") + ' <button class="chip-go" data-go="start">see it on the photo</button>', cls="q-root")}
<figure class="bx-fig">{box_svg(True)}{box_svg(False)}
<div class="bx-btns" role="group" aria-label="Go to a face of the box">{btns}</div>
<figcaption>Drawn from his page-2 sentence; the words on it are his. The three faces, into, inside and out of, are the phrasing of the round 13 session text and its 23 October restatement.</figcaption></figure>
{bands}
<h3 class="sub-h">In the same terms, what the restatement lists as open</h3>
{guide("The session’s list, in its words, from the 23 October restatement. It is not a plan; each item carries a chip to where this page holds the matter.")}
<p class="ex">{inl(C.RESTATE["threads_h"])}</p>
<ul class="ex-list">{threads}</ul>
{guide("Round 13 is one of rounds 8 to 14, which happened in the same session after round 7 and are not on disk. Only round 13 is on this page, because Sid named it. Its full text, his prompt, and his line naming it are under Rounds.")}"""
    return wrap_section("box-hub", "The box · Sid’s page 2, and what each session said about it",
                        "What goes in, what happens inside, what comes out?",
                        guide("Sid’s page 2 says what he has: a box. Below are his sentence, the box drawn from it, and what each session said about each of its three faces, in the order they were made. "
                              "None of the four readings is the answer: the last two are a fresh session’s attempt, and a session text he named as a way to talk."),
                        body)


# --------------------------------------------------------------------------
# Where did it start?
# --------------------------------------------------------------------------

def notebook_html():
    pages = []
    for p in (1, 2):
        items = []
        for i, line in enumerate(NOTEBOOK[p]):
            top, bot = C.NOTEBOOK_BANDS[p][i]
            extra = []
            for row in C.NOTEBOOK_SORTS.get((p, i), []):
                prob, lvl = cells(row)
                extra.append(f'<li><span class="sort-p">{inl(prob)}</span><span class="sort-arrow" aria-hidden="true">→</span>'
                             f'<span class="sort-l">{inl(lvl)}</span></li>')
            sorts = (f'<div class="nb-sort"><p class="nb-sort-h">Round 2 puts it on a level {src_label("02", "7")}</p>'
                     f'<ul>{"".join(extra)}</ul></div>') if extra else ""
            note = C.NOTEBOOK_NOTES.get((p, i))
            if note is not None:
                tail = (' <button class="chip-go" data-go="q-open">open question 1</button>' if note.src == "MAP" else
                        ' <button class="chip-go" data-go="seeds">the seeds</button>')
                sorts += f'<p class="nb-note">{inl(note)} {src_label(note.src)}{tail}</p>'
            items.append(f'<li class="nb-line" tabindex="0" data-band="{top},{bot}"><p class="nb-t">{esc(line)}</p>{sorts}</li>')
        alt = ("Sid’s handwritten notebook, page 1: a list of problems." if p == 1 else
               "Sid’s handwritten notebook, page 2: the box, the act, and the resident builder.")
        pages.append(f"""
<div class="nb-pair" data-page="{p}">
  <figure class="nb-photo">
    <button class="nb-zoom" data-full="img/notebook-p{p}.webp" aria-label="Open page {p} full size">
      <img src="img/notebook-p{p}.webp" width="1400" height="1866" alt="{alt}" loading="eager" decoding="async">
      <span class="nb-band" hidden></span>
    </button>
    <figcaption>Page {p}, photo. The photo is the source; select it to enlarge.</figcaption>
  </figure>
  <div class="nb-text">
    <p class="nb-h">Page {p} {badge("hand")} {src_label("MAP")}</p>
    <ol class="nb-lines">{"".join(items)}</ol>
  </div>
</div>""")
    return "".join(pages)


def first_light_html():
    cards = []
    for i, (step, q, need) in enumerate(C.FIRST_LIGHT, start=1):
        if need is not None:
            _, what, new = cells(need)
            is_new = new.startswith("**Yes")
            needs = (f'<p class="fl-need"><span class="lbl">Needs</span> {inl(what)}</p>'
                     f'<p class="fl-new{" is-new" if is_new else ""}"><span class="lbl">New code?</span> {inl(new)}</p>')
        else:
            needs = '<p class="fl-need fl-loop"><span aria-hidden="true">↺</span> back to step 2, FIND</p>'
        cards.append(f'<li class="fl-step{" fl-hot" if step == "RUN" else ""}"><p class="fl-n"><span>{i}</span> {step}</p>'
                     f'<p class="fl-q">{inl(q)}</p>{needs}</li>')
    _, under_what, under_new = cells(C.FIRST_LIGHT_UNDER)
    return f"""
<figure class="fl">
  <figcaption class="fig-h">The builder’s first light, as round 1 drew it {src_label("01", "2")}</figcaption>
  <ol class="fl-steps">{"".join(cards)}</ol>
  <div class="fl-host"><span class="lbl">Under all of it</span> {inl(under_what)} <span class="fl-new is-new">{inl(under_new)}</span></div>
  <p class="ex fl-so">{inl(C.FIRST_LIGHT_SO)} {src_label("01", "2")}</p>
</figure>"""


def seeds_html():
    cards = []
    for s in C.SEEDS:
        to = " ".join(f'<button class="chip-go" data-go="bo-{L}">{L}</button>' for L in s["to"])
        cards.append(f"""
<article class="seed" id="seed-{s['id']}">
  <h4>{inl(s['title'])}</h4>
  {quote(s['quote'], s['badge'], note=src_label('04', '1'))}
  <p class="ex">{inl(s['packed'])}</p>
  <p class="seed-to"><span class="lbl">Unpacked by</span> {to}</p>
</article>""")
    return f"""
<div class="seeds-wrap" id="seeds">
  <h3 class="sub-h">Three seeds, as the round 4 branch reads them <span class="tag-alt">alt branch 04</span></h3>
  {guide("Round 4 is an alternate branch: the session that wrote MAP.md does not hold it. Sid asked there where the exploration started, and the reply named one root question and three seeds.")}
  {quote(C.SEED_ROOT, "hand", note=src_label("04", "1"), cls="q-root")}
  <div class="seeds">{"".join(cards)}</div>
  <p class="ex">{inl(C.SEED_TOGETHER)} {src_label("04", "1")}</p>
  <p class="ex">{inl(C.SEED_TREE_NOTE)} {src_label("04", "2")}</p>
</div>"""


def start_section():
    round1 = render_md(map_section("### What round 1 proposed (the working is in 01)"))
    body = f"""
<div class="notebook">{notebook_html()}</div>
<div class="two-q">
  <div>
    <h3 class="sub-h">How the session opened</h3>
    {quote(C.OPENING_LINE, "noprov", note=src_label("01") + ' <span class="q-why">MAP.md gives no record id for this line.</span>')}
  </div>
  <div>
    <h3 class="sub-h">The question that opened the many</h3>
    {quote(C.X("MAP", MAP_Q87), "typed87", note=src_label("MAP"))}
  </div>
</div>
<h3 class="sub-h">What round 1 proposed</h3>
{guide("Round 1 answered the pages before the question about levels came. Its walk for the resident builder needs two new pieces of code, the host and the executor, and word search later.")}
{first_light_html()}
<div class="later"><p class="later-h">Later readings of the builder</p>
<p class="ex">{inl(C.R7["builder"])} {inl(C.R7["builder2"])} {src_label("07")}</p>
<p class="ex">{inl(C.RESTATE["threads"][0][1])} {src_label("13")}</p>
{guide("Two later sessions’ readings; neither is Sid’s.", "guide-inline")}</div>
<details class="more"><summary>Round 1 in MAP.md’s words</summary><div class="md-block">{round1}<p>{src_label("MAP")}</p></div></details>
{seeds_html()}"""
    return wrap_section("start", f'Start · from the notebook photos, {name_of("01")}, {name_of("04")} and MAP.md',
                        "Where did it start?",
                        guide("Two handwritten pages. Page 1 lists problems; page 2 says where things stand: a box that takes acts. "
                              "Point at a line of the transcription to find it on the photo."), body)


# --------------------------------------------------------------------------
# Does the box hold?
# --------------------------------------------------------------------------

def more_list(items):
    return "".join(f"<li>{inl(x)}</li>" for x in items)


def box_section():
    short = "".join(f'<div class="short">{inl(x)}</div>' for x in C.BOX_SHORT)
    nots = []
    for i, b in enumerate(C.BOX_NOT, start=1):
        depot = ('<button class="btn btn-small" data-go="depot">See both sides of the depot question</button>'
                 if b.get("depot") else "")
        nots.append(f'<article class="card"><p class="card-n">{i}</p><p>{inl(b["lead"])}</p>'
                    f'<ul class="ex-list">{more_list(b["more"])}</ul>{depot}</article>')
    spread = []
    for s in C.SPREAD:
        later = ""
        if s.get("later"):
            later = (f'<div class="later"><p class="later-h">Later rounds revised this</p>'
                     f'<p class="ex">{inl(s["later"][0])} {src_label("02b", "3.3")} <span class="tag-alt">alt branch 02b</span></p>'
                     f'<p class="ex">{inl(s["later"][1])} {src_label("03")}</p></div>')
        two = ('<button class="btn btn-small" data-go="waist2">See the two jobs drawn</button>'
               if "two jobs" in s["lead"].md else "")
        spread.append(f'<article class="card"><p>{inl(s["lead"])}</p><ul class="ex-list">{more_list(s["more"])}</ul>{later}{two}</article>')
    parts = "".join(f'<li data-part="{k}">{inl(x)}</li>' for k, x in C.LEVEL_PARTS)
    tests = "".join(f"<li>{inl(x)}</li>" for x in C.LEVEL_TESTS)
    dis = []
    for d in C.DISAGREE:
        pb = ""
        if d.get("pasted_back"):
            pb = (f'<p class="pasted-back">{badge("pasted")} Sid’s long prompt ‹bb0ad8a0› holds a paste that begins with these words. '
                  f'The words are the session’s. MAP.md: {inl(d["pasted_back"])} {src_label("MAP")}</p>')
        go = ('<button class="btn btn-small" data-go="tn-1">Tension 1: how fat is the act?</button>'
              if "slowest" in d["lead"].md else "")
        dis.append(f'<article class="card"><p>{inl(d["lead"])}</p><ul class="ex-list">{more_list(d["more"])}</ul>{pb}{go}</article>')
    D = C.DEPOT
    depot = f"""
<div class="depot" id="depot">
  <h3 class="sub-h">The depot inside every act-id: two sides, left open</h3>
  {guide("The 02b branch corrects what round 2 said about the word in the middle of every act-id. This page shows both sides and resolves nothing.")}
  <div class="depot-cols">
    <div class="depot-side">
      <p class="side-h">Round 2 says {src_label("02", "1")}</p>
      <p class="ex">{inl(D["r2"])}</p>
      <p class="ex">{inl(D["r2_honest"])} {src_label("02", "5")}</p>
      <p class="side-sub">The claim comes back in later rounds</p>
      <p class="ex">{inl(D["recur_A"])} {src_label("03", "A")}</p>
      <p class="ex">{inl(D["recur_G"])} {src_label("05", "G")}</p>
    </div>
    <div class="depot-side depot-alt">
      <p class="side-h">The 02b branch corrects it {src_label("02b")} <span class="tag-alt">alt branch 02b</span></p>
      <p class="ex">{inl(D["b2"])}</p>
      <p class="ex">{inl(D["b2_knob"])} {src_label("02b", "3.7")}</p>
    </div>
  </div>
  <div class="depot-agree">
    <p class="side-sub">Where the two agree</p>
    <p class="ex">{inl(D["shared"])} {src_label("02b", "2.6")}</p>
    <p class="side-sub">What MAP.md says</p>
    <p class="ex">{inl(D["map_warn"])} {src_label("MAP")}</p>
    <p class="ex">{inl(D["map_window"])} {src_label("MAP", "Time-sensitive")}</p>
    {guide("A note from this page: in 03 the depot sentence sits under breakout A, not D. MAP.md names D.", "guide guide-flag")}
  </div>
</div>"""
    body = f"""
<div class="shorts">{short}</div>
<p class="srcline">{src_label("02", "Short answers")}</p>
<h3 class="sub-h">What holds, and three ways it does not</h3>
<p class="ex">{inl(C.BOX_HOLDS)} {src_label("02", "1")}</p>
<div class="cards3">{"".join(nots)}</div>
{depot}
<h3 class="sub-h">Why every new idea spread</h3>
<p class="ex">{inl(C.SPREAD_INTRO)} {src_label("02", "2")}</p>
<div class="cards3">{"".join(spread)}</div>
<p class="ex">{inl(C.SPREAD_QUAL)} {src_label("02", "2")}</p>
<h3 class="sub-h">What a level is</h3>
<div class="anatomy">
  <figure class="anat-fig">{anatomy_svg()}<figcaption>A level, drawn from round 2’s four parts and the knobs that breakout F adds.</figcaption></figure>
  <div>
    <ul class="parts">{parts}</ul>
    <p class="ex">{inl(C.LEVEL_KNOBS)} {src_label("03", "F")}</p>
    <p class="mini-h">Four tests that a boundary is real</p>
    <ol class="tests">{tests}</ol>
    <p class="ex">{inl(C.LEVEL_PROOF)} {inl(C.LEVEL_AGENT)} {src_label("02", "7")}</p>
  </div>
</div>
<h3 class="sub-h">Where round 2 disagreed</h3>
<div class="cards2">{"".join(dis)}</div>
<p class="ex why-now">{inl(C.WHY_NOW)} {src_label("02", "6")} <button class="btn btn-small" data-go="time">The window, in the map</button></p>"""
    return wrap_section("box", f'Does it hold? · from {name_of("02")} and the 02b branch', "Does the box hold?",
                        guide("Sid asked whether “the box” holds, and what the abstraction levels are. Round 2’s answer, with the alternate branch beside it. "
                              "Round 7’s and round 13’s answers to the same question sit in The box, above. "
                              '<button class="chip-go" data-go="box-hub">The box</button>'),
                        body)


def anatomy_svg():
    return """<svg viewBox="0 0 420 300" role="img" aria-label="A level: words inside, a hidden secret, promises up to the level above, one direction down, and knobs on its side.">
<defs><marker id="an-arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="currentColor"/></marker>
<pattern id="an-hatch" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><line x1="0" y1="0" x2="0" y2="6" class="an-hatch"/></pattern></defs>
<rect x="60" y="10" width="300" height="34" rx="6" class="an-slab"/><text x="210" y="32" text-anchor="middle" class="an-t2">the level above</text>
<rect x="60" y="256" width="300" height="34" rx="6" class="an-slab"/><text x="210" y="278" text-anchor="middle" class="an-t2">the levels below</text>
<rect x="60" y="78" width="300" height="146" rx="10" class="an-box"/>
<text x="78" y="104" class="an-t">words</text><text x="78" y="124" class="an-t3">the few nouns</text><text x="78" y="140" class="an-t3">and verbs</text>
<rect x="208" y="96" width="136" height="112" rx="6" class="an-secret"/><rect x="208" y="96" width="136" height="112" rx="6" fill="url(#an-hatch)"/>
<text x="276" y="148" text-anchor="middle" class="an-t">secret</text><text x="276" y="166" text-anchor="middle" class="an-t3">hidden, free to change</text>
<line x1="120" y1="78" x2="120" y2="48" class="an-line" marker-end="url(#an-arr)"/><text x="128" y="66" class="an-t3">promises up</text>
<line x1="120" y1="224" x2="120" y2="252" class="an-line" marker-end="url(#an-arr)"/><text x="128" y="244" class="an-t3">uses only what is below</text>
<g class="an-knobs"><line x1="360" y1="122" x2="378" y2="122" class="an-line"/><circle cx="390" cy="122" r="11" class="an-knob"/><line x1="390" y1="122" x2="396" y2="113" class="an-line"/>
<line x1="360" y1="170" x2="378" y2="170" class="an-line"/><circle cx="390" cy="170" r="11" class="an-knob"/><line x1="390" y1="170" x2="382" y2="163" class="an-line"/>
<text x="392" y="210" class="an-t" text-anchor="middle">knobs</text></g>
</svg>"""


# --------------------------------------------------------------------------
# What are the levels?
# --------------------------------------------------------------------------

def touch_html(lv):
    rows = []
    for key, ex in C.TOUCH[lv]:
        if key.startswith("T"):
            t = TENSIONS[int(key[1:]) - 1]
            rows.append(f'<li><button class="chip-go chip-t" data-go="tn-{t["n"]}">T{t["n"]}</button> '
                        f'<span class="t-q">{inl(t["text"])}</span> <span class="st st-{t["cat"]}">{esc(t["status"])}</span> {src_label("MAP", "Tensions")}</li>')
        elif key == "words":
            rows.append(f'<li><span class="chip-w">words</span> <span class="ex">{inl(ex)}</span> {src_label("MAP", "Words")}</li>')
        else:
            target = f"bo-{key}"
            label = key if not key.startswith("06") else f"bridge {key[-1]}"
            rows.append(f'<li><button class="chip-go" data-go="{target}">{label}</button> <span class="ex">{inl(ex)}</span> {src_label(ex.src)}</li>')
    return f'<ul class="touch">{"".join(rows)}</ul>'


def level_detail(lv):
    L = C.LEVELS[lv]
    src = L["src"]
    sec = "B" if src == "03" else "B, continued"
    parts = [f'<p class="lv-d-h"><span class="lv-d-n">{lv}</span> {esc(L["name"])}</p>',
             f'<p class="lv-row"><span class="lbl">Settles</span> {inl(L["settles"])}</p>']
    if "words" in L:
        w = L["words"].md
        w = w[len("Words: "):] if w.startswith("Words: ") else w
        parts.append(f'<p class="lv-row"><span class="lbl">Words</span> {inl(w)}</p>')
    if "holds" in L:
        parts.append(f'<div class="lv-row"><span class="lbl">Holds</span><ul>{more_list(L["holds"])}</ul></div>')
    if "halves" in L:
        parts.append(f'<div class="lv-row"><span class="lbl">Two halves</span><ul>{more_list(L["halves"])}</ul></div>')
    if "promises" in L:
        parts.append(f'<div class="lv-row"><span class="lbl">Promises</span><ul>{more_list(L["promises"])}</ul></div>')
    if "from" in L:
        parts.append(f'<p class="lv-row"><span class="lbl">From</span> {inl(L["from"])}</p>')
    if "editor" in L:
        parts.append(f'<p class="lv-row lv-ed"><span class="lbl">Your editor</span> {inl(L["editor"])}</p>')
    if "extra" in L:
        parts.append(f'<div class="lv-row"><span class="lbl">Also</span><ul>{more_list(L["extra"])}</ul></div>')
    parts.append(f'<p class="srcline">{src_label(src, sec)}</p>')
    parts.append(f'<p class="mini-h">Where the rounds tie it to other parts</p>{touch_html(lv)}')
    return f'<div class="lv-d" data-detail="lv-{lv}">{"".join(parts)}</div>'


def stack_marks_details():
    t1, t2 = TENSIONS[0], TENSIONS[1]
    store = f"""<div class="lv-d" data-detail="store"><p class="lv-d-h">The store as built: levels 1 to 5</p>
<p class="ex">{inl(C.STORE_AS_BUILT)} {src_label("MAP", "The stack")}</p>{kind_tag("reading")}
<p class="ex">{inl(C.BOX_NOT[0]["lead"])} {src_label("02", "1")}</p>
<p class="ex">{inl(C.BREAKOUTS[1]["shows"][0])} {src_label("03", "B")}</p>
<p class="mini-h">Tensions about it</p><ul class="touch">
<li><button class="chip-go chip-t" data-go="tn-1">T1</button> {inl(t1["text"])} <span class="st st-open">{esc(t1["status"])}</span></li>
<li><button class="chip-go chip-t" data-go="tn-2">T2</button> {inl(t2["text"])} <span class="st st-open">{esc(t2["status"])}</span></li></ul>
<p><button class="btn btn-small" data-go="time">The window before the first real record</button></p></div>"""
    waist = f"""<div class="lv-d" data-detail="waist"><p class="lv-d-h">The waist runs through level 6</p>
<p class="ex">{inl(C.WAIST_LINE)} {src_label("MAP", "The stack")}</p>
<p class="ex">{inl(C.WAIST_R2)} {src_label("02", "4")}</p>
<p class="ex">{inl(C.HOURGLASS_SO)} {src_label("03", "D")}</p>
<p><button class="btn btn-small" data-go="waist2">The waist, asked to do two jobs</button></p></div>"""
    t10 = TENSIONS[9]
    nine = f"""<div class="lv-d" data-detail="910"><p class="lv-d-h">Surfaces and Components: one level or two?</p>
<p class="ex">{inl(C.LEVELS[10]["extra"][1])} {src_label("05", "B, continued")}</p>
<p class="ex">{inl(C.GREW[4]["caption"])} {src_label("06")}</p>
<ul class="touch"><li><button class="chip-go chip-t" data-go="tn-10">T10</button> {inl(t10["text"])} <span class="st st-reading">{esc(t10["status"])}</span></li></ul>
<p><button class="btn btn-small" data-go="bo-06-1">Bridge 1</button></p></div>"""
    threads = "".join(
        f'<li><p class="th-name">{esc(name)}</p><p class="ex">{inl(x)} {src_label(src)}</p></li>' for name, x, src in C.THREADS)
    thr = f"""<div class="lv-d" data-detail="threads"><p class="lv-d-h">Threads through every level</p>
<p class="ex">{inl(C.THREADS_ALL)} {src_label("02", "4")}</p><ul class="threads-list">{threads}</ul></div>"""
    strata = f"""<div class="lv-d" data-detail="strata"><p class="lv-d-h">The second axis: what it is made of</p>
<p class="ex">{inl(C.MATERIAL_AXIS)} {src_label("MAP", "The stack")}</p>
<p class="ex">{inl(C.MATERIAL_HOW)} {src_label("05", "B, continued")}</p>
<p><button class="btn btn-small" data-go="waist2">Where the two axes part ways</button></p></div>"""
    return store + waist + nine + thr + strata


def tower_html():
    rows = []
    for lv in range(11, 0, -1):
        L = C.LEVELS[lv]
        note = f' <span class="tw-note">{inl(L["note"])}</span>' if "note" in L else ""
        cls = ["tw-row"]
        if lv <= 5:
            cls.append("in-store")
        if lv == 5:
            cls.append("store-top")
        if lv == 1:
            cls.append("store-bot")
        if lv in (9, 10):
            cls.append("in-910")
        if lv == 10:
            cls.append("b910-top")
        if lv == 9:
            cls.append("b910-bot")
        if lv == 6:
            cls.append("is-waist")
        code_on = "on" if lv <= 6 else ""
        waist = (f'<span class="tw-waist-l">{inl(C.WAIST_LINE)}</span>' if lv == 6 else "")
        rows.append(f"""<button class="{' '.join(cls)}" id="lv-{lv}" data-detail="lv-{lv}" aria-expanded="false">
<span class="tw-gut" aria-hidden="true"></span><span class="tw-n">{lv}</span>
<span class="tw-main"><span class="tw-name">{esc(L['name'])}</span><span class="tw-q">{inl(L['q'])}{note}</span>{waist}</span>
<span class="tw-code {code_on}" aria-hidden="true">{"engine" if lv == 6 else ""}</span><span class="tw-mat on" aria-hidden="true">{"records" if lv == 6 else ""}</span>
<span class="tw-thr" aria-hidden="true"></span></button>""")
    return f"""
<div class="tower" role="group" aria-label="The eleven levels, top to bottom">
  <div class="tw-head" aria-hidden="true"><span class="tw-gut"></span><span class="tw-n">#</span><span class="tw-main">level, and the question it lets the levels above stop asking</span>
    <span class="tw-code">code</span><span class="tw-mat">material</span><span class="tw-thr tw-thr-h"><i>who</i><i>when</i><i>why</i><i>about whom</i></span></div>
  {"".join(rows)}
  <div class="tw-base"><span class="tw-gut"></span><span class="tw-n"></span><span class="tw-main">Rama · JVM · disks</span><span class="tw-host">host floor</span><span class="tw-thr"></span></div>
</div>
<div class="tw-marks" role="group" aria-label="Marks on the stack">
  <button class="mark" data-detail="store"><span class="mk mk-store" aria-hidden="true"></span>the store as built, 1 to 5</button>
  <button class="mark" data-detail="waist"><span class="mk mk-waist" aria-hidden="true"></span>the waist, through 6</button>
  <button class="mark" data-detail="910"><span class="mk mk-910" aria-hidden="true"></span>9 and 10, one level?</button>
  <button class="mark" data-detail="threads"><span class="mk mk-thr" aria-hidden="true"></span>the threads</button>
  <button class="mark" data-detail="strata"><span class="mk mk-strata" aria-hidden="true"></span>code and material</button>
</div>"""


def grew_html():
    cols = []
    for g in C.GREW:
        bars = ['<li class="g-base">Rama · JVM · disks</li>']
        for i, name in enumerate(g["levels"]):
            waist = g["src"] in ("05", "06") and name == "Computation"
            cls = "g-bar" + (" g-merged" if "+" in name else "") + (" g-waist" if waist else "")
            bars.append(f'<li class="{cls}"><span>{esc(name)}</span></li>')
            if g["src"] == "02" and i == 1:
                bars.append(f'<li class="g-line">{inl(g["boundary"][1])}</li>')
        extra = ""
        if g["src"] == "02b":
            extra = f'<p class="g-side">{inl(C.GREW_BESIDE)}</p><p class="g-side">{inl(C.GREW_BEFORE)}</p>'
        if g["src"] == "05":
            extra = f'<p class="g-side">{inl(g["boundary"][1])}</p>'
        alt = ' <span class="tag-alt">alt</span>' if g["alt"] else ""
        cols.append(f"""<figure class="g-col{' g-alt' if g['alt'] else ''}">
<figcaption class="g-h">{esc(g['label'])}{alt}</figcaption>
<div class="g-box"><ul class="g-stack">{"".join(bars)}</ul></div>
<p class="g-thr">through every level: {esc(g['through'])}</p>{extra}
<p class="ex g-cap">{inl(g['caption'])} {src_label(g['src'])}</p></figure>""")
    return f'<div class="grew" tabindex="0" role="group" aria-label="The stack as each round drew it, oldest first">{"".join(cols)}</div>'


def two_lines_svg():
    return """<svg viewBox="0 0 440 400" class="two-lines" role="img" aria-label="Two questions draw two different lines. Code versus data is one; low versus high level is the other. They mostly line up, but a permission grant is low and data, and the grammar reader is code serving a high need.">
<defs><marker id="tl-arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="currentColor"/></marker></defs>
<rect x="40" y="40" width="190" height="150" class="tl-q tl-part"/><rect x="230" y="40" width="190" height="150" class="tl-q tl-agree"/>
<rect x="40" y="190" width="190" height="150" class="tl-q tl-agree"/><rect x="230" y="190" width="190" height="150" class="tl-q tl-part"/>
<line x1="230" y1="30" x2="230" y2="350" class="tl-axis tl-v"/><line x1="30" y1="190" x2="430" y2="190" class="tl-axis tl-h"/>
<text x="135" y="370" text-anchor="middle" class="tl-t2">code</text><text x="325" y="370" text-anchor="middle" class="tl-t2">data</text>
<text x="20" y="115" text-anchor="middle" class="tl-t2" transform="rotate(-90 20 115)">high</text><text x="20" y="265" text-anchor="middle" class="tl-t2" transform="rotate(-90 20 265)">low</text>
<text x="230" y="20" text-anchor="middle" class="tl-t3">Sid’s waist: code | built on top</text>
<text x="46" y="182" class="tl-t3">a level line</text>
<text x="135" y="92" text-anchor="middle" class="tl-t">grammar reader</text><text x="135" y="112" text-anchor="middle" class="tl-t3">code, serves a</text><text x="135" y="128" text-anchor="middle" class="tl-t3">high-level need</text>
<text x="306" y="92" text-anchor="middle" class="tl-t">records</text><text x="306" y="112" text-anchor="middle" class="tl-t3">above the waist, data</text>
<text x="135" y="282" text-anchor="middle" class="tl-t">engines</text><text x="135" y="300" text-anchor="middle" class="tl-t3">levels 1 to 6, code</text>
<text x="306" y="282" text-anchor="middle" class="tl-t">permission grant</text><text x="306" y="300" text-anchor="middle" class="tl-t3">low-level, but data</text>
<line x1="404" y1="138" x2="404" y2="250" class="tl-arrow" marker-end="url(#tl-arr)"/>
<text x="396" y="150" text-anchor="end" class="tl-t3">key meaning:</text><text x="396" y="166" text-anchor="end" class="tl-t3">sounds high,</text>
<text x="396" y="216" text-anchor="end" class="tl-t3">lasts as long</text><text x="396" y="232" text-anchor="end" class="tl-t3">as the facts</text>
</svg>"""


def hourglass_svg(above, below):
    return (f'<svg viewBox="0 0 120 120" class="hg" aria-hidden="true">'
            f'<path d="M14 10 H106 L64 56 V64 L106 110 H14 L56 64 V56 Z" class="hg-shape"/>'
            f'<line x1="44" y1="60" x2="76" y2="60" class="hg-waist"/></svg>')


def stack_section():
    details = "".join(level_detail(lv) for lv in range(1, 12)) + stack_marks_details()
    T = C.TWO_LINES
    hg = "".join(f"""<figure class="hg-item"><p class="hg-above">{esc(a)}</p>{hourglass_svg(a, b)}<p class="hg-below">{esc(b)}</p>
<figcaption><b>{esc(name)}</b><span class="ex">{inl(x)}</span></figcaption></figure>""" for x, name, a, b in C.HOURGLASSES)
    body = f"""
<p class="ex stack-intro">{inl(C.STACK_INTRO)} {src_label("MAP", "The stack")} {kind_tag("derived")}</p>
<div class="stack-wrap">
  <div class="stack-left">{tower_html()}</div>
  <aside class="lv-panel" id="lv-panel" aria-live="polite">
    <div class="lv-empty"><p class="lv-d-h">Pick a level</p>
    <p class="guide">Each row is one level, with the question it settles. The two narrow columns are the second axis:
    whether the level has an engine in Softland code, and whether its cases live in material. Select a row or a mark below the stack.</p></div>
  </aside>
</div>
<template id="lv-details">{details}</template>
<h3 class="sub-h">How the stack grew, round by round</h3>
{guide("Each column is the stack as one round drew it; the 02b branch drew its own. None of them is a decision.")}
{grew_html()}
<div class="waist2" id="waist2">
  <h3 class="sub-h">The waist, asked to do two jobs</h3>
  {guide("Round 2 found that Sid’s word “waist” answers one question, and a level answers another. Drawn as two lines, they mostly agree. The shaded squares are where the two answers agree; the outlined squares and the arrow are round 2’s three examples where they part ways.")}
  <div class="tl-wrap">
    <figure class="tl-fig">{two_lines_svg()}<figcaption>Two lines from round 2, section 2c. {src_label("02", "2")}</figcaption></figure>
    <div class="tl-text">
      {quote(T["sid"], "older", note=src_label("02", "2"))}
      <p class="ex"><span class="lbl">Sid’s line answers</span> {inl(T["q_made"])}</p>
      <p class="ex"><span class="lbl">A level answers</span> {inl(T["q_level"])}</p>
      <ul class="ex-list">{more_list([T["grant"], T["grammar"], T["keymeaning"]])}</ul>
      <p class="ex">{inl(T["lined_up"])} {src_label("02", "2")}</p>
      <p class="ex">{inl(T["engines"])} {src_label("05", "B, continued")}</p>
    </div>
  </div>
  <h3 class="sub-h">Breakout D: several waists, each a narrow language</h3>
  <div class="hgs">{hg}</div>
  <p class="ex">{inl(C.HOURGLASS_SO)} {src_label("03", "D")}</p>
</div>"""
    return wrap_section("stack", f"Levels · from MAP.md, {name_of('02')}, {name_of('02b')}, {name_of('03')}, {name_of('05')} and {name_of('06')}",
                        "What are the levels?",
                        guide("Eleven levels, each defined by a question. The derivation is the session’s, not Sid’s, and not from the code. "
                              "Round 7 drew a different picture, five floors; it is under the two “few” parts. "
                              '<button class="chip-go" data-go="few2">five floors</button>'),
                        body)


# --------------------------------------------------------------------------
# What does the word "layer" carry?
# --------------------------------------------------------------------------

def relations_svg():
    return """<svg viewBox="0 0 640 340" class="rel-svg" data-show="kind" role="img" aria-label="The editor example with four relations: kind of, built on, made of, and words for.">
<defs><marker id="rel-arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="currentColor"/></marker></defs>
<g class="rel-e" data-rel="kind"><path d="M106 120 V71" marker-end="url(#rel-arr)"/><path d="M106 210 V161" marker-end="url(#rel-arr)"/><text x="114" y="100">kind of</text><text x="114" y="190">kind of</text></g>
<g class="rel-e" data-rel="built"><path d="M178 288 C160 272 128 262 116 251" marker-end="url(#rel-arr)"/><path d="M226 288 C262 272 306 262 322 251" marker-end="url(#rel-arr)"/><text x="132" y="282" text-anchor="end">builds on</text><text x="276" y="292">builds on</text></g>
<g class="rel-e" data-rel="made"><path d="M172 46 H452 M452 30 V150 M452 30 H469 M452 70 H469 M452 110 H469 M452 150 H469"/><circle cx="452" cy="46" r="3"/><text x="300" y="38" text-anchor="middle">made of</text></g>
<g class="rel-e" data-rel="words"><path d="M150 66 C172 94 200 110 224 118" marker-end="url(#rel-arr)"/><path d="M286 154 C262 186 214 214 178 224" marker-end="url(#rel-arr)"/><text x="206" y="94" text-anchor="middle">gives words</text><text x="250" y="200">written in</text></g>
<g class="rel-n"><rect x="40" y="30" width="132" height="36" rx="8"/><text x="106" y="53" text-anchor="middle">editor</text></g>
<g class="rel-n"><rect x="40" y="120" width="132" height="36" rx="8"/><text x="106" y="143" text-anchor="middle">code editor</text></g>
<g class="rel-n"><rect x="40" y="210" width="132" height="36" rx="8"/><text x="106" y="233" text-anchor="middle">vim-like</text></g>
<g class="rel-n"><rect x="262" y="210" width="132" height="36" rx="8"/><text x="328" y="233" text-anchor="middle">vscode-like</text></g>
<g class="rel-n rel-me"><rect x="136" y="288" width="132" height="36" rx="8"/><text x="202" y="311" text-anchor="middle">my editor</text></g>
<g class="rel-n rel-w"><rect x="226" y="120" width="216" height="34" rx="8"/><text x="334" y="142" text-anchor="middle">buffer · cursor · mode · keymap</text></g>
<g class="rel-n rel-p"><rect x="470" y="14" width="140" height="32" rx="6"/><text x="540" y="35" text-anchor="middle">buffer</text>
<rect x="470" y="54" width="140" height="32" rx="6"/><text x="540" y="75" text-anchor="middle">keymap</text>
<rect x="470" y="94" width="140" height="32" rx="6"/><text x="540" y="115" text-anchor="middle">renderer</text>
<rect x="470" y="134" width="140" height="32" rx="6"/><text x="540" y="155" text-anchor="middle">command list</text></g>
</svg>"""


def layer_section():
    rel_btns = "".join(f'<button class="seg" data-rel="{k}" aria-pressed="{"true" if k == "kind" else "false"}">{esc(name)}</button>'
                       for k, name, _, _ in C.RELATIONS)
    rel_text = "".join(f'<div class="rel-say" data-rel="{k}"{"" if k == "kind" else " hidden"}><p class="ex">{inl(x)}</p>'
                       f'<p class="rel-alg">Round 5 puts it under <button class="chip-go" data-go="alg-{alg}">{alg}</button></p></div>'
                       for k, name, x, alg in C.RELATIONS)
    algs = "".join(f"""<article class="alg" id="alg-{k}"><p class="alg-h">{esc(k)} <span class="alg-m">{esc(m)}</span></p>
<p class="ex">{inl(x)}</p><p class="ex">{inl(y)} {src_label(y.src)}</p></article>""" for k, m, x, y in C.ALGEBRAS)
    laws = "".join(f'<li><p>{inl(a)}</p><p class="ex">{inl(b)}</p></li>' for a, b in C.LAWS)
    interact = "".join(f"<li>{inl(x)}</li>" for x in C.INTERACT)
    body = f"""
{quote(C.LAYER_SID, "long", note=src_label("MAP", "The stack"))}
<p class="ex">{inl(C.LAYER_MAP)} {src_label("MAP", "The stack")}</p>
<h3 class="sub-h">Four relations in one word</h3>
{guide("Round 3 found four different relations folded into Sid’s editor example. Pick one to see its arrows.")}
<div class="rel">
  <div class="rel-ctl" role="group" aria-label="Relation">{rel_btns}<button class="seg" data-rel="all" aria-pressed="false">All four</button></div>
  <figure class="rel-fig">{relations_svg()}<figcaption>The editor example, drawn from round 3’s four sentences. {src_label("03", "First, your word “layer”")}</figcaption></figure>
  <div class="rel-text">{rel_text}<div class="rel-say" data-rel="all" hidden><p class="ex">{inl(C.RELATIONS_TROUBLE)}</p><p class="ex">{inl(C.RELATIONS_ONE)}</p></div></div>
</div>
<h3 class="sub-h">Three algebras behind the four relations</h3>
<div class="algs">{algs}</div>
<p class="ex">{inl(C.ALGEBRAS_WHY)} {src_label("05", "E, continued")}</p>
<details class="more"><summary>How delegation and containment interact: eight points from breakout E</summary>
<ol class="ex-list">{interact}</ol><p>{src_label("05", "E, continued, Part 2")} <button class="btn btn-small" data-go="bo-E">Breakout E</button></p></details>
<h3 class="sub-h">Four laws that hold them together</h3>
<ol class="laws">{laws}</ol>
<p class="ex">{inl(C.LAWS_SO)} {src_label("06", "3")}</p>
<div class="alt-read">
  <h3 class="sub-h">Another reading, in the 02b branch <span class="tag-alt">alt branch 02b</span></h3>
  {guide("The 02b branch, which the MAP session does not hold, built the same editor from two primitives instead of three algebras.")}
  <ul class="ex-list">{more_list(C.ALT_PRIMS)}</ul>
  <p class="ex">{inl(C.ALT_SAME)} {inl(C.ALT_RUNS)}</p>
  <p class="ex">{inl(C.ALT_BRIDGE)} {src_label("02b", "3")}</p>
</div>"""
    return wrap_section("layer", f"Layer · from MAP.md, {name_of('03')}, {name_of('05')}, {name_of('06')} and the {name_of('02b')} branch",
                        "What does the word “layer” carry?",
                        guide("Sid uses “layer” for something fractal. The rounds unpacked it into four relations, three algebras and four laws."),
                        body)


# --------------------------------------------------------------------------
# What did each breakout find?
# --------------------------------------------------------------------------

def links_line(key, letter=None):
    bits = []
    lv = sorted(LEVELS_OF.get(key, []))
    if lv:
        bits.append('<span class="lk-h">levels</span> ' + " ".join(f'<button class="chip-go" data-go="lv-{n}">{n}</button>' for n in lv))
    f = FEEDS.get(letter or key)
    if f and f["ag"]:
        bits.append('<span class="lk-h">agreements</span> ' + nums(f["ag"], "ag"))
    if f and f["tn"]:
        bits.append('<span class="lk-h">tensions</span> ' + nums(f["tn"], "tn"))
    return f'<p class="bo-links">{" ".join(bits)}</p>' if bits else ""


def bo_card(b):
    L = b["id"]
    dirs = " ".join(d["id"] for d in C.DIRECTIONS if L in d["letters"])
    shows = "".join(f"<li>{inl(x)}</li>" for x in b["shows"])
    editor = " ".join(inl(x) for x in b["editor"])
    tail = (f'<p class="bo-pulls"><span class="lbl">Where it pulls</span> {inl(b["pulls"])}</p>' if "pulls" in b else
            f'<p class="bo-pulls"><span class="lbl">What it adds</span> {inl(b["adds"])}</p>')
    sid = quote(C.KNOBS_SID, "long", note=src_label("MAP")) if b.get("sid") == "knobs" else ""
    kinds = " ".join(kind_tag(k) for k in b["kinds"])
    return f"""<article class="bo" id="bo-{L}" data-dirs="{dirs}">
<header class="bo-h"><span class="bo-letter">{L}</span><h3 class="bo-title">{esc(BREAKOUT_TITLES[L])}</h3><span class="bo-rounds">{" · ".join(b["rounds"])}</span></header>
{sid}<p class="bo-frame"><span class="lbl">The framing</span> {inl(b["framing"])}</p>
<p class="lbl lbl-block">What it shows</p><ul class="bo-shows">{shows}</ul>
<p class="bo-editor"><span class="lbl">Your editor</span> {editor}</p>
{tail}
{links_line(L)}
<footer class="bo-f"><span class="kinds">{kinds}</span><span class="src" title="{esc(work_label(b['work']))}">{esc(work_short(b['work']))}</span>
<button class="btn bo-open" aria-expanded="false" data-work="{work_attr(b['work'])}">Read the working</button></footer>
<div class="bo-work" hidden></div></article>"""


def bridge_card(br):
    shows = "".join(f"<li>{inl(x)}</li>" for x in br["shows"])
    kinds = " ".join(kind_tag(k) for k in br["kinds"])
    return f"""<article class="bo bo-bridge" id="bo-{br['id']}">
<header class="bo-h"><span class="bo-letter">{br['n']}</span><h3 class="bo-title">{inl(br['title'].md.split('. ', 1)[1])}</h3><span class="bo-rounds">06</span></header>
<div class="asked">{badge("pasted")} <span class="lbl">Asked</span> {inl(br['asked'])}</div>
<p class="bo-frame"><span class="lbl">Answer</span> {inl(br['answer'])}</p>
<ul class="bo-shows">{shows}</ul>
<p class="bo-editor"><span class="lbl">In the store</span> {inl(br['store'])}</p>
<p class="bo-links"><span class="lk-h">its question names</span> {" ".join(f'<button class="chip-go" data-go="bo-{L}">{L}</button>' for L in br['from'])} {links_line(br['id']).replace('<p class="bo-links">', '').replace('</p>', '')}</p>
<footer class="bo-f"><span class="kinds">{kinds}</span><span class="src" title="{esc(work_label(br['work']))}">{esc(work_short(br['work']))}</span>
<button class="btn bo-open" aria-expanded="false" data-work="{work_attr(br['work'])}">Read the working</button></footer>
<div class="bo-work" hidden></div></article>"""


def thread_card(t):
    lines = "".join(f"<li>{inl(x)}</li>" for x in t["lines"])
    kinds = " ".join(kind_tag(k) for k in t["kinds"])
    title = t["title"].md.lstrip("# ").split(". ", 1)[1]
    return f"""<article class="bo bo-alt" id="bo-{t['id']}">
<header class="bo-h"><span class="bo-letter">{t['id'][-1]}</span><h3 class="bo-title">{esc(title)}</h3><span class="bo-rounds">02b</span></header>
<ul class="bo-shows">{lines}</ul>
<footer class="bo-f"><span class="kinds">{kinds}</span><span class="src" title="{esc(work_label(t['work']))}">{esc(work_short(t['work']))}</span>
<button class="btn bo-open" aria-expanded="false" data-work="{work_attr(t['work'])}">Read the working</button></footer>
<div class="bo-work" hidden></div></article>"""


def building_svg():
    return """<svg viewBox="0 0 560 300" class="bld" role="img" aria-label="Round 4's building: floors on and under, rooms beside, a shaft through every floor, and renovation over time.">
<defs><marker id="bld-arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="currentColor"/></marker></defs>
<g class="bld-g" data-dir="beside"><rect x="20" y="20" width="380" height="64" rx="4" class="bld-room"/>
<text x="34" y="42" class="bld-t">rooms (records)</text><text x="34" y="64" class="bld-t3">editor ← vim-like ← my editor</text></g>
<g class="bld-g" data-dir="on"><rect x="20" y="84" width="380" height="36" class="bld-floor"/><text x="34" y="107" class="bld-t3">computation · record · run · budget</text>
<rect x="20" y="126" width="380" height="14" class="bld-waist"/><text x="210" y="137" text-anchor="middle" class="bld-t4">the act + the read: the waist (D)</text>
<rect x="20" y="140" width="380" height="36" class="bld-floor"/><text x="34" y="163" class="bld-t3">layers, standing · who · may</text>
<rect x="20" y="176" width="380" height="36" class="bld-floor"/><text x="34" y="199" class="bld-t3">live, facts · fact · replace · watch</text>
<rect x="20" y="212" width="380" height="36" class="bld-floor"/><text x="34" y="235" class="bld-t3">log · entry · order · replay</text>
<text x="210" y="268" text-anchor="middle" class="bld-t3">Rama underneath, as microcode · JVM · disks</text></g>
<g class="bld-g" data-dir="through"><rect x="410" y="20" width="44" height="228" rx="4" class="bld-shaft"/><line x1="432" y1="30" x2="432" y2="238" class="bld-cable"/>
<text x="464" y="120" class="bld-t">shaft</text><text x="464" y="138" class="bld-t3">knobs, who,</text><text x="464" y="154" class="bld-t3">when, why,</text><text x="464" y="170" class="bld-t3">authority</text></g>
<g class="bld-g" data-dir="time"><path d="M300 54 C350 54 380 120 380 196" class="bld-flow" marker-end="url(#bld-arr)"/><text x="210" y="292" text-anchor="middle" class="bld-t3">over time: rooms try things, floors absorb what worked</text></g>
</svg>"""


def breakouts_section():
    dirs = "".join(f'<button class="seg" data-dir="{d["id"]}" aria-pressed="false">{esc(d["name"])} <span class="seg-l">{" ".join(d["letters"])}</span></button>'
                   for d in C.DIRECTIONS)
    dir_text = "".join(f'<div class="dir-say" data-dir="{d["id"]}" hidden><p class="ex">{inl(d["lead"])} {inl(d["q"])} {src_label("04", "3")}</p></div>'
                       for d in C.DIRECTIONS)
    rail = "".join(f'<a href="#bo-{b["id"]}" class="rail-l" data-go="bo-{b["id"]}">{b["id"]}</a>' for b in C.BREAKOUTS)
    rail += "".join(f'<a href="#bo-{b["id"]}" class="rail-l rail-b" data-go="bo-{b["id"]}">{b["n"]}</a>' for b in C.BRIDGES)
    qs = "".join(f"<li>{inl(x)}</li>" for x in C.QUESTIONS_02B)
    body = f"""
<div class="dirs">
  <figure class="bld-fig">{building_svg()}<figcaption>The many as one building, as the round 4 branch draws it. {src_label("04", "3")} <span class="tag-alt">alt branch 04</span></figcaption></figure>
  <div class="dir-side">
    {guide("Round 4 placed the breakouts along four directions. Pick one to light up its breakouts below. K, L and M were opened in round 5; round 4 does not place them.")}
    <div class="dir-ctl" role="group" aria-label="Directions">{dirs}<button class="seg" data-dir="" aria-pressed="true">All</button></div>
    <div class="dir-text">{dir_text}</div>
    <p class="ex">{inl(C.DIRECTIONS_FRACTAL)} {src_label("04", "3")}</p>
    <p class="ex">{inl(C.DIRECTIONS_ALLONE)} {src_label("04", "3")}</p>
  </div>
</div>
<nav class="bo-rail" aria-label="Jump to a breakout">{rail}</nav>
<div class="bo-grid" id="bo-main">{"".join(bo_card(b) for b in C.BREAKOUTS)}</div>
<h3 class="sub-h" id="bridges">The three bridges of round 6</h3>
{guide("Round 6 answered three questions that Sid pasted in. The record shows the paste as session text, so each “Asked” line carries that badge.")}
<div class="bo-grid">{"".join(bridge_card(b) for b in C.BRIDGES)}</div>
<h3 class="sub-h">The 02b branch: three threads <span class="tag-alt">alt branch 02b</span></h3>
{guide("An alternate branch; the session that wrote MAP.md does not hold it. Where it splits off is shown under Rounds.")}
<div class="bo-grid">{"".join(thread_card(t) for t in C.THREADS_02B)}</div>
<div class="q02b"><p class="ex">{inl(C.QUESTIONS_02B_LEAD)}</p><ol class="ex-list">{qs}</ol><p>{src_label("02b", "Where the threads touch")}
<span class="guide-inline">These three are not in MAP.md’s list of open questions.</span></p></div>"""
    return wrap_section("breakouts", f"Breakouts · from {name_of('03')}, {name_of('05')}, {name_of('06')}, and the 02b and 04 branches",
                        "What did each breakout find?",
                        guide("Thirteen breakouts, each a way of looking at the same system. Each card shows the round’s own lines; the full working opens in place."),
                        body)


# --------------------------------------------------------------------------
# Play
# --------------------------------------------------------------------------

def q_line(x, show=None, src=None, sec=None):
    attr = f' data-show="{show}"' if show else ""
    return f'<p class="ex toy-q"{attr}>{inl(x)} {src_label(src or x.src, sec)}</p>'


def play_section():
    F, D, M, S, P = C.TOY_FINAL, C.TOY_DIFF, C.TOY_MERGE, C.TOY_SHARE, C.TOY_PACE
    pace_rows = "".join(
        f'<li class="pace-row"><span class="pace-t">{inl(a)}</span><span class="pace-b">{inl(b)}</span><span class="pace-s">{inl(c)}</span></li>'
        for a, b, c in (cells(r) for r in C.PACE_ROWS))
    toy_note = '<p class="toy-note">Toy values: the bindings and commands are illustrations. The lines quoted beside them are the rounds’ own.</p>'
    body = f"""
<article class="toy" id="toy-final">
  <header class="toy-h"><h3>Final or prev: the status line you did not touch</h3>{src_label("06", "3")}</header>
  <p class="ex">{inl(F["example"])}</p>
  <div class="toy-body">
    <div class="toy-ctl">
      <fieldset><legend>Your layer’s keymap</legend>
        <label><input type="radio" name="t1-grain" value="extend" checked> extends vim-like’s <code>prev.keymap // {{ C-s = save-all }}</code></label>
        <label><input type="radio" name="t1-grain" value="replace"> replaces the whole keymap</label></fieldset>
      <fieldset><legend>The status line reads the keymap through</legend>
        <label><input type="radio" name="t1-read" value="final" checked> your list of layers: <code>final</code></label>
        <label><input type="radio" name="t1-read" value="prev"> vim-like’s own layer: <code>prev</code></label></fieldset>
      <label class="check"><input type="checkbox" id="t1-cycle"> the keymap also reads the status line</label>
    </div>
    <figure class="toy-fig"><svg id="t1-svg" viewBox="0 0 480 290" role="img" aria-label="Two layers, your layer over vim-like, each with a keymap and a status line, and the path the status line reads through."></svg></figure>
    <div class="toy-out">
      <p class="out-h">Your editor’s keymap</p><table class="kt" id="t1-keys"></table>
      <p class="out-h">Status line hints</p><div class="statusline" id="t1-status"></div><p class="verdict" id="t1-verdict"></p>
    </div>
  </div>
  <div class="toy-quotes">
    {q_line(F["nix"])}{q_line(F["extend"], "extend")}{q_line(F["replace"], "replace")}{q_line(F["final"], "final")}
    {q_line(F["prev"], "prev")}{q_line(F["cycle"], "cycle")}{q_line(F["rule"])}{q_line(F["readpath"])}
  </div>
  {toy_note}
</article>

<article class="toy" id="toy-diff">
  <header class="toy-h"><h3>A diff is a span over what stayed the same</h3>{src_label("06", "2")}</header>
  <p class="ex">{inl(D["span"])}</p>
  <div class="toy-body toy-body-wide">
    <div class="toy-ctl">
      <fieldset><legend>The edit</legend>
        <label><input type="radio" name="t2-edit" value="value" checked> C-s now does save-all</label>
        <label><input type="radio" name="t2-edit" value="reorder"> move C-s to the top</label>
        <label><input type="radio" name="t2-edit" value="rename"> rename w to e</label>
        <label><input type="radio" name="t2-edit" value="both"> rename and move</label></fieldset>
      <fieldset><legend>How the differ finds the same part</legend>
        <label><input type="radio" name="t2-id" value="num" checked> each part has its own number</label>
        <label><input type="radio" name="t2-id" value="name"> by name</label>
        <label><input type="radio" name="t2-id" value="pos"> by position</label></fieldset>
    </div>
    <figure class="toy-fig"><svg id="t2-svg" viewBox="0 0 520 250" role="img" aria-label="The state before (I), the part kept in both (U), and the state after (J), with lines from U into each."></svg></figure>
    <div class="toy-out"><p class="out-h">What the diff reports</p><ul class="report" id="t2-report"></ul><p class="verdict" id="t2-verdict"></p></div>
  </div>
  <div class="toy-quotes">{q_line(D["u"])}{q_line(D["d3"])}{q_line(D["name"], "name", "01", "4")}{q_line(D["act"])}</div>
  {toy_note}
</article>

<article class="toy" id="toy-merge">
  <header class="toy-h"><h3>Combine two parents: keep everything, then choose</h3>{src_label("05", "J.7, J.8 and E")}</header>
  <p class="ex">{inl(M["pushout"])}</p>
  <div class="toy-body toy-body-wide">
    <div class="toy-ctl">
      <fieldset><legend>Fill the column a parent never had</legend>
        <label><input type="radio" name="t3-fill" value="sigma" checked> Σ: a fresh unknown for each binding</label>
        <label><input type="radio" name="t3-fill" value="pi"> Π: one shared default</label></fieldset>
      <fieldset><legend>Then choose among the C-s rows by</legend>
        <label><input type="radio" name="t3-pol" value="order" checked> order: the nearest parent wins</label>
        <label><input type="radio" name="t3-pol" value="error"> error: an ambiguity error when two parents answer</label>
        <label><input type="radio" name="t3-pol" value="record"> a recorded resolution</label></fieldset>
      <fieldset class="t3-sub" data-for="order"><legend>Builds-on order</legend>
        <label><input type="radio" name="t3-ord" value="vim" checked> vim-like first</label>
        <label><input type="radio" name="t3-ord" value="vsc"> vscode-like first</label></fieldset>
      <fieldset class="t3-sub" data-for="record" hidden><legend>Record a resolution</legend>
        <label><input type="radio" name="t3-rec" value="vim" checked> keep vim-like’s</label>
        <label><input type="radio" name="t3-rec" value="vsc"> keep vscode-like’s</label></fieldset>
    </div>
    <div class="toy-fig"><p class="out-h">P, the pushout: both parents’ rows, glued along what they share</p><table class="kt kt-wide" id="t3-p"></table>
    <p class="vocab"><code>{esc(norm(M["vocab"].md))}</code> {src_label("05", "J.7")}</p></div>
    <div class="toy-out"><p class="out-h">The policy on top</p><div id="t3-res" class="res"></div></div>
  </div>
  <div class="toy-quotes">{q_line(M["sigma"], "sigma")}{q_line(M["pi"], "pi")}{q_line(M["both"])}{q_line(M["order"], "order")}{q_line(M["self"], "error")}
  {q_line(M["git"], "record")}{q_line(M["knob"])}{q_line(M["data"])}{q_line(M["sep"])}</div>
  {toy_note}
</article>

<article class="toy" id="toy-share">
  <header class="toy-h"><h3>Share it or copy it: transclusion against a fork</h3>{src_label("06", "1")}</header>
  <p class="ex">{inl(S["palette"])} {src_label("05", "E")}</p>
  <div class="toy-body toy-body-wide">
    <div class="toy-ctl">
      <fieldset><legend>The command palette in my editor is</legend>
        <label><input type="radio" name="t4-mode" value="share" checked> shared: one state, the diagonal</label>
        <label><input type="radio" name="t4-mode" value="fork"> a fork: two states side by side</label></fieldset>
      <div class="btns">
        <button class="btn" data-t4="edit-me">Add a command from my editor</button>
        <button class="btn" data-t4="edit-vs">Add a command from vscode-like</button>
        <button class="btn" data-t4="hide">Hide “find” inside my editor</button>
        <button class="btn btn-ghost" data-t4="reset">Start again</button>
      </div>
      <fieldset class="t4-knob"><legend>An override inside one whole (open, a knob)</legend>
        <label><input type="radio" name="t4-ov" value="global" checked> merge the inputs: global</label>
        <label><input type="radio" name="t4-ov" value="place"> tag it by place: that whole only</label></fieldset>
    </div>
    <figure class="toy-fig"><svg id="t4-svg" viewBox="0 0 520 280" role="img" aria-label="Where the palette's state lives: one state shown in two wholes, or two states."></svg></figure>
    <div class="toy-out"><p class="out-h">What each whole shows</p><div id="t4-out" class="wholes"></div><p class="verdict" id="t4-verdict"></p></div>
  </div>
  <p class="toy-note">The fork drawn here is the Poly picture: two states side by side.</p>
  <div class="toy-quotes">{q_line(S["diag"])}{q_line(S["two"])}{q_line(S["where"])}{q_line(S["open"], "knob")}</div>
  {toy_note}
</article>

<article class="toy" id="toy-pace">
  <header class="toy-h"><h3>Pace is earned: dependents slow a layer down</h3>{src_label("05", "G")}</header>
  <p class="ex">{inl(P["brand"])} {src_label("03", "G–J")}</p>
  <div class="pace-wrap">
    <div class="pace-table"><p class="out-h">Brand’s layers and round 5’s proposal for Softland</p>
      <ol class="pace-rows">{pace_rows}</ol><p class="srcline">{src_label("05", "G, the table")}</p>
      <ul class="ex-list">{more_list([P["ed1"], P["ed2"], P["ed3"]])}</ul></div>
    <div class="pace-toy">
      <p class="out-h">The vim-like template and the layers built on it</p>
      <div class="btns"><button class="btn" data-t5="add">Add a layer that builds on it</button><button class="btn" data-t5="change">Change the template</button><button class="btn btn-ghost" data-t5="reset">Start again</button></div>
      <svg id="t5-svg" viewBox="0 0 480 250" role="img" aria-label="The template and the layers that build on it, each following its latest version or pinned to one."></svg>
      <p class="verdict" id="t5-verdict"></p>
      <p class="guide-inline">Select a dependent to switch it between following the latest version and pinning one.</p>
    </div>
  </div>
  <div class="toy-quotes">{q_line(P["earned"])}{q_line(P["edge"])}{q_line(P["absorb"])}{q_line(P["protect"])}{q_line(P["see"])}</div>
</article>"""
    return wrap_section("play", f"Play · from {name_of('06')}, {name_of('05')} and {name_of('01')}",
                        "How do the hardest ideas behave?",
                        guide("Five small machines, each built from one example in the rounds. Change a setting and watch what it does; the quoted lines beside it say what the round claims."),
                        body)


# --------------------------------------------------------------------------
# The map
# --------------------------------------------------------------------------

STATUS_LABEL = {"open": "open", "sharpened": "sharpened", "reframed": "reframed",
                "resolved": "resolved in principle", "reading": "a reading"}


def mx_dots(cols):
    return "".join(f'<i data-l="{c}"{" class=on" if c in cols else ""}></i>' for c in MX_COLS)


def map_section_html():
    head = "".join(f'<button class="mx-col" data-col="{c}" aria-pressed="false" title="Show only items citing {c}">{c}</button>' for c in MX_COLS)
    rows = []
    for a in AGREEMENTS:
        cols = AG_LETTERS[a["n"]].split()
        rows.append(f"""<div class="mx-row" data-kind="ag" data-cols="{' '.join(cols)}" id="ag-{a['n']}">
<span class="mx-id">A{a['n']}</span><button class="mx-text" aria-expanded="false"><span>{inl(a['title'])}</span></button>
<span class="mx-dots">{mx_dots(cols)}</span><span class="st st-signal">signal</span>
<div class="mx-more" hidden>{render_md(a['rest'])}</div></div>""")
    for t in TENSIONS:
        rows.append(f"""<div class="mx-row" data-kind="tn" data-cols="{' '.join(t['cols'])}" data-cat="{t['cat']}" id="tn-{t['n']}">
<span class="mx-id">T{t['n']}</span><button class="mx-text" aria-expanded="false"><span>{inl(t['text'])}</span></button>
<span class="mx-dots">{mx_dots(t['cols'])}</span><span class="st st-{t['cat']}">{esc(STATUS_LABEL[t['cat']])}</span>
<div class="mx-more" hidden><p><span class="lbl">From</span> {esc(t['from'])}</p><p><span class="lbl">Status</span> {inl(t['status'])}</p></div></div>""")
    cats = [("", "every status"), ("open", "open"), ("sharpened", "sharpened"), ("reframed", "reframed"),
            ("resolved", "resolved in principle"), ("reading", "a reading")]
    cat_btns = "".join(f'<button class="seg" data-cat="{k}" aria-pressed="{"true" if not k else "false"}">{esc(v)}</button>' for k, v in cats)
    lower = [
        ("q-open", "Questions put to Sid that are still open", render_md(map_section("### Questions put to Sid that are still open")), "MAP"),
        ("unopened", "Branches not opened", render_md(map_section("### Branches not opened")), "MAP"),
        ("time", "Time-sensitive", render_md(map_section("## Time-sensitive")), "MAP"),
        ("checked", "What was checked, and what was not", render_md(map_section("## What was checked, and what was not")), "MAP"),
        ("words", "Words", render_md(map_section("## Words")), "MAP"),
        ("whose", "Whose words are whose", render_md(map_section("## Whose words are whose")), "MAP"),
    ]
    lower_html = "".join(f'<article class="low" id="{i}"><h3 class="sub-h">{esc(t)}</h3><div class="md-block">{h}</div><p class="srcline">{src_label(s, t)}</p></article>'
                         for i, t, h, s in lower)
    q02b = "".join(f"<li>{inl(x)}</li>" for x in C.QUESTIONS_02B)
    body = f"""
<div class="mx-ctl">
  <div class="seg-row" role="group" aria-label="Show"><button class="seg" data-kind="" aria-pressed="true">agreements and tensions</button>
  <button class="seg" data-kind="ag" aria-pressed="false">agreements</button><button class="seg" data-kind="tn" aria-pressed="false">tensions</button></div>
  <div class="seg-row" role="group" aria-label="Tension status">{cat_btns}</div>
  <p class="guide-inline">Select a column letter to keep only the items that cite it. Select an item to read it in full.</p>
</div>
<div class="mx" id="mx" role="table" aria-label="Agreements and tensions against the breakouts that give them">
  <div class="mx-head" role="row"><span class="mx-id"></span><span class="mx-text-h">item</span><span class="mx-dots mx-cols">{head}</span><span class="st-h">status</span></div>
  {"".join(rows)}
</div>
<p class="srcline">{src_label("MAP", "The map")} · Columns: 02 is round 2, 06 is round 6. Agreements are “signals, not decisions” (MAP.md).</p>
<div class="lows">{lower_html}
<article class="low low-alt"><h3 class="sub-h">Also asked in the 02b branch <span class="tag-alt">alt branch 02b</span></h3>
<p class="ex">{inl(C.QUESTIONS_02B_LEAD)}</p><ol class="ex-list">{q02b}</ol><p class="srcline">{src_label("02b", "Where the threads touch")}</p>
<p class="guide-inline">Not in MAP.md’s list, because the MAP session does not hold the 02b branch.</p></article></div>"""
    return wrap_section("map", "Map · from MAP.md", "Where do the breakouts agree, and where do they pull apart?",
                        guide("Twelve agreements and sixteen tensions, against the breakouts each one comes from. Below them: what is still open to Sid, the branches nobody opened, and what was checked."),
                        body)


# --------------------------------------------------------------------------
# The other two "few" parts: round 7, and Sid's answer to it
# --------------------------------------------------------------------------

def open_in_place(label, work):
    """A button that opens round text in place (the Rounds mechanism: initRounds binds .rd-open)."""
    return (f'<div class="ex-open"><button class="btn rd-open" aria-expanded="false" data-label="{esc(label)}" data-work="{work}">{esc(label)}</button>'
            f'<div class="rd-work" hidden></div></div>')


def few2_section():
    R = C.R7
    step1 = f"""<article class="card">
<h3 class="sub-h">Where we started, seen from the many</h3>
{guide("Round 7 read page 2 phrase by phrase. Three of its eight readings:")}
{ex_line(R["in_atomic"], sec="Step 1")}
{ex_line(R["inside_do"], R["inside_do2"], sec="Step 1")}
{ex_line(R["in_user"], sec="Step 1")}
{open_in_place("All eight, word for word", work_attr([("07", "Page 2, phrase by phrase")]))}
</article>"""
    step2 = f"""<article class="card">
<h3 class="sub-h">Its narrowing: five floors</h3>
{ex_line(R["floors"], sec="Step 2")}
{ex_line(R["rows"], sec="Step 2")}
{ex_line(R["cols"], sec="Step 2")}
{guide("Its picture, and what it says the picture would correct in the many, open here. Both are that session’s reading.")}
{open_in_place("The picture", work_attr([("07", "The picture")]))}
{open_in_place("What it says the picture corrects in the many", work_attr([("07", "What the picture corrects in the many")]))}
{open_in_place("Where each breakout went, as it sorted them", work_attr([("07", "Where each breakout went")]))}
{guide('Round 7’s own list of what it would build next, and its questions, stay inside its full text, under Rounds. '
       '<button class="chip-go" data-go="rd-07">Round 7 in full</button>')}
</article>"""
    answer = f"""<div id="few2-answer">
<h3 class="sub-h">Sid’s answer</h3>
{quote(C.SID13["react"], "typed55", note=src_label("13") + ' <span class="q-why">‹55a15012›, 21 October 17:31 UTC, seven minutes after round 7’s reply («9df67007»).</span>')}
</div>"""
    after = f"""<h3 class="sub-h">What came after</h3>
{guide("Rounds 8 to 14 followed in the same session, between 21 and 23 October. They are not on disk. Round 13 is on this page, in The box; the others are not.")}
{quote(C.SID13["crux"], "typed07", note=src_label("13"))}"""
    body = f"""
{quote(C.GO_MESSAGE, "go", note=src_label("MAP", "Whose words are whose"))}
{step1}
{step2}
{answer}
{after}"""
    return wrap_section("few2", "Round 7 · 21 October, 17:24 UTC, a fresh session · written to 07-convergence.md on 23 October at Sid’s request",
                        "What did a fresh session make of the other two “few” parts?",
                        guide("Sid defined the two parts in his go message. A fresh session read MAP.md and the rounds and answered. This is that session’s attempt, not a result. Below: its two steps, then Sid’s answer seven minutes later."),
                        body)


# --------------------------------------------------------------------------
# Every round, word for word
# --------------------------------------------------------------------------

PASTED_RE = re.compile(r'<pasted_content id="([0-9a-f]+)">\n?(.*?)\n?</pasted_content id="\1">', re.S)


def para(text, kind, note=None):
    """One paragraph of a prompt, with the badge for whose words it is."""
    n = f'<p class="pp-note">{note}</p>' if note else ""
    return f'<div class="pp pp-{BADGE_CLS[kind]}">{badge(kind)}{n}<p class="pp-t">{esc(text.strip())}</p></div>'


def prompt_html(src):
    raw = prompt_text(src)
    prov = C.PROMPT_PROV[src]

    def frame(inner_html):
        return (f'<div class="pf"><p class="pf-tag">&lt;pasted_content id="{pid}"&gt;</p>{inner_html}'
                f'<p class="pf-tag">&lt;/pasted_content id="{pid}"&gt;</p></div>')

    m = PASTED_RE.search(raw)
    pid = m.group(1) if m else ""
    before = raw[:m.start()].strip() if m else raw.strip()
    after = raw[m.end():].strip() if m else ""
    inner = m.group(2).strip() if m else ""
    if src == "02":
        p1, p2 = [p for p in inner.split("\n\n") if p.strip()]
        body = frame(para(p1, "typed87", f"{inl(prov[0][1])} {src_label('MAP')}") +
                     para(p2, "noprov", "MAP.md and the log quote only the paragraph above."))
    elif src == "06":
        body = frame(para(inner, "pasted", f"{inl(prov[0][1])} {src_label('MAP')}")) + para(after, "typed9e", f"{inl(prov[1][1])} {src_label('MAP')}")
    elif m:
        kind, note = prov[0]
        tail = ("" if note is False else  # False: MAP.md has nothing to quote, so the badge stands alone
                f"{inl(note)} {src_label('MAP')}" if note is not None else "MAP.md does not say who wrote this prompt.")
        body = frame(para(inner, kind, tail))
    else:
        kind, note = prov[0]
        nt = (f"{inl(note)} {src_label(note.src)}" if note is not None else "MAP.md does not say who wrote this prompt.")
        body = para(before, kind, nt)
    if m and before:
        body = f'<p class="pp-t">{esc(before)}</p>' + body
    return body


BRANCH_NOTES = {
    "02b": "Branches off after round 2: its reply corrects “my last reply” on the depot, which round 2 made.",
    "04": "Branches off after round 3: its reply works from “the six open questions from the last map”, which is round 3’s map.",
}


def round_card(src, kind):
    title = CHUNKS[src][1]["title"]
    holds = ROUNDS_TABLE.get(src)  # MAP.md's one line on the round; none for a round it predates (07)
    holds_p = f'<p class="ex rd-holds">{inl(holds)} <span class="src">MAP.md · The rounds</span></p>' if holds else ""
    alt = f'<span class="tag-alt">alt branch {src}</span>' if kind == "alt" else ""
    branch = f'<p class="guide guide-flag">{BRANCH_NOTES[src]} This page read that from the text; MAP.md does not say where the branches split.</p>' if src in BRANCH_NOTES else ""
    return f"""<li class="rd rd-{kind}" id="rd-{src}">
<span class="rd-node" aria-hidden="true"></span>
<article class="rd-card">
<header class="rd-h"><h3>{esc(title)} {alt}</h3><span class="src rd-path">{esc(path_of(src))}</span></header>
{holds_p}{branch}
<p class="out-h">Prompt</p><div class="rd-prompt" data-collapsed="true">{prompt_html(src)}</div><button class="btn btn-small rd-more" aria-expanded="false">Show the whole prompt</button>
<button class="btn rd-open" aria-expanded="false" data-work="{' '.join(reply_ids(src))}">Open the reply, word for word</button>
<div class="rd-work" hidden></div>
</article></li>"""


def rounds_section():
    long_body = TEXT["LOG"][TEXT["LOG"].index("> I like some of the directions"):TEXT["LOG"].index("[after the breakouts had fanned out")]
    long_lines = [l[2:] if l.startswith("> ") else l[1:] if l.startswith(">") else l for l in long_body.strip().splitlines()]
    long_paras, cur = [], []
    for l in long_lines:
        if l.strip():
            cur.append(l)
        elif cur:
            long_paras.append(" ".join(cur))
            cur = []
    if cur:
        long_paras.append(" ".join(cur))
    lp = "".join(
        (f'<p class="pp-t pp-paste">{badge("pasted")} {esc(p)}</p>' if p.startswith("[paste") else f'<p class="pp-t">{esc(p)}</p>')
        for p in long_paras)
    interlude = f"""<li class="rd rd-sid" id="rd-long"><span class="rd-node" aria-hidden="true"></span>
<article class="rd-card rd-card-sid"><header class="rd-h"><h3>Sid’s long prompt, between rounds 2 and 3</h3><span class="src rd-path">vision/LOG.md</span></header>
<div class="pp pp-b-sidq">{badge("long")}<p class="pp-note">The record cannot tell whether he typed or pasted it. The bracketed line marks the one paste of the session’s own text. Round 3 answered a rewrite of it, the 07:26 session text he pasted.</p>
<div class="rd-prompt" data-collapsed="true">{lp}</div><button class="btn btn-small rd-more" aria-expanded="false">Show the whole prompt</button></div></article></li>"""
    nxt = f"""<li class="rd rd-next" id="rd-next"><span class="rd-node" aria-hidden="true"></span>
<article class="rd-card rd-card-next"><header class="rd-h"><h3>The handoff to a new session (21 October)</h3><span class="src rd-path">{esc(path_of("STARTER"))}</span></header>
{quote(C.TYPED_9E, "typed9e", note=src_label("06") + " " + src_label("LOG"))}
{quote(C.GO_MESSAGE, "go", note=src_label("MAP", "Whose words are whose"))}
{guide("STARTER.md holds the prompt for that session, made with the prompt-maker skill. It does not tell the new session how to converge.")}
</article></li>"""
    order = [("01", "main"), ("02", "main"), ("long", "sid"), ("02b", "alt"), ("03", "main"), ("04", "alt"), ("05", "main"), ("06", "main")]
    items = "".join(interlude if s == "long" else round_card(s, k) for s, k in order)
    new_session = round_card("07", "main")  # follows the card above, which hands off to it
    # After round 7: Sid's answer, round 13 and his lines around it (13-the-box.md).
    reply13 = " ".join(section_ids("13", REPLY_13))
    restate13 = " ".join(section_ids("13", RESTATE_13))
    note13 = ("The first paragraph of the line repeats a sentence of the reply he was answering («0e280767»); "
              "the record traces no paste, so it is marked as typed.")
    react = f"""<li class="rd rd-sid" id="rd-react"><span class="rd-node" aria-hidden="true"></span>
<article class="rd-card rd-card-sid"><header class="rd-h"><h3>Sid’s answer to round 7</h3><span class="src rd-path">{esc(name_of("13"))}</span></header>
{quote(C.SID13["react"], "typed55", note=src_label("13"))}
<p class="pp-note">Seven minutes after round 7’s reply was given in the conversation (21 October, 17:24 UTC; the record’s «9df67007»).</p>
</article></li>"""
    r13 = f"""<li class="rd rd-main" id="rd-13"><span class="rd-node" aria-hidden="true"></span>
<article class="rd-card"><header class="rd-h"><h3>Round 13: the box (21 October, 18:13 UTC)</h3><span class="src rd-path">{esc(path_of("13"))}</span></header>
<p class="out-h">Prompt</p>
{para(C.SID13["prompt1"].md, "typed9a", note13)}{para(C.SID13["prompt2"].md, "typed9a")}
<button class="btn rd-open" aria-expanded="false" data-work="{reply13}">Open the reply, word for word</button>
<div class="rd-work" hidden></div>
<p class="out-h">23 October</p>
{quote(C.SID13["names"], "typedc7", note=src_label("13"))}
{guide("Its reply to his line; the record holds no line of his on it.", "guide-inline")}
<button class="btn rd-open" aria-expanded="false" data-label="Open the session’s restatement, word for word" data-work="{restate13}">Open the session’s restatement, word for word</button>
<div class="rd-work" hidden></div>
</article></li>"""
    r814 = f"""<li class="rd rd-next" id="rd-8-14"><span class="rd-node" aria-hidden="true"></span>
<article class="rd-card rd-card-next"><header class="rd-h"><h3>Rounds 8 to 12 and 14</h3><span class="src rd-path">not on disk</span></header>
{guide("They happened in the same session after round 7, between 21 and 23 October. They are not on disk and not on this page.")}
{quote(C.SID13["crux"], "typed07", note=src_label("13"))}
</article></li>"""
    ask = f"""<li class="rd rd-sid" id="rd-ask"><span class="rd-node" aria-hidden="true"></span>
<article class="rd-card rd-card-sid"><header class="rd-h"><h3>This page (23 October)</h3><span class="src rd-path">{esc(name_of("13"))}</span></header>
{para(C.SID13["ask1"].md, "typedb3")}
<div class="pp pp-b-pasted"><p class="pp-t pp-paste">{badge("pasted")} [paste #1: round 7’s reply, as the record traces it («b4b73e72»)]</p></div>
{para(C.SID13["ask2"].md, "typedb3")}
</article></li>"""
    return wrap_section("rounds", "Rounds · every file in src/proposal/levels-2026-10-21/, and 13-the-box.md in its artifact-draft/",
                        "What did each round say, word for word?",
                        guide("The trail of the conversation. Alternate branches sit to the side. Each reply opens in full: rounds 1 to 6 as Sid extracted them, round 7 as its own session wrote it down at his request, round 13 and the session’s restatement as the record holds them."),
                        f'<ol class="trail">{items}{nxt}{new_session}{react}{r13}{r814}{ask}</ol>')


def footer_html():
    rows = "".join(f'<li><span class="src">{esc(path_of(s))}</span></li>' for s in ["MAP"] + ROUND_SRCS + ["13", "STARTER", "LOG"])
    return f"""<footer class="foot"><div class="foot-in">
<p class="foot-h">Sources</p><ul class="foot-list">{rows}<li><span class="src">vision/images/2026-10-21-notebook-p1.png</span>, <span class="src">-p2.png</span> (shown downscaled)</li></ul>
<p class="guide">The first page was built by a fresh session (Claude Opus 5.5) on 21 October from these files. This draft was redrawn from the box on 23 October by a session (Claude Fable 5.1) with a Sonnet worker for the hands. Every quoted line is checked against its file by
<span class="src">src/proposal/levels-2026-10-21/artifact-draft/build.py</span> before the page is written. Nothing on this page is decided.</p>
</div></footer>"""


# --------------------------------------------------------------------------
# Toy and figure data for the page's script
# --------------------------------------------------------------------------

def page_data():
    return {"hero": hero_data()}


def images():
    for p in (1, 2):
        src = REPO / "vision" / "images" / f"2026-10-21-notebook-p{p}.png"
        out = HERE / "img" / f"notebook-p{p}.webp"
        if not out.exists() or out.stat().st_mtime < src.stat().st_mtime:
            out.parent.mkdir(exist_ok=True)
            subprocess.run(["magick", str(src), "-resize", "1400x", "-strip", "-quality", "72", str(out)], check=True)


def main():
    bad = check(C.REGISTRY)
    if bad:
        for ex in bad:
            print(f"not in {ex.src}: {ex.md[:140]!r}", file=sys.stderr)
        sys.exit(f"build: {len(bad)} excerpt(s) not found in their files")
    main_html = "".join([masthead(), hub_section(), start_section(), box_section(), stack_section(), layer_section(),
                         breakouts_section(), play_section(), map_section_html(), few2_section(), rounds_section()])
    tpl = (HERE / "template.html").read_text(encoding="utf-8")
    page = (tpl.replace("<!--@MAIN@-->", main_html)
               .replace("<!--@FOOTER@-->", footer_html())
               .replace("<!--@TEMPLATES@-->", templates_html())
               .replace("/*@DATA@*/null", json.dumps(page_data(), ensure_ascii=False).replace("</", "<\\/")))
    if check(C.REGISTRY):  # excerpts registered while building the page
        sys.exit("build: an excerpt added during the build is not in its file")
    (HERE / "levels.html").write_text(page, encoding="utf-8")
    images()
    print(f"levels.html: {len(page.encode()):,} bytes, {len(C.REGISTRY)} excerpts checked")


if __name__ == "__main__":
    main()
