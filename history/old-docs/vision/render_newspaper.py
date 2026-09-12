#!/usr/bin/env python3
"""Render the Newspaper Discourse Graph with minimal color: only node type tags."""

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
╔═══════════════════════════════════════════════════════════════════════════════════════════════════════╗
║  ┌─────────────────────────────────────────────────────────────────────────────────────────────────┐  ║
║  │                             T  H  E     D  A  I  L  Y     D  I  F  F                            │  ║
║  │                                "All the Code That's Fit to Ship"                                │  ║
║  │ Vol. CXLVII  ·  No. 847  ·  Thu Feb 13, 2026                                      FINAL EDITION │  ║
║  └─────────────────────────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                                       ║
║  [Q] AUTH MIDDLEWARE OVERHAULED;                  ║  STATS                                            ║
║  PER-ROUTE VALIDATION REPLACES                    ║  Files: 3   Lines: +47/-12   Tests: 4             ║
║  GLOBAL POLICY AFTER TWO                          ║                                                   ║
║  FAILED ATTEMPTS                                  ║  DISCOURSE GRAPH                                  ║
║                                                   ║  ──────────────────────────────────────           ║
║  Three routes gain independent token              ║  [Q] Questions:  1 root (+3 sub)                  ║
║  validation, ending months of security            ║  [C] Claims:     5  ( 3● · 1◐ · 1○ )              ║
║  workarounds. Author @sid delivers a              ║  [E] Evidence:   4  (+1 gap)                      ║
║  composable schema-based approach after           ║  [D] Decisions:  3  ( 1✓ · 2✗ )                   ║
║  evaluating and rejecting two alternatives.       ║  [R] Risks:      2  (both open)                   ║
║  Two risks remain open.                           ║  [F] Feedback:   1  objection                     ║
║                                                   ║                                                   ║
║                                                   ║  Confidence: █████████████████░░░░░░░ 72%         ║
║                                                   ║  Risk: MEDIUM    Est. review: ~12 min             ║
╠═══════════════════════════════════════════════════╬═══════════════════════════════════════════════════╣
║  SECTION A: WHAT CHANGED [Q]                      ║  SECTION B: WHY THIS WAY [Q]                      ║
║  ───────────────────────────────────────────      ║  ──────────────────────────────────────           ║
║                                                   ║                                                   ║
║  [C] "New wrap-per-route-auth fn replaces         ║  [D:rejected] "GLOBAL WHITELIST"                  ║
║       old single-policy handler"                  ║  Rigid, doesn't scale past 5 routes.              ║
║                                                   ║  ── opposed_by: "unscalable"                      ║
║  ◆ [E:code] middleware.clj:47-63                  ║  ── superseded_by: per-route schema               ║
║  │  (defn wrap-per-route-auth                     ║                                                   ║
║  │    [handler route-schemas]                     ║  [D:rejected] "PER-ROLE CHAINS"                   ║
║  │    (fn [req] (case (validate ...)              ║  N+1 middleware calls, 3x latency.                ║
║  │      :valid   (handler req)                    ║  ── opposed_by: [E:bench] p99=6ms                 ║
║  │      :expired {:status 401 ...})))             ║  ── superseded_by: per-route schema               ║
║  ── supports ──▶ C1, C4                           ║                                                   ║
║                                                   ║  [D:accepted] "PER-ROUTE SCHEMA MAP"              ║
║  ◆ [E:test] auth_test.clj:109-140                 ║  O(1) lookup. Composable. Testable.               ║
║  │  ✓ expired-token    ✓ scope-mismatch           ║  No performance regression.                       ║
║  │  ✓ malformed        ✓ valid-passthrough        ║  ── supported_by: Ex.A, Ex.C                      ║
║  ── supports ──▶ C1, C3, C4                       ║  [E:bench] p99=2ms · throughput=12k rps           ║
╠═══════════════════════════════════════════════════╩═══════════════════════════════════════════════════╣
║                                                                                                       ║
║  E D I T O R I A L    [Q] "What risks remain before this ships?"                                      ║
║                                                                                                       ║
║  ┌─ ▲ [R] RISK 1 ───────────────────────────────┐   ┌─ ▲ [R] RISK 2 ───────────────────────────────┐  ║
║  │ "Legacy tokens in production may             │   │ "Refresh tokens with mixed OAuth             │  ║
║  │  not conform to new per-route                │   │  scopes have not been tested.                │  ║
║  │  schema. No migration plan or                │   │  Raised by @alex, Objection #1."             │  ║
║  │  rollback strategy presented."               │   │                                              │  ║
║  │ ── opposes C5 · blocks Verdict               │   │ ── opposes C3 · blocks Verdict               │  ║
║  │ Severity: MEDIUM       [WAIVE]               │   │ Severity: HIGH         [WAIVE]               │  ║
║  └──────────────────────────────────────────────┘   └──────────────────────────────────────────────┘  ║
║                                                                                                       ║
║  LETTERS TO THE EDITOR [F]                                                                            ║
║  @alex: "Count 3 claims complete edge-case coverage, yet no exhibit shows refresh token + mixed       ║
║   scope testing. This gap undermines confidence."    ── opposes C3  ── informs R2     [REPLY]         ║
║                                                                                                       ║
╠═══════════════════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                                       ║
║  [D] V E R D I C T                                                                                    ║
║  depends_on:  C1 ✓    C2 ✓    C3 ⚡    C4 ✓    C5 ✗             blocked_by:  R1 ⚠    R2 ⚠              ║
║                                                                                                       ║
║  ┌─────────────────┐        ┌──────────────────────────────┐        ┌──────────────────────┐          ║
║  │  ✓ SHIP IT      │        │  ◐ HOLD THE PRESS            │        │  ✗ KILL THE STORY    │          ║
║  │  (locked)       │        │     ◀── RECOMMENDED          │        │  (request changes)   │          ║
║  └─────────────────┘        └──────────────────────────────┘        └──────────────────────┘          ║
║                                                                                                       ║
╚═══════════════════════════════════════════════════════════════════════════════════════════════════════╝"""

def main():
    # Print legend
    print(f"\n  {BOLD}{GRAY}THE DAILY DIFF — NEWSPAPER DISCOURSE GRAPH{RESET}")
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
