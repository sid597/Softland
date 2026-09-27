;; The repository's files at this branch's commit read into the base by the
;; operator, through the citation session's reader. Until the citation branch is
;; merged (its full suite first, as it asked), its rig.material is loaded from its
;; own folder, read only: the namespaces it needs (rig.revision,
;; rig.store.envelope) are identical in both branches.
;;   clojure -J-Xss16m -M:inland src/proposal/pointer-screen-2026-09-27/tools/read_material.clj
(require '[clojure.java.shell :as sh] '[clojure.string :as str])
(when-not (find-ns 'rig.material)
  (try (require 'rig.material)
       (catch Throwable _
         (load-file "/Users/sid/projects/Softland-citation/src/proposal/rig-2026-09-25/src/rig/material.clj"))))
(require '[softland.inland.material :as mat] '[softland.inland.store :as store])

(def repo "/Users/sid/projects/Softland-pointer-screen")
(def rev (str/trim (:out (sh/sh "git" "rev-parse" "HEAD" :dir repo))))
(def paths ["src-inland/softland/inland/store.clj"
            "src-inland/softland/inland/reader.clj"
            "src-inland/softland/inland/gesture.clj"
            "src/proposal/rig-2026-09-25/src/rig/store/bell.clj"
            "src/proposal/rig-2026-09-25/src/rig/store/standing.clj"
            "src-inland/softland/inland/README.md"
            "src/proposal/pointer-screen-2026-09-27/PROGRESS.md"
            "resources/inland/README.md"])

(store/connect!)
(println "reading" (count paths) "files at" rev)
(doseq [a (mat/read! (:store @store/connection) repo rev paths)]
  (println (select-keys a [:answer :reason :path :record :file])))
(shutdown-agents)
(System/exit 0)
