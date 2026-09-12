# How we got here

This account follows the questions and changes that led from the rendering work
to the Inland build. It is a selective explanation of that path, not a complete
project history or a new set of requirements. The [vision summary](carry-on.md)
and [primary vision log](../vision/LOG.md) describe the larger aim. The
[Inland guide](builds/inland/README.md) and source maps describe the implementation.

## Rendering made a further question concrete

The rendering work developed ways to describe, query, and draw text, paths,
images, and 3D material. That raised a question beyond whether the renderers
worked: what must remain in the compiled foundation, and what can people and
agents construct as editable material above it?

The September 3–4 exploration worked backward from desired capabilities to the
foundation they would require. Its proposed boundary was called the waist.
Those arguments and their challenges explain what the work was trying to make
possible; they do not establish that every proposed primitive was necessary or
that the current engine is the final boundary.

Read the [ceiling and waist exploration](../history/docs/below-the-waist/ceiling-and-waist.md)
and the [visual argument](../history/docs/below-the-waist/waist-argument.md) for
that reasoning. Use the [client map](../src/app/client/README.md) for the code
that exists now.

## Organizing material and keeping it reactive did not settle authorship

The following experiments tried ECS-shaped workspaces, reactive computation,
and Smalltalk-inspired tools. They helped make different parts of the problem
visible: how material is organized, how a change reaches its readers, and how an
inhabitant reaches the definition of something they are using.

The [September 11 comparison](../history/docs/below-the-waist/softland-in-softland-2026-09-11/FACTS.md)
preserves those distinctions and the limits of its own evidence. Some earlier
builds were compared through their write-ups; the narrower Smalltalk build also
received source inspection. Those forms of evidence should remain distinguishable.

In the earlier Smalltalk experiment, an editable targeting expression could
change which object a later gesture selected. The comparison also found that
the available expression vocabulary was small and the surrounding buttons,
dispatch, and interaction consequences still lived in compiled code. That made
the next question sharper: how much of the tool's construction could itself
become material that someone could inspect, change, and reuse?

## The next build widened what could be authored

The [records proposal](../history/docs/below-the-waist/softland-in-softland-2026-09-11/PROPOSAL.md)
argued for making the layer between storage and rendering editable. The
[Inland intended design](builds/inland/intended-design.md) scoped a build to test
that direction with actual Electric, Rama, and Softland's renderers. Those are
records of a proposal and an authorized experiment; neither is proof that its
host or language is the permanent choice for Softland.

The resulting build uses addressed definitions for targeting, presentation,
editor behavior, and repeated work. A kept instrument can share another
instrument's named definition or refer to a separately kept definition.
Candidate contexts and pinning expose which definitions are being used. These
are more useful tests of construction than changing a setting alone, because
one authored thing participates in the construction of another.

The [original build account](../history/docs/build-softland-in-softland/HANDOFF.md#what-the-build-revealed)
explains that compositional result and the compiled work it still required.
The [close record](../history/docs/build-softland-in-softland/NOW.md) reports
five focused tests with 72 assertions and seven browser/recovery checks,
including retrieval of kept instruments and an accepted provider reply after
replacing the app and browser. Those are recorded results for the source
versions named there, not new measurements made by this account.

## The demonstrated construction still has a compiled foundation

The build account records compiled repairs for pending reads, scheduling,
native input, GPU ownership, and external-result handling. Its success does not
show that the smallest sufficient foundation has been found. Editing the
workbench's own material also does not establish a complete in-world loop for
changing, rebuilding, and recovering the compiler or renderer.

Current source maps document further limits separately from those earlier
receipts. Start at [Inland](../src-inland/README.md) and follow the runtime,
authored-material, or verification boundary needed for the question. The
retained question is how to extend useful authorship while keeping acceptance,
dependencies, resource ownership, and recovery understandable. Existing code
shows what that extension would encounter; the vision still determines what
we are trying to make possible.

## The working method was also an experiment

The parallel build-and-review work produced its own proposed workflow, later
written as a standing method under `meta/`. During the September 2026 cleanup,
Sid said it had not turned out particularly well and questioned treating it as
a method to use indefinitely. It is now preserved as a
[workflow experiment](../history/workflow-experiments/parallel-build-review/README.md),
with the original steps and observations available for examination.

A useful observation from one round can inform another without making the
whole procedure compulsory. The same applies to the technical history: consult
the relevant attempt, keep its reasoning and evidence limits visible, and
allow the next question to change the approach.
