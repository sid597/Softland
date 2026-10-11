#!/usr/bin/env python3
"""A model, not a design: the steps of a callback of "Live" in PLAN-read-path.md,
for one live read of "what points at a thing V" through a list of layers,
played in EVERY order in which Rama's client could deliver.

It is the check's own, written to settle whether "no put-together value need
be kept" holds. No form or name of the store comes from it. Python 3 alone:

    python3 model/crossing.py            every scenario, a summary line each
    python3 model/crossing.py show       also the shortest failing order of each

What is modelled
  - Layers 1..k, 1 the nearest. Each has a facts table (place -> row) and,
    when it marks the key, an order: the places whose current fact has the
    value V. A write in a layer changes both, as two pushes with two queues.
  - The read holds one push for each order of a layer that it may see, and
    one push for each check (ruling 12): the facts row of a nearer layer at
    a place that a farther order gives.
  - Rama's client: for each push, its changes in order; between two pushes,
    no order. For one change two moments are apart: the push takes the new
    value (U), and the callback is handled under the read's lock (H). A push
    that the read asks for is served at some moment (S) with the table as it
    is then, and its arrival is handled at some later moment (A). A push
    that the read left gives it nothing more. (Run C1 of this folder's probe:
    a push holds the new value when its callback runs.)
  - The asker's screen: `:now`, then each `{:changed ..}` put in, each
    `{:anew ..}` in the place of everything.
  - The truth at the end: what a read asked once gives from the tables, by
    ruling 64: at a place that some order gives, the nearest layer that holds
    a row wins, and the place is given when the winner's value is V.

Four switches, so that the plan as worded and the plan as fixed are both played
  told     'before-leave'  told-by? looks at the values gathered before this
                           step left the sources that are no longer wanted
           'after-leave'   it looks only at sources that are still wanted
  arrival  'by-kind'       a check that arrives gives {:changed}; any other
                           source gives {:anew} when nothing is missing
           'owed'          the read holds one mark, "the whole answer is
                           owed"; it is set when what shapes the read changed
                           and cleared when {:anew} is given
  wait     'lazy'          a place waits only for a source that the walk
                           from the nearest layer reaches before it stops
           'strict'        a place waits while any wanted source at it is
                           missing
  checks   'plan'          a check in each nearer layer of each order that
                           gives the place, unless its own order holds it
           'tight'         a check only in layers nearer than the nearest
                           order that gives the place
"""
import sys
from collections import deque

V = 'V'
WAIT = 'WAIT'


def freeze(x):
    if isinstance(x, dict):
        return tuple(sorted((k, freeze(v)) for k, v in x.items()))
    if isinstance(x, (set, frozenset)):
        return tuple(sorted(x))
    if isinstance(x, (list, tuple)):
        return tuple(freeze(v) for v in x)
    return x


