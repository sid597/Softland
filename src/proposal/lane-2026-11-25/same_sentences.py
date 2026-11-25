"""For each proof: are the PASSED/FAILED sentences of the renamed tree the same as the unchanged head's,
once the head's are put through the rename map?"""
import os, re, sys
S=os.path.dirname(os.path.abspath(__file__))
r1=re.compile(r"(?<![A-Za-z])layer"); r2=re.compile(r"Layer")
def lines(p, swap):
    out=[]
    for l in open(p,encoding="utf-8",errors="replace"):
        if l.startswith(("PASSED","FAILED")):
            out.append(r2.sub("Lane",r1.sub("lane",l)) if swap else l)
    return sorted(out)
for f in sorted(os.listdir(os.path.join(S,"after-out"))):
    if "__" not in f: continue
    b=os.path.join(S,"baseline-out",f); a=os.path.join(S,"after-out",f)
    if not os.path.exists(b): continue
    lb=lines(b,True); la=lines(a,False)
    if not la and not lb: print(f"{f:<52} no sentences (rc only)"); continue
    same = lb==la
    print(f"{f:<52} head {len(lb):>4}  renamed {len(la):>4}  {'SAME' if same else 'DIFFERENT'}")
    if not same:
        sb,sa=set(lb),set(la)
        for x in list(sb-sa)[:3]: print("   only in head:   ", x.strip()[:200])
        for x in list(sa-sb)[:3]: print("   only in renamed:", x.strip()[:200])
