#!/usr/bin/env python3
"""A second model, as small as crossing.py: one live read of ONE PLACE through
two layers, the nearer of which has a setting and, for this asker, a
permission or none. It plays the steps of "Live" in every order, to settle
when a step owes the whole answer. Python 3 alone:

    python3 model/standing.py            a summary line for each way of owing
    python3 model/standing.py show       also the first failing order of each

The nearer layer N: a setting, "everyone" or "members" (the push S1); the
asker's permission row or none (the push S2, held only while the setting is
"members"); a fact at the place (the push FN, held while she may see N, and
kept while her standing is not known yet). The farther layer F: always seen;
a fact at the place (the push FF).

The switch `owes`, the rule by which a step owes the whole answer:
  'by-kind'    the plan as worded: no mark. A step of S1 or S2 whose meaning
               differs gives {:anew} when nothing is missing; else the arrival
               of a source that is no check gives it when nothing is missing
  'standing'   a mark; owed when the standing differs before and after
  'setting'    a mark; owed when the setting, or whether her permission is
               there, differs before and after
  'any-step'   a mark; owed as 'setting', and also by any step that joined or
               left a source: the plan as the check leaves it
"""
import sys
import itertools
from collections import deque

WAIT = 'WAIT'


def freeze(x):
    if isinstance(x, dict):
        return tuple(sorted((k, freeze(v)) for k, v in x.items()))
    if isinstance(x, (list, tuple)):
        return tuple(freeze(v) for v in x)
    return x


class World:
    def __init__(self, setting, perm, writes, owes):
        self.t = {'S1': setting, 'S2': perm, 'FN': None, 'FF': ('f0', 1)}
        self.writes = list(writes)
        self.wi = 0
        self.owes = owes
        self.proxy, self.queue, self.pend, self.phase = {}, {}, {}, {}
        self.owed = False
        self.screen = None
        for s in ('S1', 'FF'):
            self._open(s)
        for s in self.wanted():
            if s not in self.phase:
                self._open(s)
        for s in self.wanted():
            if s not in self.phase:
                self._open(s)
        self.screen = self.whole()

    def _open(self, s):
        self.proxy[s] = self.t[s]
        self.queue[s], self.pend[s], self.phase[s] = [], [], 'given'

    def clone(self):
        w = World.__new__(World)
        w.t = dict(self.t)
        w.writes, w.wi, w.owes = self.writes, self.wi, self.owes
        w.proxy = dict(self.proxy)
        w.queue = {s: list(q) for s, q in self.queue.items()}
        w.pend = {s: list(q) for s, q in self.pend.items()}
        w.phase = dict(self.phase)
        w.owed, w.screen = self.owed, self.screen
        return w

    def key(self):
        return freeze((self.wi, self.t, self.proxy, self.queue, self.pend, self.phase, self.owed, self.screen))

    def has(self, s):
        return self.phase.get(s) in ('served', 'given')

    # ------------------------------------------------------------ pure core
    def standing(self, s1=None, s2='?'):
        setting = self.proxy['S1'] if s1 is None else s1
        if setting == 'everyone':
            return 'yes'
        if s2 == '?':
            if not self.has('S2'):
                return 'not-known-yet'
            s2 = self.proxy['S2']
        return 'yes' if s2 is not None else 'may-not-see'

    def wanted(self):
        w = {'S1', 'FF'}
        if self.proxy['S1'] == 'members':
            w.add('S2')
        st = self.standing()
        if st == 'yes' or (st == 'not-known-yet' and 'FN' in self.phase):
            w.add('FN')
        return w

    def missing(self):
        return [s for s in self.wanted() if not self.has(s)]

    def answer(self):
        st = self.standing()
        if st == 'not-known-yet':
            return WAIT
        if st == 'yes':
            if not self.has('FN'):
                return WAIT
            if self.proxy['FN'] is not None:
                return ('N',) + self.proxy['FN']
        return ('F',) + self.proxy['FF']

    def whole(self):
        a = self.answer()
        assert a != WAIT
        return (self.standing() == 'yes', a)

    def reading(self):
        wanted = self.wanted()
        moved = False
        for s in list(self.phase):
            if s not in wanted:
                for d in (self.proxy, self.queue, self.pend, self.phase):
                    d.pop(s, None)
                moved = True
        for s in wanted:
            if s not in self.phase:
                self.phase[s], self.queue[s], self.pend[s] = 'asked', [], []
                moved = True
        return moved

    def anew_if_whole(self):
        if not self.missing():
            self.screen = self.whole()
            self.owed = False

    # --------------------------------------------------------------- events
    def events(self):
        ev = []
        if self.wi < len(self.writes):
            ev.append(('W',))
        for s in sorted(self.phase):
            ph = self.phase[s]
            if ph == 'asked':
                ev.append(('S', s))
            elif ph == 'served':
                ev.append(('A', s))
            if ph in ('served', 'given') and self.queue[s]:
                ev.append(('U', s))
            if ph in ('served', 'given') and self.pend[s]:
                ev.append(('H', s))
        return ev

    def do(self, e):
        if e[0] == 'W':
            s, v = self.writes[self.wi]
            self.wi += 1
            self.t[s] = (v, 100 + self.wi) if s in ('FN', 'FF') else v
            if s in self.phase and self.phase[s] in ('served', 'given'):
                self.queue[s].append(self.t[s])
            return
        s = e[1]
        if e[0] == 'S':
            self.proxy[s] = self.t[s]
            self.phase[s] = 'served'
        elif e[0] == 'U':
            new = self.queue[s].pop(0)
            self.pend[s].append((self.proxy[s], new))
            self.proxy[s] = new
        elif e[0] == 'A':
            self.phase[s] = 'given'
            moved = self.reading()
            if self.owes == 'by-kind':
                if not self.missing():
                    self.screen = self.whole()
                return
            if self.owes == 'any-step' and moved:
                self.owed = True
            if self.owed:
                self.anew_if_whole()
        elif e[0] == 'H':
            old, new = self.pend[s].pop(0)
            if old == new:
                return
            differs = False
            if s == 'S1':
                if self.owes == 'standing':
                    differs = self.standing(s1=old) != self.standing(s1=new)
                else:
                    differs = True
            elif s == 'S2':
                differs = True
            moved = self.reading()
            if self.owes == 'by-kind':
                if differs:
                    if not self.missing():
                        self.screen = self.whole()
                    return
            else:
                if differs or (self.owes == 'any-step' and moved):
                    self.owed = True
                if self.owed:
                    self.anew_if_whole()
                    return
            if s in ('S1', 'S2'):
                return
            # a fact source: told-by?
            st = self.standing()
            if s == 'FN' and st != 'yes':
                return
            if s == 'FF' and st == 'yes' and self.has('FN') and self.proxy['FN'] is not None:
                return
            a = self.answer()
            if a != WAIT:
                self.screen = (self.screen[0], a)

    def truth(self):
        yes = self.t['S1'] == 'everyone' or self.t['S2'] is not None
        if yes and self.t['FN'] is not None:
            return (True, ('N',) + self.t['FN'])
        return (yes, ('F',) + self.t['FF'])


