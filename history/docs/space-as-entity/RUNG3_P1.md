# space-as-entity — rung 3 P1 receipts

2026-07-26 · Codex implementation context · **IMPLEMENTER PASS; FABLE GATE
REVIEW PENDING**. Phase P1 owns exactly contract deliverables 1–7 and gates
R3-G1…R3-G8. Nested spaces, per-space cameras, instance preview,
reaction-declarations, and zoom bands were not started.

No commit or push was made.

## Pre-code manifest verification

The full `RUNG3_CONTRACT.md` input manifest and every claim attached to its
line citations were checked against disk before the first product edit.

- `binding_material.cljc`: sites 77–83; instance-site enumeration/docstring
  102–115; one-arity legality predicate 117–119; the one fence predicate
  121–135; instance dispatch lookup 298–315; space floor + keyword claim subject
  400–437; twelve probes 448–489; drill chain/report 491–542. Frozen grammar
  v1/v2 validators at 222–249 were also read before editing.
- `ground.cljs`: shared wears 262–273; `wears-for` 275–322; floor rows
  2205–2218; console lane 2224–2267; served extraction 2275–2316; merged
  instance rows 2318–2324; one claim builder 2351–2370; binding table
  2431–2468; shared-tier clamp read 2668–2685; drill worlds 3130–3183; console
  JS instance handler 3196–3206.
- Read-only `facet_material.cljc`: instance validators 273–282; inherited
  grammars 284–298; full parent-material snapshot and durable subject
  stringification 304–321; instance wear law 370–459. Pre-code SHA-256:
  `f77871c25fa0151a177109c17f679c0eb80613e18d96332f2b1a408f515347d1`.
- `facet_master.clj`: durable subject digest/stringification 446–450 and
  `write-instance-revision!` 517 onward; its site refusal is computed at
  545–550 and precedes every append at 569 onward.
- Read-only `material_truth.clj`: register/deviate/release/pin/unpin 32–75;
  served instance 118–146; served instances 148–172, including explicit
  subjects bypassing registry lag at 148–153. Pre-code SHA-256:
  `8f9301333c2b43ae5489efd3ed8c60bd80ff647cc310a64dcde2f2ba8fb5cc12`.
- Read-only `face_projection.clj`: served instance tier 911–957 consumes
  request `:subjects`; interaction table 1034–1070 derives tiers only from
  shared masters. Pre-code SHA-256:
  `1379fe874fcd8e0824fcc00c646f897fad5d7ecf174f925f591776182bc44835`.
- `face_wiring.cljs`: material request 444–449 and epoch debounce 524–544
  matched exactly. `space_material.cljc` was read in full; its grammar fence was
  61–67 and both v0/v1 declarations were 69–102.
- Tests: the G10 refusal pin was exactly
  `test/app/material_truth_test.clj:491–503`; the one-builder chain pins were
  `test/app/binding_dispatch_test.clj:904–913`. The pre-code edited-file/pin
  sweep found `binding_dispatch_test`, `material_circulation_test`,
  `material_truth_test`, `provenance_material_test`, `reply_to_block_test`, and
  `space_material_test`; dynamic product coverage also reached
  `face_projection_test` and `material_portal_test`.

Manifest drift, banked before code:

1. The manifest calls `space_material.cljc` a 118-line file; it ended at line
   117 before P1. The whole-file claim and cited semantics matched.
2. The manifest names the two tests by filename only; their on-disk paths are
   `test/app/material_truth_test.clj` and
   `test/app/binding_dispatch_test.clj`. The cited line ranges matched.

No semantic drift or stop-clause conflict was found. The pre-code worktree was
clean.

## Seven deliverables, exactly

1. `binding_material.cljc`: `instance-site-legal?` now has an owner-aware
   arity. Block sites remain the single `instance-legal-sites` enumeration and
   the one-arity path remains block-only/fail-closed. Only owner `:space` may
   use `:space/ground`. The G10 prose moved with the law.
2. `ground.cljs`: the console lane passes claim subject as owner and keeps the
   camera fence ordered first; the JS bridge coerces only exact durable
   `"space"` at normalized `:space/ground`; served extraction passes facet as
   owner and bridges only facet `:space` + exact subject `"space"` to dispatch
   keyword `:space`; the table label is exactly `instance:space`; the clamp
   reads `wears-for space-subject`. The existing cache is prewarmed for that
   fixed subject only when the subject has no served instance under any facet,
   preserving the contract's cached-map-probe promise without a new atom.
3. `facet_master.clj`: the durable `illegal-sites` lane passes the parent facet
   as owner. It does not add a fourth camera-fence read; fm:space's inherited
   grammar validator remains the write-lane fence.
4. `space_material.cljc`: added the single durable `space-subject` constant,
   `"space"`.
5. `face_wiring.cljs`: the existing debounced material request now names
   `:subjects [space-material/space-subject]`.
