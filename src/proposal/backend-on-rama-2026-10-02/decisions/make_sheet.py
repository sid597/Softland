#!/usr/bin/env python3
"""Lay out one group's sheet of questions as A4 portrait pages, in the form that METHOD.md sets.

Run:  python3 make_sheet.py groups/<group>.txt <out-folder>

It reads the group's text file and writes <slug>.html and, through headless Chrome, <slug>.pdf into the
out folder, then <out-folder>/check-N.png for each page (pdftoppm -r 70). The slug comes from the sheet's
title. The out folder is the research depot (/mnt/data/projects/research/softland/...), never the repo.

The mini-format (METHOD.md, "The sheet files and the build"):
  === id | title     opens a question. The block whose id is "Sheet" is the first page: its title is the
                     sheet's title, and it holds the sheet's opening line and "Words on this sheet".
  ## heading         opens a part
  - text             a word (one line each); in the closing list, one listed item
  [ ] **A** — text   a choice, with a box to tick
  If A: text         the "if" of the choice above it (a line that starts "If <letter>:")
  Today: text        the question's "Today" line
  ``` ... ```        a thing shown as code
  =ANSWER text       the answer line: what to write, and room to write it
  **text**           bold; `text` code; {{text}} grey source text
Any other line is a paragraph.
"""
import html, io, os, re, subprocess, sys

CSS = """
@page { size: A4 portrait; margin: 12mm 15mm 13mm 15mm;
  @bottom-right { content: counter(page) " / " counter(pages); font: 7.5pt "Noto Sans", Arial, sans-serif; color: #555; }
  @bottom-left { content: "%(footer)s"; font: 7.5pt "Noto Sans", Arial, sans-serif; color: #555; } }
html, body { margin: 0; padding: 0; background: #fff; color: #111; }
body { font-family: "Noto Sans", "Open Sans", "Segoe UI", Arial, sans-serif; font-size: 9.2pt; line-height: 1.3; }
h1.sheet { font-size: 16pt; margin: 0 0 1.5mm 0; }
.lead { font-size: 8.8pt; color: #333; margin: 0 0 2.5mm 0; border-bottom: 1.2pt solid #111; padding-bottom: 2mm; }
section.front h2 { font-size: 10.5pt; margin: 3mm 0 1.2mm 0; }
section.front ul.words li { margin-bottom: .7mm; }
section.q { break-inside: avoid; page-break-inside: avoid; border-top: 1pt solid #111; padding-top: 1.6mm; margin: 0 0 5mm 0; }
section.q h1 { font-size: 11pt; margin: 0 0 1.4mm 0; }
section.q h1 .n { display: inline-block; background: #111; color: #fff; padding: 0 1.8mm; margin-right: 2mm; font-size: 10pt; }
section.q h2 { font-size: 9.2pt; margin: 2mm 0 .8mm 0; }
p { margin: 0 0 1.2mm 0; }
ul.words { margin: 0 0 1.4mm 0; padding-left: 4.4mm; font-size: 8.8pt; }
ul.words li { margin: 0 0 .3mm 0; }
section.q ul.words { color: #222; }
pre { font-family: "DejaVu Sans Mono", "Noto Sans Mono", monospace; font-size: 7.5pt; line-height: 1.28; border: .6pt solid #111;
      padding: 1.3mm 2mm; margin: .8mm 0 1.6mm 0; white-space: pre; overflow: hidden; }
code { font-family: "DejaVu Sans Mono", "Noto Sans Mono", monospace; font-size: 8.4pt; }
.src { color: #666; font-size: 7.8pt; }
.ask { margin: 1.6mm 0 1.2mm 0; }
.tick { margin: 1mm 0 .2mm 0; padding-left: 6.4mm; text-indent: -6.4mm; }
.box { display: inline-block; width: 3.3mm; height: 3.3mm; border: .9pt solid #111; margin-right: 2.2mm; vertical-align: -0.6mm; text-indent: 0; }
.if { margin: 0 0 .6mm 6.4mm; padding-left: 2.2mm; border-left: .6pt solid #999; font-size: 8.6pt; color: #222; }
.today { margin: 1.8mm 0 1mm 0; font-size: 8.6pt; }
.today b.l { text-transform: uppercase; letter-spacing: .05em; font-size: 7.8pt; margin-right: 1.4mm; }
.ans { border: 1pt solid #111; padding: 1.2mm 2.4mm 0 2.4mm; margin-top: 1.6mm; break-inside: avoid; }
.ans b.l { text-transform: uppercase; letter-spacing: .05em; font-size: 7.8pt; margin-right: 1.6mm; }
.ans .hint { font-size: 8.4pt; }
.ans .line { height: 7.5mm; border-bottom: .5pt dotted #777; margin: 0 0 1.2mm 0; }
table.m { border-collapse: collapse; width: 100%%; margin: 1mm 0 2mm 0; font-size: 8.6pt; }
table.m td, table.m th { border: .5pt solid #777; padding: .8mm 1.6mm; vertical-align: top; text-align: left; }
table.m th { background: #eee; font-size: 8pt; }
"""

IF_LINE = re.compile(r"^If [A-Za-z0-9]{1,3}:")


def inline(s):
    s = html.escape(s, quote=False)
    s = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", s)
    s = re.sub(r"`(.+?)`", r"<code>\1</code>", s)
    s = re.sub(r"\{\{(.+?)\}\}", r'<span class="src">\1</span>', s)
    return s


