# Dependency references

These collections describe dependencies or preserve copies of their source.
They are reference material, not Softland's design decisions or implementation
guides. Use [docs/](../docs/README.md) for those project explanations. Each
collection keeps its owner's terms, not Softland's licence; see
[THIRD-PARTY.md](../THIRD-PARTY.md).

| Collection | Kind and provenance | Recorded version or date |
|---|---|---|
| Rama docs, kept outside git | Local documentation extraction. Numbered pages identify their Red Planet Labs source URLs; the collection README also contains a local reading guide. It lives in the local research depot, `/mnt/data/projects/research/rama/docs/`; `reference/rama/docs` is a gitignored local link to it, so citations of `reference/rama/docs/...` resolve on this machine. Taken out of the public repo on 2026-11-02 (Sid's word): Rama's licence agreement counts its documentation as part of the Software and forbids copying or distributing it without Red Planet Labs' authorization. Earlier commits still hold it. | The collection records generation on 2026-03-22. Checked on 2026-10-12: its words are the words of the docs while Rama 1.6.0, which the store runs, was the newest release; its Maven example names 1.5.0. The collection README, "Which version this copy is", gives what that rests on. |
| Rama release notes, kept outside git | What each Rama release changed. The docs site keeps one version of a page; this is the only text that is by release. It lives at `/mnt/data/projects/research/rama/release-notes.md`, with a gitignored local link at `reference/rama/release-notes.md`. Taken out of the public repo on 2026-11-02 with the docs: the `rama-release-notes` repository carries no licence. | Red Planet Labs' `rama-release-notes`, its `README.md` at commit `76677562` of 2026-07-06, fetched 2026-10-12 and unchanged below a four-line header. |
| Electric tutorial, kept outside git | Collected tutorial text, including DOM-oriented examples. At `reference/electric/docs/tutorial.txt` through the local link described below the table. | Capture date and exact version are not recorded in its header. |
| Electric source files, kept outside git | Selected source files, including `electric3.cljc`, DOM integration, and incseq. At `reference/electric/source/` through the local link. | Capture date and exact source revision are not recorded in the file headers inspected for this index. |
| Electric codebase snapshot, kept outside git | A flattened source tree with its own directory listing. It is a separate collection from the selected source files. At `reference/electric/source/codebase-snapshot.txt` through the local link. | Capture date and exact revision are not recorded in its header; do not assume the two collections are the same version. |
| [Missionary collected reference](missionary/docs/collected-reference.txt) | A collected README, tutorials, and API reference, as labelled by the file. | The collection labels itself b.46; its capture date is not recorded in the header. |

The three Electric collections live in the local research depot,
`/mnt/data/projects/research/electric/`; `reference/electric` is a gitignored
local link to it, so citations of `reference/electric/...` resolve on this
machine. Taken out of the public repo on 2026-11-03 (Sid's word): Hyperfiddle's
repository has no licence file, and its README says Electric v3 "is free for
bootstrappers and non-commercial use, but is otherwise a commercial project".
Earlier commits still hold them.

[deps.edn](../deps.edn) records the project's dependency pins. A local reference's
presence does not establish that it matches the resolved dependency. Follow the
[Electric/Missionary guidance](../.agents/skills/electric-docs/SKILL.md) or
[Rama guidance](../.agents/skills/rama/SKILL.md) for source lookup and relevant
checks. No dependency was upgraded or fetched as part of this reorganization.

Softland targets its own rendering system. Electric's DOM examples explain that
integration; whether and how other Electric capabilities serve Softland remains
a project implementation question, with its scope recorded in the build guides
and intended decisions.
