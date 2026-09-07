# Feedback on the supplied working-model text — not the session-2 review

2026-09-06. This reviewed the supplied `3d-kind-working-model.md` text. The reviewer incorrectly offered it as one of the requested session reviews before establishing the session mapping. Sid clarified that session 2 is commit `a85fd1e`, `3d-kind-2` and `bench-2`; its actual review is [feedback-a85fd1e.md](feedback-a85fd1e.md). This file must not be sent as feedback for session 2. It remains only a record of feedback on the separately supplied text.

Your strongest move is separating source, construction, representation, occurrence, scene and view. That prevents an evaluated render scene from silently becoming the authoring model. Separating presentation, attachment and derivation likewise gives the two directions of the 2D/3D seam useful meanings. Preserve those distinctions.

The hardest case is worked convincingly at the conceptual level: gesture attachment, material interpretation, stationary hover under an occluder, portals with different camera relationships, and correspondence after topology changes all remain visible. Your argument for one computational waist rests on substantial shared requirements while allowing different geometry libraries. That is a defensible position under the requirement that their inputs and results remain composable.

The unfinished work is concentrated in your own final question: the executable extension interface. At present, “typed spatial values,” “supported operations,” “executor” and “geometry libraries” still permit very different engines. They name responsibilities without yet showing exactly what an above-waist author can supply. The next step should make one such definition concrete, without turning this exploration into a full contract.

Use one implicit or sampled object that the engine does not recognize as a hardcoded kind. Show its saved definition, which executable operations it supplies, which existing kernels they call, and the values passed between construction, presentation and query. Specify what an editable hit addresses when the representation has no native named face. A surface point or field sample is not automatically a unique editable cause.

Carry one parameter edit through the same example. Identify the changed authored address, affected bulk data or derived values, operation invalidations and resulting hit changes. Your model correctly permits large rendering consequences; it still owes a concrete account of what makes the data changes fine-grained. Keep intermediate execution versions separate from user-visible answer changes.

This would also make the library test tangible: the construction runs without a window, its result feeds another construction, and two views reuse it without acquiring separate authoring truth. State what must remain native for this example and why; do not let a general evaluator's existence stand in for numerical capability or throughput.

The next question is the one you already identified: what must an above-waist representation definition provide for the existing engine to construct, present and return an editable hit? One complete input-to-output example will advance that question more than another inventory of capabilities.