def explore(make):
    start = make()
    seen = {start.key()}
    todo = deque([(start, ())])
    fail = None
    while todo:
        w, path = todo.popleft()
        ev = w.events()
        if not ev:
            if w.screen != w.truth() and fail is None:
                fail = (path, w.screen, w.truth())
            continue
        for e in ev:
            n = w.clone()
            n.do(e)
            k = n.key()
            if k not in seen:
                seen.add(k)
                todo.append((n, path + (e,)))
    return len(seen), fail


WRITES = [('S1', 'members'), ('S1', 'everyone'), ('S2', 'P'), ('FN', 'n'), ('FF', 'f')]


def main():
    show = len(sys.argv) > 1 and sys.argv[1] == 'show'
    for owes in ('by-kind', 'standing', 'setting', 'any-step'):
        lists = states = 0
        bad = []
        for setting in ('everyone', 'members'):
            for perm in (None, 'P'):
                for ln in (1, 2, 3):
                    for seq in itertools.product(WRITES, repeat=ln):
                        lists += 1
                        st, fail = explore(lambda: World(setting, perm, seq, owes))
                        states += st
                        if fail:
                            bad.append((setting, perm, seq, fail))
        print('owes=%-9s %d lists of up to 3 writes, %d states, %d lists with a failing order' % (owes, lists, states, len(bad)))
        if bad and show:
            setting, perm, seq, (path, screen, truth) = bad[0]
            print('     the first: setting=%s permission=%s writes=%s' % (setting, perm, seq))
            print('     a failing order: ' + ' '.join(e[0] + ('' if len(e) == 1 else e[1]) for e in path))
            print('     the screen at the end (may see N, the place): %s   the tables give: %s' % (screen, truth))


if __name__ == '__main__':
    main()