6. Suite coverage: the live twelve-probe list is unchanged; injected
   `[:space :space/ground]` rows move tap to `:instance` while all three camera
   probes stay `:floor`; a foreign durable space subject cannot hijack THE
   space. Server-side refusal tests prove camera, foreign-owner, and invalid
   clamp candidates append nothing.
7. T-R6 sweep completed and itemized below.

No read-only material/registry/projection file changed. No grammar declaration,
claim chain, verb, state atom, Rama module, depot, PState, topology, or routing
branch was added.

## Gate verdicts

| Gate | Implementer verdict | Principal proof |
|---|---|---|
| R3-G1 | GREEN | pre-deviation server read, unchanged 12-probe baseline, zero instance rows, trusted wheel 8.0/0.1 |
| R3-G2 | GREEN | live install → instance drill → ordered camera refusal → foreign-owner refusal → block regression → clear |
| R3-G3 | GREEN | fresh-ID server drive, exact served subject bridge, instance tap/marquee, floor camera, trusted 3.0 clamp, release to 8.0 |
| R3-G4 | GREEN | pin held 8.0 over shared 2.0, unpin felt 2.0, shared pointer rolled back to base 8.0 |
| R3-G5 | GREEN | all three server refusals, physical revision/pointer counts unchanged |
| R3-G6 | GREEN | one legality var/three owner reads; one fence var/three reads; no read-only diff, grammar diff, chain diff, or atom |
| R3-G7 | GREEN | client `instance:space`; server deliberately zero instance tier; four floor rows byte-agree |
| R3-G8 | GREEN | final fast lane + all affected isolated suites + final shadow compile |

Fable owns the independent gate verdict. This implementer did not write a
Fable verdict into `decisions.md`.

## Numbered receipt bank

Warnings about the pre-existing `reader-conditional?` name replacement are
omitted; receipt bodies are verbatim stdout.

### R3-G1 — behavior-identical at the cut

Server read before the first durable space instance:

```clojure
{:server-instance-table-rows [], :space-instance nil, :checks {:shared-base? true, :no-space-instance? true, :camera-floor? true, :twelve-probes? true, :all-claimed? true, :zero-server-instance-table-rows? true, :worn-base? true, :marquee-master? true, :tap-master? true}, :source :live-cluster-server-read, :zoom-range [0.1 8.0], :status :green, :gate :R3-G1, :probe-decisions [["tap a user block → focus" :focus/place-caret :master :attention :claimed] ["shift-press a user block → focus in the same gesture" :focus/enter-block :master :attention :claimed] ["drag a user block" :placement/drag-group :master :positioned :claimed] ["shift-drag a user block → text selection" :selection/text-begin :master :attention :claimed] ["tap a machine block → release focus" :focus/release :master :attention :claimed] ["drag a machine block" :placement/drag-group :master :positioned :claimed] ["tap a fold header → toggle its section" :fold/toggle-section :master :foldable :claimed] ["tap empty space → caret anchor" :anchor/place :master :space :claimed] ["drag empty space → pan the camera" :camera/pan :floor :space :claimed] ["shift-drag empty space → marquee" :selection/marquee-begin :master :space :claimed] ["wheel → zoom at the pointer" :camera/zoom-at-pointer :floor :space :claimed] ["wheel at a block → zoom through the space rung" :camera/zoom-at-pointer :floor :space :claimed]], :active-shared-revision "rev:fm:space:df1e3a0b68c603be2b9d96b11fda9be75b5d779b074eced569ed0af144671fda:dab5f109f04f564adfccef4eed23392fcf52d1e971226c4d76e1343623953327"}
```

Trusted-wheel camera saturation from the live app:

```json
{"atMax":{"x":125660.8667175008,"y":-78184.15943486617,"zoom":8},"atMin":{"x":2202.7608339687604,"y":-582.3019929358272,"zoom":0.1},"checks":{"maxExact":true,"minExact":true}}
```

### R3-G2 — console lane

