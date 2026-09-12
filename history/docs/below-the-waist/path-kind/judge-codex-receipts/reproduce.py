"""Rebuild and recapture the focused judge probes against the named lane.

Usage: python3 reproduce.py claude|codex
Writes only judge artifacts and ordinary compiler caches. Does not edit lane code.
The supplied CLJS entry calls the lane's actual preparation and draw functions.
"""
import json
from pathlib import Path
import subprocess
import sys
import time

lane = sys.argv[1]
assert lane in ("claude", "codex")
here = Path(__file__).resolve().parent
root = Path("/mnt/data/projects/Softland-" + lane)
frozen = {"claude": "4d9e0d6", "codex": "07ea014"}[lane]
subprocess.run(["git", "diff", "--exit-code", frozen, "--", "src/app/client"], cwd=root, check=True)
deps = '{:paths ["src" "%s"] :deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' % here
config = ('{:output-dir "%s/%s-probe-js" :modules {:main '
          '{:entries [judge-%s] :init-fn judge-%s/init}}}') % (here, lane, lane, lane)
commands = [
    ["clj", "-Sdeps", deps, "-M", "-m", "shadow.cljs.devtools.cli", "release", "render-verifier", "--config-merge", config],
    ["node", str(here / "capture.mjs"), str(root), str(here / (lane + "-probes")),
     str(here / (lane + "-probe-js") / "main.js")],
]
runs = []
for label, command in zip(("build", "capture"), commands):
    start = time.monotonic()
    with (here / (lane + "-reproduce-" + label + ".log")).open("w") as log:
        result = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT)
    runs.append({"command": command, "cwd": str(root), "exit": result.returncode,
                 "elapsedSeconds": time.monotonic() - start})
    (here / (lane + "-reproduce.json")).write_text(json.dumps(runs, indent=2) + "\n")
    result.check_returncode()
print(json.dumps(runs, indent=2))
