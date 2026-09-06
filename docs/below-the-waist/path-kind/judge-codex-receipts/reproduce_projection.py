"""Replay the frozen Codex repository verifier and the bounded viewport probe.

Uses the existing client build, after confirming its captured build hash.
Writes diagnostic outputs and compiler caches, never builder source.
"""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

here = Path(__file__).resolve().parent
root = Path('/mnt/data/projects/Softland-codex')
subprocess.run(['git', 'diff', '--exit-code', '07ea014', '--',
                'src/app/client', 'test'], cwd=root, check=True)
build = root / 'target/render-verifier/js/main.js'
prior = json.loads((here / 'codex/run.json').read_text())
assert hashlib.sha256(build.read_bytes()).hexdigest() == prior['buildSha256']
deps = '{:paths ["src" "%s"] :deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' % here
config = ('{:output-dir "%s/codex-projection-js" :modules {:main '
          '{:entries [judge-codex-projection] :init-fn judge-codex-projection/init}}}') % here
commands = [
    ('repository-verifier', ['node', 'test/render_engine/run_verifier.mjs']),
    ('projection-build', ['clj', '-Sdeps', deps, '-M', '-m',
                          'shadow.cljs.devtools.cli', 'release', 'render-verifier',
                          '--config-merge', config]),
    ('projection-capture', ['node', str(here / 'capture.mjs'), str(root),
                            str(here / 'codex-projection'),
                            str(here / 'codex-projection-js/main.js')]),
]
env = {key: value for key, value in os.environ.items()
       if not key.startswith('RENDER_VERIFIER_RECORD_')}
runs = []
for label, command in commands:
    started = time.monotonic()
    with (here / ('codex-' + label + '.log')).open('w') as log:
        result = subprocess.run(command, cwd=root, env=env,
                                stdout=log, stderr=subprocess.STDOUT)
    runs.append({'label': label, 'command': command, 'cwd': str(root),
                 'exit': result.returncode, 'elapsedSeconds': time.monotonic() - started})
    if label == 'repository-verifier':
        (here / 'codex-repository-verifier.json').write_bytes(
            (root / 'target/render-verifier/receipt.json').read_bytes())
    (here / 'projection-reproduction.json').write_text(json.dumps({
        'judge': prior['judge'], 'commands': runs}, indent=2) + '\n')
    if label != 'repository-verifier':
        result.check_returncode()
print(json.dumps(runs, indent=2))
