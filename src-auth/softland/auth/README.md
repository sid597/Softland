# src-auth/softland/auth

Parent: [softland](../README.md).

Implements the Rama module `Auth` and its foreign client contract (`softland.auth.client`), following `src/proposal/runtime-2026-11-21/spine/auth/SPEC.md` and `PLAN.md` (with cross-key agreement verified in `spine/auth/agreement/`).

## Files

- `fit.clj` — Pure input-form validation for all 18 calls, enforcing Sid's Call 1 (`contains?` on `-or-nil` keys, `:driven-by` alone optional, `Locale/ROOT` lower-case emails, `fn?` for `watch-root`, fixed-width integer coercion to `Long`, throwing-comparator safety).
- `schema.clj` — Typed Java marker interfaces and `defrecord`s for all 4 depots and 6 PStates (zero `Object` positions, zero byte arrays), 7-bit ASCII key encoders, read projections, and pure single-task state-machine transitions.
- `module.clj` — `defmodule Auth`: declares `*root-events`, `*asks`, `*invites`, `*enters`; `$$root-data`, `$$live` (`pstate.proxy.limit = 20000`), `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`; stream topology `"auth"` (`{:start-from :beginning :retry-mode :all-after}`); and query topologies `"who-is"` (2-seek fast path), `"under"` (live sign-ins only), and `"ping-task"`.
- `client.clj` — Public contract namespace `softland.auth.client` exposing all 18 functions (`open`, `close!`, 12 writes, `who-is`, `ask-of`, `under`, `watch-root`). Never throws; validates at the client edge before touching Rama.
