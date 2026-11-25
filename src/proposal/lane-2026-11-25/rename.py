"""The word, by one map: the store's unit `layer` becomes `lane`.
   (?<![A-Za-z])layer -> lane   and   Layer -> Lane.
   Left as typed: the 'Decisions of ...' sections of src-store/README.md (from its first '## Decisions of').
   Not touched: the run-again fixture's two .edn files (make-fixture writes them), runs/, build output."""
import os, re, sys, subprocess, json
TREES = ["src-store","test-store","src-runtime","test-runtime","src-wire","test-wire","src-auth","test-auth",
         "src-command","src-server","src-server-browser","src-cli","test-server"]
SKIP_DIRS = {"runs","node_modules",".shadow-cljs","target","out","dist",".cpcache","run_again_fixture"}
EXTS = {"clj","cljc","cljs","edn","md","sh","txt","html","json","js","css"}
r1 = re.compile(r"(?<![A-Za-z])layer"); r2 = re.compile(r"Layer")
def swap(s): return r2.sub("Lane", r1.sub("lane", s))
apply = "--apply" in sys.argv
changed = []; total = 0; kept_as_typed = 0; renames = []
for t in TREES:
    for root, dirs, fs in os.walk(t):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for f in fs:
            p = os.path.join(root, f)
            if f == "env.clj" or f.startswith(".env") or f.startswith("raw-"): continue
            if swap(f) != f: renames.append((p, os.path.join(root, swap(f))))
            ext = f.rsplit(".",1)[-1] if "." in f else ""
            if ext not in EXTS: continue
            try: s = open(p, encoding="utf-8").read()
            except Exception: continue
            if p == "src-store/README.md":
                cut = s.find("\n## Decisions of ")
                head, tail = (s, "") if cut < 0 else (s[:cut], s[cut:])
                kept_as_typed += len(r1.findall(tail)) + len(r2.findall(tail))
                new = swap(head) + tail
            else:
                new = swap(s)
            if new != s:
                n = len(r1.findall(s)) + len(r2.findall(s)) - (kept_as_typed if p == "src-store/README.md" else 0)
                total += n; changed.append(p)
                if apply: open(p, "w", encoding="utf-8").write(new)
if apply:
    for a, b in renames: subprocess.check_call(["git", "mv", a, b])
json.dump({"changed": changed, "renames": renames}, open(os.path.join(os.path.dirname(__file__), "rename-manifest.json"), "w"), indent=1)
print(("APPLIED" if apply else "DRY RUN"), "| files changed:", len(changed), "| uses swapped:", total,
      "| left as typed in the Decisions sections:", kept_as_typed, "| files renamed:", renames)
by = {}
for p in changed: by[p.split("/")[0]] = by.get(p.split("/")[0], 0) + 1
print("files by tree:", by)
