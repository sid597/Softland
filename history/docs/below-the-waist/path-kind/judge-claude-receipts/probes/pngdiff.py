import sys, numpy as np
from PIL import Image
a=np.asarray(Image.open(sys.argv[1]).convert("RGBA")).astype(int)
b=np.asarray(Image.open(sys.argv[2]).convert("RGBA")).astype(int)
if a.shape!=b.shape: print("shape differs",a.shape,b.shape); sys.exit(0)
d=np.abs(a-b)
n=(d.sum(axis=2)>0).sum()
print(f"{sys.argv[1].split('/')[-1]}: {n} of {a.shape[0]*a.shape[1]} px differ; max byte delta {d.max()}; mean over differing {d.sum()/max(n,1):.2f}")
if n and len(sys.argv)>3:
    ys,xs=np.nonzero(d.sum(axis=2)>0)
    for y,x in list(zip(ys,xs))[:int(sys.argv[3])]:
        print("  ",(int(x),int(y)),"golden",a[y,x].tolist(),"now",b[y,x].tolist())
