All four write-ups are loaded. Pulling exact line anchors from the baseline before I reason on top of them.

Read all four. Here is the answer, then the working, then the gaps.

**The thing all three made up is the mounted tool.** A tool record, pointed at the records it reads, placed in a saved position, with its last full input kept so the loop knows when to rerun it and a hit knows what it landed on. Each build then wrote, by hand, the loop that walks its own mounts: gather the roots, decide whether to run, publish the result, answer a pointer. Three loops, three field lists, three names. Place calls it an active tool declaration. Workspace calls it a representation. Vantage calls it a tool instance. As one thing in the layer it is one record kind and one loop, and the vantage becomes a list of mounts plus a selector for which revisions their addresses resolve to. The next build gets smaller because every "X reruns Y but not Z" rule the three accounts list becomes a consequence of the binding rather than a line of code. More of it lives in records because Vantage's fixed roots assembly and Workspace's brush adapter become data the Place build already has, and Place's missing selector becomes data the other two already have.

Everything below is read off the four documents. Nothing was rerun. Anchors: B = BASELINE.md, E = ECS-LAYER-WRITEUP.md, W = SESSION-HANDOFF.md, V = ROUND-ACCOUNT.md, with line numbers.

## The one thing

The baseline names the hole exactly: an executor that runs a `{:program :roots}` record when handed roots as values, three kinds with locators and renderers, and no owner of "a world atom and ongoing browser events, the scheduling of its affected tools, the common pointer identity across representations, and the view/navigation/editing records" (B:44, B:85-88). Every build filled that hole with the same four-edged object.

```
                 roots in                      results out
   records ───(bound by address)───▶ MOUNT ───(declared ids)───▶ records
                                      │  ▲
              answers in              │  │        hits out
   pointer / next point / store ──────┘  └──── point → record address

   mount = tool record + roots-from + outputs + placement + last full input
   vantage = subject + mounts + selector (pinned rev | layers) + pins + parent
```

What it is built out of, all of it already in the baseline: the executor's roots contract (B:44), the executor's pending-read request and answer (B:80-82), the kind locators (B:47-48, the Path and Text rows), and the store's id plus revision envelope (B:136). The only new substrate is an address vocabulary, a record id with a path and a revision selector. The loop is then small: gather by address, run or resume when the gathered value differs from the last, publish under declared ids, answer a hit from the same gathered value.

`★ Insight ─────────────────────────────────────`
In a textbook ECS the systems are code and the scheduler is generic: it queries components and runs whichever systems match. Softland inverts half of that. The system is already data, a tool record the executor interprets. What none of the three made generic is the other half, the scheduler. Place's own write-up says this in its words: the ECS name covers the loop over id-addressed records, not a generic component-query scheduler, and the client carries specific dispatch for the brush, inspector, flow lanes and Inside (E:183-186). Workspace says the same: systems are explicit functions and commands, no scheduler, no automatic dependency graph (W:86). Vantage says it as a fixed assembly interpreting the vantage (V:258).
`─────────────────────────────────────────────────`

Which build got which edge as data:

| Edge | Place | Workspace | Vantage |
|---|---|---|---|
| Roots bound by address | Yes, `:roots-from` names a record and a path (E:227-228) | No, the sphere recipe adapter is code (W:76) | No, the fixed conversation/map/roots assembly is code (V:258); roots are saved values (V:209-210, V:259) |
| Outputs declared as record ids | Yes, the painting declaration and produced painting have separate ids (E:200-201, E:230-231) | Run entities hold painting and observations (W:76) | No, retained results are temporary derived state (V:261) |
| Rerun decided by the full input | Yes, dirty-exactness (E:198-201, E:229-232) | No, runs are commands (W:48) | Yes, complete returned values decide; markers only route (V:178-179) |
| Hit grounded in the same input as the pixels | Yes, shared inputs and the picking-input-boundary repair (E:203-204, E:312-317) | Not stated (W:52) | Yes, a read against complete snapshots, refused if stale (V:163-166) |
| Placement in the position record | Fractional placements in the vantage (E:172) | Main, companion or kept (W:80) | Not stated; composition from records not built (V:258) |
| Selector for which revision an address resolves to | None; overlays listed as missing (E:346) | Pinned in the reference itself, id plus revision (W:64, W:98) | Selected layers in the vantage over shared base (V:26-28, V:46, V:104-106) |

Derived: no single build has the whole object. Place has the binding and the rerun law. Workspace has the exact reference and durable results. Vantage has the selector, the snapshot-guarded hit, and the durable vantage with a parent. The one thing is the union at the seam, not one build's shape.

Why the next build gets smaller. Each account writes down, as prose or as tests, rules of the form "this edit reruns that tool and not the other". Every one of them is a fact the binding plus full-input comparison yields with no rule written:

- Place: radius re-executes the saved stroke, flow width changes the lanes without rerunning the brush (E:260-261); painting and hover change composition without rebuilding binding geometry, camera movement changes neither (E:243-245).
- Vantage: map edits rerun the map, camera edits prepare the scene without brush computation, brush edits rerun the sphere (V:231-233); a promoted source edit reruns one tool, a font change reruns all visible sources, a placement change shapes zero lines (V:186-190).
- Workspace: Anatomy selection, orbit and reveal never run the brush; a tab change waits for the server but does not rerun the brush (W:104).

