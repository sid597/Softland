#!/usr/bin/env python3
"""Build reading bundles per ledger corner from Codex's tagged line ranges.

Codex reads one research file against the ledger in yardstick.prompt.md and returns rows of
(start, end, tag, ledger, note). This script copies the tagged ranges verbatim, by line number,
into bundles per corner with every voice side by side, so no model retypes anything.

Each corner gets two files. `<corner>.md` holds the ranges from the pressed zones of the research
files: round three, round two, the short version, above the table, the file's own list. The pilot
found the value there. `<corner>.body.md` holds the ranges from the team-by-team body. The main
session reads the first whole and picks from the second by INDEX.md.

SAME and SKIP ranges are counted, never copied. A range that bears on two corners is copied once,
under the first; its label names every corner it bears on. Inside a bundle each voice reads in its
own line order. Codex's notes live in INDEX.md only, because the reader has the text itself.
Ranges the main session read whole itself are listed in INDEX.md, not copied. Coverage problems (gaps, overlaps, bad rows) are reported in REPORT.md, not fatal:
text is copied by line number, so a gap hides nothing as long as it is listed.

usage: bundle.py RESEARCH_DIR PLANS_DIR OUT_DIR [--read NAME:LO-HI ...]
"""
import sys
from collections import defaultdict
from pathlib import Path

TAGS = ["OWN-LIST", "DISAGREES", "ABOVE", "NEW-CORNER", "CARRIED", "NEW-CASE", "NEW-REASON",
        "SAME", "SKIP"]
COPIED = ["DISAGREES", "ABOVE", "NEW-CORNER", "NEW-CASE", "NEW-REASON", "CARRIED", "OWN-LIST"]
TITLES = {
    "P0": "P0 premise (0)", "C1": "C1 name (2)(1)", "C2": "C2 as-of (10)(5)",
    "C3": "C3 order (10)", "C4": "C4 key (3)(17)", "C5": "C5 erasure (9)(1)",
    "C6": "C6 replaces (6)", "C7": "C7 envelope (13)(11)", "C8": "C8 by-whom (8)",
    "E": "E early losses", "A": "A above the table", "X": "X also held, and can wait",
    "N": "N new corners", "O": "O the files' own lists",
}
ORDER = ["P0", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "E", "A", "X", "N", "O"]
ALIAS = {
    "defaults-skeptics-bigtech": "skeptics", "facts-datalog": "datalog",
    "frontiers-views": "frontiers", "log-as-truth": "log",
    "meaning-objects-substrates": "meaning", "rama-marz": "rama",
    "sync-versioning-defaults": "sync",
}
ZONES = ["R3", "R2", "SHORT", "ABOVE", "OWN", "BODY"]


def bundle_of(code):
    if code in TITLES:
        return code
    if code and code[0] in "EAXW" and code[1:].isdigit():
        return "X" if code[0] == "W" else code[0]
    return None


def home(tag, codes):
    """The bundle a copied range is printed in."""
    if tag == "OWN-LIST":
        return "O"
    if tag == "NEW-CORNER":
        return "N"
    first = next((bundle_of(c) for c in codes if bundle_of(c)), None)
    if first:
        return first
    return "A" if tag == "ABOVE" else "N"


def read_rows(path, total):
    rows, problems = [], []
    for n, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip() or raw.startswith("```"):
            continue
        cols = [c.strip() for c in raw.split("\t")]
        if len(cols) < 5:
            problems.append(f"row {n}: {len(cols)} columns, want 5: {raw[:80]!r}")
            continue
        try:
            lo, hi = int(cols[0]), int(cols[1])
        except ValueError:
            problems.append(f"row {n}: start/end are not numbers: {raw[:80]!r}")
            continue
        tag, ledger, note = cols[2], cols[3], " ".join(cols[4:])
        if tag not in TAGS:
            problems.append(f"row {n}: unknown tag {tag!r} at L{lo}-{hi}; treated as NEW-REASON")
            tag = "NEW-REASON"
        if lo > hi or lo < 1 or hi > total:
            problems.append(f"row {n}: bad range L{lo}-{hi} (file has {total} lines)")
            continue
        codes = [c for c in ledger.replace(" ", "").split(",") if c and c != "-"]
        for c in codes:
            if not bundle_of(c):
                problems.append(f"row {n}: unknown ledger code {c!r} at L{lo}-{hi}")
        rows.append((lo, hi, tag, codes, note))
    rows.sort()
    want = 1
    for lo, hi, *_ in rows:
        if lo > want:
            problems.append(f"gap: L{want}-{lo - 1} is in no row")
        elif lo < want:
            problems.append(f"overlap: L{lo}-{min(hi, want - 1)} is in two rows")
        want = max(want, hi + 1)
    if want <= total:
        problems.append(f"gap: L{want}-{total} is in no row")
    return rows, problems


