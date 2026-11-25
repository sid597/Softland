"""same_code.py REV FILE...

Exit 0 when each FILE holds the same code as it did at REV once docstrings and
comments are taken out. A docstring is: the string in third place of an `ns` or
`def...` form that has more after it; the last string of a protocol method; the
string after `:doc`. Every other string is code. Whitespace and commas count for
nothing.
"""
import subprocess
import sys


def tokens(s):
    out = []
    i = 0
    n = len(s)
    while i < n:
        c = s[i]
        if c.isspace() or c == ',':
            i += 1
        elif c == ';':
            j = s.find('\n', i)
            i = n if j < 0 else j
        elif c == '"' or (c == '#' and i + 1 < n and s[i + 1] == '"'):
            j = i + (2 if c == '#' else 1)
            while j < n and s[j] != '"':
                j += 2 if s[j] == '\\' else 1
            out.append(('re' if c == '#' else 'str', s[i:j + 1]))
            i = j + 1
        elif c == '\\':
            j = i + 2
            while j < n and s[j].isalnum():
                j += 1
            out.append(('atom', s[i:j]))
            i = j
        elif c in '([{':
            out.append(('open', c))
            i += 1
        elif c in ')]}':
            out.append(('close', c))
            i += 1
        elif c in "'`~@^#":
            if s.startswith('~@', i):
                out.append(('pre', '~@'))
                i += 2
            elif c == '#' and i + 1 < n and s[i + 1] in "({":
                out.append(('pre', '#'))
                i += 1
            elif c == '#' and s.startswith('#?@', i):
                out.append(('pre', '#?@'))
                i += 3
            elif c == '#' and i + 1 < n and s[i + 1] in "'_?":
                out.append(('pre', s[i:i + 2]))
                i += 2
            elif c == '#':
                j = i + 1
                while j < n and not (s[j].isspace() or s[j] in '()[]{}";,'):
                    j += 1
                out.append(('pre', s[i:j]))
                i = j
            else:
                out.append(('pre', c))
                i += 1
        else:
            j = i
            while j < n and not (s[j].isspace() or s[j] in '()[]{}";,'):
                j += 1
            out.append(('atom', s[i:j]))
            i = j
    return out


def tree(toks):
    pos = 0

    def form():
        nonlocal pos
        k, v = toks[pos]
        pos += 1
        if k == 'open':
            items = []
            while toks[pos][0] != 'close':
                items.append(form())
            pos += 1
            return ['L', v] + items
        if k == 'pre':
            if v == '^':
                m = form()
                f = form()
                return ['M', m, f]
            return ['P', v, form()]
        return (k, v)

    out = []
    while pos < len(toks):
        out.append(form())
    return out


def bare(f):
    while isinstance(f, list) and f[0] == 'M':
        f = f[2]
    return f


def is_str(f):
    return isinstance(f, tuple) and f[0] == 'str'


def norm(f, in_protocol=False):
    if isinstance(f, tuple):
        return f
    if f[0] == 'M':
        return ['M', norm(f[1]), norm(f[2])]
    if f[0] == 'P':
        return ['P', f[1], norm(f[2])]
    items = f[2:]
    head = bare(items[0]) if items else None
    hv = head[1] if isinstance(head, tuple) and head[0] == 'atom' else ''
    hv = hv.split('/')[-1]
    if f[1] == '(' and (hv == 'ns' or hv.startswith('def')) and len(items) > 3 and is_str(items[2]):
        items = items[:2] + items[3:]
    if f[1] == '(' and in_protocol and len(items) >= 3 and is_str(items[-1]):
        items = items[:-1]
    kept = []
    skip = False
    for it in items:
        if skip and is_str(it):
            skip = False
            continue
        skip = isinstance(it, tuple) and it == ('atom', ':doc')
        kept.append(it)
    proto = hv in ('defprotocol', 'definterface')
    if proto and len(kept) > 2 and is_str(kept[2]):
        kept = kept[:2] + kept[3:]
    return ['L', f[1]] + [norm(i, proto) for i in kept]


def flat(f):
    if isinstance(f, tuple):
        return f[1]
    if f[0] == 'M':
        return '^' + flat(f[1]) + ' ' + flat(f[2])
    if f[0] == 'P':
        return f[1] + flat(f[2])
    close = {'(': ')', '[': ']', '{': '}'}[f[1]]
    return f[1] + ' '.join(flat(i) for i in f[2:]) + close


def canon(s):
    return [flat(norm(f)) for f in tree(tokens(s))]


def first_difference(a, b):
    for x, y in zip(a, b):
        if x != y:
            k = next((i for i, (u, v) in enumerate(zip(x, y)) if u != v), min(len(x), len(y)))
            return ('   was: ...' + x[max(0, k - 60):k + 90].replace('\n', ' '),
                    '   now: ...' + y[max(0, k - 60):k + 90].replace('\n', ' '))
    return ('   top-level forms: was %d' % len(a), '   top-level forms: now %d' % len(b))


if __name__ == '__main__':
    rev = sys.argv[1]
    bad = 0
    for p in sys.argv[2:]:
        try:
            old = subprocess.check_output(['git', 'show', '%s:%s' % (rev, p)],
                                          stderr=subprocess.DEVNULL).decode()
        except subprocess.CalledProcessError:
            print('NEW FILE  ', p)
            continue
        a = canon(old)
        b = canon(open(p, encoding='utf-8').read())
        if a == b:
            print('same code ', p)
        else:
            bad += 1
            print('DIFFERENT ', p)
            for line in first_difference(a, b):
                print(line)
    sys.exit(1 if bad else 0)
