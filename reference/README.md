# Dependency references

These collections describe dependencies or preserve copies of their source.
They are reference material, not Softland's design decisions or implementation
guides. Use [docs/](../docs/README.md) for those project explanations.

| Collection | Kind and provenance | Recorded version or date |
|---|---|---|
| [Rama docs](rama/docs/README.md) | Local documentation extraction. Numbered pages identify their Red Planet Labs source URLs; the collection README also contains a local reading guide. | The collection records generation on 2026-03-22. Checked on 2026-10-12: its words are the words of the docs while Rama 1.6.0, which the store runs, was the newest release; its Maven example names 1.5.0. The collection README, "Which version this copy is", gives what that rests on. |
| [Rama release notes](rama/release-notes.md) | What each Rama release changed. The docs site keeps one version of a page; this is the only text that is by release. | Red Planet Labs' `rama-release-notes`, its `README.md` at commit `76677562` of 2026-07-06, fetched 2026-10-12 and unchanged below a four-line header. |
| [Electric tutorial](electric/docs/tutorial.txt) | Collected tutorial text, including DOM-oriented examples. | Capture date and exact version are not recorded in its header. |
| [Electric source files](electric/source/) | Selected source files, including `electric3.cljc`, DOM integration, and incseq. | Capture date and exact source revision are not recorded in the file headers inspected for this index. |
| [Electric codebase snapshot](electric/source/codebase-snapshot.txt) | A flattened source tree with its own directory listing. It is a separate collection from the selected source files. | Capture date and exact revision are not recorded in its header; do not assume the two collections are the same version. |
| [Missionary collected reference](missionary/docs/collected-reference.txt) | A collected README, tutorials, and API reference, as labelled by the file. | The collection labels itself b.46; its capture date is not recorded in the header. |

[deps.edn](../deps.edn) records the project's dependency pins. A local reference's
presence does not establish that it matches the resolved dependency. Follow the
[Electric/Missionary guidance](../.agents/skills/electric-docs/SKILL.md) or
[Rama guidance](../.agents/skills/rama/SKILL.md) for source lookup and relevant
checks. No dependency was upgraded or fetched as part of this reorganization.

Softland targets its own rendering system. Electric's DOM examples explain that
integration; whether and how other Electric capabilities serve Softland remains
a project implementation question, with its scope recorded in the build guides
and intended decisions.
