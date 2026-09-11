# Inland authored material and browser shell

[Product source entry](../../src-inland/README.md) ·
[Runtime consumer](../../src-inland/softland/inland/README.md).

This folder supplies genesis material and the minimal browser shell. It owns no
accepted runtime state. Once seeded, Rama owns the workspace's records; editing
this file does not directly change a live accepted definition.

| Immediate file | Role |
|---|---|
| [seed.edn](seed.edn) | 56 starting records: world/session defaults, instrument references, event patterns, named recipes, views and example material. |
| [index.html](index.html) | Loads the compiled Electric browser entry and stylesheet; visible application content is created by Softland rendering. |
| [style.css](style.css) | Page/canvas sizing and hidden native input delivery; authored descriptions choose visible controls and arrangement. |

`module/seed-rows` reads the packaged seed. An accepted seed request installs it
only when the world's current record is absent. New workspace URLs use that
packaged genesis; existing workspaces keep accepted edits. `bin/inland seed`
explicitly refreshes only missing/still-genesis-owned records in `workbench`,
through `seed/-main` and ordinary admission. It does not remove obsolete records
or migrate other workspaces. Repackage/deploy when changing the module's genesis
resource; edit through the product when changing accepted live material.

The seed has several cooperating families. Use these names to locate a record in
[seed.edn](seed.edn), then follow references instead of reading the whole vector:

- `world` supplies session defaults; `admission-policy` supplies the narrow machine
  behavior policy read by admission. `assembly`, `orb` and `ring` are ordinary
  material with relationships and shape components.
- `pointer` refers to `targeting`. `pointing-target` resolves the active instrument
  and calls its targeting definition; `point-rule` turns the result into selection.
  `containing-object-or-self` is the reusable alternate behavior.
- `frame-view`, `stage-view`, `instrument-view`, `instrument-appearance`,
  `editor-view` and the library/tool rows compose presentation. View patterns read
  session context to decide applicability. Text/path/hit/input/scene descriptions
  are consumed by `paint`; a `:repeat` description calls its named row recipe.
- Editing, keep, candidate, context, pin, promotion and visibility rules return
  generic session/admission/visibility effects. They do not mutate Rama directly.
  `targeting-example` and `draft-example-rule` put a real editable recipe into the
  record editor; the example is material, not a client tool-name branch.
- `walk-from-scene` calls `walk-step`, whose frontier/visited computation lives in
  the recipe. `walk-rule` requests a view-owned repeated activity; the host supplies
  bounded stepping, yield and cancellation rather than a native graph traversal.
- `ask-rule` requests a durable external activity. `resident-view`, `activity-row`
  and `observe-rule` expose its accepted status/reply. Cancellation can leave an
  outcome unconfirmed; the authored record does not make an uncertain call safe
  to retry.
- `support-orb` and `support-ring` answer the same demand independently.
  `support-view` exposes their retained alternatives. `no-parent` demonstrates
  complete-absence matching; unknown or pending reads cannot establish absence.

A named recipe is a record with `:body`, ordered `:steps`, explicit output names
and a `:return` expression. For example, this existing alternate behavior reads
an addressed parent and falls back to the subject:

```clojure
{:name "containing-object-or-self"
 :body {:steps [{:out :parent :op :read
                 :args {:name [:get :subject] :attr :parent}}]
        :return [:or [:get :parent] [:get :subject]]}}
```

`[:get ...]` reads bindings; `[:literal ...]` preserves data without evaluating it.
The exact expression language lives in `total/expression` and its leaf table.
Tracked step capabilities live in `execution/Step`; named calls resolve accepted
records in the active layers or pins. Pattern read conditions live in
`execution/Conditions`. Generic effects are validated by `total/effect-error` and
delivered by `session/effects!`. See the [runtime map](../../src-inland/softland/inland/README.md)
to descend to those contracts. There is no host `eval` for these records.

Accepted metadata such as revision, actor and request id is assigned by admission,
not proof supplied by the seed. Record validation is structural; bounds and known
language limits are beside `total`, `execution`, `logic` and `activity`. The
[demonstration receipts](../../docs/build-softland-in-softland/HANDOFF.md) establish
which authored paths actually ran.
