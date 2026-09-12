#!/usr/bin/env python3
"""Generate the Markdown twin of a 3D-kind page (3d-kind-2.html → 3d-kind-2.md).

Usage: python3 twin-2.py 3d-kind-2.html 3d-kind-2.md "session 2, folded"
The drawings (svg) are replaced by a pointer line; captions are kept. Tables become pipe tables;
dl becomes bold terms; the moment grid becomes "**term** — text"; pre becomes a code fence.
"""
import re, sys
from html.parser import HTMLParser


class Twin(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.out = []          # finished blocks
        self.buf = []          # current inline text
        self.stack = []        # open tags
        self.skip = 0          # depth inside svg/style/script
        self.list_stack = []   # ('ul'|'ol', counter)
        self.table = None      # rows being built
        self.row = None
        self.in_pre = False
        self.moment_term = None

    # -- helpers
    def text(self):
        t = ''.join(self.buf); self.buf = []
        return t if self.in_pre else re.sub(r'[ \t\n]+', ' ', t).strip()

    def flush(self, block):
        if block:
            self.out.append(block)

    def cls(self, attrs):
        return dict(attrs).get('class', '') or ''

    # -- parser hooks
    def handle_starttag(self, tag, attrs):
        if tag in ('svg', 'style', 'script', 'defs'):
            self.skip += 1; return
        if self.skip: return
        c = self.cls(attrs)
        if tag == 'pre':
            self.flush(self.text()); self.in_pre = True
        elif tag in ('h1', 'h2', 'h3'):
            self.flush(self.text())
        elif tag == 'p':
            self.flush(self.text())
        elif tag == 'figure':
            self.flush(self.text()); self.out.append('[FIGURE: see the html for the drawing]')
        elif tag == 'figcaption':
            self.flush(self.text())
        elif tag in ('ul', 'ol'):
            self.flush(self.text()); self.list_stack.append([tag, 0])
        elif tag == 'li':
            self.flush(self.text())
        elif tag == 'dl':
            self.flush(self.text())
        elif tag in ('dt', 'dd'):
            self.flush(self.text())
        elif tag == 'table':
            self.flush(self.text()); self.table = []
        elif tag == 'tr':
            self.row = []
        elif tag in ('td', 'th'):
            self.buf = []
        elif tag == 'div' and 'moment' in c:
            self.flush(self.text()); self.moment_term = 'grid'
        elif tag == 'b' and self.moment_term == 'grid':
            self.flush(self.text()); self.moment_term = 'term'
        elif tag == 'div' and self.moment_term in ('term', 'text'):
            self.moment_term = 'text'
        elif tag == 'br':
            self.buf.append('\n')
        elif tag == 'span' and ('tag' in c.split()):
            self.buf.append('[')
        elif tag in ('b', 'strong') and not self.in_pre and self.table is None:
            self.buf.append('**')
        elif tag in ('i', 'em'):
            self.buf.append('*')
        elif tag == 'code' and not self.in_pre:
            self.buf.append('`')
        self.stack.append((tag, c))

    def handle_endtag(self, tag):
        if tag in ('svg', 'style', 'script', 'defs'):
            self.skip -= 1; return
        if self.skip: return
        c = ''
        for i in range(len(self.stack) - 1, -1, -1):
            if self.stack[i][0] == tag:
                c = self.stack[i][1]; del self.stack[i]; break
        if tag == 'pre':
            t = ''.join(self.buf).strip('\n'); self.buf = []; self.in_pre = False
            self.out.append('```\n' + t + '\n```')
        elif tag == 'h1': self.out.append('# ' + self.text())
        elif tag == 'h2': self.out.append('## ' + self.text())
        elif tag == 'h3': self.out.append('### ' + self.text())
        elif tag == 'p': self.flush(self.text())
        elif tag == 'figcaption':
            self.out.append('Caption: ' + self.text())
        elif tag == 'li':
            kind, n = self.list_stack[-1] if self.list_stack else ('ul', 0)
            t = self.text()
            if kind == 'ol':
                self.list_stack[-1][1] += 1; self.out.append('%d. %s' % (self.list_stack[-1][1], t))
            else:
                self.out.append('- ' + t)
        elif tag in ('ul', 'ol'):
            if self.list_stack: self.list_stack.pop()
        elif tag == 'dt': self.out.append('**' + self.text() + '**')
        elif tag == 'dd': self.flush(self.text())
        elif tag in ('td', 'th'):
            if self.row is not None: self.row.append(self.text().replace('|', '\\|'))
        elif tag == 'tr':
            if self.table is not None and self.row: self.table.append(self.row)
            self.row = None
        elif tag == 'table':
            rows = self.table or []; self.table = None
            if rows:
                lines = ['| ' + ' | '.join(rows[0]) + ' |', '|' + '---|' * len(rows[0])]
                lines += ['| ' + ' | '.join(r) + ' |' for r in rows[1:]]
                self.out.append('\n'.join(lines))
        elif tag == 'b' and self.moment_term == 'term':
            self.moment_buf = self.text()
        elif tag == 'div' and self.moment_term == 'text':
            self.out.append('**' + getattr(self, 'moment_buf', '') + '** — ' + self.text()); self.moment_term = 'grid'
        elif tag == 'div' and self.moment_term == 'grid' and 'moment' in c:
            self.moment_term = None
        elif tag == 'span' and ('tag' in c.split()):
            self.buf.append('] ')
        elif tag in ('b', 'strong') and not self.in_pre and self.table is None and self.moment_term != 'term':
            self.buf.append('**')
        elif tag in ('i', 'em'):
            self.buf.append('*')
        elif tag == 'code' and not self.in_pre:
            self.buf.append('`')
        elif tag == 'div' and ('fence' in c.split() or 'eyebrow' in c.split() or 'foot' in c.split()):
            self.flush(self.text())

    def handle_data(self, data):
        if self.skip: return
        if self.in_pre: self.buf.append(data); return
        self.buf.append(data)


def main():
    src, dst, label = sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else ''
    html = open(src, encoding='utf-8').read()
    m = re.search(r'<title>(.*?)</title>', html)
    body = html[m.end():] if m else html
    t = Twin(); t.feed(body); t.flush(t.text())
    head = 'Markdown twin of %s, generated from the page by twin-2.py; the drawings live in the html. %s' % (src.split('/')[-1], label)
    text = head + '\n\n' + '\n\n'.join(b for b in t.out if b.strip()) + '\n'
    text = text.replace('** **', ' ').replace('****', '')
    open(dst, 'w', encoding='utf-8').write(text)
    print('wrote', dst, len(text), 'bytes')


if __name__ == '__main__':
    main()