class World:
    """One state of the tables, of Rama's client, and of the read."""

    def __init__(self, k, marks, facts, visible, writes, cfg):
        self.k = k
        self.marks = marks                    # {layer: bool}
        self.facts = dict(facts)              # {(layer, place): (value, version)}
        self.orders = {i: {} for i in range(1, k + 1)}   # {layer: {place: version}}
        for (i, p), (val, ver) in self.facts.items():
            if marks[i] and val == V:
                self.orders[i][p] = ver
        self.writes = list(writes)
        self.wi = 0
        self.cfg = cfg
        self.visible = set(visible)
        # Rama's client, per source that the read holds
        self.proxy = {}       # source -> value that the push holds
        self.queue = {}       # source -> [new value, ...] not yet taken by the push
        self.pend = {}        # source -> [(old, new), ...] callbacks not yet handled
        self.phase = {}       # source -> 'asked' | 'served' | 'given'
        self.screen = {}
        self.owed = False
        self.told = 0
        self.see = []         # callbacks of a permission's push, not yet handled
        # the opening: everything wanted is given, and `:now` is the whole answer
        for i in sorted(self.visible):
            if marks[i]:
                self._open_now(('O', i))
        for s in self.wanted_checks():
            self._open_now(s)
        self.screen = self.whole()

    # ---------------------------------------------------------------- tables
    def table_value(self, s):
        if s[0] == 'O':
            return dict(self.orders[s[1]])
        return self.facts.get((s[1], s[2]))

    def _open_now(self, s):
        self.proxy[s] = self.table_value(s)
        self.queue[s] = []
        self.pend[s] = []
        self.phase[s] = 'given'

    def clone(self):
        w = World.__new__(World)
        w.k, w.marks, w.cfg, w.writes, w.wi = self.k, self.marks, self.cfg, self.writes, self.wi
        w.facts = dict(self.facts)
        w.orders = {i: dict(o) for i, o in self.orders.items()}
        w.visible = set(self.visible)
        w.proxy = {s: (dict(v) if isinstance(v, dict) else v) for s, v in self.proxy.items()}
        w.queue = {s: list(q) for s, q in self.queue.items()}
        w.pend = {s: list(q) for s, q in self.pend.items()}
        w.phase = dict(self.phase)
        w.screen = dict(self.screen)
        w.owed = self.owed
        w.told = self.told
        w.see = list(self.see)
        return w

    def key(self):
        return freeze((self.wi, self.facts, self.orders, self.visible, self.proxy, self.queue,
                       self.pend, self.phase, self.screen, self.owed, self.see))

    # ------------------------------------------------------- the pure core
    def has(self, s):
        """The push of s is given: its future is done, whether or not its arrival was handled."""
        return self.phase.get(s) in ('served', 'given')

    def known_orders(self):
        return {i: self.proxy[('O', i)] for i in self.visible
                if self.marks[i] and self.has(('O', i))}

    def wanted_checks(self):
        ko = self.known_orders()
        out = set()
        places = set()
        for o in ko.values():
            places |= set(o)
        for p in places:
            holders = sorted(i for i, o in ko.items() if p in o)
            if self.cfg['checks'] == 'tight':
                for i in sorted(self.visible):
                    if i < holders[0]:
                        out.add(('C', i, p))
            else:
                for j in holders:
                    for i in sorted(self.visible):
                        if i < j and i not in holders:
                            out.add(('C', i, p))
        return out

    def wanted(self):
        w = {('O', i) for i in self.visible if self.marks[i]}
        return w | self.wanted_checks()

    def answer(self, p):
        """What the read gives at p now, from what the pushes hold: a triple, None
        for a place that is not given, or WAIT."""
        wanted = self.wanted()
        if self.cfg['wait'] == 'strict':
            for s in wanted:
                if (s[0] == 'C' and s[2] == p) and not self.has(s):
                    return WAIT
        for i in sorted(self.visible):
            if self.marks[i]:
                if not self.has(('O', i)):
                    return WAIT
                o = self.proxy[('O', i)]
                if p in o:
                    return (i, o[p], 'order')
            c = ('C', i, p)
            if c in wanted:
                if not self.has(c):
                    return WAIT
                row = self.proxy[c]
                if row is not None:
                    return (i, row[1], 'check') if row[0] == V else None   # ruling 64
        return None

    def whole(self):
        out = {}
        places = set()
        for o in self.known_orders().values():
            places |= set(o)
        for p in places:
            a = self.answer(p)
            assert a != WAIT
            if a is not None:
                out[p] = a
        return out

    def missing(self):
        return [s for s in self.wanted() if not self.has(s)]

    def reading(self):
        """Join what is wanted and not held; leave what is held and not wanted.
        Gives the checks' values as they were before the leaving."""
        before = {s: self.proxy[s] for s in self.proxy if s[0] == 'C' and self.has(s)}
        wanted = self.wanted()
        for s in list(self.phase):
            if s not in wanted:
                for d in (self.proxy, self.queue, self.pend, self.phase):
                    d.pop(s, None)
        for s in wanted:
            if s not in self.phase:
                self.phase[s] = 'asked'
                self.queue[s] = []
                self.pend[s] = []
        return before

    def told_by(self, layer, p, before):
        for i in sorted(self.visible):
            if i >= layer:
                break
            if self.marks[i] and self.has(('O', i)) and p in self.proxy[('O', i)]:
                return False
            c = ('C', i, p)
            if self.cfg['told'] == 'before-leave':
                if before.get(c) is not None:
                    return False
            else:
                if self.has(c) and self.proxy[c] is not None:
                    return False
        return True

    def tell_place(self, p):
        a = self.answer(p)
        if a == WAIT:
            return
        self.told += 1
        if a is None:
            self.screen.pop(p, None)
        else:
            self.screen[p] = a

    def tell_anew(self):
        self.told += 1
        self.screen = self.whole()
        self.owed = False

    # ----------------------------------------------------------- the events
    def events(self):
        ev = []
        if self.wi < len(self.writes):
            ev.append(('W',))
        if self.see:
            ev.append(('P',))
        for s in sorted(self.phase):
            ph = self.phase[s]
            if ph == 'asked':
                ev.append(('S', s))
            elif ph == 'served':
                ev.append(('A', s))
            if ph in ('served', 'given'):
                if self.queue[s]:
                    ev.append(('U', s))
            if ph in ('served', 'given') and self.pend[s]:
                ev.append(('H', s))
        return ev

    def _changed(self, s):
        """A table value under an open push changed: Rama queues the new value."""
        if s in self.phase and self.phase[s] in ('served', 'given'):
            self.queue[s].append(self.table_value(s))

    def do(self, e):
        if e[0] == 'W':
            w = self.writes[self.wi]
            self.wi += 1
            if w[0] == 'see':                       # a permission is given; its push calls back later
                self.see.append(w[1])
                return
            _, i, p, val = w
            ver = 100 + self.wi
            self.facts[(i, p)] = (val, ver)
            self._changed(('C', i, p))
            if self.marks[i]:
                had = p in self.orders[i]
                if val == V:
                    self.orders[i][p] = ver
                    self._changed(('O', i))
                elif had:
                    del self.orders[i][p]
                    self._changed(('O', i))
        elif e[0] == 'P':                            # step 5, for her permission's push: the layer joins
            self.visible.add(self.see.pop(0))
            self.shaped()
        elif e[0] == 'S':                            # Rama serves the opening
            s = e[1]
            self.proxy[s] = self.table_value(s)
            self.phase[s] = 'served'
        elif e[0] == 'A':                            # step 6: a push that the read came to need is given
            s = e[1]
            self.phase[s] = 'given'
            self.reading()
            if self.cfg['arrival'] == 'owed':
                if self.owed:
                    if not self.missing():
                        self.tell_anew()
                elif s[0] == 'C':
                    self.tell_place(s[2])
            else:
                if s[0] == 'C':
                    self.tell_place(s[2])
                elif not self.missing():
                    self.tell_anew()
        elif e[0] == 'U':                            # the push takes the next value
            s = e[1]
            new = self.queue[s].pop(0)
            old = self.proxy[s]
            self.proxy[s] = new
            self.pend[s].append((old, new))
        elif e[0] == 'H':                            # step 5: the callback, under the read's lock
            s = e[1]
            old, new = self.pend[s].pop(0)
            if s[0] == 'O':
                places = {p for p in set(old) | set(new) if old.get(p) != new.get(p)}
            else:
                places = {s[2]}
            before = self.reading()
            if self.cfg['arrival'] == 'owed' and self.owed:
                if not self.missing():
                    self.tell_anew()
                return
            for p in sorted(places):
                if self.told_by(s[1], p, before):
                    self.tell_place(p)

    def shaped(self):
        """What shapes the read changed (here: a layer joined)."""
        self.reading()
        if self.cfg['arrival'] == 'owed':
            self.owed = True
        if not self.missing():
            self.tell_anew()

    # ------------------------------------------------------------- the truth
    def truth(self):
        out = {}
        places = set()
        assert not self.see
        for i in self.visible:
            if self.marks[i]:
                places |= set(self.orders[i])
        for p in places:
            for i in sorted(self.visible):
                if self.marks[i] and p in self.orders[i]:
                    out[p] = (i, self.orders[i][p], 'order')
                    break
                row = self.facts.get((i, p))
                if row is not None:
                    if row[0] == V:
                        out[p] = (i, row[1], 'check')
                    break
        return out


