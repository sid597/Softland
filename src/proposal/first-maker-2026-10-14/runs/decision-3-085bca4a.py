# A throwaway run of the decision-3 agent, 15 October 2026, HEAD c90c7bcc.
# For each file that 085bca4a moved (git's rename detection), compare the file
# before and after: the ns form, and each top-level definition by its bare name.
# Approximate: a top-level form begins at a line that starts with "(", and runs
# to the line before the next such line, with trailing blank and ";" lines cut.
# Also counts facts under the walk's cut A (2 file facts + 2 ns facts + 4 per
# definition, runs/walk-parse.clj clj-facts) and names defined twice.
# Run from the repository root: python3 -I <this file>
import subprocess, re, collections

def git(*a):
    return subprocess.run(["git", *a], capture_output=True, text=True, check=True).stdout

pairs = []
for line in git("diff", "-M", "--name-status", "085bca4a^", "085bca4a").splitlines():
    parts = line.split("\t")
    if parts[0].startswith("R"):
        pairs.append((parts[0], parts[1], parts[2]))

def forms(text):
    lines = text.split("\n")
    starts = [i for i, l in enumerate(lines) if l.startswith("(")]
    out = []
    for j, s in enumerate(starts):
        e = starts[j + 1] if j + 1 < len(starts) else len(lines)
        block = lines[s:e]
        while block and (block[-1].strip() == "" or block[-1].lstrip().startswith(";")):
            block.pop()
        head = block[0]
        m = re.match(r"\((\S+)\s+(?:\^\S+\s+)*([^\s()\[\]]+)?", head)
        kind = m.group(1) if m else "?"
        name = m.group(2) if m else None
        out.append((kind, name, "\n".join(block)))
    return out

tot = collections.Counter()
for status, old, new in pairs:
    a = forms(git("show", f"085bca4a^:{old}"))
    b = forms(git("show", f"085bca4a:{new}"))
    ns_a, ns_b = a[0][2], b[0][2]
    ns_rest_changed = ns_a.split("\n")[1:] != ns_b.split("\n")[1:]
    da = collections.defaultdict(list); db = collections.defaultdict(list)
    for k, n, t in a[1:]: da[n].append(t)
    for k, n, t in b[1:]: db[n].append(t)
    common = set(da) & set(db)
    changed = [n for n in common if da[n] != db[n]]
    twice = [n for n in db if len(db[n]) > 1]
    facts_a = 4 + 4 * (len(b) - 1)
    tot["files"] += 1; tot["ns-rest-changed"] += ns_rest_changed
    tot["defs-after"] += len(b) - 1; tot["defs-changed"] += len(changed)
    tot["files-with-changed-defs"] += bool(changed); tot["facts-cut-a"] += facts_a
    tot["files-over-256"] += facts_a > 256
    print(f"{status:5} {new}: forms after {len(b)}, ns beyond line 1 changed {ns_rest_changed}, "
          f"definitions kept by name {len(common)}, of them changed {len(changed)}, "
          f"gone {len(set(da)-set(db))}, new {len(set(db)-set(da))}, named twice {twice}, cut-A facts {facts_a}")
print("TOTAL", dict(tot))
