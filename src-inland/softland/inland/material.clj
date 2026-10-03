(ns softland.inland.material
  "The repository's files at a revision read into the base, by the operator
  (placeholder 21: no tool can run in the base on the rig as built, since a
  tool's lease is sealed under the layer's person owner and the base's owner
  is the root actor). Takes the repository's path, a revision and paths; gives
  one act per file, each the citation session's reading of it
  (`rig.material/reading-step`, called, never copied), then the `material`
  record that lists the files read. The reading's facts name their own act, so
  the act's name is made first."
  (:require [clojure.string :as str]
            [rig.material :as material]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.micro-client :as mc]
            [softland.inland.facts :as f]))

(def repo-id "The repository's id in material facts (placeholder)." "softland")
(def line "The line of work the files are read on (placeholder)." :pointer-screen)

(defn- label-of [path] (last (str/split path #"/")))

(defn read-file!
  "One file at revision `rev` read into the base as one operator act: the
  reading, its text, one fact per named form. A first reading only: it names
  no previous reading. The act's answer, with the file's thing."
  [store repo-path rev path]
  (let [class (mc/layer-class store :base)
        run (env/make-name :base class)
        file (material/file-thing repo-id line path)
        r (material/reading-step repo-path repo-id {:request {:repo repo-id :line line :rev rev :path path}
                                                    :file file :prev [] :run run :same :name-in-file})]
    (if (:error r)
      {:answer :no :reason (:error r) :path path}
      (assoc (mc/offer-into! store (c/build {:name run :who :operator :layer :base :class class :facts (:facts r)}))
             :file file :path path))))

(defn read!
  "The files at `rev` read into the base, then the `material` record listing
  them: `{:name \"material\" :commit :short :names [record names] :paths
  {name path} :labels {name file name}}`. Returns every answer."
  [store repo-path rev paths]
  (let [read (mapv #(read-file! store repo-path rev %) paths)
        ok (filter #(= :yes (:answer %)) read)
        names (mapv #(f/record-name (:file %)) ok)
        record {:name "material" :commit rev :short (subs rev 0 8) :names names
                :paths (zipmap names (map :path ok)) :labels (zipmap names (map (comp label-of :path) ok))}
        listing (mc/offer-into! store (c/build {:who :operator :layer :base :class (mc/layer-class store :base)
                                                :facts (f/record->facts record)}))]
    (conj read (assoc listing :record "material"))))
