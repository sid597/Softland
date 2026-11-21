"""Writes the file that `clojure -M:record` reads: one EDN map a line for each
thing said, in the order said.

usage: python3 make_record_edn.py /path/to/softland-memory/base /path/to/out.edn

It reads `lines.jsonl` (what Sid typed) and `replies.jsonl` (what sessions
wrote) of the record. The record is private: write the output outside the
repository.
"""
import datetime
import json
import sys

base, out_path = sys.argv[1], sys.argv[2]


def millis(ts):
    try:
        return int(datetime.datetime.fromisoformat(ts.replace('Z', '+00:00')).timestamp() * 1000)
    except Exception:
        return 0


def quoted(value):
    return json.dumps(value if isinstance(value, str) else str(value), ensure_ascii=False)


rows = []
for kind, name in (('line', 'lines'), ('reply', 'replies')):
    with open(f'{base}/{name}.jsonl', encoding='utf-8', errors='replace') as fh:
        for line in fh:
            try:
                row = json.loads(line)
            except Exception:
                continue
            rows.append((row.get('ts') or '', kind, row))
rows.sort(key=lambda r: (r[0], r[1]))

with open(out_path, 'w', encoding='utf-8') as out:
    for ts, kind, row in rows:
        out.write('{:id %s :kind :%s :ts %s :ms %d :sid %s :tool %s :text %s}\n' % (
            quoted(str(row.get('id'))), kind, quoted(ts), millis(ts),
            quoted(row.get('sid') or ''), quoted(row.get('tool') or ''),
            quoted(row.get('text') or '')))
print('things said:', len(rows))
