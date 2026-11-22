# Auth verification

The checks of Auth, the module that holds sign-in. Their source is
[Auth's spec](../src/proposal/runtime-2026-11-21/spine/auth/SPEC.md).

| Immediate child | Responsibility |
|---|---|
| [softland/auth/](softland/auth/README.md) | The reference of Auth's contract in plain Clojure, the proof that holds it to the spec, the run that holds a module against it, and the questions of the spec. |

```sh
clojure -M:auth -m softland.auth.reference-proof
clojure -M:auth -m softland.auth.module-check
```

The alias `:auth` of `deps.edn` puts `src-auth` and `test-auth` on the classpath,
with Clojure and Rama. The reference, its proof, and the run held against the
reference itself need nothing beyond Clojure: no cluster, and no library passed
with `-Sdeps`. Held against a module, the run needs what the module needs.
