# Ground check part 2: reads, from stdin, the "actual:" line of the clojure.test FAIL
# report of the check "through three layers" (got by grep from a raw run file), and says
# which of the check's four parts differ between expected and observed. A run, not code.
import sys
s=sys.stdin.read().strip()
s=s[s.index("(= ")+3:]
def top(s,i):
    d=0;j=i
    while True:
        c=s[j]
        if c in "[{(": d+=1
        elif c in "]})": d-=1
        elif c=="\"":
            j+=1
            while s[j]!="\"": j+= 2 if s[j]=="\\" else 1
        j+=1
        if d==0: return s[i:j],j
a,j=top(s,0); b,_=top(s,j+1)
def parts(v):
    v=v[1:-1]; out=[];i=0
    while i<len(v):
        if v[i]==" ": i+=1; continue
        if v[i] in "[{#(":
            k=i+1 if v[i]=="#" else i
            p,e=top(v,k); out.append(v[i:e]); i=e
        else:
            e=v.find(" ",i); e=len(v) if e<0 else e; out.append(v[i:e]); i=e
    return out
pa,pb=parts(a),parts(b)
print("parts:",len(pa),len(pb),"| equal by part:",[x==y for x,y in zip(pa,pb)],"| second part, expected then observed:",pa[1],pb[1])
