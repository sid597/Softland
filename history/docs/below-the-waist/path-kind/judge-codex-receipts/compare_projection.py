"""Compare this judge's client pixels with the frozen repository goldens."""
import hashlib
from io import BytesIO
import json
from pathlib import Path
import subprocess

import numpy as np
from PIL import Image

here = Path(__file__).resolve().parent
root = Path('/mnt/data/projects/Softland-codex')
rows = []
for filename in ('gpu-region3d-floor-tree.png', 'gpu-slug-default-unit-z1.png'):
    golden_path = 'test/app/fixtures/render_engine/gpu-goldens/' + filename
    golden = subprocess.check_output(['git', 'show', '07ea014:' + golden_path], cwd=root)
    expected = np.asarray(Image.open(BytesIO(golden)).convert('RGBA')).astype(int)
    cases = ['claude', 'codex']
    if filename == 'gpu-region3d-floor-tree.png':
        cases.append('codex-projection')
    for case in cases:
        actual_path = here / case / 'png' / filename
        actual_bytes = actual_path.read_bytes()
        actual = np.asarray(Image.open(BytesIO(actual_bytes)).convert('RGBA')).astype(int)
        assert actual.shape == expected.shape
        delta = np.abs(actual - expected)
        row = {'case': case, 'file': filename,
               'golden': {'commit': '07ea014', 'path': golden_path,
                          'sha256': hashlib.sha256(golden).hexdigest()},
               'actual': {'path': str(actual_path.relative_to(here)),
                          'sha256': hashlib.sha256(actual_bytes).hexdigest(),
                          'capture': str(Path(case) / 'run.json')},
               'changedPixels': int((delta.sum(axis=2) > 0).sum()),
               'maxByteDelta': int(delta.max())}
        if filename == 'gpu-region3d-floor-tree.png':
            row['sample'] = {'pixel': [55, 58], 'expected': expected[58, 55].tolist(),
                             'actual': actual[58, 55].tolist()}
        rows.append(row)
result = {'method': 'Absolute RGBA byte differences against the frozen test PNG.',
          'rows': rows}
(here / 'projection-pixel-comparison.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
