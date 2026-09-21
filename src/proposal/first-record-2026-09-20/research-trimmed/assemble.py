#!/usr/bin/env python3
"""Build a smaller reading copy of a research file with no model retyping any text.

The main session chooses, from the headings alone, which line ranges are read whole
(KEPT: every line outside the candidate ranges). Codex reads the whole file and marks
each block inside the candidate ranges PROMOTE or DEFER (the plan). It can add to what
is read, never take away from it.

This script refuses a plan that skips or double-counts a candidate line, leaves its
range, or defers a block by pointing at text that is not kept. It then copies by line
number: kept and promoted text verbatim, one marker line where a block was deferred,
and a table of everything deferred so any of it can be opened in the original.

usage: assemble.py ORIGINAL PLAN_TSV OUT_DIR NAME CAND_RANGES [MARKER]
       CAND_RANGES like 136-173,234-266   MARKER names the model that wrote the plan
"""
import sys
from pathlib import Path

P_TAGS = {"P-claim", "P-reason", "P-condition", "P-number", "P-mechanism",
          "P-quote", "P-regret", "P-disagreement", "P-unsure"}
D_TAGS = {"D1", "D2"}


def parse_ranges(text):
    ranges = []
    for part in text.split(","):
        lo, hi = part.strip().split("-")
        ranges.append((int(lo), int(hi)))
    return ranges


def inside(lo, hi, ranges):
    return any(r_lo <= lo and hi <= r_hi for r_lo, r_hi in ranges)


def complement(cands, total):
    kept, cursor = [], 1
    for lo, hi in cands:
        if lo > cursor:
            kept.append((cursor, lo - 1))
        cursor = hi + 1
    if cursor <= total:
        kept.append((cursor, total))
    return kept


def read_plan(plan_path, cands, kept):
    rows, errors = [], []
    for n, raw in enumerate(Path(plan_path).read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip() or raw.startswith("```"):
            continue
        cols = [c.strip() for c in raw.split("\t")]
        if len(cols) != 6:
            errors.append(f"row {n}: {len(cols)} columns, want 6")
            continue
        try:
            lo, hi = int(cols[0]), int(cols[1])
        except ValueError:
            errors.append(f"row {n}: start/end are not numbers")
            continue
        action, tag, ref, note = cols[2:]
        if lo > hi:
            errors.append(f"row {n}: start {lo} is after end {hi}")
        if not inside(lo, hi, cands):
            errors.append(f"row {n}: L{lo}-{hi} is not inside one candidate range")
        if action == "PROMOTE":
            if tag not in P_TAGS:
                errors.append(f"row {n}: PROMOTE with unknown tag {tag!r}")
        elif action == "DEFER":
            if tag not in D_TAGS:
                errors.append(f"row {n}: DEFER with unknown tag {tag!r}")
        else:
            errors.append(f"row {n}: unknown action {action!r}")
        if action == "DEFER" and tag == "D1":
            try:
                refs = parse_ranges(ref)
            except ValueError:
                refs = []
                errors.append(f"row {n}: D1 must name kept ranges, got {ref!r}")
            for r_lo, r_hi in refs:
                if not inside(r_lo, r_hi, kept):
                    errors.append(f"row {n}: D1 points at L{r_lo}-{r_hi}, which is not kept text")
        rows.append((lo, hi, action, tag, ref, note))
    rows.sort()
    for c_lo, c_hi in cands:
        want = c_lo
        for lo, hi, *_ in (r for r in rows if c_lo <= r[0] <= c_hi):
            if lo != want:
                errors.append(f"candidate L{c_lo}-{c_hi}: expected a block at {want}, got {lo}")
            want = hi + 1
        if want != c_hi + 1:
            errors.append(f"candidate L{c_lo}-{c_hi}: blocks stop at {want - 1}")
    return rows, errors


def main():
    original, plan_path, out_dir, name, cand_text = sys.argv[1:6]
    marker = sys.argv[6] if len(sys.argv) > 6 else "Codex"
    lines = Path(original).read_text(encoding="utf-8").split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    total = len(lines)
    cands = sorted(parse_ranges(cand_text))
    kept = complement(cands, total)
    rows, errors = read_plan(plan_path, cands, kept)
    if errors:
        print(f"PLAN REFUSED: {len(errors)} problem(s)")
        for e in errors[:40]:
            print("  " + e)
        sys.exit(1)

    def size(lo, hi):
        return sum(len(l.encode("utf-8")) + 1 for l in lines[lo - 1:hi])

    segments = [(lo, hi, "kept", "", "", "") for lo, hi in kept]
    segments += [(lo, hi, "promoted" if action == "PROMOTE" else "deferred", tag, ref, note)
                 for lo, hi, action, tag, ref, note in rows]
    segments.sort()

    trim = [f"<!-- Reading copy of {name}.md ({total} lines). Text is copied by line number by",
            "     assemble.py, never retyped. L-numbers are line numbers in the original.",
            "     kept = chosen from the headings by the main session (Claude Fable 5.1, max).",
            f"     promoted / DEFERRED = marked by {marker} under {name}.prompt.md.",
            f"     Everything deferred is listed in {name}.deferred.md. -->", ""]
    table = []
    tally = {"kept": 0, "promoted": 0, "deferred": 0}
    runs = []  # neighbouring blocks of one status share a marker; per-block detail lives in the table
    for lo, hi, status, tag, ref, note in segments:
        tally[status] += size(lo, hi)
        if status == "deferred":
            first = next((l for l in lines[lo - 1:hi] if l.strip()), "")
            table.append((size(lo, hi), lo, hi, tag, ref, note, first[:100].replace("|", "/")))
        if runs and runs[-1][2] == status and runs[-1][1] + 1 == lo:
            runs[-1][1] = hi
            runs[-1][3] += 1
        else:
            runs.append([lo, hi, status, 1])
    for lo, hi, status, count in runs:
        if status == "deferred":
            trim += [f"<!-- DEFERRED L{lo}-{hi} ({count} block{'s' if count > 1 else ''}, "
                     f"listed in {name}.deferred.md) -->", ""]
            continue
        trim.append(f"<!-- L{lo}-{hi} {status} -->")
        for n in range(lo, hi + 1):
            if lines[n - 1].startswith("#") and n != lo:
                trim.append(f"<!-- L{n} -->")
            trim.append(lines[n - 1])

    whole = sum(tally.values())
    share = {k: f"{v} B ({100 * v / whole:.1f}%)" for k, v in tally.items()}
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    (out / f"{name}.trim.md").write_text("\n".join(trim) + "\n", encoding="utf-8")
    report = [f"# Deferred from {name}.md", "",
              f"Kept by the main session {share['kept']}, promoted by {marker} {share['promoted']}, "
              f"deferred {share['deferred']}.",
              "D1: everything in the block also appears in the kept range named. "
              "D2: bibliographic entry or heading only.", "",
              "| lines | bytes | tag | see | subject | first words |", "|---|---|---|---|---|---|"]
    report += [f"| L{lo}-{hi} | {b} | {tag} | {ref} | {note} | {first} |"
               for b, lo, hi, tag, ref, note, first in table]
    (out / f"{name}.deferred.md").write_text("\n".join(report) + "\n", encoding="utf-8")

    print(f"{name}: kept {share['kept']}, promoted {share['promoted']}, deferred {share['deferred']}")
    print("largest D1 deferrals to check against the text they point at:")
    for b, lo, hi, tag, ref, note, _ in sorted(t for t in table if t[3] == "D1")[::-1][:6]:
        print(f"  L{lo}-{hi} ({b} B) -> {ref} | {note}")


if __name__ == "__main__":
    main()