def heading_path(lines, lo):
    """Headings above a range, nearest first kept per level; the file's own title is left out."""
    path = {}
    for n in range(lo, 0, -1):  # from the range's own first line, which may be a heading
        line = lines[n - 1]
        if line.startswith("#"):
            level = len(line) - len(line.lstrip("#"))
            if level not in path and all(level < k for k in path):
                path[level] = line.lstrip("#").strip()
            if level == 1:
                break
    if n == 1 and 1 in path and len(path) > 1:
        del path[1]
    return " › ".join(path[k][:60] for k in sorted(path))


def zone_of(path):
    """Where in a file a range sits. Rounds two and three press Sid's leans, so they read first."""
    p = path.lower()
    if "round three" in p:
        return "R3"
    if "round two" in p or "r2." in p:
        return "R2"
    if "short version" in p or "in five lines" in p or "in one page" in p or "one picture" in p:
        return "SHORT"
    if "above the table" in p or "wrong question" in p or "challenge" in p:
        return "ABOVE"
    if "before the first record" in p or "gone for ever" in p or "cannot" in p:
        return "OWN"
    return "BODY"


def table_header(lines, lo):
    """If a range starts inside a table, give the table's header line."""
    if not lines[lo - 1].lstrip().startswith("|"):
        return []
    top = lo
    while top > 1 and lines[top - 2].lstrip().startswith("|"):
        top -= 1
    if top >= lo - 1 or len(lines[top - 1]) > 400:
        return []
    return [lines[top - 1]]


