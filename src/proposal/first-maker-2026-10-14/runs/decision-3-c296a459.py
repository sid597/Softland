# Throwaway run, decision-3 agent, 15 Oct 2026. For each definition that c296a459
# renamed (a "-(def" line whose name is gone and a "+(def" line with "class"
# replaced by "depot"), compare the old form with the name swapped against the
# new form: does anything beyond the name change? Line-based forms, approximate.
# Run from the repository root: python3 -I <this file>
import subprocess, re
def git(*a): return subprocess.run(["git", *a], capture_output=True, text=True, check=True).stdout
files = [l.split("\t")[-1] for l in git("diff", "--name-status", "c296a459^", "c296a459").splitlines() if l.endswith(".clj")]
def forms(text):
    lines = text.split("\n"); starts = [i for i, l in enumerate(lines) if l.startswith("(")]
    out = {}
    for j, s in enumerate(starts):
        e = starts[j+1] if j+1 < len(starts) else len(lines); block = lines[s:e]
        while block and (block[-1].strip() == "" or block[-1].lstrip().startswith(";")): block.pop()
        m = re.match(r"\((def\S*)\s+(?:\^\S+\s+)*([^\s()\[\]]+)", block[0])
        if m: out[m.group(2)] = "\n".join(block)
    return out
for f in files:
    try: a = forms(git("show", f"c296a459^:{f}"))
    except subprocess.CalledProcessError: continue
    b = forms(git("show", f"c296a459:{f}"))
    for old in a:
        if "class" in old and old not in b:
            new = old.replace("class", "depot")
            if new in b:
                same_but_name = a[old].replace(old, new, 1) == b[new]
                print(f"{f}: {old} -> {new}; beyond the name unchanged: {same_but_name}")