Place gets those for free already and its doc says the test name (E:231-232). Vantage gets the second half free and hand-wires the first half through which dirty target keys map to which tool (V:176-178). Workspace writes them as command semantics. One loop, zero written rerun rules.

`★ Insight ─────────────────────────────────────`
The same value does three jobs in this design and the three builds each discovered one or two of them. The full gathered input is the rerun key in Place (E:229). It is the read-reuse key in Workspace, where read result rows sit under the capability plus its full argument values (W:82). It is the hit ground in Vantage, where a pointer read is answered only if the snapshot still matches (V:163-166). The baseline executor already compares every input on resume (B:80-81). Vantage states the negative half of the law outright: delivery markers route work, they are not semantic identity or cache keys (V:178-179). Keeping the last full input on the mount is what lets one field serve all three.
`─────────────────────────────────────────────────`

Why more of it lives in records. Under one mount, these move from code to data: Vantage's roots assembly (V:258) and Workspace's brush adapter (W:76) become `:roots-from` entries; Workspace's command list for what runs (W:48) becomes the comparison; Place's per-vantage layer question becomes the selector Vantage already saves (V:87-88). What stays code, and all three agree, is capabilities (E:170, W:79, V:259), one locator-to-address rule per kind (E:342 names identity-to-lines and trace-to-lines), gestures and key bindings (E:342, W:81, V:263), and panel templates (E:175, W:92, V:258).

That last item is the lived-failure point. The previous client died of a block anatomy fixed in code: pointer and cursor disagreed, typing slowed, paste and layout misbehaved (E:32-34, V:19-21). All three new clients have a panel anatomy fixed in code, one level up: Stage, Flow and Inside (E:140-149); Spatial, Flow, Text and Inspect (W:50); thread, map, sphere and roots (V:236). Two of the three built the pointer-agreement guard the old client lacked. None of them made the anatomy a record. A mount with a placement field is the smallest step that does.

## Same thing, three shapes

The full translation, one row per piece, so each arc sits under each build's own words.

| Piece | Baseline had | Place, branch codex/ecs-layer | Workspace, same tree as Vantage | Vantage, same tree as Workspace |
|---|---|---|---|---|
| Record envelope | Store revisions, kind, visibility (B:136) | Id, revision, asserter, in memory (E:341) | Entity: id, kind, revision, components, provenance; reference carries id and revision (W:64) | Container per asserter, immutable revisions (V:88-89) |
| Saved position | None (B:85-88) | Vantage: camera, placements, ancestry via `:from`, trail leaf (E:172, E:272-275) | Workspace context: baseline and candidate refs, selected run and view, note, kept refs, saved view (W:81, W:58) | Vantage: subject, open tool ids, pins, zoom, selected layers, asserter, parent (V:87-88) |
| A tool on screen | Executor takes roots as values (B:44) | Active tool declaration with `:roots-from` and output ids (E:67, E:227-231) | Representation: subject ref, tool ref, view, placement (W:80) | Tool record with saved roots and program, listed in the vantage (V:29, V:236, V:259) |
| Outside input mid-run | Pending reads with request and answer (B:80-82) | New `:hold-at-end` for the next stroke point; pending reads stay barriers (E:233-238) | Withheld coating read, answered with work allowance; continuation transient (W:54, W:82) | Pointer-down opens a read, the later event answers it, snapshot-checked (V:163-166) |
| Pixel to record | Per-kind locators, no join (B:47-48) | Identity rules per kind; pointer identity and stable inspection as separate records (E:249-253, E:342) | Bounded correspondence per specimen (W:52) | Compositor retains hit anatomy; roots panel rows carry field paths (V:73-75, V:169-171) |
| Edit a tool from inside | Nothing | Numeric field, Enter emits a normal change (E:259-263) | Relation and arrangement picker, new tool revision, pinned original (W:56) | Full EDN root and program editor with lossless parser spans, same courier as a passage (V:169-173, V:319-321) |
| Keep the earlier one | Immutable revisions in the store (B:136) | Replacement only (E:346) | Exact references; baseline keeps its own brush and result (W:48, W:98) | Authoring layer per asserter, promotion refuses stale (V:104-110) |
| The wire | Import, edit, revision read APIs; edit path loses kind (B:136) | Proposed only, ASK-1, a separate place-records store (E:357-359) | Adapter over material-import and revision APIs, bypasses the legacy edit path (W:94) | Legacy edit path repaired: kind and visibility preserved, graduation ids widened (V:91-92, V:304-312) |
| A run's record of itself | None | Trace per tick in a bounded ring; flow lanes read it (E:119, E:207) | Flow steps per dab, run observations as entity data (W:39, W:76) | Not stated as a record; a run is addressable behind a hit (V:53) |
| Text as a tool | Pure layout, not a capability (B, Text row) | Executor text capability and program, ops unnamed (E:72) | Native text through compiled templates, not a tool (W:90, W:92) | Text tool record with `:text/layout` and `:text/place` (V:56-58) |
| A string for the agent | Turn rows, invocation defaults (B:138) | None (E:345) | Context tool renders text with exact source revisions, no run (W:60) | Prompt-tool record, preview, turn, reply staged in the agent's layer (V:130-150) |
| Explain yourself | None | Inside: authored nodes; amber records, rose client code, teal shared code (E:131-138, E:149-150) | Anatomy: authored studies; teal records, amber application code, blue engine (W:112-119) | M6 map from imported code and analyzer relations, discovered not authored (V:197-212) |
| Panel layout | None | Stage, Flow, Inside in code (E:175) | Four tabs as templates (W:92) | Fixed assembly (V:258) |

