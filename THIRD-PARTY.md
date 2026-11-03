# Third-party material

Softland's own code is under the Eclipse Public License 2.0 ([LICENSE](LICENSE)).
Nothing listed here is covered by that license: each item keeps its owner's
terms. The licenses below were read on 2026-11-02 from each package's metadata,
its jar, its source repository, or the file itself.

## In this repository

| Material | Where | Owner | Terms | Licence text |
|---|---|---|---|---|
| JetBrains Mono Regular 2.304 | `resources/public/JetBrainsMono-Regular.ttf` | The JetBrains Mono Project Authors | SIL Open Font License 1.1 | [JetBrainsMono-OFL.txt](resources/public/JetBrainsMono-OFL.txt) |
| DejaVu Sans Mono | `resources/public/fonts/dejavu_sans_mono.ttf` | Bitstream, Inc. (Vera); Tavmjong Bah (Arev glyphs); DejaVu changes are public domain | Bitstream Vera and Arev font licences | [dejavu-LICENSE.txt](resources/public/fonts/dejavu-LICENSE.txt) |
| Noto Sans Regular 2.004 | `resources/public/fonts/noto_sans_regular.ttf` | Google LLC; The Noto Project Authors | SIL Open Font License 1.1 | [noto_sans-OFL.txt](resources/public/fonts/noto_sans-OFL.txt) |
| Ubuntu Sans 1.006 (variable) | `resources/public/fonts/ubuntu_sans_variable.ttf` | Canonical Ltd. | Ubuntu Font Licence 1.0 | [ubuntu_sans-LICENCE.txt](resources/public/fonts/ubuntu_sans-LICENCE.txt) |
| Missionary collected reference | `reference/missionary/` | Léo Noel | EPL-2.0 | upstream repository |

## Kept outside this repository

Rama's documentation extraction, Red Planet Labs' release notes, and copies of
Electric's tutorial and source are used locally and not published. Rama's
licence agreement counts its documentation as part of the Software and says
"you may not use, copy, or distribute the Software without Red Planet Labs's
authorization"; the `rama-release-notes` repository carries no licence; and
Hyperfiddle's Electric repository has no licence file, its README saying
Electric v3 "is free for bootstrappers and non-commercial use, but is
otherwise a commercial project". See [reference/README.md](reference/README.md).

## Dependencies

Fetched by the build tools, not stored in this repository. Each brings its own
dependencies under their own terms. Aliases are named where a dependency is
not in the top-level `:deps` of [deps.edn](deps.edn).

| Dependency | Version | Licence |
|---|---|---|
| `org.clojure/clojure` | 1.12.4 | EPL-1.0 |
| `org.clojure/clojurescript` | 1.11.132 | EPL-1.0 |
| `org.clojure/data.json`, `data.xml`, `tools.logging` | 2.4.0, 0.2.0-alpha8, 1.2.4 | EPL-1.0 |
| `missionary/missionary` | b.46 | EPL-2.0 |
| `com.rpl/rama` | 1.6.0 | Proprietary: Red Planet Labs' licence agreement (`META-INF/RAMA-LICENSE.txt` in the jar) |
| `com.rpl/rama-helpers` | 0.10.0 | Apache-2.0 |
| `com.rpl/specter` | 1.1.4 | Apache-2.0 |
| `com.hyperfiddle/electric` | v3-alpha-20260519.115706-45 (`:inland`) | Hyperfiddle's terms: free for bootstrappers and non-commercial use, otherwise commercial |
| `com.roamresearch/backend-sdk` | 0.0.4 | No licence found in its package or in a public repository |
| `org.babashka/sci` | 0.8.43 | EPL-1.0 |
| `clj-kondo/clj-kondo` | 2025.06.05 | EPL-1.0 |
| `ring/ring` | 1.11.0 | MIT |
| `clj-http/clj-http` | 3.12.3 | MIT |
| `rewrite-clj/rewrite-clj` | 1.1.47 | MIT |
| `com.google.guava/guava` | 33.1.0-jre | Apache-2.0 |
| `ch.qos.logback/logback-classic` | 1.2.11 | EPL-1.0 or LGPL-2.1 |
| `org.apache.logging.log4j/log4j-slf4j2-impl` | 2.23.1 | Apache-2.0 |
| `thheller/shadow-cljs` | 2.28.23 (`:inland`, `:dev`) | EPL (version not stated in its package metadata) |
| `io.github.clojure/tools.build` | 0.9.6 (`:dev`, `:build`) | EPL-1.0 |
| `nrepl/nrepl`, `cider/cider-nrepl` | 1.0.0, 0.42.1 (`:nrepl`) | EPL (version not stated in their package metadata) |

npm ([package.json](package.json)): `bidi-js` MIT, `harfbuzzjs` MIT,
`shadow-cljs` ISC; development only: `karma`, `karma-chrome-launcher`,
`karma-cljs-test` MIT, `puppeteer` Apache-2.0.

## The deployable jar

The [Dockerfile](Dockerfile) builds `app.jar` from the top-level dependencies,
which include Rama and the Roam backend SDK. That jar runs Softland's own
instance. It is not published, because Rama's licence does not allow
distributing it.