def main():
    research, plans, out = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
    read_whole = defaultdict(list)
    args = sys.argv[4:]
    for i, a in enumerate(args):
        if a == "--read":
            name, span = args[i + 1].split(":")
            lo, hi = span.split("-")
            read_whole[name].append((int(lo), int(hi)))

    printed = defaultdict(list)   # (bundle, part) -> [dict]
    pointers = defaultdict(list)  # (bundle, part) -> [str]
    same = defaultdict(lambda: defaultdict(list))   # code -> alias -> [(lo, hi)]
    carried = []
    read_rows_index = []
    matrix = defaultdict(int)     # (bundle, part, tag) -> bytes
    report = ["# Bundles: what Codex marked, per file", ""]

    for plan in sorted(plans.glob("*.rows.tsv")):
        name = plan.name[:-len(".rows.tsv")]
        alias = ALIAS.get(name, name)
        lines = (research / f"{name}.md").read_text(encoding="utf-8").split("\n")
        if lines and lines[-1] == "":
            lines.pop()
        total = len(lines)
        rows, problems = read_rows(plan, total)

        def size(lo, hi):
            return sum(len(l.encode("utf-8")) + 1 for l in lines[lo - 1:hi])

        whole = size(1, total)
        tally = defaultdict(lambda: [0, 0, 0])
        zone_bytes = defaultdict(int)
        for lo, hi, tag, codes, note in rows:
            t = tally[tag]
            t[0] += 1
            t[1] += hi - lo + 1
            t[2] += size(lo, hi)
            if tag == "SAME":
                for c in codes or ["-"]:
                    same[c][alias].append((lo, hi))
                continue
            if tag == "SKIP":
                continue
            path = heading_path(lines, lo)
            zone = zone_of(path)
            part = "body" if zone == "BODY" else "main"
            zone_bytes[zone] += size(lo, hi)
            matrix[(home(tag, codes), part, tag)] += size(lo, hi)
            if tag == "CARRIED":
                carried.append(f"- {alias} L{lo}-{hi} · {zone} · {','.join(codes) or '-'} · {note}")
            where = home(tag, codes)
            label = f"{alias} L{lo}-{hi} · {zone} · {tag} {','.join(codes) or '-'}"
            if any(r_lo <= lo and hi <= r_hi for r_lo, r_hi in read_whole[name]):
                read_rows_index.append(f"| {where} | read-whole | {tag} | {zone} | {alias} | L{lo}-{hi} | "
                                       f"{size(lo, hi)} | {','.join(codes) or '-'} | {note} |")
            else:
                head = table_header(lines, lo)
                printed[(where, part)].append({
                    "key": (alias, lo), "label": label, "path": path,
                    "body": head + lines[lo - 1:hi], "bytes": size(lo, hi),
                    "index": f"| {where} | {part} | {tag} | {zone} | {alias} | L{lo}-{hi} | {size(lo, hi)} | "
                             f"{','.join(codes) or '-'} | {note} |"})

        copied = sum(tally[t][2] for t in COPIED)
        report += [f"## {name}.md = {alias} ({total} lines, {whole} B)", "",
                   f"Marked for copying: {copied} B ({100 * copied / whole:.1f}%). Rows: {len(rows)}.",
                   "By zone: " + ", ".join(f"{z} {zone_bytes[z]} B" for z in ZONES if zone_bytes[z]), "",
                   "| tag | ranges | lines | bytes | share |", "|---|---|---|---|---|"]
        report += [f"| {t} | {tally[t][0]} | {tally[t][1]} | {tally[t][2]} | {100 * tally[t][2] / whole:.1f}% |"
                   for t in TAGS if tally[t][0]]
        report += ["", f"Problems: {len(problems)}"] + [f"- {p}" for p in problems[:30]] + [""]

    out.mkdir(parents=True, exist_ok=True)
    sizes = []
    index = ["# Index of copied ranges, in reading order", "",
             "| bundle | part | tag | zone | file | lines | bytes | codes | note |",
             "|---|---|---|---|---|---|---|---|---|"]
    for b in ORDER:
        slug = TITLES[b].split(" (")[0].replace(",", "").replace("'", "").replace(" ", "-")
        for part in ("main", "body"):
            zone_note = ("Zones: round three, round two, short version, above the table, own list."
                         if part == "main" else "Zone: the team-by-team body of the research files.")
            text = [f"# {TITLES[b]}: {part}", "",
                    "Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under",
                    f"yardstick.prompt.md. L-numbers are lines in the named research file. {zone_note}", ""]
            items = sorted(printed[(b, part)], key=lambda d: d["key"])
            last_path = None
            for d in items:
                text += ["---", f"**{d['label']}**"]
                if d["path"] != last_path:
                    text.append(f"*{d['path']}*")
                    last_path = d["path"]
                text += [""] + d["body"] + [""]
                index.append(d["index"])
            if pointers[(b, part)]:
                text += ["---", "## Bears on this corner, copied elsewhere", ""] + sorted(pointers[(b, part)]) + [""]
            if part == "main":
                codes_here = sorted(c for c in same if bundle_of(c) == b)
                if codes_here:
                    text += ["---", "## Says the same as the ledger (counted, not copied)", ""]
                    for c in codes_here:
                        for alias in sorted(same[c]):
                            spans = ", ".join(f"L{lo}-{hi}" for lo, hi in same[c][alias])
                            text.append(f"- {c} · {alias} · {len(same[c][alias])}: {spans}")
                    text.append("")
            if not items and not pointers[(b, part)] and part == "body":
                continue
            fname = f"{slug}.md" if part == "main" else f"{slug}.body.md"
            body_text = "\n".join(text) + "\n"
            (out / fname).write_text(body_text, encoding="utf-8")
            sizes.append((fname, len(body_text.encode("utf-8")), len(items), len(pointers[(b, part)])))

    aliases = sorted({a for c in same for a in same[c]})
    report += ["## Agreement with the ledger: SAME passages per line per file", "",
               "| code | " + " | ".join(aliases) + " |", "|---|" + "---|" * len(aliases)]
    for c in sorted(same):
        report.append(f"| {c} | " + " | ".join(str(len(same[c].get(a, []))) for a in aliases) + " |")
    report += ["", "## Bytes marked, by bundle and tag (main / body)", "",
               "| bundle | " + " | ".join(COPIED) + " |", "|---|" + "---|" * len(COPIED)]
    for b in ORDER:
        report.append(f"| {b} | " + " | ".join(
            f"{matrix[(b, 'main', t)]} / {matrix[(b, 'body', t)]}" for t in COPIED) + " |")
    report += ["", "## Carried questions answered", ""] + carried
    report += ["", "## Bundle sizes", "", "| bundle | bytes | copied ranges | pointers |", "|---|---|---|---|"]
    report += [f"| {f} | {b} | {n} | {p} |" for f, b, n, p in sizes]
    (out / "REPORT.md").write_text("\n".join(report) + "\n", encoding="utf-8")
    (out / "INDEX.md").write_text("\n".join(index + read_rows_index) + "\n", encoding="utf-8")
    main_total = sum(s[1] for s in sizes if not s[0].endswith(".body.md"))
    body_total = sum(s[1] for s in sizes if s[0].endswith(".body.md"))
    for f, b, n, p in sizes:
        print(f"{f}: {b} B, {n} ranges, {p} pointers")
    print(f"main {main_total} B, body {body_total} B")


if __name__ == "__main__":
    main()