def explore(make):
    """Every order of the events. Gives (states, ends, the shortest failing order or None)."""
    start = make()
    seen = {start.key()}
    todo = deque([(start, ())])
    ends = 0
    fail = None
    while todo:
        w, path = todo.popleft()
        ev = w.events()
        if not ev:
            ends += 1
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
    return len(seen), ends, fail


PLAN = dict(told='before-leave', arrival='by-kind', wait='lazy', checks='plan')
FIXED = dict(told='after-leave', arrival='owed', wait='lazy', checks='tight')

# name, layers, which mark the key, the facts at the start, who is seen at the start, the writes
SCENARIOS = [
    ('1. the plan\'s own crossing: the farther order loses p while the nearer layer gets another row at p',
     2, {1: True, 2: True}, {(2, 'p'): (V, 1)}, {1, 2},
     [('w', 1, 'p', 'U'), ('w', 2, 'p', 'X')]),
    ('2. ruling 64: a nearer layer that does not mark the key writes the same thing; then the farther order loses p',
     2, {1: False, 2: True}, {(2, 'p'): (V, 1)}, {1, 2},
     [('w', 1, 'p', V), ('w', 2, 'p', 'X')]),
    ('3. ruling 64, and back: the nearer writes the same thing, the farther moves away and moves back',
     2, {1: False, 2: True}, {(2, 'p'): (V, 1)}, {1, 2},
     [('w', 1, 'p', V), ('w', 2, 'p', 'X'), ('w', 2, 'p', V)]),
    ('4. the nearer hides, then says the same thing, while the farther is written again',
     2, {1: False, 2: True}, {(2, 'p'): (V, 1)}, {1, 2},
     [('w', 1, 'p', 'U'), ('w', 2, 'p', V), ('w', 1, 'p', V)]),
    ('5. both mark: the nearer moves p away and back while the farther moves it away',
     2, {1: True, 2: True}, {(2, 'p'): (V, 1)}, {1, 2},
     [('w', 1, 'p', 'U'), ('w', 1, 'p', V), ('w', 2, 'p', 'X')]),
    ('6. three layers, all mark: the middle loses p, the nearest gets a row, the farthest is written again',
     3, {1: True, 2: True, 3: True}, {(2, 'p'): (V, 1), (3, 'p'): (V, 2)}, {1, 2, 3},
     [('w', 2, 'p', 'U'), ('w', 1, 'p', 'W'), ('w', 3, 'p', V)]),
    ('7. three layers: the middle loses p and the farthest loses p, then the middle gets it back',
     3, {1: False, 2: True, 3: True}, {(2, 'p'): (V, 1), (3, 'p'): (V, 2), (1, 'p'): (V, 3)}, {1, 2, 3},
     [('w', 2, 'p', 'U'), ('w', 3, 'p', 'U'), ('w', 2, 'p', V)]),
    ('8. three layers, four writes on one place',
     3, {1: False, 2: True, 3: True}, {(3, 'p'): (V, 1)}, {1, 2, 3},
     [('w', 2, 'p', V), ('w', 1, 'p', V), ('w', 2, 'p', 'U'), ('w', 3, 'p', 'U')]),
    ('9. a permission is given: a nearer layer joins, with a place q of its own, over a farther place p',
     2, {1: True, 2: True}, {(1, 'q'): (V, 1), (2, 'p'): (V, 2)}, {2},
     [('see', 1)]),
    ('10. the same, while the farther layer is written',
     2, {1: True, 2: True}, {(1, 'q'): (V, 1), (2, 'p'): (V, 2), (1, 'p'): ('U', 3)}, {2},
     [('see', 1), ('w', 2, 'p', V), ('w', 1, 'q', 'U')]),
    ('11. a new place in the farther order, with a nearer row there already',
     2, {1: False, 2: True}, {(1, 'p'): (V, 1)}, {1, 2},
     [('w', 2, 'p', V), ('w', 1, 'p', 'U'), ('w', 2, 'p', 'X')]),
    ('12. four layers: the second loses p, so checks are asked in the second and the third; then the third gets p',
     4, {1: True, 2: True, 3: True, 4: True}, {(2, 'p'): (V, 1), (4, 'p'): (V, 2)}, {1, 2, 3, 4},
     [('w', 2, 'p', 'U'), ('w', 3, 'p', V)]),
    ('13. a nearer layer joins while a check of a new place is on its way',
     3, {1: True, 2: False, 3: True}, {(1, 'q'): (V, 1), (2, 'p'): ('U', 2)}, {2, 3},
     [('w', 3, 'p', V), ('see', 1), ('w', 2, 'p', V)]),
]


