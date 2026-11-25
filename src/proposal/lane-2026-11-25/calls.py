"""calls.py [--not-decided | --decided] [--edn] [ROOT...]

Lists whose call each thing in the code is, read from the source: the
metadata `:softland/whose-call` of a namespace or of a var. Each call is a map

    {:state  :not-decided | :decided
     :what   a string
     :source a string; a :decided has one}

`:not-decided`: Sid has not decided it; what stands is a session's, or is kept
so that the build runs. `:decided`: Sid decided it, and `:source` names where.

The metadata is the attr-map of an `ns`, `defn`, `defn-` or `defmacro`, after
its docstring, and metadata on the name of any other `def`. A call is on its
var, so it moves with it.

Exit 1 when a call is not in this form. With no ROOT it reads the five trees.
Run from the repository root.
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import same_code as sc  # noqa: E402

KEY = ('atom', ':softland/whose-call')
ROOTS = ['src-store', 'src-runtime', 'src-wire', 'src-auth', 'src-command']
STATES = (':not-decided', ':decided')
ATTR_HEADS = ('ns', 'defn', 'defn-', 'defmacro')


def is_map(f):
    return isinstance(f, list) and f[:2] == ['L', '{']


def value_under(m, key):
    items = m[2:]
    for k, v in zip(items[::2], items[1::2]):
        if k == key:
            return v
    return None


def text_of(tok):
    s = tok[1][1:-1].replace('\\"', '"').replace('\\\\', '\\')
    return re.sub(r'\s+', ' ', s).strip()


def calls_of(form):
    """(name, vector-form or None) for a top-level form."""
    f = form
    if not (isinstance(f, list) and f[0] == 'L' and f[1] == '(' and len(f) > 3):
        return None, None
    head = sc.bare(f[2])
    if not (isinstance(head, tuple) and head[0] == 'atom'):
        return None, None
    hv = head[1].split('/')[-1]
    if not (hv == 'ns' or hv.startswith('def')):
        return None, None
    name_form = f[3]
    found = None
    while isinstance(name_form, list) and name_form[0] == 'M':
        if is_map(name_form[1]) and value_under(name_form[1], KEY) is not None:
            found = value_under(name_form[1], KEY)
        name_form = name_form[2]
    if not isinstance(name_form, tuple):
        return None, None
    rest = f[4:]
    if hv in ATTR_HEADS:
        if rest and sc.is_str(rest[0]) and len(rest) > 1:
            rest = rest[1:]
        if rest and is_map(rest[0]) and value_under(rest[0], KEY) is not None:
            found = value_under(rest[0], KEY)
    return (hv, name_form[1]), found


def read(path):
    out, bad = [], []
    forms = sc.tree(sc.tokens(open(path, encoding='utf-8').read()))
    ns = None
    for form in forms:
        named, vec = calls_of(form)
        if named is None:
            continue
        hv, name = named
        if hv == 'ns':
            ns = name
        if vec is None:
            continue
        at = ns if hv == 'ns' else '%s/%s' % (ns, name)
        if not (isinstance(vec, list) and vec[:2] == ['L', '[']) or len(vec) < 3:
            bad.append('%s: %s: not a vector of calls' % (path, at))
            continue
        for m in vec[2:]:
            if not is_map(m) or len(m) % 2:
                bad.append('%s: %s: a call is not a map' % (path, at))
                continue
            d = dict(zip([k[1] if isinstance(k, tuple) else '?' for k in m[2::2]], m[3::2]))
            ps = [k for k in d if k not in (':state', ':what', ':source')]
            if ps:
                bad.append('%s: %s: unknown keys %s' % (path, at, ps))
            st = d.get(':state')
            if not (isinstance(st, tuple) and st[1] in STATES):
                bad.append('%s: %s: state %s' % (path, at, st[1] if isinstance(st, tuple) else st))
                continue
            if not sc.is_str(d.get(':what')):
                bad.append('%s: %s: no :what string' % (path, at))
                continue
            if ':source' in d and not sc.is_str(d[':source']):
                bad.append('%s: %s: :source is not a string' % (path, at))
                continue
            if st[1] == ':decided' and ':source' not in d:
                bad.append('%s: %s: :decided with no :source' % (path, at))
            out.append({'at': at, 'state': st[1], 'what': text_of(d[':what']),
                        'source': text_of(d[':source']) if ':source' in d else None, 'file': path})
    return out, bad


def all_calls(roots):
    out, bad = [], []
    for root in roots:
        for d, _, fs in sorted(os.walk(root)):
            for f in sorted(fs):
                if f.endswith('.clj') or f.endswith('.cljc'):
                    o, b = read(os.path.join(d, f))
                    out += o
                    bad += b
    return out, bad


def main(argv):
    flags = [a for a in argv if a.startswith('--')]
    roots = [a for a in argv if not a.startswith('--')] or ROOTS
    calls, bad = all_calls(roots)
    want = None
    if '--not-decided' in flags:
        want = ':not-decided'
    if '--decided' in flags:
        want = ':decided'
    shown = [c for c in calls if want is None or c['state'] == want]
    for c in shown:
        if '--edn' in flags:
            q = lambda s: '"' + s.replace('\\', '\\\\').replace('"', '\\"') + '"'  # noqa: E731
            print('{:at %s :state %s :what %s%s}' % (
                c['at'], c['state'], q(c['what']), (' :source ' + q(c['source'])) if c['source'] else ''))
        else:
            print('%s  %s\n    %s%s\n' % (c['at'], c['state'], c['what'],
                                         ('\n    source: ' + c['source']) if c['source'] else ''))
    n = {s: sum(1 for c in calls if c['state'] == s) for s in STATES}
    print('%d calls: %d not decided, %d decided' % (len(calls), n[':not-decided'], n[':decided']),
          file=sys.stderr)
    for b in bad:
        print('NOT IN FORM  ' + b, file=sys.stderr)
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
