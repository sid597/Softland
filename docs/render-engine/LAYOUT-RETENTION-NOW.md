# layout-retention — NOW

## STANDING (frozen at open, 2026-08-08)
The memory floor of the text substrate: 228KB boot corpus currently ~1.3–1.45GB
dev heap. Budgets are LAW: **B1** release heap ≤100MB post-boot post-GC ·
**B2** layout planes ≤15MB (≤64B per laid-out code unit). Mechanism:
Contract-T schema change — columnar typed planes + derived rich views behind
one accessor seam; full corpus stays resident (viewport rung REFUSED to
LATER). GPU: capacity literal dies, stride/atlas/leases refused. Contract:
`LAYOUT-RETENTION-CONTRACT.md` (28KB). Receipts: 2026-08-08 memprobe suite,
prior session scratchpad `f68db61e-…`. Tripwires at close: S1–S5.

## NOW

**2026-08-08 — contract cut (Fable 5, xhigh).** One pass. Key rulings:
115MB "clock-source holder" is an attribution artifact — the atom holds
`identity`; structure lands on the render loop's prev-generation refs (S3
kill-probe pre-registered). "230B/instance" premise corrected: stride is 52B;
the waste is `:initial-capacity 1000000` (renderer.cljs:2043). Prior session
already dropped cluster caret-stops/ink-bounds in-tree (~150MB, comment
receipt at text_layout.cljc:841-847). NEXT → ONE bounded fresh-eyes
falsification round at Sid's hand (paste-prompt delivered in-session), author
repairs in place, then the implementer prompt at contract tail. Self-audit:
contract-cut session — all process lines, zero code lines, by design.
