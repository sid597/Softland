# Inland authored material and browser shell

[Product source entry](../../src-inland/README.md) ·
[Runtime consumer](../../src-inland/softland/inland/README.md).

This folder supplies genesis material and the minimal browser shell. It owns no
accepted runtime state. Once seeded, the rig's store holds the workbench's records
as facts in the base; editing this file does not directly change a live accepted
definition.

| Immediate file | Role |
|---|---|
| [seed.edn](seed.edn) | 64 starting records: world/session defaults, the pointer and its rules, event patterns, named recipes, views of the repository's material, the editor, the pair's controls, notes, and the older example material. |
| [index.html](index.html) | Loads the compiled Electric browser entry and stylesheet; visible application content is created by Softland rendering. |
| [style.css](style.css) | Page/canvas sizing and hidden native input delivery; authored descriptions choose visible controls and arrangement. |

At the store's first launch the host puts every record into the base as facts, by
the operator (`seed/put-genesis!`), one fact per attribute. `bin/inland seed` puts
again the records whose genesis changed, each change a new fact replacing the head
it changes, while the base is one-owner; after the first group makes the base
shared, a change is written through the editor like any other. It never removes a
record. Edit through the product when changing accepted live material.

The seed has several cooperating families. Use these names to locate a record in
[seed.edn](seed.edn), then follow references instead of reading the whole vector:

- `world` supplies session defaults; `admission-policy` supplies the narrow machine
  behavior policy. The selection and the context (with its pins) are stored cells:
  facts on the page's session in the person's hand layer, standing on what the
  gesture that wrote them read. Every other cell stays in the browser.
- `pointer` refers to `targeting`, the rule a pointed line goes through:
  `targeting` selects the line; `containing-unit` the passage or form around it;
  `function-or-section` the function, or the Markdown section. Each reads the
  material's reading (`:material/file`, the citation session's) and indexes its
  `:unit-by-line` or `:section-by-line`. `pointing-target` calls the active
  instrument's rule; `point-rule` stores the result as the selection, or, pointing
  at the instrument itself, opens its rule in the editor.
- `material-view`, `material-line`, `file-row` and `note-row` show the repository's
  files at the revision read, a page of lines, the selection's band and the notes on
  the file. `frame-view`, `instrument-view`, `instrument-appearance`, `editor-view`,
  `selection-view`, `status-view` and the library/tool rows compose the rest. View
  patterns read session context to decide applicability. A `:repeat` description
  calls its named row recipe.
- Editing, keep, context, pin, share and the pair's rules return generic
  session/admission effects; the host writes them as acts standing on what the
  gesture read. `keep-rule` keeps a copy of the rule with the copy of the tool, so a
  kept instrument stands on the rule it was kept with. `share-rule` promotes the
  inspected record from the person's layer into the pair. `start-pair-rule`,
  `add-member-rule` and `use-pair-rule` make the pair, accept a person into it and
  read it. `note-rule` writes a note on the selection into the pair, naming a person.
  `draft-example-rule` puts either material rule into the editor.
- `selection-view`, `library-row`, `editor-view` and `pair-view` show marks: what
  stood on a fact the screen saw change, as the store's lookup found it.
- `walk-from-scene` calls `walk-step`, whose frontier/visited computation lives in
  the recipe over `assembly`, `orb` and `ring`; `walk-rule` requests a view-owned
  repeated activity. (The walk has no tab in the frame now.)
- `ask-rule` requests a durable external activity. `resident-view`, `activity-row`
  and `observe-rule` expose its status and reply; the reply is a stand-in until Sid
  says yes. Cancellation can leave an outcome unconfirmed.
- `support-orb` and `support-ring` answer the same demand independently; `no-parent`
  demonstrates complete-absence matching. No view shows them now.

A named recipe is a record with `:body`, ordered `:steps`, explicit output names
and a `:return` expression. For example, the rule that selects the passage or form
around a pointed line reads the file's reading and indexes it:

```clojure
{:name "containing-unit"
 :body {:steps [{:out :reading :op :read :args {:name [:get :subject :file] :attr :material/file}}
                {:out :i :op :value :args [:nth [:get :reading :unit-by-line] [:- [:get :subject :line] 1]]}
                {:out :unit :op :value :args [:nth [:get :reading :units] [:get :i]] :when [:get :i]}]
        :return [:if [:get :unit] {:file [:get :subject :file] :from [:first [:get :unit :lines]] ...} ...]}}
```

`[:get ...]` reads bindings; `[:literal ...]` preserves data without evaluating it.
The exact expression language lives in `total/expression` and its leaf table.
Tracked step capabilities live in `execution/Step`; a gesture runs the same steps
once in `gesture`. Named calls resolve accepted records in the active layers or
pins. Pattern read conditions live in `execution/Conditions`. Generic effects are
validated by `total/effect-error`. See the [runtime map](../../src-inland/softland/inland/README.md)
to descend to those contracts. There is no host `eval` for these records.

Who wrote a record, when, and under which permission is the store's envelope, not
something the seed supplies. Record validation is structural, and the store checks
each record key's grammar at the gate; bounds and known language limits are beside
`total`, `execution`, `logic` and `activity`.