```json
{"gate":"R3-G2-browser","status":"green","source":"live-dev-app-console-seam","install":{"status":"installed","subject":"space","site":"ground","rows":1},"spaceProbeDecisions":{"tap a user block → focus":{"verb":"place-caret","tier":"master","outcome":"claimed"},"shift-press a user block → focus in the same gesture":{"verb":"enter-block","tier":"master","outcome":"claimed"},"drag a user block":{"verb":"drag-group","tier":"master","outcome":"claimed"},"shift-drag a user block → text selection":{"verb":"text-begin","tier":"master","outcome":"claimed"},"tap a machine block → release focus":{"verb":"release","tier":"master","outcome":"claimed"},"drag a machine block":{"verb":"drag-group","tier":"master","outcome":"claimed"},"tap a fold header → toggle its section":{"verb":"toggle-section","tier":"master","outcome":"claimed"},"tap empty space → caret anchor":{"verb":"marquee-begin","tier":"instance","outcome":"claimed"},"drag empty space → pan the camera":{"verb":"pan","tier":"floor","outcome":"claimed"},"shift-drag empty space → marquee":{"verb":"marquee-begin","tier":"master","outcome":"claimed"},"wheel → zoom at the pointer":{"verb":"zoom-at-pointer","tier":"floor","outcome":"claimed"},"wheel at a block → zoom through the space rung":{"verb":"zoom-at-pointer","tier":"floor","outcome":"claimed"}},"instanceRows":[{"priority":10,"verb-version":0,"modifiers":[],"floor?":false,"effect-class":"pure-projection","gesture":"tap","verb":"marquee-begin","revision-id":null,"phase":"complete","site":"ground","tier":"instance","facet":null,"master-id":"instance:space"}],"cameraRefusal":{"status":"refused","error":"camera-gesture-reserved","site":"ground","subject":"space"},"nonSpaceRefusal":{"status":"refused","error":"instance-site-refused","site":"ground","subject":"not-space","legal-sites":["fold-header","machine-hit-area","user-hit-area"]},"blockInstall":{"status":"installed","subject":"du:r3-g2-block","site":"user-hit-area","rows":1},"clear":{"status":"cleared"},"restoredTap":{"verb":"place","tier":"master","outcome":"claimed"},"checks":{"installed":true,"tapInstance":true,"cameraFloor":true,"labelExact":true,"cameraOrderedRefusal":true,"nonSpaceRefused":true,"blockUnchanged":true,"clearRestored":true}}
```

### R3-G3 — durable end to end

Final rerunnable server-read receipt under fresh request IDs:

```clojure
{:worn-zoom-range [0.1 3.0], :stage :deviation-active-awaiting-browser-drive, :space-probe-tiers {"tap empty space → caret anchor" :instance, "shift-drag empty space → marquee" :instance, "drag empty space → pan the camera" :floor, "wheel → zoom at the pointer" :floor, "wheel at a block → zoom through the space rung" :floor}, :reimport-revision-id "rev:fm:space~i~3f49dbbf:3a8d8c58116f1f012bdafdce17e5aff9fb62c083e2806b8b87a3229b1c0472e4:56f1431b4fa159aeaa01d4ac04bc150750d160fd5dfb16194ece27f528b69ead", :instance-revision-id "rev:fm:space~i~3f49dbbf:3a8d8c58116f1f012bdafdce17e5aff9fb62c083e2806b8b87a3229b1c0472e4:56f1431b4fa159aeaa01d4ac04bc150750d160fd5dfb16194ece27f528b69ead", :checks {:camera-floor? true, :tap-instance? true, :served-subject-string? true, :reimport-byte-identical? true, :fresh-write-path-accepted? true, :marquee-instance? true, :deviation-landed? true, :instance-clamp-three? true, :shared-stays-eight? true, :served-facet-space? true}, :source :live-cluster-server-read, :shared-revision-id "rev:fm:space:df1e3a0b68c603be2b9d96b11fda9be75b5d779b074eced569ed0af144671fda:dab5f109f04f564adfccef4eed23392fcf52d1e971226c4d76e1343623953327", :deviation-write {:accepted? false, :bootstrap? false, :import {:accepted? false, :replay? false, :status :rejected, :reason :idempotency/material-fingerprint-conflict, :errors [{:type :idempotency/material-fingerprint-conflict, :idempotency-key "imp:fm:fm:space~i~3f49dbbf:0a114997b344a9413f11a8a48815d05065b229c4ed85b6c9a254ec9c70ea47d0", :expected "f6f8e07779e1259d527c9d2908498f9740b1d59669d24f5a8713f20ace460bf8", :actual "8a7a52397788550865cc337f3282b19287d0f18f20f3aacf18fb7b5052493a39", :conflict-with-decision-id "fm:space~i~3f49dbbf/request/space-r3-g3-deviate-4e28f21f-f8ad-4236-a9c4-1a9933a2aaa9/decision"}]}, :activation {:accepted? true, :replay? false, :status :accepted, :reason nil, :errors []}}, :status :green, :gate :R3-G3, :shared-zoom-range [0.1 8.0], :request-ids {:request-id "space-r3-g3-deviate-f5746545-0419-4f46-a13a-dd72344cfcd4", :activation-request-id "space-r3-g3-deviate-activate-94e5ef8b-bb3e-4455-becd-d752a880eeec", :reimport-request-id "space-r3-g3-reimport-111366c6-a7ec-46f4-a113-7598192f6a61"}}
```

The first-ever fresh deviation, before the receipt helper itself was corrected,
returned `:deviation-accepted? true` under request
`space-r3-g3-deviate-4e28f21f-f8ad-4236-a9c4-1a9933a2aaa9`. The repeated
receipt above exposes the bootstrap-vs-normal import-fingerprint asymmetry
instead of hiding it: the content-addressed revision ID is identical and the
fresh activation is accepted. See Judgment Calls.

Final live client receipt after the falsification repairs:

