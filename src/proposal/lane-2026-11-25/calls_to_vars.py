"""calls_to_vars.py [--check]

Run once, on 25 November 2026, to move each entry of the five `whose-call.edn`
files onto the namespace or the var that it names, as the metadata
`:softland/whose-call` (`calls.py` says the form and lists them). Kept as the
record of how the move was made; the five files it read are in `as-files/`.

What it changes of an entry, and nothing else:

- `:a-sessions` becomes `:not-decided`. The forks could not tell the two
  apart alike ("the builder's, kept and not decided" went both ways), and the
  sentence still says whose the thing is. Sid's to turn.
- The four entries under MENDED, which the review of the cut found wrong.

With --check it writes nothing and says what it would do. Run from the
repository root.
"""
import os
import re
import sys
import textwrap

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import same_code as sc  # noqa: E402

ROOTS = ['src-store', 'src-runtime', 'src-wire', 'src-auth', 'src-command']
TREES = ['store', 'runtime', 'wire', 'auth', 'command']
WIDTH = 78
ATTR_HEADS = ('ns', 'defn', 'defn-', 'defmacro')

# (at, the start of the entry's :what) -> the fields that change
MENDED = {
    ('softland.store.read-path.lanes/standing', 'Any permission that stands allows seeing'):
        {'what': '`standing` is the one function of who may see a lane.'},
    ('softland.store.read-path', 'Room that the read path leaves'):
        {'what': 'Room that the read path leaves, each a thing that is not in this build and that it '
                 'decides nothing of: sign-in, in front of the read path (who asks is what the caller '
                 'says, and `:system` is a who like any other); a network edge, a screen, a view, a '
                 'tool, an agent; a record of who read what (nothing is kept of a read); a new word of '
                 'a query (finding by text, looking back, what stands on a fact, everything one person '
                 'wrote), which is one entry in each of three maps '
                 '(`softland.store.read-path.query/words`).'},
    ('softland.store.read-path', 'Both numbers of the read path'):
        {'what': "The read path's two bounds on pushes, `:max-pushes-under-one-routing-id` and "
                 "`:max-checks-of-one-live-read`, are the build's, kept and not decided, and so is the "
                 'way of the checks that needs a push for each.'},
    ('softland.auth.schema/max-actor-depth', 'The most makers below a root'):
        {'source': 'Revision 7, which names nobody; '
                   '`src/proposal/runtime-2026-11-21/spine/auth/BUILD.md` says "Closed for this build '
                   'per Sid\'s instruction"'},
}


def unesc(tok):
    return tok[1][1:-1].replace('\\"', '"').replace('\\\\', '\\')


def esc(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')


def entries():
    out = []
    used = set()
    for tree in TREES:
        p = os.path.join(HERE, 'as-files', tree + '.edn')
        forms = sc.tree(sc.tokens(open(p, encoding='utf-8').read()))
        for m in forms[0][2:]:
            d = dict(zip([k[1] for k in m[2::2]], m[3::2]))
            e = {'at': d[':at'][1], 'state': d[':state'][1], 'what': unesc(d[':what']),
                 'source': unesc(d[':source']) if ':source' in d else None}
            if e['state'] == ':a-sessions':
                e['state'] = ':not-decided'
            for (at, start), change in MENDED.items():
                if e['at'] == at and e['what'].startswith(start):
                    e.update(change)
                    used.add((at, start))
            out.append(e)
    missing = set(MENDED) - used
    if missing:
        raise SystemExit('MENDED entries not found: %s' % sorted(missing))
    return out


def file_of(ns):
    rel = ns.replace('.', '/').replace('-', '_') + '.clj'
    for r in ROOTS:
        p = os.path.join(r, rel)
        if os.path.exists(p):
            return p
    raise SystemExit('no file for %s' % ns)


def string_lines(key, text, col):
    """`key "text"` wrapped so that each line of the text starts at one column."""
    first = col + len(key) + 2
    lines = textwrap.wrap(esc(re.sub(r'\s+', ' ', text).strip()), WIDTH - first - 1,
                          break_long_words=False, break_on_hyphens=False)
    out = [' ' * col + key + ' "' + lines[0]]
    out += [' ' * first + l for l in lines[1:]]
    out[-1] += '"'
    return out


def calls_text(calls, brace_col, opener):
    """The map, from its opener (`{` or `^{`) at column brace_col to its `}`."""
    c = brace_col + len(opener)
    lines = [opener + ':softland/whose-call']
    for i, e in enumerate(calls):
        body = string_lines(':what', e['what'], c + 2)
        if e['source']:
            body += string_lines(':source', e['source'], c + 2)
        head = ' ' * c + ('[' if i == 0 else ' ') + '{:state ' + e['state']
        body[-1] += '}' + (']}' if i == len(calls) - 1 else '')
        lines += [head] + body
    return '\n'.join(lines)


def string_end(s, i):
    j = i + 1
    while s[j] != '"':
        j += 2 if s[j] == '\\' else 1
    return j


def place(src, var, calls):
    if var is None:
        m = re.search(r'^\((ns)\s+((?:\^(?:\{[^}]*\}|\S+)\s+)*)(\S+?)(?=[\s)])', src, re.M)
    else:
        m = re.search(r'^\(((?:[\w.-]+/)?def[\w-]*)\s+((?:\^(?:\{[^}]*\}|\S+)\s+)*)(' + re.escape(var) +
                      r')(?=[\s)\]])', src, re.M)
    if not m:
        raise SystemExit('no form for %s' % var)
    head = m.group(1).split('/')[-1]
    if ':softland/whose-call' in src[m.start():m.end() + 40]:
        raise SystemExit('%s already holds calls' % var)
    if head in ATTR_HEADS:
        pos = m.end()
        k = pos
        while src[k].isspace():
            k += 1
        if src[k] == '"':
            pos = string_end(src, k) + 1
            k = pos
            while src[k].isspace():
                k += 1
        if src[k] == '{':
            raise SystemExit('%s has an attr-map already' % var)
        if '\n' in src[pos:k] or src[k] == ')':
            return src[:pos] + '\n  ' + calls_text(calls, 2, '{') + src[pos:]
        return src[:pos] + '\n  ' + calls_text(calls, 2, '{') + '\n  ' + src[k:]
    name_at = m.start(3)
    if m.group(2).strip():
        before = src[:name_at].rstrip() + '\n  '
        return before + calls_text(calls, 2, '^{') + '\n  ' + src[name_at:]
    col = name_at - m.start()
    return src[:name_at] + calls_text(calls, col, '^{') + '\n  ' + src[name_at:]


def main(check):
    by_file = {}
    for e in entries():
        ns, _, var = e['at'].partition('/')
        by_file.setdefault(file_of(ns), {}).setdefault(var or None, []).append(e)
    n = 0
    for path, by_var in sorted(by_file.items()):
        src = open(path, encoding='utf-8').read()
        for var, calls in by_var.items():
            src = place(src, var, calls)
            n += len(calls)
        if not check:
            open(path, 'w', encoding='utf-8').write(src)
        print('%-56s %2d vars %3d calls' % (path, len(by_var), sum(len(c) for c in by_var.values())))
    print('%d calls on %d files%s' % (n, len(by_file), ' (nothing written)' if check else ''))


if __name__ == '__main__':
    main('--check' in sys.argv[1:])
