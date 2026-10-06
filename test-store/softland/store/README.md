# Store proofs

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

| File | What it checks | State that it owns or changes |
|---|---|---|
| [act_proof.clj](act_proof.clj) | [The act](../../../src-store/softland/store/act.clj): its declared shape and rules, and the check that reads them. It needs no cluster. | One atom: the lines that it showed. |

## The act's proof

Run it from the repository root:

```sh
clojure -M:store -m softland.store.act-proof
```

Each check is a case: what it is about, a function that calls the act's code, and
what that function must give.

What it checks, group by group:

1. The parts. The two declarations, the shape and the rules, each written out a
   second time as literals; the parts of an act and of a fact, and the reason
   words, read from them; that a name, a class, a seal, a lock id, a layer and a
   mark are no parts; the version and the limits. And that the check reads the
   declarations: for each part of an act and of a fact, a thing without the part,
   and a thing with a thing of no shape in it, get what the declaration gives.
2. Acts that pass, each shown in its one form: from the least act (version, who,
   layer, permission, session and one fact), through a retract, plain values
   under any key, ids that are UUIDs, a permission that a parent granted, and an
   act that replaces a fact and says what it was based on, to an act with each
   optional part, given with subvectors, narrow integers and a list of facts. Its
   one form has only plain vectors and longs, and each one form parses again to
   itself.
3. One refused act for each fault, with its reason. Each differs from the least
   act in one thing. The proof reads the reason words from the declarations, and
   each word must come from an act that earns it.
4. The order of the checks, which is the order of the declarations: an act with
   two faults gets the reason of the earlier part, and of the shape before a rule.
5. Nothing throws: the check, and the test of each declared shape, are given
   things that are not acts, a thing that throws when it is read among them, and
   each gives back data.
6. The limits at their edges: the subjects, the collections around a value, a
   carried stamp, and the depth of a permission.
7. Ids and values: an id that is a UUID passes, and one that is a string fails;
   a key that is a UUID fails; a retract is a fact with no `:v`; what is a value,
   and its one normal form; the shapes of a name, a fact id and a permission id.

### What the act's proof does not reach

- an act after Rama wrote it to a depot and read it back: each case is a value
  of this process;
- facts that are a sequence with no end, for which the check does not come back;
- an act of many facts, and how long the check takes;
- the use of the act by a door or a gate: none is built.