Three rows deserve a sentence each.

**Explain yourself was built twice from the same question.** Sid asked both Codex clients what was records and what was code (E:127-129, W:110). Both answered with an authored catalog, a pull-apart 3D view, a flat map and text, and the same three-way partition. They even swapped the colours: Place paints records amber and shared code teal, Workspace paints records teal and application code amber. Vantage's map is the discovered version of that catalog, built from the importer and analyzer over committed HEAD. As one thing: the partition is a tag on imported units, Inside and Anatomy become a view over Vantage's catalog, and the authored nodes go to zero. The question you are asking me now is that same question a third time, and in no build is it a query. With mounts as records it would be.

**Two wires into one store, on one branch.** Workspace bypasses the edit path that loses kind; Vantage fixes that path and later widens graduation ids because "a Vantage-only workaround would leave record identity wrong elsewhere" (V:311-312). Both sit in one checkout (W:144). Place proposes a third wire. One typed-record adapter, using the repaired path, replaces all three.

**Two text capabilities.** Place and Vantage each turned text layout into an executor capability. Place names no ops and keeps the production text tree unchanged (E:72, E:96). Vantage names two ops and puts the tool inside the text tree (V:56-58, V:77). Same capability, two vocabularies, two homes.

The strongest alternative answer is "the vantage itself". All three built one, and its parent chain is the piece all three kept as data. I rank it second because the vantage already is a record in every build; where the three disagree is what a tool on screen is, and that is the mount. The vantage is the list of them.

## Where the write-ups don't say

Each of these is a finding for you, not a gap I filled.

1. **Vantage: are tool roots addresses or values?** The map's columns and colours are editable roots (V:209-210) and the sphere's roots expose host, brush, resolution and camera (V:230), but the account never says a root can name another record. This decides whether Vantage's assembly can become data at all.
2. **Workspace: how run inputs bind to the subject.** The adapter is code (W:76). The doc does not say whether the run's inputs are references into the subject entity or copied values. It also never describes any automatic rerun; every run is a command (W:48).
3. **Place: does the vantage list its mounts?** The vantage row lists camera, placements, ancestry and spread (E:172). Active tools are declared somewhere (E:67), but whether per vantage or globally is not said.
4. **Place: why a second suspension kind.** `:hold-at-end` was added to the executor for the next stroke point while pending reads stay barriers (E:233-238). The doc gives the measurement that motivated it and not why the next point was not modelled as a read.
5. **Executor edits on two branches.** Place changed the executor's continuation handling (E:279). Vantage gave a completed layout the resolved-read boundary after profiling unresolved-read traversal (V:66-68), and does not say whether that was an executor change or a capability change. If both touched read handling, nothing says they compose.
6. **Two wires, one tree.** Neither Workspace nor Vantage says whether Workspace's bypass route inherits Vantage's graduation-id fix, or whether Workspace could now use the repaired edit path.
7. **Workspace hit guard.** Nothing says a hit during a pending change is checked against the inputs that drew the pixels. Both other builds do this and both report a real failure that forced it (E:312-317, V:163-167).
8. **Vantage run records.** A run is addressable behind a hit (V:53), but the account never says what a run record holds, if there is one. Place and Workspace both describe what a run leaves behind.
9. **Vantage placement.** No field for where a tool sits is named anywhere in the vantage value.
10. **Selector composition.** Workspace pins a revision in the reference. Vantage selects a layer in the vantage. Nobody built both, and no account says what a pinned reference read through a candidate layer means.
11. **Kinds.** Vantage says plainly that kinds are a code-defined vocabulary (V:270-271). Workspace and Place do not raise it. No build makes kinds records, and no build says whether the mount's tool field is itself a kind.
12. **Results as records.** This one is stated, not missing, and the three diverge: Place keeps run results in the world atom (E:218), Workspace makes them durable entities (W:76), Vantage calls them temporary derived state and recomputes at boot (V:220, V:261). Under one mount, durable or recomputed is a policy on top of a keyed result, which is your caching-only-on-top line, and Place says it held that line (E:240).

My position: build the mount and the loop once, from Place's binding, Workspace's exact reference, and Vantage's selector and snapshot-guarded hit, and let the three existing clients become three vantages over it. What would refute this: if Vantage's roots turn out to be values by design because a root that names a record breaks the layer selector, item one above, then the binding has to carry the selector and the mount is bigger than I drew it.