```json
{"gate":"R3-G3-browser-active-final","status":"green","source":"live-dev-app-trusted-wheel","before":{"servedSubject":"space","wornSpace":{"tier":"instance","pinned?":false,"revision-id":"rev:fm:space~i~3f49dbbf:3a8d8c58116f1f012bdafdce17e5aff9fb62c083e2806b8b87a3229b1c0472e4:56f1431b4fa159aeaa01d4ac04bc150750d160fd5dfb16194ece27f528b69ead","floor?":false},"spaceProbeDecisions":{"tap a user block → focus":{"verb":"place-caret","tier":"master","outcome":"claimed"},"shift-press a user block → focus in the same gesture":{"verb":"enter-block","tier":"master","outcome":"claimed"},"drag a user block":{"verb":"drag-group","tier":"master","outcome":"claimed"},"shift-drag a user block → text selection":{"verb":"text-begin","tier":"master","outcome":"claimed"},"tap a machine block → release focus":{"verb":"release","tier":"master","outcome":"claimed"},"drag a machine block":{"verb":"drag-group","tier":"master","outcome":"claimed"},"tap a fold header → toggle its section":{"verb":"toggle-section","tier":"master","outcome":"claimed"},"tap empty space → caret anchor":{"verb":"place","tier":"instance","outcome":"claimed"},"drag empty space → pan the camera":{"verb":"pan","tier":"floor","outcome":"claimed"},"shift-drag empty space → marquee":{"verb":"marquee-begin","tier":"instance","outcome":"claimed"},"wheel → zoom at the pointer":{"verb":"zoom-at-pointer","tier":"floor","outcome":"claimed"},"wheel at a block → zoom through the space rung":{"verb":"zoom-at-pointer","tier":"floor","outcome":"claimed"}},"instanceRows":[{"priority":0,"verb-version":0,"modifiers":["shift"],"floor?":false,"effect-class":"pure-projection","gesture":"press","verb":"marquee-begin","revision-id":null,"phase":"threshold","site":"ground","tier":"instance","facet":null,"master-id":"instance:space"},{"priority":0,"verb-version":0,"modifiers":"any","floor?":false,"effect-class":"pure-projection","gesture":"tap","verb":"place","revision-id":null,"phase":"complete","site":"ground","tier":"instance","facet":null,"master-id":"instance:space"}]},"atMax":{"x":-1280,"y":-800,"zoom":3},"checks":{"subjectExact":true,"wornInstance":true,"tapInstance":true,"marqueeInstance":true,"cameraFloor":true,"labelsExact":true,"maxExact":true}}
```

Final release:

```clojure
{:worn-zoom-range [0.1 8.0], :stage :released-and-restored, :space-probe-tiers {"tap empty space → caret anchor" :master, "shift-drag empty space → marquee" :master, "drag empty space → pan the camera" :floor, "wheel → zoom at the pointer" :floor, "wheel at a block → zoom through the space rung" :floor}, :worn-tier :shared, :instance-revision-id "rev:fm:space~i~3f49dbbf:3a8d8c58116f1f012bdafdce17e5aff9fb62c083e2806b8b87a3229b1c0472e4:8179aa1d943aaec3fb2d299b2a6905696f46ff88be87f5df0f5b33323ae8964e", :checks {:release-accepted? true, :wear-restored-to-master? true, :clamp-restored-to-eight? true, :tap-master? true, :marquee-master? true, :camera-floor? true}, :source :live-cluster-server-read, :status :green, :gate :R3-G3, :request-ids {:request-id "space-r3-g3-release-6cf97da0-0df4-446b-8135-1b4980a7fe65", :activation-request-id "space-r3-g3-release-activate-3b2be6c0-74bc-4f73-93ad-f70a8231059d"}}
```

### R3-G4 — pin the space

Pin + shared 2.0 activation:

```clojure
{:shared-two-revision-id "rev:fm:space:df1e3a0b68c603be2b9d96b11fda9be75b5d779b074eced569ed0af144671fda:ec986bc027dc13c0744df5e135faf636e53ff2c2fa3b92bf7c1b887143565d8a", :worn-zoom-range [0.1 8.0], :stage :pin-holds-awaiting-browser-drive, :base-revision-id "rev:fm:space:df1e3a0b68c603be2b9d96b11fda9be75b5d779b074eced569ed0af144671fda:dab5f109f04f564adfccef4eed23392fcf52d1e971226c4d76e1343623953327", :checks {:pin-accepted? true, :shared-activation-accepted? true, :shared-is-two? true, :pin-holds-eight? true, :wear-is-pinned? true, :pin-target-is-base? true}, :source :live-cluster-server-read, :status :green, :gate :R3-G4, :shared-zoom-range [0.1 2.0], :request-ids {:request-id "space-r3-g4-pin-6cbb035d-33f6-4f83-be91-52c1dcd4659b", :activation-request-id "space-r3-g4-pin-activate-f16ede55-0bdd-4a2d-98a9-97c7b0e23313", :shared-candidate-request-id "space-r3-g4-shared-candidate-2b3a2aef-e4c5-46f1-9f88-7d5c666102aa", :shared-activation-request-id "space-r3-g4-shared-activate-5c872ae2-f105-46d5-9206-d0a5b5ccf6fa"}}
```

