"""assemble_calls.py [--check]

Superseded on 25 November 2026: Sid took whose call a thing is out of the
code. The five files that this wrote are in `as-files/`. Kept as the record of
how they were made; a run of it would write the five files into the trees
again.

Reads every `calls/*.edn` beside this file and writes one `whose-call.edn` per
tree, beside that tree's code. An entry goes to the tree of its `:at`. Inside a
tree the entries follow the order of the files in `fork-lists.txt`, and inside
a file the order of the vars in the source; an entry on the namespace comes
first. Each entry's strings are written as the fork wrote them.

With --check it writes nothing and prints what it would write and what it
could not place. Run from the repository root.
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import same_code as sc  # noqa: E402

TREES = [('softland.store', 'src-store/softland/store'),
         ('softland.runtime', 'src-runtime/softland/runtime'),
         ('softland.wire', 'src-wire/softland/wire'),
         ('softland.auth', 'src-auth/softland/auth'),
         ('softland.command', 'src-command/softland/command')]
ROOTS = ['src-store', 'src-runtime', 'src-wire', 'src-auth', 'src-command']
STATES = (':not-decided', ':a-sessions', ':decided')
KEYS = (':at', ':state', ':what', ':source')

HEADER = """;; Whose call each thing in this tree's code is. One entry per place that is
;; not decided (:not-decided: kept so that the build runs; nobody chose it),
;; that a session or the builder chose (:a-sessions: Sid's to turn), or that
;; carries a decision of Sid's (:decided, with :source). :at is the var, or the
;; namespace. An entry moves with its var.
"""


def file_of(ns):
    rel = ns.replace('.', '/').replace('-', '_') + '.clj'
    for r in ROOTS:
        p = os.path.join(r, rel)
        if os.path.exists(p):
            return p
    return None


def entries(path):
    forms = sc.tree(sc.tokens(open(path, encoding='utf-8').read()))
    if len(forms) != 1 or forms[0][:2] != ['L', '[']:
        raise SystemExit('%s: not one vector' % path)
    out = []
    for m in forms[0][2:]:
        if not (isinstance(m, list) and m[:2] == ['L', '{']) or len(m) % 2:
            raise SystemExit('%s: an entry is not a map: %s' % (path, sc.flat(m)[:80]))
        d = {}
        for k, v in zip(m[2::2], m[3::2]):
            if not isinstance(k, tuple) or not isinstance(v, tuple):
                raise SystemExit('%s: not a flat map: %s' % (path, sc.flat(m)[:80]))
            d[k[1]] = v
        out.append(d)
    return out


def problems(d, src):
    bad = [k for k in d if k not in KEYS]
    if bad:
        yield 'unknown keys %s' % bad
    for k in (':at', ':state', ':what'):
        if k not in d:
            yield 'no %s' % k
    if ':state' in d and d[':state'][1] not in STATES:
        yield 'state %s' % d[':state'][1]
    if d.get(':state', ('', ''))[1] == ':decided' and ':source' not in d:
        yield ':decided with no :source'
    for k in (':what', ':source'):
        if k in d and d[k][0] != 'str':
            yield '%s is not a string' % k
    if ':at' in d:
        ns, _, var = d[':at'][1].partition('/')
        f = file_of(ns)
        if f is None:
            yield 'no file for namespace %s' % ns
        elif var and position(f, var) is None:
            yield 'no var %s in %s' % (var, f)


_text = {}


def position(f, var):
    if f not in _text:
        _text[f] = open(f, encoding='utf-8').read()
    m = re.search(r'\((?:[\w.-]+/)?def[\w-]*\s+(?:\^(?:\{[^}]*\}|\S+)\s+)*' + re.escape(var) + r'(?=[\s)\]])',
                  _text[f])
    return m.start() if m else None


def main(check):
    order = [l.strip() for l in open(os.path.join(HERE, 'fork-lists.txt')) if l.startswith('src-')]
    order.insert(0, 'src-store/softland/store/gate/first_act.clj')
    found = []
    bad = 0
    calls = os.path.join(HERE, 'calls')
    for name in sorted(os.listdir(calls)):
        if not name.endswith('.edn'):
            continue
        for d in entries(os.path.join(calls, name)):
            ps = list(problems(d, name))
            if ps:
                bad += 1
                print('PROBLEM %s %s: %s' % (name, d.get(':at', ('', '?'))[1], '; '.join(ps)))
                continue
            ns, _, var = d[':at'][1].partition('/')
            f = file_of(ns)
            fi = order.index(f) if f in order else len(order)
            found.append((fi, -1 if not var else position(f, var), len(found), d, name))
    if bad:
        raise SystemExit('%d entries with problems; nothing written' % bad)
    found.sort(key=lambda t: t[:3])
    for prefix, folder in TREES:
        mine = [t for t in found if t[3][':at'][1].startswith(prefix + '.')]
        body = []
        for _, _, _, d, _ in mine:
            lines = ['{:at %s' % d[':at'][1], ' :state %s' % d[':state'][1], ' :what %s' % d[':what'][1]]
            if ':source' in d:
                lines.append(' :source %s' % d[':source'][1])
            body.append('\n '.join(lines) + '}')
        text = HEADER + '[' + '\n\n '.join(body) + ']\n'
        dest = os.path.join(folder, 'whose-call.edn')
        counts = {s: sum(1 for t in mine if t[3][':state'][1] == s) for s in STATES}
        print('%-48s %3d entries  %s' % (dest, len(mine), '  '.join('%s %d' % kv for kv in counts.items())))
        if not check:
            open(dest, 'w', encoding='utf-8').write(text)
    placed = sum(1 for t in found if any(t[3][':at'][1].startswith(p + '.') for p, _ in TREES))
    if placed != len(found):
        raise SystemExit('%d entries belong to no tree' % (len(found) - placed))


if __name__ == '__main__':
    main('--check' in sys.argv[1:])