def render(text):
    out, lines, i = [], text.split("\n"), 0
    while i < len(lines):
        ln = lines[i]
        if not ln.strip():
            i += 1; continue
        if ln.startswith("```"):
            j, buf = i + 1, []
            while j < len(lines) and not lines[j].startswith("```"):
                buf.append(lines[j]); j += 1
            out.append("<pre>%s</pre>" % html.escape("\n".join(buf), quote=False)); i = j + 1; continue
        if ln.startswith("## "):
            out.append("<h2>%s</h2>" % inline(ln[3:])); i += 1; continue
        if ln.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")]); i += 1
            t = ['<table class="m">']
            for k, r in enumerate(rows):
                tag = "th" if k == 0 else "td"
                t.append("<tr>" + "".join("<%s>%s</%s>" % (tag, inline(c), tag) for c in r) + "</tr>")
            t.append("</table>"); out.append("".join(t)); continue
        if ln.startswith("[ ] "):
            out.append('<div class="tick"><span class="box"></span>%s</div>' % inline(ln[4:])); i += 1; continue
        if IF_LINE.match(ln):
            out.append('<div class="if">%s</div>' % inline(ln)); i += 1; continue
        if ln.startswith("Today:"):
            out.append('<div class="today"><b class="l">Today</b>%s</div>' % inline(ln[6:].strip())); i += 1; continue
        if ln.startswith("- "):
            out.append('<ul class="words">')
            while i < len(lines) and lines[i].startswith("- "):
                out.append("<li>%s</li>" % inline(lines[i][2:].strip())); i += 1
            out.append("</ul>"); continue
        if ln.strip() == "=BREAK":
            out.append('<div style="break-before: page"></div>'); i += 1; continue
        if ln.startswith("=ANSWER"):
            out.append('<div class="ans"><b class="l">Your choice</b><span class="hint">%s</span><div class="line"></div></div>'
                       % inline(ln[7:].strip())); i += 1; continue
        cls = ' class="ask"' if ln.startswith("**") else ""
        out.append("<p%s>%s</p>" % (cls, inline(ln))); i += 1
    return "\n".join(out)


def slug(title):
    head = title.split("—")[0].strip().lower()
    return re.sub(r"[^a-z0-9]+", "-", head).strip("-") or "sheet"


def build(src, outdir):
    raw = io.open(src, encoding="utf-8").read()
    title, secs = "Questions", []
    for chunk in re.split(r"(?m)^=== ", raw):
        if not chunk.strip():
            continue
        head, _, body = chunk.partition("\n")
        qid, _, qtitle = head.partition(" | ")
        qid, qtitle = qid.strip(), qtitle.strip()
        if qid == "Sheet":
            title = qtitle
            lines = body.strip("\n").split("\n")
            lead = []
            while lines and not lines[0].startswith("## "):
                if lines[0].strip():
                    lead.append(lines[0].strip())
                lines.pop(0)
            secs.append('<section class="front"><h1 class="sheet">%s</h1><div class="lead">%s</div>\n%s</section>'
                        % (html.escape(title), inline(" ".join(lead)), render("\n".join(lines))))
        else:
            secs.append('<section class="q"><h1><span class="n">%s</span>%s</h1>\n%s</section>'
                        % (html.escape(qid), html.escape(qtitle), render(body)))
    name = slug(title)
    css = CSS % {"footer": title.replace('"', "'") + " · a sheet to answer on"}
    doc = ('<!doctype html>\n<html lang="en"><head><meta charset="utf-8"><title>%s</title><style>%s</style></head><body>\n%s\n</body></html>\n'
           % (html.escape(title), css, "\n".join(secs)))
    os.makedirs(outdir, exist_ok=True)
    out_html = os.path.join(outdir, name + ".html")
    out_pdf = os.path.join(outdir, name + ".pdf")
    io.open(out_html, "w", encoding="utf-8").write(doc)
    subprocess.run(["google-chrome", "--headless=new", "--no-pdf-header-footer", "--print-to-pdf=" + out_pdf,
                    "file://" + os.path.abspath(out_html)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    pages = int(re.search(r"Pages:\s+(\d+)", subprocess.run(["pdfinfo", out_pdf], check=True, capture_output=True,
                                                            text=True).stdout).group(1))
    subprocess.run(["pdftoppm", "-r", "70", "-png", out_pdf, os.path.join(outdir, "check")], check=True)
    # pdftoppm pads the page number when there are ten pages or more (check-01.png); name them check-N.png
    for f in os.listdir(outdir):
        m = re.fullmatch(r"check-0+(\d+)\.png", f)
        if m:
            os.replace(os.path.join(outdir, f), os.path.join(outdir, "check-%d.png" % int(m.group(1))))
    stale = sorted(f for f in os.listdir(outdir)
                   if re.fullmatch(r"check-\d+\.png", f) and int(re.search(r"\d+", f).group()) > pages)
    print("wrote %s and %s: %d blocks after the first page, %d pages" % (out_html, out_pdf, len(secs) - 1, pages))
    if stale:
        print("note: older page images beyond page %d are still in the folder: %s" % (pages, ", ".join(stale)))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("usage: python3 make_sheet.py groups/<group>.txt <out-folder>")
    build(sys.argv[1], sys.argv[2])
