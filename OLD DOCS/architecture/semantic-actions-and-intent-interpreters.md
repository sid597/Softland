# Semantic Actions And Intent Interpreters

That’s a big missing piece, yes.

What you’re describing is:

```text
semantic actions are not only hand-coded
they are also INFERRED
from
- current world
- user history
- personal style
- local context
- longer-term patterns
```

That means the system is not just event-sourced and reactive.

It is also **interpretive**.

## The key architecture shift

The old/simple model is:

```text
raw input
  ->
explicitly coded semantic action
  ->
world update
```

What you’re pointing at is:

```text
raw input
  + current world
  + personal history
  + learned user model
  ->
inferred semantic action
  ->
committed semantic action
  ->
world update
```

That is a different kind of system.

## This is the important split

You should separate:

```text
1. SIGNAL
What physically happened

2. INTERPRETATION
What the system thinks you meant

3. COMMITMENT
What action becomes real in the shared world
```

ASCII:

```text
click / key / drag / utterance / gesture
                  |
                  v
         +-------------------+
         | semantic interpreter|
         | personal AI         |
         | global AI(s)        |
         +-------------------+
                  |
        candidates / confidences
                  |
                  v
         +-------------------+
         | commitment policy |
         | accept / confirm  |
         | defer / ask       |
         +-------------------+
                  |
                  v
         canonical semantic action
                  |
                  v
              Rama event
                  |
                  v
              world update
```

That middle part is what your current framing adds.

## Why this matters

Because the user expectation is often not fully explicit.

Examples:

```text
undo
```

might mean:
- undo last typed token
- undo last edit block
- undo last semantic transform
- undo last accepted AI patch
- undo last workspace reconfiguration

Which one is “correct” depends on:
- current mode
- recent behavior
- learned personal conventions
- maybe even the rhythm of your work

So yes, a personal AI can help infer the right semantic action better than a rigid hardcoded ruleset.

## But there is one crucial constraint

You cannot let the inferred intent be the final truth by itself.

Because Softland wants:
- replayability
- public form
- transfer across minds
- calibration
- lineage

If the meaning lives only inside a private learned model, then the world becomes non-replayable and non-public.

So you need:

```text
PRIVATE INFERENCE
!=
PUBLIC COMMITMENT
```

This is the crucial architecture law.

## Best version

```text
personal AI
  infers candidate intent

system
  commits canonical semantic action

event log
  stores the canonical action
  plus provenance about inference
```

For example:

```clojure
{:event/type :semantic-action-committed
 :action {:type :undo
          :target :last-edit-burst
          :scope :editor}
 :interpreted-from {:signals [...]}
 :proposed-by {:agent :personal-ai
               :confidence 0.91}
 :confirmed? true}
```

That gives you both:
- learned adaptive semantics
- durable explicit replayable world history

## This is probably the right stack

```text
RAW SIGNALS
mouse, keyboard, text, commands, pauses, rhythm, context

        |
        v

PERSONAL SEMANTIC INTERPRETER
your AI, trained on your patterns

        |
        +----> candidate action A (0.91)
        +----> candidate action B (0.06)
        +----> candidate action C (0.03)

        |
        v

COMMITMENT LAYER
auto-accept / ask / preview / negotiate

        |
        v

CANONICAL SEMANTIC ACTION
explicit, replayable, shareable

        |
        v

RAMA EVENT LOG
durable history

        |
        v

ELECTRIC WORLD DERIVATION
live current world

        |
        v

WEBGPU / UI / TRAILS / PUBLIC FORM
```

## This also explains “many AIs”

Exactly as you said, there can be many AIs at different levels.

Something like:

```text
GLOBAL AI
general world knowledge / structural suggestions

PERSONAL AI
your habits, your preferences, your style of intent

LOCAL-WORLD AI
specific to this file / trail / topic / project

WORKFLOW AI
editor AI, synthesis AI, DG AI, review AI

PUBLIC AI
shared interpretation available to others
```

That means LLMs are not just “tools inside the app.”
They become part of the **semantic interpretation ecology**.

## What must still stay explicit

Even if LLMs infer intent well, these things still need explicit structure:

- canonical action grammar
- confidence / calibration
- provenance of interpretation
- confirmation policy
- scope of action
- reversibility / undo model
- which actions are private vs shared vs durable

Otherwise the world becomes magical but unverifiable.

## The deepest reframe

Softland may need to be thought of as:

```text
not just an event-sourced world
but an event-sourced world
with learned semantic interpreters
sitting between signal and commitment
```

That’s a very important refinement.

## Short version

You’re pointing to a real missing layer:

```text
signal -> semantic interpretation -> commitment -> world
```

And yes, the “semantic interpretation” layer can be partly embodied in personal AIs that learn your intent over time.

The only hard rule is:

```text
learned intent may propose meaning
but committed world meaning must become explicit
```

That preserves replay, calibration, and public form.
