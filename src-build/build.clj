(ns build
  (:require [build.slug-font :as slug-font]
            [clojure.edn :as edn]
            [clojure.tools.build.api :as b]
            [clojure.tools.logging :as log]))

(def build-version
  (b/git-process {:git-args "describe --tags --long --always --dirty"}))

(def class-dir "target/classes")

(defn uberjar
  [{:keys [::jar-name] :as args}]
  (log/info `uberjar (pr-str args))
  (b/delete {:path "target"})
  (b/copy-dir {:target-dir class-dir :src-dirs ["src" "src-prod" "resources"]})
  (let [jar-name (or (some-> jar-name str)
                     (format "target/softland-%s.jar" build-version))
        aliases [:prod]]
    (log/info `uberjar "included aliases:" aliases)
    (b/uber {:class-dir class-dir
             :uber-file jar-name
             :basis (b/create-basis {:project "deps.edn" :aliases aliases})})
    (log/info jar-name)))

(defn module-jar
  "Slim source jar for Rama deployment; excludes the cluster-provided Rama
   implementation and the server environment namespace."
  [_argmap]
  (b/delete {:path "target/land-modules"})
  (b/delete {:path "target/land-modules.jar"})
  (b/copy-dir {:target-dir "target/land-modules/classes" :src-dirs ["src"]
               :ignores [#"env\.clj"]})
  (let [deps (-> (slurp "deps.edn")
                 edn/read-string
                 (update :deps dissoc 'com.rpl/rama)
                 (update-in [:deps 'com.rpl/rama-helpers]
                            assoc :exclusions ['com.rpl/rama]))]
    (b/uber {:class-dir "target/land-modules/classes"
             :uber-file "target/land-modules.jar"
             :basis (b/create-basis {:project deps})})
    (log/info "module jar: target/land-modules.jar")))

(defn build-slug-font
  "Generate Slug assets for the default DejaVu Sans Mono font bundle."
  [_argmap]
  (let [result (slug-font/write-font-assets! (slug-font/default-config))]
    (log/info "Slug font assets generated:" (pr-str result))
    result))

;; clj -X:build:prod uberjar :build/jar-name "app.jar"
;; java -cp app.jar clojure.main -m prod