Pinned felt receipt:

```json
{"gate":"R3-G4-browser-pinned","status":"green","source":"live-dev-app-trusted-wheel","atMax":{"x":-4480,"y":-2800,"zoom":8},"checks":{"holdsPin":true,"wornPinnedInstance":true,"tapInstance":true,"marqueeInstance":true,"cameraFloor":true,"labelsExact":true,"pinHoldsEight":true}}
```

Unpin server + felt receipt:

```clojure
{:gate :R3-G4, :stage :unpinned-awaiting-browser-drive, :status :green, :source :live-cluster-server-read, :request-ids {:request-id "space-r3-g4-unpin-b3997f86-d054-490b-b490-a794b80b6057", :activation-request-id "space-r3-g4-unpin-activate-bc6b091a-8e53-4b7a-92db-6dd8c2cd326f"}, :shared-zoom-range [0.1 2.0], :worn-zoom-range [0.1 2.0], :checks {:unpin-accepted? true, :wear-joins-shared? true, :shared-is-two? true, :wear-is-two? true}}
```

```json
{"gate":"R3-G4-browser-unpinned","status":"green","source":"live-dev-app-trusted-wheel","atMax":{"x":-640,"y":-400,"zoom":2},"checks":{"holdsInherit":true,"wornShared":true,"tapMaster":true,"marqueeMaster":true,"cameraFloor":true,"noInstanceRows":true,"unpinFeelsTwo":true}}
```

Rollback server + felt receipt:

```clojure
{:worn-zoom-range [0.1 8.0], :request-id "space-r3-g4-rollback-aab28648-7858-4d93-a02c-7d60b7b77268", :stage :shared-pointer-rolled-back, :checks {:rollback-accepted? true, :base-active? true, :shared-restored-eight? true, :wear-restored-eight? true}, :source :live-cluster-server-read, :status :green, :gate :R3-G4, :shared-zoom-range [0.1 8.0], :active-shared-revision "rev:fm:space:df1e3a0b68c603be2b9d96b11fda9be75b5d779b074eced569ed0af144671fda:dab5f109f04f564adfccef4eed23392fcf52d1e971226c4d76e1343623953327"}
```

```json
{"gate":"R3-G4-browser-restored","status":"green","source":"live-dev-app-trusted-wheel","before":{"wornSpace":{"tier":"shared","pinned?":false,"revision-id":"rev:fm:space:df1e3a0b68c603be2b9d96b11fda9be75b5d779b074eced569ed0af144671fda:dab5f109f04f564adfccef4eed23392fcf52d1e971226c4d76e1343623953327","floor?":false},"instanceRows":[],"camera":{"x":0,"y":0,"zoom":1}},"atMax":{"x":-4480,"y":-2800,"zoom":8},"checks":{"wornBase":true,"noInstanceRows":true,"baseFeelsEight":true}}
```

### R3-G5 — server-side refusals, nothing appended

```clojure
{:camera-result {:accepted? false, :reason :facet-master/instance-form-invalid, :errors [{:type :facet-master/bindings-invalid, :actual #:space{:ground [#:binding{:gesture :wheel, :phase :complete, :modifiers #{}, :verb #:verb{:name :camera/zoom-at-pointer, :version 0}, :priority 10}]}}], :instance-master-id "fm:space~i~3f49dbbf", :subject "space"}, :checks {:camera-refused? true, :camera-error? true, :foreign-refused? true, :foreign-site-error? true, :clamp-refused? true, :clamp-error? true, :space-history-unchanged? true, :foreign-history-unchanged? true}, :source :live-cluster-server-read, :foreign-history {:before {:revisions 0, :pointers 0}, :after {:revisions 0, :pointers 0}}, :status :green, :foreign-result {:accepted? false, :reason :facet-master/instance-site-refused, :sites [:space/ground], :legal-sites [:block/fold-header :block/machine-hit-area :block/user-hit-area], :instance-master-id "fm:attention~i~7fde8f29", :subject "space-r3-g5-foreign-7edf3f90-0f5c-42a0-95c1-5c235d365973"}, :gate :R3-G5, :clamp-result {:accepted? false, :reason :facet-master/instance-form-invalid, :errors [{:type :space/zoom-clamp-invalid, :actual {:facet-master/bindings #:space{:ground [#:binding{:gesture :pointer/tap, :phase :complete, :modifiers :any, :verb #:verb{:name :anchor/place, :version 0}, :priority 0} #:binding{:gesture :pointer/press, :phase :threshold, :modifiers #{:shift}, :verb #:verb{:name :selection/marquee-begin, :version 0}, :priority 0}]}, :facet-master/pin nil, :space/zoom-max 4.0, :facet-master/deviates? true, :space/zoom-min 5.0, :facet-master/subject "space"}}], :instance-master-id "fm:space~i~3f49dbbf", :subject "space"}, :space-history {:before {:revisions 4, :pointers 9}, :after {:revisions 4, :pointers 9}}, :request-ids {:camera {:request-id "space-r3-g5a-camera-74adb31e-39c9-425b-b16a-4bc2323650c5", :activation-request-id "space-r3-g5a-camera-activate-a46bef87-9961-4524-b5cb-d6042aac244b"}, :foreign {:request-id "space-r3-g5b-foreign-b8a7c951-ee0f-4e1d-99fa-91dfa0c003fb", :activation-request-id "space-r3-g5b-foreign-activate-001fe3be-7642-4e8a-b4f8-fbdc6ddfaf3a"}, :clamp {:request-id "space-r3-g5c-clamp-409581fb-4600-45d8-8017-415cce41bce5", :activation-request-id "space-r3-g5c-clamp-activate-f41c96eb-b039-44c0-b022-7abf7cee4990"}}}
```

