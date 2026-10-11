"""The check's own tool for its fixes of PLAN-read-path.md: each fix is one
replacement of text that must be in the plan exactly once. Nothing of the
store is here. The fixes themselves are in PLAN_VALIDATION-read-path.md."""
import sys
PLAN = '/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md'

class Plan:
    def __init__(self):
        self.s = open(PLAN).read()
        self.n = 0
    def swap(self, old, new):
        c = self.s.count(old)
        if c != 1:
            sys.exit('NOT ONCE (%d): %r' % (c, old[:90]))
        self.s = self.s.replace(old, new)
        self.n += 1
    def between(self, start, end, new):
        """Replaces from the start of `start` up to, not with, `end`."""
        if self.s.count(start) != 1:
            sys.exit('START NOT ONCE (%d): %r' % (self.s.count(start), start[:90]))
        i = self.s.index(start)
        j = self.s.find(end, i + len(start))
        if j < 0:
            sys.exit('NO END: %r' % end[:90])
        self.s = self.s[:i] + new + self.s[j:]
        self.n += 1
    def save(self):
        open(PLAN, 'w').write(self.s)
        print('fixes applied:', self.n)
