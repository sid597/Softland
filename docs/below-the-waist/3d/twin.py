#!/usr/bin/env python3
"""Generate the Markdown twin of a 3D-kind page (3d-kind.html → 3d-kind.md).

Usage: python3 twin.py 3d-kind.html > 3d-kind.md

The twin is for reading and diffing; the drawings live only in the html. The
mapping: h1/h2/h3 → #/##/###, .tag spans → [bracketed], p → paragraph, dl →
**dt** then dd lines, tables → pipe tables, ol → numbered (the lived list's
.model/.today spans → " — " lines), ul → bullets, pre → fenced, .words →
bullets separated by a blank line, figure → a placeholder plus its caption,
<b> → **bold**, <code> → `code`, <sub> → _sub, <a> → its text.
"""
import re
import sys
from html.parser import HTMLParser

PREAMBLE = ("Markdown twin of 3d-kind.html, generated from the page; the three "
            "drawings live in the html. Landed 2026-09-06 (session 1, the Claude lane).")


class Twin(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.out = []          # finished blocks
        self.stack = []        # open tags: (tag, attrs)
        self.buf = None        # current inline buffer (list of str) or None
        self.table = None      # rows being collected
        self.row = None
        self.cell = None
        self.list_stack = []   # ('ol'|'ul', counter)
        self.in_pre = False
        self.in_svg = False
        self.in_style = False
        self.li_parts = None   # for lived items: [main, model, today]
        self.dd_pending = False

    # ---- helpers
    def cls(self, attrs):
        d = dict(attrs)
        return (d.get('class') or '').split()

    def text(self, s):
        if self.in_style or self.in_svg:
            return
        if self.in_pre:
            self.buf.append(s)
            return
        if self.buf is not None:
            self.buf.append(s)
        elif self.cell is not None:
            self.cell.append(s)

    def flush_inline(self):
        if self.buf is None:
            return ''
        s = ''.join(self.buf)
        self.buf = None
        return norm(s)

    def emit(self, block, kind='p'):
        self.out.append((kind, block))

    # ---- tags
    def handle_starttag(self, tag, attrs):
        c = self.cls(attrs)
        self.stack.append((tag, attrs))
        if tag == 'style':
            self.in_style = True
            return
        if tag == 'svg':
            self.in_svg = True
            return
        if self.in_svg or self.in_style:
            return
        if tag in ('h1', 'h2', 'h3'):
            self.buf = []
        elif tag == 'p':
            self.buf = []
        elif tag == 'div' and 'eyebrow' in c:
            self.buf = []
        elif tag == 'div' and 'fence' in c:
            self.buf = []
            self.fence_spans = []
        elif tag == 'div' and 'words' in c:
            self.words = []
        elif tag == 'div' and 'foot' in c:
            pass
        elif tag == 'span' and self.stack[-2][0] == 'div' and 'fence' in self.cls(self.stack[-2][1]):
            self.buf = []
        elif tag == 'span' and self.stack[-2][0] == 'div' and 'words' in self.cls(self.stack[-2][1]):
            self.buf = []
            self.word_is_key = 'w' in c
        elif tag == 'span' and 'tag' in c:
            self.text('[')
        elif tag == 'span' and ('model' in c or 'today' in c):
            lead = norm(''.join(self.buf))
            if lead:
                self.li_parts.append(lead)
            self.buf = []
        elif tag == 'sub':
            self.text('_')
        elif tag == 'b':
            self.text('**')
        elif tag == 'code' and not self.in_pre:
            self.text('`')
        elif tag == 'pre':
            self.in_pre = True
            self.buf = []
        elif tag == 'dl':
            pass
        elif tag == 'dt':
            self.buf = []
        elif tag == 'dd':
            self.buf = []
        elif tag == 'table':
            self.table = []
        elif tag == 'tr':
            self.row = []
        elif tag in ('th', 'td'):
            self.cell = []
        elif tag in ('ol', 'ul'):
            self.list_stack.append([tag, 0, c])
        elif tag == 'li':
            self.list_stack[-1][1] += 1
            self.buf = []
            self.li_parts = []
        elif tag == 'figure':
            pass
        elif tag == 'figcaption':
            self.buf = []

    def handle_endtag(self, tag):
        if tag == 'style':
            self.in_style = False
            self.stack.pop()
            return
        if tag == 'svg':
            self.in_svg = False
            self.emit('[FIGURE: see 3d-kind.html for the drawing]', 'fig')
            self.stack.pop()
            return
        if self.in_svg or self.in_style:
            self.stack.pop()
            return
        c = self.cls(self.stack[-1][1]) if self.stack else []
        parent = self.stack[-2] if len(self.stack) > 1 else ('', [])
        if tag == 'h1':
            self.emit('# ' + self.flush_inline(), 'h')
        elif tag == 'h2':
            self.emit('## ' + self.flush_inline(), 'h')
        elif tag == 'h3':
            self.emit('### ' + self.flush_inline(), 'h')
        elif tag == 'p':
            self.emit(self.flush_inline())
        elif tag == 'div' and 'eyebrow' in c:
            self.emit(self.flush_inline())
        elif tag == 'div' and 'fence' in c:
            self.buf = None
            self.emit('\n '.join(self.fence_spans))
        elif tag == 'div' and 'words' in c:
            self.emit('\n \n'.join(self.words))
        elif tag == 'span' and parent[0] == 'div' and 'fence' in self.cls(parent[1]):
            self.fence_spans.append(self.flush_inline())
        elif tag == 'span' and parent[0] == 'div' and 'words' in self.cls(parent[1]):
            s = self.flush_inline()
            if self.word_is_key:
                self.words.append('- **' + s + '**')
            else:
                self.words[-1] += ' ' + s
        elif tag == 'span' and 'tag' in c:
            self.text('] ')
        elif tag == 'span' and ('model' in c or 'today' in c):
            self.li_parts.append(' — ' + norm(''.join(self.buf)))
            self.buf = []
        elif tag == 'b':
            self.text('**')
        elif tag == 'code' and not self.in_pre:
            self.text('`')
        elif tag == 'pre':
            body = ''.join(self.buf)
            self.buf = None
            self.in_pre = False
            self.emit('```\n' + body + '\n```')
        elif tag == 'dt':
            self.emit('**' + self.flush_inline() + '**', 'dt')
        elif tag == 'dd':
            self.emit(self.flush_inline(), 'dd')
        elif tag in ('th', 'td'):
            self.row.append(norm(''.join(self.cell)))
            self.cell = None
        elif tag == 'tr':
            self.table.append(self.row)
            self.row = None
        elif tag == 'table':
            rows = self.table
            self.table = None
            lines = ['| ' + ' | '.join(rows[0]) + ' |', '|' + '---|' * len(rows[0])]
            for r in rows[1:]:
                lines.append('| ' + ' | '.join(r) + ' |')
            self.emit('\n'.join(lines))
        elif tag == 'li':
            kind, n, lc = self.list_stack[-1]
            tail = norm(''.join(self.buf))
            self.buf = None
            parts = [p for p in self.li_parts + [tail] if p]
            self.li_parts = None
            prefix = f'{n}. ' if kind == 'ol' else '- '
            self.emit(prefix + '\n'.join(parts), 'li')
        elif tag in ('ol', 'ul'):
            self.list_stack.pop()
        elif tag == 'figcaption':
            self.emit('Caption: ' + self.flush_inline(), 'cap')
        self.stack.pop()

    def handle_data(self, data):
        self.text(data)


def norm(s):
    s = s.replace('\n', ' ')
    s = re.sub(r'[ \t]+', ' ', s)
    return s.strip()


def main(path):
    html = open(path, encoding='utf-8').read()
    t = Twin()
    t.feed(html)
    blocks = [('p', PREAMBLE)] + t.out
    text = ''
    prev = None
    for kind, b in blocks:
        if prev is not None:
            tight = (prev == 'h' or (prev in ('dt', 'dd') and kind in ('dt', 'dd'))
                     or (prev == 'li' and kind == 'li') or (prev == 'fig' and kind == 'cap'))
            text += '\n' if tight else '\n\n'
        text += b
        prev = kind
    sys.stdout.write(text + '\n\n')


if __name__ == '__main__':
    main(sys.argv[1])
