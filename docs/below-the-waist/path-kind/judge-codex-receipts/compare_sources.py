"""Reproduce physical source-line counts for the two frozen deliveries.

Counts include comments, namespace/function docstrings and embedded WGSL.
Every path is classified explicitly in the output; no runtime cost is inferred.
"""
import json
from pathlib import Path
import subprocess

here = Path(__file__).resolve().parent

def git(root, *args):
    return subprocess.check_output(["git", *args], cwd=root, text=True)

out = {}
for lane, revision in (("claude", "4d9e0d6"), ("codex", "07ea014")):
    root = Path("/mnt/data/projects/Softland-" + lane)
    rows = []
    for line in git(root, "diff", "--numstat", "bedb890", revision, "--", "src/app/client", "test").splitlines():
        added, deleted, path = line.split("\t")
        if added == "-":
            category = "binary"
        elif path.endswith(".md"):
            category = "hierarchical-docs" if path.startswith("src/app/client/") else "test-docs"
        elif lane == "claude" and path == "src/app/client/path/records.cljc":
            category = "fixtures"
        elif path.startswith("src/app/client/harness/"):
            category = "fixtures" if path.endswith(("path_fixtures.cljc", "path_goldens.json")) else "harness-and-tests"
        elif path.startswith("test/"):
            category = "external-tests-and-baselines"
        elif path.endswith((".cljc", ".cljs", ".clj")):
            category = "production-source"
        else:
            category = "other"
        rows.append({"path": path, "category": category,
                     "added": None if added == "-" else int(added),
                     "deleted": None if deleted == "-" else int(deleted)})
    categories = {}
    for row in rows:
        count = categories.setdefault(row["category"], {"added": 0, "deleted": 0, "files": 0})
        count["files"] += 1
        if row["added"] is not None:
            count["added"] += row["added"]
            count["deleted"] += row["deleted"]
    for count in categories.values():
        count["net"] = count["added"] - count["deleted"]
    tree = git(root, "ls-tree", "-r", "--name-only", revision, "src/app/client").splitlines()
    groups = {
        "path-implementation": [p for p in tree if p.startswith("src/app/client/path/")
                                and p.endswith((".cljc", ".cljs"))
                                and not (lane == "claude" and p.endswith("/records.cljc"))],
        "new-shared-engine": [p for p in ("src/app/client/engine/executor.cljc",
                              "src/app/client/engine/expression.cljc", "src/app/client/engine/coverage.cljs",
                              "src/app/client/engine/coverage_gpu.cljs") if p in tree],
        "placement-rendering": ["src/app/client/region3d/on_plane_renderer.cljs"] +
                               (["src/app/client/region3d/path_projection.cljc"] if lane == "codex" else []),
    }
    current = {}
    for name, paths in groups.items():
        counts = {p: len(git(root, "show", revision + ":" + p).splitlines()) for p in paths}
        current[name] = {"lines": sum(counts.values()), "files": counts}
    out[lane] = {"frozen": git(root, "rev-parse", revision).strip(), "baseline": "bedb890",
                 "definition": "Physical diff lines including comments, docstrings and embedded WGSL. Binary files excluded from line totals. Categories listed per path.",
                 "categories": categories, "current-groups": current, "paths": rows}

(here / "production-comparison-lines.json").write_text(json.dumps(out, indent=2) + "\n")
for lane, result in out.items():
    print(lane, json.dumps(result["categories"], sort_keys=True))