def every_small(cfg):
    """Every list of up to 3 writes on one place, in 2 layers, with each way of marking."""
    import itertools
    bad = []
    n = 0
    states = 0
    ws = [('w', i, 'p', val) for i in (1, 2) for val in (V, 'U')]
    for m1 in (True, False):
        for start in ({}, {(2, 'p'): (V, 1)}, {(1, 'p'): ('U', 1), (2, 'p'): (V, 2)}, {(1, 'p'): (V, 1), (2, 'p'): (V, 2)}):
            for ln in (1, 2, 3):
                for seq in itertools.product(ws, repeat=ln):
                    n += 1
                    st, _, fail = explore(lambda: World(2, {1: m1, 2: True}, start, {1, 2}, list(seq), cfg))
                    states += st
                    if fail:
                        bad.append((m1, start, seq, fail))
    return n, states, bad


def main():
    show = len(sys.argv) > 1 and sys.argv[1] == 'show'
    for label, cfg in (('THE PLAN AS WORDED', PLAN), ('THE PLAN AS FIXED BY THE CHECK', FIXED)):
        print('== ' + label + '  ' + str(cfg))
        for name, k, marks, facts, visible, writes in SCENARIOS:
            states, ends, fail = explore(lambda: World(k, marks, facts, visible, writes, cfg))
            print('  %-4s %6d states %5d ends  %s' % ('FAIL' if fail else 'ok', states, ends, name))
            if fail and show:
                path, screen, truth = fail
                print('         a failing order: ' + ' '.join(e[0] + ('' if len(e) == 1 else str(e[1]).replace(' ', '')) for e in path))
                print('         the screen at the end: %s   the tables give: %s' % (screen, truth))
        n, states, bad = every_small(cfg)
        print('  every list of up to 3 writes on one place in 2 layers: %d lists, %d states, %d lists with a failing order' % (n, states, len(bad)))
        if bad and show:
            m1, start, seq, (path, screen, truth) = bad[0]
            print('         the first: nearer marks=%s start=%s writes=%s' % (m1, start, seq))
            print('         a failing order: ' + ' '.join(e[0] + ('' if len(e) == 1 else str(e[1]).replace(' ', '')) for e in path))
            print('         the screen at the end: %s   the tables give: %s' % (screen, truth))
    # one switch at a time, from the fixed plan, to show which fix each case needs
    for sw, val in (('told', 'before-leave'), ('arrival', 'by-kind'), ('wait', 'strict'), ('checks', 'plan')):
        cfg = dict(FIXED)
        cfg[sw] = val
        fails = [name.split('.')[0] for name, k, marks, facts, visible, writes in SCENARIOS
                 if explore(lambda: World(k, marks, facts, visible, writes, cfg))[2]]
        n, states, bad = every_small(cfg)
        print('== the fixed plan with %s=%s: scenarios that fail: %s; small lists that fail: %d of %d' % (sw, val, fails or 'none', len(bad), n))


if __name__ == '__main__':
    main()