### R3-G6 — one-place censuses

Legality:

```text
src/app/server/rama/object_container/facet_master.clj:551:         (remove #(binding-material/instance-site-legal?
src/app/client/workspace/ground.cljs:2263:      (not (binding-material/instance-site-legal? site subject))
src/app/client/workspace/ground.cljs:2319:                         (binding-material/instance-site-legal? site facet)
src/app/shared/binding_material.cljc:114:(defn instance-site-legal?
```

Fence:

```text
src/app/shared/binding_material.cljc:128:(defn camera-gesture-reserved?
src/app/client/workspace/ground.cljs:2257:      (some #(binding-material/camera-gesture-reserved? site %) rows)
src/app/client/workspace/ground.cljs:2323:                          #(binding-material/camera-gesture-reserved? site %)
src/app/shared/space_material.cljc:68:          (not-any? #(binding-material/camera-gesture-reserved? site %) rows))
```

Read-only hashes, identical before and after:

```text
f77871c25fa0151a177109c17f679c0eb80613e18d96332f2b1a408f515347d1  src/app/shared/facet_material.cljc
09c89938ab37af320f22f82e8fd7308dc1787ec85199ebe48f0175b02d6c7ce9  src/app/shared/verb_registry.cljc
3c9372581b6a9ea8f20f714062deeeadf4d1631474d50c578d9b10d5e0c7d04b  src/app/shared/facet_masters.cljc
8f9301333c2b43ae5489efd3ed8c60bd80ff647cc310a64dcde2f2ba8fb5cc12  src/app/server/rama/material_truth.clj
1379fe874fcd8e0824fcc00c646f897fad5d7ecf174f925f591776182bc44835  src/app/server/rama/face_projection.clj
b7c7039a54f9676f11aad93be175857afb0e218a93999ffecb6fb882c2769a4e  src/app/server/episode.clj
```

`git diff --numstat` over those files emitted nothing. Added-line atom census
emitted nothing. `git diff --check` emitted nothing. The current and `HEAD`
`claim-chain` bodies are byte-identical apart from shifted line numbers, ending
in the same `(into claims binding-material/space-claim)`. The
`space_material.cljc` diff is only `space-subject`; all grammar declarations
and validators are unchanged.

The final read-plan falsification result is PASS: in the pristine state, the
fixed outer-space subject is already in `!wears-cache`; if the subject has an
instance under any facet, the slot is left cold for exactly one existing
resolve+memoize. No new atom or per-event read path exists.

### R3-G7 — label/table coherence

```json
{"gate":"R3-G7","status":"green","source":"live-dev-app-plus-server-interaction-table","install":{"status":"installed","subject":"space","site":"ground","rows":1},"clientInstance":[{"tier":"instance","facet":null,"site":"ground","gesture":"tap","phase":"complete","verb":"marquee-begin","masterId":"instance:space","revisionId":null}],"serverInstance":[],"clientFloor":[{"tier":"floor","facet":"space","site":"ground","gesture":"press","phase":"threshold","verb":"marquee-begin","masterId":"code-floor:fm:space:v0","revisionId":"code-floor:fm:space:v0"},{"tier":"floor","facet":"space","site":"ground","gesture":"press","phase":"threshold","verb":"pan","masterId":"code-floor:fm:space:v0","revisionId":"code-floor:fm:space:v0"},{"tier":"floor","facet":"space","site":"ground","gesture":"tap","phase":"complete","verb":"place","masterId":"code-floor:fm:space:v0","revisionId":"code-floor:fm:space:v0"},{"tier":"floor","facet":"space","site":"ground","gesture":"wheel","phase":"complete","verb":"zoom-at-pointer","masterId":"code-floor:fm:space:v0","revisionId":"code-floor:fm:space:v0"}],"serverFloor":[{"tier":"floor","facet":"space","site":"ground","gesture":"press","phase":"threshold","verb":"marquee-begin","masterId":"code-floor:fm:space:v0","revisionId":"code-floor:fm:space:v0"},{"tier":"floor","facet":"space","site":"ground","gesture":"press","phase":"threshold","verb":"pan","masterId":"code-floor:fm:space:v0","revisionId":"code-floor:fm:space:v0"},{"tier":"floor","facet":"space","site":"ground","gesture":"tap","phase":"complete","verb":"place","masterId":"code-floor:fm:space:v0","revisionId":"code-floor:fm:space:v0"},{"tier":"floor","facet":"space","site":"ground","gesture":"wheel","phase":"complete","verb":"zoom-at-pointer","masterId":"code-floor:fm:space:v0","revisionId":"code-floor:fm:space:v0"}],"clear":{"status":"cleared"},"checks":{"installed":true,"clientLabelExact":true,"serverDeclaresNoInstanceTier":true,"floorLabelsExact":true,"cleared":true}}
```

