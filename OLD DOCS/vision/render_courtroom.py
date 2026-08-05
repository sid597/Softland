#!/usr/bin/env python3
"""Render the Courtroom Discourse Graph with minimal color: only node type tags."""

import re

RESET = "\033[0m"
BOLD  = "\033[1m"

def fg(r, g, b):
    return f"\033[38;2;{r};{g};{b}m"

# Everything is gray except node type tags
GRAY = fg(160, 160, 165)

# 6 node type colors — the ONLY colors in the whole render
Q_COLOR = fg(255, 210, 80)   # gold/yellow  — Question
C_COLOR = fg(100, 200, 255)  # cyan/blue    — Claim
E_COLOR = fg(200, 140, 255)  # purple       — Evidence
D_COLOR = fg(255, 170, 70)   # orange       — Decision
R_COLOR = fg(255, 90, 90)    # red          — Risk
F_COLOR = fg(120, 220, 160)  # green/teal   — Feedback

# Pattern matches [Q], [C], [E], [E:code], [E:test], [E:bench], [E:???],
# [D], [D:rejected], [D:accepted], [R], [F]
TAG_RE = re.compile(r'(\[(?:Q|C|E(?::\w+)?|D(?::\w+)?|R|F)\])')

def color_for_tag(tag):
    if tag.startswith("[Q"):  return Q_COLOR
    if tag.startswith("[C"):  return C_COLOR
    if tag.startswith("[E"):  return E_COLOR
    if tag.startswith("[D"):  return D_COLOR
    if tag.startswith("[R"):  return R_COLOR
    if tag.startswith("[F"):  return F_COLOR
    return GRAY

def colorize(line):
    parts = TAG_RE.split(line)
    out = ""
    for part in parts:
        if TAG_RE.fullmatch(part):
            out += BOLD + color_for_tag(part) + part + RESET + GRAY
        else:
            out += part
    return GRAY + out + RESET

