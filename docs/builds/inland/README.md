# Inland — run and use the build

Inland is the current authored-workbench build under `src-inland/`. It uses
Electric, Rama, and Softland's renderers to test editing and reusing definitions
inside the running environment. This guide describes that build's use and scope;
the larger aim remains in [carry-on](../../carry-on.md).

For implementation questions, start at the [source map](../../../src-inland/README.md).
The [intended design](intended-design.md) describes the experiment's construction
model. [How we got here](../../how-we-got-here.md) explains the questions behind it.

## Run it

From the repository root:

```sh
bin/inland up
```

Open **http://localhost:8127**. Add `?workspace=your-name` for a separate durable
workspace created from the authored seed. Revisiting that URL retrieves its
accepted material. Selection, drafts, pins, and active context belong to each
browser session.

The launcher requires the Java/Clojure and Node tooling, a Rama distribution,
Electric compiler activation, and a WebGPU-capable browser. Provider actions
also require the normal Claude CLI authentication. [deps.edn](../../../deps.edn)
and the [launcher](../../../bin/inland) hold the dependency and launch settings;
`INLAND_RAMA_RELEASE` can select another local Rama distribution.

```sh
bin/inland status
bin/inland down
```

`down` stops owned processes and preserves saved material under `.inland-runtime`.
Startup refuses occupied ports with unknown ownership. `bin/inland seed` refreshes
untouched genesis records in the default workbench while preserving user-edited
records. This build uses isolated storage; these commands do not migrate the
canonical server's existing data into it.

## Try the authored construction

1. Point at Orb and Ring. Each initially selects itself. Point at the instrument
   below the scene to open its executable `targeting` definition.
2. Choose **Draft a containing-object rule**, then **Apply record**. Point at
   the parts again: the accepted rule selects Assembly. An invalid record is
   rejected and leaves the previously accepted rule active.
3. Use **Open presentation** to edit `instrument-appearance`, or **Open this
   editor** to edit `editor-view`. The visible arrangement and editor behavior
   are authored records over the build's available primitives.
4. Name a variation and choose **Keep**. Its named references initially remain
   shared. **Keep record as…** creates a separate definition; changing the tool's
   reference makes the variation use it. Candidate and pin/follow controls make
   the chosen definition context explicit.
5. In **Walk**, inspect the authored frontier/visited algorithm, change its
   budget, or cancel. In **Resident**, **Ask** makes a bounded real provider
   request. **Open reply record** retrieves the accepted text; activity history
   also exposes non-success outcomes. Closing and reopening the canvas preserves
   durable material and distinguishes it from view-owned work.

The editor is a small EDN record editor. The build has not demonstrated a full
visual recipe editor or an in-world source/compiler/renderer rebuild loop.

## Check behavior and follow the evidence

The [verification map](../../../test-inland/README.md) explains the focused,
browser, and recovery checks. To run the registered checks:

```sh
bin/inland up --test-controls
bin/inland check
bin/inland verify
```

The browser verification uses the local graphical display and makes a small
real provider request. Its recovery scenario replaces the browser and app JVM
against the retained Rama cluster; it does not establish full cluster failover
or disk-loss recovery.

The [recorded close](../../../history/docs/build-softland-in-softland/NOW.md)
names its source version and reports 5 tests / 72 assertions and 7 browser/recovery
checks. [Preserved evidence](../../../history/docs/build-softland-in-softland/evidence/)
contains the corresponding logs, browser JSON, and images. Those are historical
results, not a new run performed for this guide.

The [runtime map](../../../src-inland/softland/inland/README.md) records source
limits beyond those receipts, including event slots, repeated admission,
relational closure, shared scene ownership, and version identity. Narrow
dependency and lifetime checks do not establish general performance, language
sufficiency, permanent Electric adoption, or integration with every existing
server capability.

The [original build account](../../../history/docs/build-softland-in-softland/HANDOFF.md)
and [native capability log](../../../history/docs/build-softland-in-softland/NATIVE-CAPABILITIES.md)
preserve what the experiment required and revealed, including compiled repairs.
