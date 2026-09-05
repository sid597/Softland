# The markdown twin of path-kind.html: python3 twin-10.py path-kind.html path-kind.md  (session 10; headings, lists, tables, pre blocks, dl; figures become a pointer line)
import sys, re, html
from html.parser import HTMLParser
class Twin(HTMLParser):
    def __init__(s):
        super().__init__(); s.out=[]; s.buf=[]; s.stack=[]; s.skip=0; s.pre=False; s.row=[]; s.cell=None; s.table=[]; s.intable=False; s.list=[]; s.fig=0
    def flush_para(s, prefix=''):
        t=''.join(s.buf); s.buf=[]
        if not s.pre: t=re.sub(r'\s+',' ',t).strip()
        if t: s.out.append(prefix+t+'\n')
    def handle_starttag(s, tag, attrs):
        a=dict(attrs)
        if tag=='link': return
        if tag in ('style','script','svg','title'): s.skip+=1; return
        if s.skip: return
        s.stack.append(tag)
        if tag in ('h1','h2','h3'): s.flush_para(); 
        elif tag=='p': s.flush_para()
        elif tag=='pre': s.flush_para(); s.pre=True
        elif tag=='figure': s.flush_para(); s.fig+=1; s.out.append('\n[FIGURE: see path-kind.html for the drawing]\n')
        elif tag=='figcaption': s.flush_para(); s.buf.append('Caption: ')
        elif tag in ('ul','ol'): s.flush_para(); s.list.append(tag)
        elif tag=='li': s.flush_para(); s.buf.append('- ')
        elif tag=='dt': s.flush_para(); s.buf.append('**')
        elif tag=='dd': s.flush_para()
        elif tag=='table': s.flush_para(); s.intable=True; s.table=[]
        elif tag=='tr': s.row=[]
        elif tag in ('td','th'): s.cell=[]
        elif tag=='b' and not s.intable: s.buf.append('**')
        elif tag=='code': (s.cell if s.cell is not None else s.buf).append('`')
        elif tag=='span' and 'tag' in (a.get('class') or ''): (s.cell if s.cell is not None else s.buf).append('[')
        elif tag=='br': (s.cell if s.cell is not None else s.buf).append(' ')
        elif tag=='div' and 'foot' in (a.get('class') or ''): s.flush_para(); s.out.append('\n---\n')
    def handle_endtag(s, tag):
        if tag=='link': return
        if tag in ('style','script','svg','title'): s.skip-=1; return
        if s.skip: return
        if s.stack and s.stack[-1]==tag: s.stack.pop()
        if tag=='h1': s.flush_para('\n# ')
        elif tag=='h2': s.flush_para('\n## ')
        elif tag=='h3': s.flush_para('\n### ')
        elif tag=='p': s.flush_para(); s.out.append('\n')
        elif tag=='pre': t=''.join(s.buf); s.buf=[]; s.out.append('```\n'+html.unescape(t).strip('\n')+'\n```\n\n'); s.pre=False
        elif tag=='figcaption': s.flush_para(); s.out.append('\n')
        elif tag=='li': s.flush_para(); s.out.append('\n')
        elif tag in ('ul','ol'): s.list.pop()
        elif tag=='dt': s.buf.append('**'); s.flush_para(); s.out.append('\n')
        elif tag=='dd': s.flush_para(); s.out.append('\n')
        elif tag in ('td','th'): s.row.append(re.sub(r'\s+',' ',''.join(s.cell)).strip().replace('|','\\|')); s.cell=None
        elif tag=='tr': s.table.append(s.row)
        elif tag=='table':
            if s.table:
                s.out.append('| '+' | '.join(s.table[0])+' |\n|'+'---|'*len(s.table[0])+'\n')
                for r in s.table[1:]: s.out.append('| '+' | '.join(r)+' |\n')
                s.out.append('\n')
            s.intable=False
        elif tag=='b' and not s.intable: s.buf.append('**')
        elif tag=='code': (s.cell if s.cell is not None else s.buf).append('`')
        elif tag=='span' and s.stack and False: pass
    def handle_data(s, d):
        if s.skip: return
        if s.cell is not None: s.cell.append(d)
        else: s.buf.append(d)
    def handle_startendtag(s, tag, attrs): s.handle_starttag(tag, attrs); s.handle_endtag(tag)
src=open(sys.argv[1]).read()
# render tag spans as [tag] with a trailing space
src=re.sub(r'<span class="tag [a-z]+">([^<]*)</span>', r'[\1] ', src)
t=Twin(); t.feed(src); t.flush_para()
body=''.join(t.out)
body=re.sub(r'\n{3,}', '\n\n', body)
head='Markdown twin of path-kind.html, generated from the page; the three drawings live in the html. Landed 2026-09-05, regenerated 2026-09-06 (session 10).\n\n'
open(sys.argv[2],'w').write(head+body.strip()+'\n')
print(len(body))
