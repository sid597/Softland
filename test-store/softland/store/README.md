# Store proofs

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

| File | What it checks | State that it owns or changes |
|---|---|---|
| [act_proof.clj](act_proof.clj) | [The act](../../../src-store/softland/store/act.clj): its declared shape and rules, the check that reads them, its names, the stand-in sealed form and the digest. It needs no cluster. | One atom: the lines that it showed. |

## The act's proof

Each check is a case: what it is about, a function of an implementation, and what
that function must give. The proof gives each case the act's own functions.

What it checks, group by group:

1. The two declarations, the shape and the rules, each written out a second time;
   and that the check reads them: for each part that the shape of an act names, an
   act without the part, and an act with a thing of no shape in it, get the reason
   that the declaration gives.
2. Acts that pass, each in its one form: one sealed value by a person into her own
   layer; the same act as said, through its sealed view; an act of store-key facts
   only; a retract; several facts with a mark on one; an act that the store places
   itself, under a name with no class; an act into a layer that is placed by
   entity; an act with each optional part, whose one form has plain vectors and
   longs and parses again to itself; and the least act.
3. One refused act for each fault, with its reason. Each differs from an act that
   passes in one thing only. Each reason word comes from an act that earns it.
4. The order of the checks, which is the order of the declarations: an act with
   two faults gets the reason of the shape before the reason of a rule, and of the
   earlier part or rule before the later one.
5. Nothing throws: the check, the sealed view and the test of a name are each given
   things that are not acts, and each gives back data.
6. The limits at their edges: the subjects, the collections around a store key's
   value, a carried stamp, and the depth of a permission.
7. Names: a made name and its tag, the name for an act that the store places
   itself, the two names that the store makes from a request, and what is no name.
8. The stand-in sealed form: an act as said is refused as it is and taken through
   its sealed view.
9. The digest: what it leaves out, and that a change of each part that it covers
   gives another digest. What the digest does not see is a check of its own: two
   acts that differ only in a sealed value have one digest.
10. Ids and values: what is a value, the one normal form, and the canonical text.

### What the act's proof does not reach

- an act after Rama wrote it to a depot and read it back: each case is a value
  of this process;
- facts that are a sequence with no end, for which the check does not come back;
- an act of many facts, and how long the check takes;
- that two different values never give one canonical text, beyond the examples;
- the id of a made name, beyond its version and that two ids differ;
- the use of the act by a door or a gate: none is built.