### R3-G8 — sweep and regression

Final focused receipt:

```text
Testing app.binding-dispatch-test
Testing app.space-material-test

Ran 24 tests containing 400 assertions.
0 failures, 0 errors.
TEST-RECEIPT {:test 24, :pass 400, :fail 0, :error 0}
```

Final fail-closed fast lane:

```clojure
TEST-RECEIPT {:shared-cluster-namespaces 8, :namespaces 25, :flake-registry-namespaces [app.server.ingest-watchers-test app.server.rama.dogfood-llm-probe-test app.server.rama.dogfood-llm-test], :shared-clusters 1, :lane :fast, :shared-modules {"app.server.rama.dogfood.llm/llm-module" {:tasks 4, :threads 2}, "app.server.rama.dogfood.space/space-kernel-module" {:tasks 4, :threads 2}, "app.server.rama.dogfood.transcript/transcript-module" {:tasks 4, :threads 2}, "app.server.rama.face-arsenal/face-arsenal-module" {:tasks 4, :threads 2}, "app.server.rama.object-container/object-container-module" {:tasks 4, :threads 2}, "app.server.rama.object-container/object-container-transcript-ops-module" {:tasks 4, :threads 2}, "app.server.rama.relation-kernel/relation-kernel-module" {:tasks 4, :threads 2}, "app.server.rama.text-kernel/text-kernel-module" {:tasks 4, :threads 2}}, :fail 0, :isolated-namespaces 26, :error 0, :pass 1795, :elapsed-ms 47809.015245, :flake-registry-status :unchanged, :pure-namespaces 17, :assertions 1795, :test 190}
```

Affected isolated receipts:

```text
app.material-truth-test       15 tests / 232 assertions / 0 failures / 0 errors
app.material-circulation-test  8 tests /  61 assertions / 0 failures / 0 errors
app.provenance-material-test   5 tests / 165 assertions / 0 failures / 0 errors
app.material-portal-test      16 tests / 284 assertions / 0 failures / 0 errors
app.face-projection-test      11 tests / 103 assertions / 0 failures / 0 errors
```

The pure fast tier also ran `app.reply-to-block-test`,
`app.binding-dispatch-test`, and `app.space-material-test`.

Final CLJS compile:

```text
[:dev] Compiling ...
[:dev] Compiling ...
[:dev] Build completed. (272 files, 3 compiled, 0 warnings, 1.47s)
```

## T-R6 moved pins and final test-tree sweep

Moved pins:

1. `binding_material.cljc`: replaced the “space excluded forever, until built”
   G10 prose with owner-aware block-vs-space law; retained the block-only
   enumeration and one-arity fail-closed behavior.
2. `ground.cljs`: moved both stale pre-rung-3 console comments—the fence
   “before rung 3 lifts” prose and “space has no instance tier” prose—to T-R4
   ordering and T-R3 owner law.
3. `material_truth_test.clj`: moved the old
   `(false? (instance-site-legal? :space/ground))` pin to one-arity
   compatibility, and added positive `:space` / negative non-space owner pins.
4. `binding_dispatch_test.clj`: the “eleven pre-cut” partial enumeration now
   pins all twelve rungs-1+2 decisions byte-for-byte; the chain grep now names
   unchanged chain shape rather than “space rows are code.”
5. `ground.cljs` interaction table: normalized keyword `:space` to exact
   `instance:space`, replacing the stale possible `instance::space`.
6. New counter-pins from falsification: a foreign durable space subject cannot
   pair with THE space claim; the outer subject's cache prewarm considers
   instances under every facet before caching the complete shared wear.

Final edited-path/symbol sweep found these test namespaces:

```text
test/app/binding_dispatch_test.clj
test/app/material_circulation_test.clj
test/app/material_inspector_test.clj
test/app/material_portal_test.clj
test/app/material_truth_test.clj
test/app/provenance_material_test.clj
test/app/reply_to_block_test.clj
test/app/space_material_test.clj
```

The unchanged namespaces were inspected and selected into the fast/isolated
receipts above as appropriate. A stale-phrase search leaves only intentional
historical language in the moved-pin test comment and the explicit
`instance::space` regression comment.

