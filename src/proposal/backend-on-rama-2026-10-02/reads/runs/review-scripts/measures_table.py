# Review helper: two runs of the read build's measures side by side, as a table.
# From the repository root:
#   python3 <this file> <reads/runs/measures-read-path.txt> <reads/runs/measures-final.txt>
import re,sys
B=sys.argv[1]   # the run before, as it was kept
A=sys.argv[2]   # the run after: a kept file, or a raw output
def lines(p): return [l.rstrip('\n') for l in open(p,encoding='utf-8',errors='replace') if l.startswith('MEASURED')]
b,a=lines(B),lines(A)
def find(ls, *subs):
    hits=[l for l in ls if all(s in l for s in subs)]
    return hits[0] if hits else None
def mid(l):
    if not l: return ('-','-')
    m=re.search(r'middle ([0-9.]+) ms \| slowest 1 in 100 ([0-9.]+) ms', l)
    return (m.group(1), m.group(2)) if m else ('-','-')
def fld(l, pat):
    if not l: return '-'
    m=re.search(pat, l); return m.group(1) if m else '-'
def row(label, bl, al, extra_b='', extra_a=''):
    mb,ma=mid(bl),mid(al)
    print(f"| {label} | {mb[0]} / {mb[1]}{extra_b} | {ma[0]} / {ma[1]}{extra_a} |")
print('| What, with the middle and the slowest 1 in 100 in ms | Before the repairs | The final tree |\n|---|---|---|')
row('M1 the streaming gate, 100 acts a second, the key not marked, from the call of `send-act` to its return', find(b,'M1 the streaming gate, the same, the key not marked'), find(a,'M1 the streaming gate, the same, the key not marked'))
row('M1 the same, the key marked', find(b,'M1 the streaming gate, the same, the key marked'), find(a,'M1 the streaming gate, the same, the key marked'))
row('M1 the base, the same acts, from the call to "accepted"', find(b,'M1 the base, the same |'), find(a,'M1 the base, the same |'))
for n in ['1','100','1000']:
    for shape in ['places','runs']:
        bl=find(b,f'M2 the same, {n} live reads, {shape}'); al=find(a,f'M2 the same, {n} live reads, {shape}')
        bo=find(b,f'with {n} live reads open on the layer', 'place that is not written' if shape=='places' else 'run that holds'); ao=find(a,f'with {n} live reads asked for on the layer', 'place that is not written' if shape=='places' else 'run that holds')
        row(f'M2 the streaming gate with {n} live reads asked for, each on another {"place that is not written" if shape=="places" else "run that holds the written places"}', bl, al,
            f"; open {fld(bo, r':live-reads (\d+)')}; told {fld(bl, r'told: (\d+)')}", f"; open {fld(ao, r':live-reads (\d+)')}; told {fld(al, r'told: (\d+)')}")
for canvas in ['a canvas in a streaming layer','a canvas in the base']:
    for asked in ['the run across the entities','what points at the canvas']:
        for how in ['asked once','kept live']:
            for through in ['one layer','three layers']:
                key=f'M3 {canvas} | {asked} {how} through {through} |'
                bl,al=find(b,key),find(a,key)
                wb=' (a word, `:reach-the-store`)' if bl and 'reach-the-store' in bl else ''
                wa=' (a word, `:serve-a-read-that-large`)' if al and 'serve-a-read-that-large' in al else ''
                print(f"| M3 {canvas}, {asked}, {how}, through {through} | {mid(bl)[0]}{wb} | {mid(al)[0]}{wa} |")
al=find(a,'M3 a canvas of 100 facts'); print(f"| M3 a canvas of 100, what points at it, kept live, through three layers | not run | {mid(al)[0]} |")
for what in ['of a place of the facts table, in a streaming layer','of a place of the facts table, in the base','of a place of the facts by value, in a streaming layer']:
    bl,al=find(b,'M4 the same, '+what),find(a,'M4 the same, '+what)
    row(f'M4 how late a push is, {what}, from the ack', bl, al, f"; {fld(bl, r'call of their own: (\d+)')} of 500 told by a call of their own", f"; {fld(al, r'call of their own: (\d+)')} of 500")