ART = r"""
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                                          ║
║        C O U R T   O F   C O D E   R E V I E W  ──  C A S E   # 8 4 7                                    ║
║        The People v. auth-per-route                                                                      ║
║                                                                                                          ║
║  ┌── [Q] THE CASE ──────────────────────────────────────────────────────────────────────────────┐        ║
║  │  "Should Softland adopt per-route token validation to replace the global auth policy?"       │        ║
║  └──────────────────────────────────────────────────────────────────────────────────────────────┘        ║
║                                                                                                          ║
║  Judge: @alex          Counts: 3/5 examined          Confidence: █████████████████░░░░░░░░ 72%           ║
║                                                                                                          ║
╠═══  PROSECUTION [C]  ═══════════════════════════════╦═══ EVIDENCE LOCKER [E] ════════════════════════════╣
║                                                     ║                                                    ║
║  "The existing auth applies a single global         ║  ◆ EXHIBIT A  [E:code]                             ║
║   policy to all routes. This fails three            ║    middleware.clj:47-63                            ║
║   security requirements."                           ║    (defn wrap-per-route-auth                       ║
║                                                     ║      [handler route-schemas]                       ║
║                                                     ║      (fn [req] (case (validate ...)                ║
║  C1 ● "Global policy unsafe"                        ║        :valid   (handler req)                      ║
║       ◆ Ex.A, Ex.B              PROVEN              ║        :expired {:status 401 ...})))               ║
║                                                     ║    ── supports ──▶ C1, C4                          ║
║  C2 ● "No perf regression"                          ║                                                    ║
║       ◆ Ex.C                    PROVEN              ║  ◆ EXHIBIT B  [E:test]                             ║
║                                                     ║    auth_test.clj:109-125                           ║
║  C3 ◐ "All edge cases covered"                      ║    (deftest expired-token-per-route-test           ║
║       ◆ Ex.B  ✗ Obj#1          CHALLENGED           ║      (is (= 401 (:status (handler ...)))))         ║
║                                                     ║    ── supports ──▶ C1, C3, C4                      ║
║  C4 ● "Tests cover core paths"                      ║                                                    ║
║       ◆ Ex.B, Ex.D              PROVEN              ║  ◆ EXHIBIT C  [E:bench]                            ║
║                                                     ║    $ ab -n 1000 -c 50 /api/admin                   ║
║  C5 ○ "Migration is safe"                           ║    p99 latency: 2ms  ·  throughput: 12k rps        ║
║       ◆ (none)                  UNSUBSTANTIATED     ║    ── supports ──▶ C2                              ║
║       ⚠ No exhibit entered                          ║                                                    ║
║                                                     ║  ◆ EXHIBIT D  [E:???]                              ║
║                                                     ║    N O T   S U B M I T T E D                       ║
║                                                     ║    Required to prove C5: "Migration safe"          ║
║                                                     ║                                                    ║
╠═════════════════════════════════════════════════════╬════════════════════════════════════════════════════╣
║                                                     ║                                                    ║
║  WARNINGS TO THE COURT [R]                          ║  ⚡ OBJECTION #1  [F]  ── @alex, 2m ago            ║
║                                                     ║                                                    ║
║  ▲ R1  "Legacy tokens in prod — no migration"       ║  "Count 3 claims all edge cases are covered,       ║
║     opposes C5  ·  blocks Verdict                   ║   but no exhibit demonstrates refresh token        ║
║     Severity: MEDIUM  ·  Court's own motion         ║   handling with mixed OAuth scopes."               ║
║                                                     ║                                                    ║
║  ▲ R2  "Refresh tokens + mixed scopes untested"     ║  ── opposes ──▶ C3                                 ║
║     opposes C5  ·  blocks Verdict                   ║  ── informs ──▶ R2                                 ║
║     Severity: HIGH  ·  From Objection #1            ║                                                    ║
║                                                     ║  [SUSTAIN]    [OVERRULE]    [REPLY]                ║
║                                                     ║                                                    ║
╠═══════════════════════════════════════════════════ ═╩════════════════════════════════════════════════════╣
║  PRIOR ART [D:rejected]                                                                                  ║
║  ✗ "Global whitelist" — rigid, unscalable           ✗ "Per-role chains" — N+1 calls, 3x latency          ║
║  ── both superseded_by ──▶  ✓ "Per-route schema map" — O(1) lookup, composable, testable     ADOPTED    ║
╠══════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                                          ║
║  [D] V E R D I C T                                                                                       ║
║  depends_on:  C1 ✓    C2 ✓    C3 ⚡    C4 ✓    C5  ✗           blocked_by:  R1 ⚠    R2 ⚠                 ║
║                                                                                                          ║
║  ┌─────────────────┐        ┌──────────────────────────────┐        ┌──────────────────────┐            ║
║  │   ✓  ACQUIT     │        │   ◐  CONTINUE DELIBERATION   │        │   ✗  CONVICT         │            ║
║  │   (approve)     │        │      ◀── RECOMMENDED          │        │   (request changes)  │            ║
║  │   LOCKED — 2    │        │                               │        │                      │            ║
║  │   open risks    │        │                               │        │                      │            ║
║  └─────────────────┘        └───────────────────────────────┘        └──────────────────────┘            ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝"""

def main():
    # Print legend
    print(f"\n  {BOLD}{GRAY}COURTROOM DISCOURSE GRAPH{RESET}")
    print(f"  {BOLD}{Q_COLOR}[Q]{RESET}{GRAY} Question  "
          f"{BOLD}{C_COLOR}[C]{RESET}{GRAY} Claim  "
          f"{BOLD}{E_COLOR}[E]{RESET}{GRAY} Evidence  "
          f"{BOLD}{D_COLOR}[D]{RESET}{GRAY} Decision  "
          f"{BOLD}{R_COLOR}[R]{RESET}{GRAY} Risk  "
          f"{BOLD}{F_COLOR}[F]{RESET}{GRAY} Feedback{RESET}\n")

    for line in ART.strip('\n').split('\n'):
        print(colorize(line))
    print(RESET)

if __name__ == "__main__":
    main()