## Failures encountered and retained

1. The first `material_truth_test` run failed four assertions because the new
   G5 camera fixture began from fm:space v0. That frozen grammar has no bindings
   material key, so the hostile override was correctly dropped before the
   fence. The fixture now explicitly `ensure-master!`s v1 before the candidate.
   The final suite is 15 tests / 232 assertions, green.
2. The first G3 helper output had a valid accepted deviation, exact 3.0 wear,
   and correct camera floor, but the helper nested the complete bindings map
   under `[subject site]`; its tap/marquee checks were red. The helper now
   expands bindings into `[claim-subject site] → rows`, and the final server
   and browser receipts are green.
3. Repeating the already-content-addressed first instance revision exposed an
   existing Object Container adapter asymmetry: the first bootstrap import
   includes its active pointer, while a later normal import of the same source
   uses the same import key with a different material fingerprint. The later
   import is rejected, but the exact revision ID remains identical and the
   fresh activation lands. The helper reports both facts; no read-only
   Object Container code was changed.
4. Early Puppeteer runs used the wrong headless GPU shape and timed out. The
   successful drives use Chrome with `--headless=new` and unsafe WebGPU
   enabled. Two later red browser checks were harness-key mistakes
   (`table/tier` instead of `tier`; `revisionId` instead of `revision-id`);
   corrected receipts are banked above.
5. The fresh-context finder falsified three iterations before PASS:
   day-one wheel cache misses introduced per-event work; a facet-only durable
   bridge let another space subject hijack THE space; the first cache repair
   could mask a non-space-facet instance on subject `"space"`. All three were
   repaired and re-attacked before the final 24/400 and fast-lane receipts.

## Judgment calls

- The console coercion is deliberately guarded by both normalized
  `:space/ground` and exact string `"space"`. Coercing every subject by site
  would make R3-G2c impossible by turning a foreign subject into the legal
  space owner.
- Served extraction uses the same exact durable subject guard. Other
  space-facet subjects remain under their own durable key; they cannot hijack
  the current outer claim and remain available to the explicitly out-of-scope
  nested-space package.
- The cache repair uses only existing `!wears-cache`. It prewarms the whole
  shared wear for the fixed outer subject only if that subject has no instance
  under any facet. This is the smallest correction that makes the contract's
  no-instance wheel path a true cache hit without masking generic per-subject
  material.
- The G3 repeat-import conflict is reported, not interpreted as a stop clause:
  the contract requires fresh IDs, server-read durable effect, and
  content-addressed revision-ID equality; all are proved. The first deviation
  itself reported accepted. Fable should explicitly confirm this reading in
  gate review.
- No contract stop clause fired: the two bridge sites remained exactly two;
  claim subject/chain shape, fence count, frozen grammars, and verb registry
  stayed intact. Therefore no `decisions.md` Open Question was added.

## Fresh-context falsification verdict

PASS after the three finder-raised issues above were fixed. The final finder
re-derived:

- exact durable `"space"` vs foreign/keyword spellings at both bridges;
- exactly three owner-aware legality product reads;
- exactly three camera-fence product reads, with console ordering intact and
  the write lane inherited;
- a cached day-one wheel and one resolve+memoize after any served identity
  containing an instance for subject `"space"`;
- exact `instance:space` labeling;
- unchanged claim chain, no added product atom, and zero read-only diff.

The finder made no edits, commits, or live-cluster writes.

## Rama-retro lens

- Durable truth stayed in the existing Object Container/facet-master arteries;
  P1 added no source-specific truth island, PState, depot, topology, or module.
- Product proof crossed the full path:
  fresh action request → durable instance revision/pointer → server projection
  under facet `:space` / subject `"space"` → client bridge → worn instance
  bindings/clamp → trusted gesture → server-read release/pin/unpin/rollback.
- G5 proves retry/refusal placement physically: invalid candidates do not
  append revision or pointer history.
- The implementation did not complete a helper before durable truth was
  queryable; the live helper reads the deployed cluster and the browser drives
  the working-tree app.
- The pre-existing bootstrap import-fingerprint asymmetry is surfaced as a
  review fact rather than silently reclassified as accepted.

## Live handoff state

- Shared fm:space pointer is rolled back to the base revision
  `rev:fm:space:df1e3a0b68c603be2b9d96b11fda9be75b5d779b074eced569ed0af144671fda:dab5f109f04f564adfccef4eed23392fcf52d1e971226c4d76e1343623953327`
  with `[0.1 8.0]` active.
- The space instance master remains durably present in released/inherit state;
  no deviation or pin is worn.
- The console instance lane is cleared.
- Rama cluster remains `:active`; cluster UI on `:8888` returns HTTP 200.
- Working-tree dev app PID `1029880` remains live; `http://localhost:8080/`
  returns HTTP 200.
- Next owner: Fable independent slim gate review. No commit word has been
  received; the worktree remains uncommitted.
