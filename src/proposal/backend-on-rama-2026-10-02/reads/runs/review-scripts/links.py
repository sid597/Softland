# Review helper: every link of every map of the store's two trees, and whether it leads to a file.
# From the repository root:  python3 <this file>
import re, os
bad=[]; n=0
for root in ['src-store','test-store']:
    for d,_,fs in os.walk(root):
        for f in fs:
            if not f.endswith('.md'): continue
            p=os.path.join(d,f); s=open(p,encoding='utf-8').read()
            for m in re.finditer(r'\]\(([^)]+)\)', s):
                link=m.group(1)
                if link.startswith('http') or link.startswith('#'): continue
                t=link.split('#')[0]
                if not t: continue
                n+=1
                if not os.path.exists(os.path.normpath(os.path.join(d,t))): bad.append((p,link))
print('links in the maps of both trees:', n, '| that lead nowhere:', len(bad))
for b in bad: print('  ', b)
